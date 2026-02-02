# -*- coding: UTF-8 -*-
"""
AI检测微服务 - Flask应用入口
"""
from flask import Flask, request, jsonify
from flask_cors import CORS
import logging
import sys
import os

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from config import Config
from models.yolo_detector import YOLODetector
from models.qwen_analyzer import QwenAnalyzer
from services.detection_service import DetectionService
from services.alert_evaluator import AlertEvaluator
from training.model_manager import ModelManager
from training.trainer import YOLOTrainer

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 创建Flask应用
app = Flask(__name__)
CORS(app)  # 启用CORS

# 初始化AI模型和服务
logger.info("正在初始化AI模型...")
try:
    yolo_detector = YOLODetector(Config.YOLO_MODEL_PATH)
    qwen_analyzer = QwenAnalyzer(Config.DASHSCOPE_API_KEY)
    detection_service = DetectionService(yolo_detector, qwen_analyzer)
    alert_evaluator = AlertEvaluator()

    # 初始化模型管理器和训练器
    model_manager = ModelManager(
        models_dir='model_versions',
        current_model_name=Config.YOLO_MODEL_PATH
    )
    yolo_trainer = YOLOTrainer(
        model_manager=model_manager,
        base_model_path=Config.YOLO_MODEL_PATH,
        training_dir='training_workspace',
        backend_url=Config.BACKEND_URL
    )

    logger.info("AI模型初始化成功")
except Exception as e:
    logger.error(f"AI模型初始化失败: {e}")
    raise


@app.route('/api/health', methods=['GET'])
def health():
    """健康检查接口"""
    return jsonify({
        'status': 'healthy',
        'service': 'AI Detection Service',
        'version': Config.VERSION,
        'models': {
            'yolo': 'loaded',
            'qwen': 'connected'
        },
        'current_model_version': model_manager.get_current_version()
    }), 200


@app.route('/api/detect', methods=['POST'])
def detect():
    """
    检测接口

    请求体:
    {
        "image": "base64编码的图像字符串"
    }

    响应:
    {
        "code": 200,
        "message": "success",
        "data": [
            {
                "detection": {
                    "type": "person",
                    "bbox": [x1, y1, x2, y2],
                    "confidence": 0.85
                },
                "analysis": {
                    "analysis": "分析文本",
                    "success": true
                },
                "alerts": [
                    {
                        "category": "suspicious_behavior",
                        "keyword": "打架",
                        "severity": "high",
                        "combined_score": 0.85
                    }
                ]
            }
        ]
    }
    """
    try:
        # 获取请求数据
        data = request.get_json()
        if not data or 'image' not in data:
            return jsonify({
                'code': 400,
                'message': '缺少image参数',
                'data': None
            }), 400

        image_base64 = data.get('image')

        # 获取配置参数
        conf_threshold = data.get('conf_threshold', Config.YOLO_CONF_THRESHOLD)
        fine_threshold = data.get('fine_threshold', Config.FINE_DETECTION_THRESHOLD)

        logger.info(f"收到检测请求，置信度阈值: {conf_threshold}, 精细检测阈值: {fine_threshold}")

        # 执行检测和分析
        results = detection_service.detect_and_analyze(
            image_base64,
            conf_threshold=conf_threshold,
            fine_threshold=fine_threshold
        )

        # 评估预警
        for result in results:
            analysis_text = result['analysis'].get('analysis', '')
            confidence = result['detection']['confidence']

            # 生成预警
            alerts = alert_evaluator.evaluate(analysis_text, confidence)
            result['alerts'] = alerts

        logger.info(f"检测完成，返回 {len(results)} 个结果")

        return jsonify({
            'code': 200,
            'message': 'success',
            'data': results
        }), 200

    except ValueError as e:
        logger.error(f"参数错误: {e}")
        return jsonify({
            'code': 400,
            'message': str(e),
            'data': None
        }), 400

    except Exception as e:
        logger.error(f"检测失败: {e}", exc_info=True)
        return jsonify({
            'code': 500,
            'message': f'检测失败: {str(e)}',
            'data': None
        }), 500


@app.route('/api/config/rules', methods=['GET'])
def get_rules():
    """获取预警规则"""
    return jsonify({
        'code': 200,
        'data': {
            'warning_rules': AlertEvaluator.WARNING_RULES,
            'keyword_weights': AlertEvaluator.KEYWORD_WEIGHTS
        }
    }), 200


# ==================== 模型管理API ====================

@app.route('/api/models/versions', methods=['GET'])
def get_model_versions():
    """获取所有模型版本"""
    try:
        versions = model_manager.get_all_versions()
        current_version = model_manager.get_current_version()

        return jsonify({
            'code': 200,
            'message': 'success',
            'data': {
                'current_version': current_version,
                'versions': versions
            }
        }), 200
    except Exception as e:
        logger.error(f"获取模型版本失败: {e}")
        return jsonify({
            'code': 500,
            'message': str(e),
            'data': None
        }), 500


@app.route('/api/models/current', methods=['GET'])
def get_current_model():
    """获取当前模型信息"""
    try:
        current_version = model_manager.get_current_version()
        version_info = model_manager.get_version_info(current_version)

        return jsonify({
            'code': 200,
            'message': 'success',
            'data': {
                'version': current_version,
                'info': version_info
            }
        }), 200
    except Exception as e:
        logger.error(f"获取当前模型信息失败: {e}")
        return jsonify({
            'code': 500,
            'message': str(e),
            'data': None
        }), 500


