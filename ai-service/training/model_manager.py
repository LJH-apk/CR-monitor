# -*- coding: UTF-8 -*-
"""
模型管理器 - 负责模型版本管理和热替换
"""
import os
import shutil
import json
import logging
import threading
from datetime import datetime
from pathlib import Path

logger = logging.getLogger(__name__)


class ModelManager:
    """YOLO模型版本管理和热替换"""

    def __init__(self, models_dir='model_versions', current_model_name='yolo11n.pt'):
        self.models_dir = Path(models_dir)
        self.models_dir.mkdir(parents=True, exist_ok=True)

        self.current_model_name = current_model_name
        self.current_model_path = Path(current_model_name)
        self.metadata_file = self.models_dir / 'metadata.json'

        # 热替换锁
        self._swap_lock = threading.Lock()

        # 模型更新回调
        self._on_model_updated_callbacks = []

        # 初始化元数据
        self._init_metadata()

    def _init_metadata(self):
        """初始化模型元数据"""
        if not self.metadata_file.exists():
            metadata = {
                'current_version': 'v0',
                'versions': {
                    'v0': {
                        'name': 'baseline',
                        'description': '预训练基础模型',
                        'created_at': datetime.now().isoformat(),
                        'model_file': str(self.current_model_path),
                        'metrics': {},
                        'training_samples': 0,
                        'is_active': True
                    }
                },
                'training_history': []
            }
            self._save_metadata(metadata)

    def _load_metadata(self):
        """加载元数据"""
        try:
            with open(self.metadata_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"加载元数据失败: {e}")
            return None

    def _save_metadata(self, metadata):
        """保存元数据"""
        try:
            with open(self.metadata_file, 'w', encoding='utf-8') as f:
                json.dump(metadata, f, ensure_ascii=False, indent=2)
        except Exception as e:
            logger.error(f"保存元数据失败: {e}")

    def get_current_version(self):
        """获取当前模型版本"""
        metadata = self._load_metadata()
        if metadata:
            return metadata.get('current_version', 'v0')
        return 'v0'

    def get_all_versions(self):
        """获取所有模型版本"""
        metadata = self._load_metadata()
        if metadata:
            return metadata.get('versions', {})
        return {}

    def get_version_info(self, version):
        """获取指定版本信息"""
        versions = self.get_all_versions()
        return versions.get(version)

    def register_new_version(self, model_path, name, description, metrics=None, training_samples=0):
        """注册新模型版本"""
        metadata = self._load_metadata()
        if not metadata:
            return None

        # 生成新版本号
        versions = metadata.get('versions', {})
        version_nums = [int(v[1:]) for v in versions.keys() if v.startswith('v') and v[1:].isdigit()]
        new_version_num = max(version_nums) + 1 if version_nums else 1
        new_version = f'v{new_version_num}'

        # 复制模型文件到版本目录
        version_dir = self.models_dir / new_version
        version_dir.mkdir(parents=True, exist_ok=True)

        model_filename = f'model_{new_version}.pt'
        dest_path = version_dir / model_filename
        shutil.copy2(model_path, dest_path)

        # 注册版本信息
        versions[new_version] = {
            'name': name,
            'description': description,
            'created_at': datetime.now().isoformat(),
            'model_file': str(dest_path),
            'metrics': metrics or {},
            'training_samples': training_samples,
            'is_active': False
        }

        metadata['versions'] = versions
        self._save_metadata(metadata)

        logger.info(f"注册新模型版本: {new_version} - {name}")
        return new_version

    def hot_swap_model(self, version, yolo_detector):
        """
        热替换模型

        Args:
            version: 要切换到的版本
            yolo_detector: YOLODetector实例

        Returns:
            bool: 是否成功
        """
        with self._swap_lock:
            try:
                version_info = self.get_version_info(version)
                if not version_info:
                    logger.error(f"版本不存在: {version}")
                    return False

                model_file = version_info.get('model_file')
                if not model_file or not os.path.exists(model_file):
                    logger.error(f"模型文件不存在: {model_file}")
                    return False

                logger.info(f"开始热替换模型: {version}")

                # 备份当前模型路径
                old_model_path = yolo_detector.model_path

                # 加载新模型
                yolo_detector.model_path = model_file
                yolo_detector.load_model()

                # 更新元数据
                metadata = self._load_metadata()
                if metadata:
                    # 将旧版本设为非活跃
                    old_version = metadata.get('current_version')
                    if old_version and old_version in metadata['versions']:
                        metadata['versions'][old_version]['is_active'] = False

                    # 将新版本设为活跃
                    metadata['versions'][version]['is_active'] = True
                    metadata['current_version'] = version
                    self._save_metadata(metadata)

                logger.info(f"模型热替换成功: {old_model_path} -> {model_file}")

                # 触发回调
                for callback in self._on_model_updated_callbacks:
                    try:
                        callback(version, version_info)
                    except Exception as e:
                        logger.error(f"模型更新回调执行失败: {e}")

                return True

            except Exception as e:
                logger.error(f"模型热替换失败: {e}")
                return False

    def rollback_model(self, yolo_detector, target_version=None):
        """
        回滚模型到指定版本或上一个版本

        Args:
            yolo_detector: YOLODetector实例
            target_version: 目标版本，None则回滚到上一个版本
        """
        metadata = self._load_metadata()
        if not metadata:
            return False

        current_version = metadata.get('current_version', 'v0')

        if target_version is None:
            # 回滚到上一个版本
            versions = list(metadata.get('versions', {}).keys())
            versions.sort(key=lambda x: int(x[1:]) if x[1:].isdigit() else 0)

            current_idx = versions.index(current_version) if current_version in versions else -1
            if current_idx > 0:
                target_version = versions[current_idx - 1]
            else:
                logger.warning("已经是最早版本，无法回滚")
                return False

        return self.hot_swap_model(target_version, yolo_detector)

    def on_model_updated(self, callback):
        """注册模型更新回调"""
        self._on_model_updated_callbacks.append(callback)

    def get_training_history(self):
        """获取训练历史"""
        metadata = self._load_metadata()
        if metadata:
            return metadata.get('training_history', [])
        return []

    def add_training_record(self, record):
        """添加训练记录"""
        metadata = self._load_metadata()
        if metadata:
            history = metadata.get('training_history', [])
            history.append({
                **record,
                'timestamp': datetime.now().isoformat()
            })
            # 只保留最近100条记录
            metadata['training_history'] = history[-100:]
            self._save_metadata(metadata)

    def cleanup_old_versions(self, keep_count=5):
        """清理旧版本，保留最近的N个版本"""
        metadata = self._load_metadata()
        if not metadata:
            return

        versions = metadata.get('versions', {})
        current_version = metadata.get('current_version', 'v0')

        # 按版本号排序
        sorted_versions = sorted(
            versions.keys(),
            key=lambda x: int(x[1:]) if x[1:].isdigit() else 0,
            reverse=True
        )

        # 保留最近的版本和当前版本
        versions_to_keep = set(sorted_versions[:keep_count])
        versions_to_keep.add(current_version)
        versions_to_keep.add('v0')  # 始终保留基础版本

        # 删除旧版本
        for version in sorted_versions:
            if version not in versions_to_keep:
                version_info = versions.get(version)
                if version_info:
                    model_file = version_info.get('model_file')
                    if model_file and os.path.exists(model_file):
                        try:
                            os.remove(model_file)
                            # 删除版本目录
                            version_dir = self.models_dir / version
                            if version_dir.exists():
                                shutil.rmtree(version_dir)
                            logger.info(f"已清理旧版本: {version}")
                        except Exception as e:
                            logger.error(f"清理版本 {version} 失败: {e}")

                del versions[version]

        metadata['versions'] = versions
        self._save_metadata(metadata)
