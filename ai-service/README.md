# AI检测微服务

基于YOLO11n和千问VL的智能异常检测服务

## 功能特性

- **双层AI检测**：YOLO11n快速人物检测 + 千问VL深度行为分析
- **智能预警**：基于关键词匹配和权重评分的多级预警系统
- **RESTful API**：简单易用的HTTP接口
- **高性能**：异步处理，支持并发请求

## 环境要求

- Python 3.10+
- 4GB+ RAM（推荐8GB）
- 千问API密钥

## 安装步骤

### 1. 创建虚拟环境

```bash
cd /Users/liujiahang/Page/ai-service
python3 -m venv venv
source venv/bin/activate  # macOS/Linux
# 或 venv\Scripts\activate  # Windows
```

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

### 3. 配置API密钥

编辑 `config.py`，设置千问API密钥：

```python
DASHSCOPE_API_KEY = 'your-api-key-here'
```

### 4. 启动服务

```bash
python app.py
```

服务将在 `http://localhost:5000` 启动

## API接口

### 1. 健康检查

```bash
GET /api/health
```

响应：
```json
{
  "status": "healthy",
  "service": "AI Detection Service",
  "models": {
    "yolo": "loaded",
    "qwen": "connected"
  }
}
```

### 2. 图像检测

```bash
POST /api/detect
Content-Type: application/json

{
  "image": "base64编码的图像字符串",
  "conf_threshold": 0.5,  // 可选，YOLO置信度阈值
  "fine_threshold": 0.6   // 可选，精细检测阈值
}
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "detection": {
        "type": "person",
        "bbox": [100, 200, 300, 500],
        "confidence": 0.85,
        "timestamp": "2026-01-27T19:30:00"
      },
      "analysis": {
        "analysis": "检测到两人发生肢体冲突，建议立即处理",
        "success": true,
        "timestamp": "2026-01-27T19:30:02"
      },
      "alerts": [
        {
          "category": "suspicious_behavior",
          "keyword": "打架",
          "severity": "high",
          "combined_score": 0.765,
          "detection_confidence": 0.85,
          "keyword_weight": 0.9
        }
      ]
    }
  ]
}
```

### 3. 获取预警规则

```bash
GET /api/config/rules
```

## 预警类别

- **suspicious_behavior**（可疑行为）：打架、斗殴、追逐、攀爬、翻越
- **safety_hazard**（安全隐患）：烟火、吸烟、明火、漏电、摔倒、晕倒
- **security_risk**（安全风险）：可疑包裹、破坏设备、非法进入
- **emergency**（紧急情况）：急救、晕厥、突发疾病、事故

## 严重等级

- **high**（高）：需要立即处理
- **medium**（中）：需要关注
- **low**（低）：一般情况

## 测试

### 使用curl测试

```bash
# 健康检查
curl http://localhost:5000/api/health

# 图像检测（需要准备base64编码的图像）
curl -X POST http://localhost:5000/api/detect \
  -H "Content-Type: application/json" \
  -d '{"image":"<base64-encoded-image>"}'
```

### 使用Python测试

```python
import requests
import base64

# 读取图像并编码
with open('test.jpg', 'rb') as f:
    image_base64 = base64.b64encode(f.read()).decode()

# 调用API
response = requests.post(
    'http://localhost:5000/api/detect',
    json={'image': image_base64}
)

print(response.json())
```

## 性能指标

- **YOLO检测速度**：~50ms/帧（CPU）
- **千问API响应**：~1-2秒/次
- **总体处理时间**：~2-3秒/图像
- **内存占用**：~2GB

## 故障排除

### YOLO模型加载失败

确保 `yolo11n.pt` 文件存在于项目根目录。如果不存在，模型会自动下载。

### 千问API调用失败

1. 检查API密钥是否正确
2. 确认网络连接正常
3. 查看API调用配额是否充足

### 内存不足

降低并发请求数量，或增加服务器内存。

## 日志

日志输出到控制台，包含：
- 模型加载状态
- 检测请求信息
- 分析结果
- 错误信息

## 许可证

MIT License
