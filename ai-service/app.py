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
        'models': {
            'yolo': 'loaded',
            'qwen': 'connected'
        }
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
