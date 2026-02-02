# -*- coding: UTF-8 -*-
"""
YOLO增量训练器 - 使用用户上传的样本进行增量学习
"""
import os
import json
import shutil
import logging
import threading
import requests
from datetime import datetime
from pathlib import Path
from ultralytics import YOLO

logger = logging.getLogger(__name__)


class YOLOTrainer:
    """YOLO模型增量训练器"""

    def __init__(self, model_manager, base_model_path='yolo11n.pt',
                 training_dir='training_workspace', backend_url='http://localhost:8080/api'):
        self.model_manager = model_manager
        self.base_model_path = base_model_path
        self.training_dir = Path(training_dir)
        self.backend_url = backend_url

        # 创建训练工作目录
        self.training_dir.mkdir(parents=True, exist_ok=True)
        (self.training_dir / 'datasets').mkdir(exist_ok=True)
        (self.training_dir / 'runs').mkdir(exist_ok=True)

        # 训练状态
        self._training_lock = threading.Lock()
        self._is_training = False
        self._training_progress = 0
        self._training_status = 'idle'
        self._current_task_id = None

        # 训练完成回调
        self._on_training_complete_callbacks = []

    def get_training_status(self):
        """获取训练状态"""
        return {
            'is_training': self._is_training,
            'progress': self._training_progress,
            'status': self._training_status,
            'task_id': self._current_task_id
        }

    def fetch_approved_samples(self, admin_token):
        """从后端获取已批准的样本（COCO格式）"""
        try:
            headers = {'Authorization': f'Bearer {admin_token}'}
            response = requests.get(
                f'{self.backend_url}/samples/export/coco',
                headers=headers,
                timeout=30
            )

            if response.status_code == 200:
                return response.json()
            else:
                logger.error(f"获取样本失败: {response.status_code}")
                return None

        except Exception as e:
            logger.error(f"获取样本异常: {e}")
            return None

    def prepare_yolo_dataset(self, coco_data, samples_base_path):
        """
        将COCO格式数据转换为YOLO格式

        Args:
            coco_data: COCO格式的数据
            samples_base_path: 样本图片的基础路径

        Returns:
            str: 数据集配置文件路径
        """
        dataset_dir = self.training_dir / 'datasets' / f'dataset_{datetime.now().strftime("%Y%m%d_%H%M%S")}'
        images_dir = dataset_dir / 'images' / 'train'
        labels_dir = dataset_dir / 'labels' / 'train'

        images_dir.mkdir(parents=True, exist_ok=True)
        labels_dir.mkdir(parents=True, exist_ok=True)

        # 构建类别映射
        categories = {cat['id']: idx for idx, cat in enumerate(coco_data.get('categories', []))}
        category_names = [cat['name'] for cat in sorted(coco_data.get('categories', []), key=lambda x: x['id'])]

        # 构建图片ID到标注的映射
        image_annotations = {}
        for ann in coco_data.get('annotations', []):
            img_id = ann['imageId']
            if img_id not in image_annotations:
                image_annotations[img_id] = []
            image_annotations[img_id].append(ann)

        # 处理每张图片
        processed_count = 0
        for image_info in coco_data.get('images', []):
            img_id = image_info['id']
            file_name = image_info['fileName']
            width = image_info['width']
            height = image_info['height']

            # 复制图片
            src_path = Path(samples_base_path) / file_name
            if not src_path.exists():
                # 尝试从filePath获取
                file_path = image_info.get('filePath', '')
                if file_path and os.path.exists(file_path):
                    src_path = Path(file_path)
                else:
                    logger.warning(f"图片不存在: {src_path}")
                    continue

            dst_image_path = images_dir / f'{img_id}.jpg'
            shutil.copy2(src_path, dst_image_path)

            # 生成YOLO格式标注
            annotations = image_annotations.get(img_id, [])
            if annotations:
                label_path = labels_dir / f'{img_id}.txt'
                with open(label_path, 'w') as f:
                    for ann in annotations:
                        bbox = ann['bbox']  # [x, y, width, height]
                        cat_id = ann['categoryId']

                        # 转换为YOLO格式 (class_id, x_center, y_center, width, height) - 归一化
                        x_center = (bbox[0] + bbox[2] / 2) / width
                        y_center = (bbox[1] + bbox[3] / 2) / height
                        w = bbox[2] / width
                        h = bbox[3] / height

                        class_idx = categories.get(cat_id, 0)
                        f.write(f'{class_idx} {x_center:.6f} {y_center:.6f} {w:.6f} {h:.6f}\n')

            processed_count += 1

        # 创建数据集配置文件
        data_yaml_path = dataset_dir / 'data.yaml'
        data_config = {
            'path': str(dataset_dir),
            'train': 'images/train',
            'val': 'images/train',  # 增量训练时使用相同数据
            'names': {i: name for i, name in enumerate(category_names)}
        }

        with open(data_yaml_path, 'w', encoding='utf-8') as f:
            import yaml
            yaml.dump(data_config, f, allow_unicode=True)

        logger.info(f"数据集准备完成: {processed_count} 张图片, {len(category_names)} 个类别")
        return str(data_yaml_path)

    def start_training(self, admin_token, samples_base_path, epochs=10, batch_size=16,
                       auto_swap=True, yolo_detector=None):
        """
        开始增量训练

        Args:
            admin_token: 管理员token，用于获取样本
            samples_base_path: 样本图片基础路径
            epochs: 训练轮数
            batch_size: 批次大小
            auto_swap: 训练完成后是否自动热替换
            yolo_detector: YOLODetector实例（auto_swap=True时需要）

        Returns:
            str: 任务ID
        """
        if self._is_training:
            logger.warning("已有训练任务在进行中")
            return None

        # 生成任务ID
        task_id = f'train_{datetime.now().strftime("%Y%m%d_%H%M%S")}'

        # 在后台线程中执行训练
        thread = threading.Thread(
            target=self._training_worker,
            args=(task_id, admin_token, samples_base_path, epochs, batch_size, auto_swap, yolo_detector)
        )
        thread.daemon = True
        thread.start()

        return task_id

    def _training_worker(self, task_id, admin_token, samples_base_path, epochs, batch_size,
                         auto_swap, yolo_detector):
        """训练工作线程"""
        with self._training_lock:
            self._is_training = True
            self._training_progress = 0
            self._training_status = 'preparing'
            self._current_task_id = task_id

        try:
            # 1. 获取样本数据
            logger.info(f"[{task_id}] 获取训练样本...")
            self._training_status = 'fetching_samples'
            coco_data = self.fetch_approved_samples(admin_token)

            if not coco_data or not coco_data.get('images'):
                raise ValueError("没有可用的训练样本")

            sample_count = len(coco_data.get('images', []))
            logger.info(f"[{task_id}] 获取到 {sample_count} 个样本")

            # 2. 准备数据集
            self._training_status = 'preparing_dataset'
            self._training_progress = 10
            data_yaml = self.prepare_yolo_dataset(coco_data, samples_base_path)

            # 3. 加载基础模型
            self._training_status = 'loading_model'
            self._training_progress = 20
            logger.info(f"[{task_id}] 加载基础模型: {self.base_model_path}")
            model = YOLO(self.base_model_path)

            # 4. 开始训练
            self._training_status = 'training'
            logger.info(f"[{task_id}] 开始增量训练: epochs={epochs}, batch={batch_size}")

            # 训练回调
            def on_train_epoch_end(trainer):
                current_epoch = trainer.epoch + 1
                progress = 20 + int((current_epoch / epochs) * 60)
                self._training_progress = min(progress, 80)
                logger.info(f"[{task_id}] 训练进度: {current_epoch}/{epochs} epochs")

            # 执行训练
            results = model.train(
                data=data_yaml,
                epochs=epochs,
                batch=batch_size,
                imgsz=640,
                project=str(self.training_dir / 'runs'),
                name=task_id,
                exist_ok=True,
                verbose=True,
                patience=5,  # 早停
                save=True,
                plots=False
            )

            self._training_progress = 85

            # 5. 获取训练结果
            best_model_path = self.training_dir / 'runs' / task_id / 'weights' / 'best.pt'
            if not best_model_path.exists():
                best_model_path = self.training_dir / 'runs' / task_id / 'weights' / 'last.pt'

            if not best_model_path.exists():
                raise FileNotFoundError("训练后的模型文件不存在")

            # 提取训练指标
            metrics = {}
            if hasattr(results, 'results_dict'):
                metrics = {
                    'mAP50': float(results.results_dict.get('metrics/mAP50(B)', 0)),
                    'mAP50-95': float(results.results_dict.get('metrics/mAP50-95(B)', 0)),
                    'precision': float(results.results_dict.get('metrics/precision(B)', 0)),
                    'recall': float(results.results_dict.get('metrics/recall(B)', 0))
                }

            # 6. 注册新版本
            self._training_status = 'registering'
            self._training_progress = 90
            new_version = self.model_manager.register_new_version(
                model_path=str(best_model_path),
                name=f'增量训练 {datetime.now().strftime("%Y-%m-%d %H:%M")}',
                description=f'基于 {sample_count} 个样本的增量训练',
                metrics=metrics,
                training_samples=sample_count
            )

            logger.info(f"[{task_id}] 新模型版本注册成功: {new_version}")

            # 7. 自动热替换
            if auto_swap and yolo_detector and new_version:
                self._training_status = 'swapping'
                self._training_progress = 95
                logger.info(f"[{task_id}] 执行模型热替换...")
                swap_success = self.model_manager.hot_swap_model(new_version, yolo_detector)
                if swap_success:
                    logger.info(f"[{task_id}] 模型热替换成功")
                else:
                    logger.warning(f"[{task_id}] 模型热替换失败")

            # 8. 记录训练历史
            self.model_manager.add_training_record({
                'task_id': task_id,
                'version': new_version,
                'samples': sample_count,
                'epochs': epochs,
                'metrics': metrics,
                'auto_swapped': auto_swap
            })

            self._training_status = 'completed'
            self._training_progress = 100
            logger.info(f"[{task_id}] 训练任务完成")

            # 触发完成回调
            for callback in self._on_training_complete_callbacks:
                try:
                    callback(task_id, new_version, metrics)
                except Exception as e:
                    logger.error(f"训练完成回调执行失败: {e}")

        except Exception as e:
            logger.error(f"[{task_id}] 训练失败: {e}")
            self._training_status = f'failed: {str(e)}'

        finally:
            self._is_training = False

    def on_training_complete(self, callback):
        """注册训练完成回调"""
        self._on_training_complete_callbacks.append(callback)

    def cancel_training(self):
        """取消当前训练（注意：可能无法立即停止）"""
        if self._is_training:
            self._training_status = 'cancelling'
            logger.warning("训练取消请求已发送，但可能需要等待当前epoch完成")
            return True
        return False
