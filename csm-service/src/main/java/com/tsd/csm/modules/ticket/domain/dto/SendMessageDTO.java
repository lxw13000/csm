package com.tsd.csm.modules.ticket.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 发送消息入参（H5 用户 / 客服回复共用）。{@code clientMsgId} 用于去重。
 */
public class SendMessageDTO {

    /** 消息内容（文本或媒体引用 URL）。 */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 内容类型：1 文本 / 2 图片 / 3 其他多媒体，默认文本。 */
    private Integer contentType;

    /** 客户端生成的唯一 id，用于去重。 */
    private String clientMsgId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public String getClientMsgId() {
        return clientMsgId;
    }

    public void setClientMsgId(String clientMsgId) {
        this.clientMsgId = clientMsgId;
    }
}
