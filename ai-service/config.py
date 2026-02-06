import os


class Config:
    # 应用版本
    VERSION = '1.0.0.0'

    # 千问API密钥（从环境变量读取）
    DASHSCOPE_API_KEY = os.environ.get('DASHSCOPE_API_KEY', '')

    # YOLO模型路径
    YOLO_MODEL_PATH = 'yolo26n.pt'

    # Flask配置
    HOST = '0.0.0.0'
    PORT = 5001  # 改为5001避免与AirPlay冲突
    DEBUG = False

    # 检测配置
    YOLO_CONF_THRESHOLD = 0.5  # YOLO置信度阈值
    FINE_DETECTION_THRESHOLD = 0.6  # 精细检测置信度阈值

    # 后端服务URL
    BACKEND_URL = os.environ.get('BACKEND_URL', 'http://localhost:8080/api')

    # 训练样本基础路径
    SAMPLES_BASE_PATH = os.environ.get(
        'SAMPLES_BASE_PATH',
        os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'backend', 'storage', 'training-samples')
    )

    # 训练配置
    TRAINING_EPOCHS = 10
    TRAINING_BATCH_SIZE = 16
    TRAINING_IMG_SIZE = 640
