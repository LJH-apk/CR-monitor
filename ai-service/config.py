class Config:
    # 千问API密钥（从Flask项目复制）
    DASHSCOPE_API_KEY = 'sk-6963e4b8bb5840de98983e63ed1ae012'

    # YOLO模型路径
    YOLO_MODEL_PATH = 'yolo11n.pt'

    # Flask配置
    HOST = '0.0.0.0'
    PORT = 5001  # 改为5001避免与AirPlay冲突
    DEBUG = False

    # 检测配置
    YOLO_CONF_THRESHOLD = 0.5  # YOLO置信度阈值
    FINE_DETECTION_THRESHOLD = 0.6  # 精细检测置信度阈值
