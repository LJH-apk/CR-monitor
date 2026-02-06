package com.security.monitor.util;

import com.security.monitor.dto.AIAlert;
import com.security.monitor.dto.AIDetectionResult;
import com.security.monitor.entity.DangerBehavior;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预警相关的共享工具方法
 */
public final class AlertUtil {

    private AlertUtil() {}

    /**
     * 关键词到危险行为英文名的映射
     */
    public static final Map<String, String> KEYWORD_TO_BEHAVIOR_NAME;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("打架", "Fighting");
        map.put("斗殴", "Fighting");
        map.put("争执", "Fighting");
        map.put("推搡", "Fighting");
        map.put("拉扯", "Fighting");
        map.put("冲突", "Fighting");
        map.put("摔倒", "Falling");
        map.put("晕倒", "Falling");
        map.put("非法进入", "Intrusion");
        map.put("翻越", "Intrusion");
        map.put("吸烟", "Smoking");
        map.put("烟火", "Fire");
        map.put("明火", "Fire");
        KEYWORD_TO_BEHAVIOR_NAME = Collections.unmodifiableMap(map);
    }

    /**
     * 根据关键词映射构建 keyword -> DangerBehavior 的映射
     */
    public static Map<String, DangerBehavior> buildKeywordBehaviorMap(List<DangerBehavior> behaviors) {
        Map<String, DangerBehavior> result = new HashMap<>();
        for (DangerBehavior behavior : behaviors) {
            for (Map.Entry<String, String> entry : KEYWORD_TO_BEHAVIOR_NAME.entrySet()) {
                if (behavior.getName().equalsIgnoreCase(entry.getValue())) {
                    result.put(entry.getKey(), behavior);
                }
            }
        }
        return result;
    }

    /**
     * 映射严重等级（返回Integer：1=LOW, 2=MEDIUM, 3=HIGH）
     */
    public static Integer mapSeverity(String aiSeverity) {
        switch (aiSeverity.toLowerCase()) {
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 2;
        }
    }

    /**
     * 构建预警描述
     */
    public static String buildDescription(AIDetectionResult result, AIAlert aiAlert) {
        StringBuilder desc = new StringBuilder();
        desc.append("检测到异常行为: ").append(aiAlert.getKeyword());

        if (result.getAnalysis() != null && result.getAnalysis().getAnalysis() != null) {
            String analysis = result.getAnalysis().getAnalysis();
            if (analysis.length() > 100) {
                analysis = analysis.substring(0, 100) + "...";
            }
            desc.append("\n分析: ").append(analysis);
        }

        desc.append("\n类别: ").append(aiAlert.getCategory());
        desc.append("\n严重等级: ").append(aiAlert.getSeverity());
        desc.append(String.format("\n置信度: %.2f%%", aiAlert.getCombinedScore() * 100));

        return desc.toString();
    }
}
