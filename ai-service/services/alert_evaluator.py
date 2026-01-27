# -*- coding: UTF-8 -*-
"""
预警评估器：基于关键词匹配和权重计算的预警生成
"""
import logging

logger = logging.getLogger(__name__)


class AlertEvaluator:
    """预警评估器"""

    # 预警规则：类别 -> 关键词列表
    WARNING_RULES = {
        "suspicious_behavior": ["打架", "斗殴", "追逐", "攀爬", "翻越", "争执", "推搡", "拉扯", "冲突"],
        "safety_hazard": ["烟火", "吸烟", "明火", "漏电", "摔倒", "晕倒"],
        "security_risk": ["可疑包裹", "破坏设备", "非法进入"],
        "emergency": ["急救", "晕厥", "突发疾病", "事故"]
    }

    # 关键词权重：类别 -> {关键词: 权重}
    KEYWORD_WEIGHTS = {
        "suspicious_behavior": {
            "打架": 0.9, "斗殴": 0.9, "追逐": 0.6,
            "攀爬": 0.7, "翻越": 0.7, "争执": 0.8,
            "推搡": 0.8, "拉扯": 0.8, "冲突": 0.8
        },
        "safety_hazard": {
            "烟火": 0.8, "吸烟": 0.7, "明火": 0.9,
            "漏电": 0.9, "摔倒": 0.7, "晕倒": 0.8
        },
        "security_risk": {
            "可疑包裹": 0.9, "破坏设备": 0.8, "非法进入": 0.9
        },
        "emergency": {
            "急救": 0.9, "晕厥": 0.8, "突发疾病": 0.8, "事故": 0.9
        }
    }

    # 高风险关键词（自动设为高严重等级）
    HIGH_RISK_KEYWORDS = [
        "打架", "斗殴", "明火", "漏电", "事故", "急救", "晕厥", "冲突"
    ]

    # 紧急指示词（出现在分析文本中会提升严重等级）
    EMERGENCY_INDICATORS = [
        "紧急", "危险", "立即", "马上", "救命", "报警", "火灾", "爆炸"
    ]

    def evaluate(self, analysis_text, detection_confidence):
        """
        评估分析结果，生成预警

        Args:
            analysis_text: 千问分析的文本结果
            detection_confidence: YOLO检测的置信度

        Returns:
            list: 预警列表，每个预警包含类别、关键词、严重等级、综合评分
        """
        if not analysis_text:
            return []

        alerts = []
        analysis_lower = analysis_text.lower()

        # 遍历所有预警规则
        for category, keywords in self.WARNING_RULES.items():
            for keyword in keywords:
                if keyword in analysis_text:
                    # 计算综合评分
                    keyword_weight = self.KEYWORD_WEIGHTS[category].get(keyword, 0.5)
                    combined_score = detection_confidence * keyword_weight

                    # 计算严重等级
                    severity = self._calculate_severity(
                        category, keyword, combined_score, analysis_text
                    )

                    alert = {
                        'category': category,
                        'keyword': keyword,
                        'severity': severity,
                        'combined_score': float(combined_score),
                        'detection_confidence': float(detection_confidence),
                        'keyword_weight': float(keyword_weight)
                    }

                    alerts.append(alert)
                    logger.info(f"生成预警: {keyword} (类别: {category}, 严重等级: {severity}, 评分: {combined_score:.2f})")

        return alerts

    def _calculate_severity(self, category, keyword, combined_score, analysis_text):
        """
        根据综合评分和上下文计算预警等级

        Args:
            category: 预警类别
            keyword: 触发关键词
            combined_score: 综合评分
            analysis_text: 分析文本

        Returns:
            str: 严重等级 (high/medium/low)
        """
        # 1. 高风险关键词直接返回高等级
        if keyword in self.HIGH_RISK_KEYWORDS:
            return "high"

        # 2. 检查紧急指示词
        if any(indicator in analysis_text for indicator in self.EMERGENCY_INDICATORS):
            return "high"

        # 3. 基于类别的基础等级
        base_severity_map = {
            "emergency": "high",
            "safety_hazard": "high",
            "security_risk": "medium",
            "suspicious_behavior": "low"
        }

        # 4. 根据综合评分调整等级
        if combined_score >= 0.7:
            return "high"
        elif combined_score >= 0.5:
            return "medium"
        elif combined_score >= 0.3:
            return "low"
        else:
            # 评分太低，使用基础等级
            return base_severity_map.get(category, "low")

    def get_category_description(self, category):
        """获取类别的中文描述"""
        descriptions = {
            "suspicious_behavior": "可疑行为",
            "safety_hazard": "安全隐患",
            "security_risk": "安全风险",
            "emergency": "紧急情况"
        }
        return descriptions.get(category, category)

    def get_severity_description(self, severity):
        """获取严重等级的中文描述"""
        descriptions = {
            "high": "高",
            "medium": "中",
            "low": "低"
        }
        return descriptions.get(severity, severity)
