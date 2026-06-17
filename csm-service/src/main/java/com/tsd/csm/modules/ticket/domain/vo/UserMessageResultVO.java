package com.tsd.csm.modules.ticket.domain.vo;

/**
 * 用户发送消息的处理结果：落库的用户消息 + 当前工单 + 智能问答机器人回复（命中时）。
 */
public class UserMessageResultVO {

    /** 当前工单。 */
    private TicketVO ticket;
    /** 落库的用户消息。 */
    private MessageVO message;
    /** 智能问答机器人回复（命中 QA 时有值）。 */
    private MessageVO botReply;
    /** 是否已进入人工转接（QA 未命中或主动转人工）。 */
    private boolean transferred;

    public TicketVO getTicket() {
        return ticket;
    }

    public void setTicket(TicketVO ticket) {
        this.ticket = ticket;
    }

    public MessageVO getMessage() {
        return message;
    }

    public void setMessage(MessageVO message) {
        this.message = message;
    }

    public MessageVO getBotReply() {
        return botReply;
    }

    public void setBotReply(MessageVO botReply) {
        this.botReply = botReply;
    }

    public boolean isTransferred() {
        return transferred;
    }

    public void setTransferred(boolean transferred) {
        this.transferred = transferred;
    }
}
