# -*- coding: UTF-8 -*-
"""
检测服务：整合YOLO检测和千问分析
"""
import logging
import base64
import io
import numpy as np
from PIL import Image
import cv2

logger = logging.getLogger(__name__)


class DetectionService:
    def __init__(self, yolo_detector, qwen_analyzer):
        """
        初始化检测服务

        Args:
            yolo_detector: YOLO检测器实例
            qwen_analyzer: 千问分析器实例
        """
        self.yolo = yolo_detector
        self.qwen = qwen_analyzer
        logger.info("检测服务初始化完成")

    def _decode_base64_image(self, image_base64):
        """
        解码Base64图像

        Args:
            image_base64: Base64编码的图像字符串

        Returns:
            numpy.ndarray: OpenCV格式的图像数组
        """
        try:
            # 解码Base64
            image_data = base64.b64decode(image_base64)

            # 转换为PIL Image
            pil_image = Image.open(io.BytesIO(image_data))

            # 转换为numpy数组
            image_array = np.array(pil_image)

            # 如果是RGB，转换为BGR（OpenCV格式）
            if len(image_array.shape) == 3 and image_array.shape[2] == 3:
                image_array = cv2.cvtColor(image_array, cv2.COLOR_RGB2BGR)

            return image_array

        except Exception as e:
            logger.error(f"Base64图像解码失败: {e}")
            raise ValueError(f"图像解码失败: {str(e)}")

    def detect_and_analyze(self, image_base64, conf_threshold=0.5, fine_threshold=0.6):
        """
        双层检测：YOLO粗筛 + 千问精筛

        Args:
            image_base64: Base64编码的图像
            conf_threshold: YOLO检测置信度阈值
            fine_threshold: 精细检测置信度阈值

        Returns:
            list: 检测和分析结果列表
        """
        try:
            # 1. 解码图像
            image = self._decode_base64_image(image_base64)
            logger.info(f"图像解码成功，尺寸: {image.shape}")

            # 2. YOLO检测人物
            detections = self.yolo.detect(image, conf_threshold=conf_threshold)
            logger.info(f"YOLO检测到 {len(detections)} 个人物")

            # 3. 对每个检测到的人物进行千问分析
            results = []
            for idx, detection in enumerate(detections):
                if detection['confidence'] > fine_threshold:
                    try:
                        # 提取ROI区域
                        x1, y1, x2, y2 = detection['bbox']
                        x1, y1, x2, y2 = int(x1), int(y1), int(x2), int(y2)

                        # 确保坐标在图像范围内
                        h, w = image.shape[:2]
                        x1 = max(0, min(x1, w))
                        y1 = max(0, min(y1, h))
                        x2 = max(0, min(x2, w))
                        y2 = max(0, min(y2, h))

                        # 检查ROI是否有效
                        if x2 <= x1 or y2 <= y1:
                            logger.warning(f"无效的ROI坐标: [{x1}, {y1}, {x2}, {y2}]")
                            continue

                        roi = image[y1:y2, x1:x2]

                        # 检查ROI尺寸
                        if roi.size == 0:
                            logger.warning(f"ROI为空，跳过分析")
                            continue

                        logger.info(f"正在分析第 {idx+1} 个检测对象，ROI尺寸: {roi.shape}")

                        # 千问分析行为
                        analysis = self.qwen.analyze_behavior(roi, detection)

                        results.append({
                            'detection': detection,
                            'analysis': analysis
                        })

                        logger.info(f"第 {idx+1} 个对象分析完成")

                    except Exception as e:
                        logger.error(f"分析第 {idx+1} 个检测对象时出错: {e}")
                        # 继续处理下一个检测对象
                        continue
                else:
                    logger.debug(f"检测置信度 {detection['confidence']:.2f} 低于阈值 {fine_threshold}，跳过精细分析")

            logger.info(f"完成分析，共 {len(results)} 个结果")
            return results

        except Exception as e:
            logger.error(f"检测和分析过程出错: {e}")
            raise