@app.route('/api/models/swap', methods=['POST'])
def swap_model():
    """热替换模型"""
    try:
        data = request.get_json()
        version = data.get('version')

        if not version:
            return jsonify({
                'code': 400,
                'message': '缺少version参数',
                'data': None
            }), 400

        success = model_manager.hot_swap_model(version, yolo_detector)

        if success:
            return jsonify({
                'code': 200,
                'message': f'模型已切换到版本 {version}',
                'data': {
                    'version': version,
                    'info': model_manager.get_version_info(version)
                }
            }), 200
        else:
            return jsonify({
                'code': 500,
                'message': '模型切换失败',
                'data': None
            }), 500

    except Exception as e:
        logger.error(f"模型切换失败: {e}")
        return jsonify({
            'code': 500,
            'message': str(e),
            'data': None
        }), 500


@app.route('/api/models/rollback', methods=['POST'])
def rollback_model():
    """回滚模型"""
    try:
        data = request.get_json() or {}
        target_version = data.get('version')  # 可选，不指定则回滚到上一版本

        success = model_manager.rollback_model(yolo_detector, target_version)

        if success:
            current_version = model_manager.get_current_version()
            return jsonify({
                'code': 200,
                'message': f'模型已回滚到版本 {current_version}',
                'data': {
                    'version': current_version,
                    'info': model_manager.get_version_info(current_version)
                }
            }), 200
        else:
            return jsonify({
                'code': 500,
                'message': '模型回滚失败',
                'data': None
            }), 500

    except Exception as e:
        logger.error(f"模型回滚失败: {e}")
        return jsonify({
            'code': 500,
            'message': str(e),
            'data': None
        }), 500


# ==================== 训练API ====================

@app.route('/api/training/start', methods=['POST'])
def start_training():
    """开始增量训练"""
    try:
        data = request.get_json()

        # 获取管理员token（从请求头或请求体）
        admin_token = request.headers.get('Authorization', '').replace('Bearer ', '')
        if not admin_token:
            admin_token = data.get('admin_token')

        if not admin_token:
            return jsonify({
                'code': 401,
                'message': '需要管理员认证',
                'data': None
            }), 401

        # 获取训练参数
        samples_base_path = data.get('samples_base_path', Config.SAMPLES_BASE_PATH)
        epochs = data.get('epochs', 10)
        batch_size = data.get('batch_size', 16)
        auto_swap = data.get('auto_swap', True)

        # 开始训练
        task_id = yolo_trainer.start_training(
            admin_token=admin_token,
            samples_base_path=samples_base_path,
            epochs=epochs,
            batch_size=batch_size,
            auto_swap=auto_swap,
            yolo_detector=yolo_detector if auto_swap else None
        )

        if task_id:
            return jsonify({
                'code': 200,
                'message': '训练任务已启动',
                'data': {
                    'task_id': task_id,
                    'epochs': epochs,
                    'batch_size': batch_size,
                    'auto_swap': auto_swap
                }
            }), 200
        else:
            return jsonify({
                'code': 409,
                'message': '已有训练任务在进行中',
                'data': None
            }), 409

    except Exception as e:
        logger.error(f"启动训练失败: {e}")
        return jsonify({
            'code': 500,
            'message': str(e),
            'data': None
        }), 500


@app.route('/api/training/status', methods=['GET'])
def get_training_status():
    """获取训练状态"""
    try:
        status = yolo_trainer.get_training_status()
        return jsonify({
            'code': 200,
            'message': 'success',
            'data': status
        }), 200
    except Exception as e:
        logger.error(f"获取训练状态失败: {e}")
        return jsonify({
            'code': 500,
            'message': str(e),
            'data': None
        }), 500


@app.route('/api/training/cancel', methods=['POST'])
def cancel_training():
    """取消训练"""
    try:
        success = yolo_trainer.cancel_training()
        if success:
            return jsonify({
                'code': 200,
                'message': '训练取消请求已发送',
                'data': None
            }), 200
        else:
            return jsonify({
                'code': 400,
                'message': '没有正在进行的训练任务',
                'data': None
            }), 400
    except Exception as e:
        logger.error(f"取消训练失败: {e}")
        return jsonify({
            'code': 500,
            'message': str(e),
            'data': None
        }), 500


@app.route('/api/training/history', methods=['GET'])
def get_training_history():
    """获取训练历史"""
    try:
        history = model_manager.get_training_history()
        return jsonify({
            'code': 200,
            'message': 'success',
            'data': history
        }), 200
    except Exception as e:
        logger.error(f"获取训练历史失败: {e}")
        return jsonify({
            'code': 500,
            'message': str(e),
            'data': None
        }), 500


@app.errorhandler(404)
def not_found(error):
    return jsonify({
        'code': 404,
        'message': '接口不存在',
        'data': None
    }), 404


@app.errorhandler(500)
def internal_error(error):
    return jsonify({
        'code': 500,
        'message': '服务器内部错误',
        'data': None
    }), 500


if __name__ == '__main__':
    logger.info(f"启动AI检测服务，监听 {Config.HOST}:{Config.PORT}")
    app.run(
        host=Config.HOST,
        port=Config.PORT,
        debug=Config.DEBUG
    )
