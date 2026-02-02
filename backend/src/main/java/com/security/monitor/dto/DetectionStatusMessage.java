package com.security.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检测状态消息 - 用于WebSocket推送
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectionStatusMessage {

    /**
     * 消息类型: "alert" - 预警, "normal" - 无异常
     */
    private String type;

    /**
     * 视频ID
     */
    private Long videoId;

    /**
     * 检测周期内分析的帧数
     */
    private Integer analyzedFrames;

    /**
     * 消息内容（无异常时显示的文本）
     */
    private String message;

    /**
     * 预警数据（type为alert时有值）
     */
    private Object alertData;

    public static DetectionStatusMessage normal(Long videoId, int analyzedFrames) {
        DetectionStatusMessage msg = new DetectionStatusMessage();
        msg.setType("normal");
        msg.setVideoId(videoId);
        msg.setAnalyzedFrames(analyzedFrames);
        msg.setMessage("检测周期内画面无异常");
        return msg;
    }

    public static DetectionStatusMessage alert(Long videoId, Object alertData) {
        DetectionStatusMessage msg = new DetectionStatusMessage();
        msg.setType("alert");
        msg.setVideoId(videoId);
        msg.setAlertData(alertData);
        return msg;
    }
}
