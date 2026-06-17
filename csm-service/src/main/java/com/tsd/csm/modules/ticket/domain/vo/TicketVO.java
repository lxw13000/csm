package com.tsd.csm.modules.ticket.domain.vo;

import java.time.LocalDateTime;

/**
 * 工单展示 VO。{@code nickname/avatar} 来自 C 端用户缓存（客服会话列表展示用）；
 * {@code unreadCount} 为请求方未读数（按场景填充）。
 */
public class TicketVO {

    /** 工单 id。 */
    private Long id;
    /** 所属 C 端用户 id。 */
    private String userId;
    /** 用户昵称（来自缓存，客服会话列表展示用）。 */
    private String nickname;
    /** 用户头像（来自缓存）。 */
    private String avatar;
    /** 状态：1 智能问答 / 2 人工转接中 / 3 处理中 / 4 已完结。 */
    private Integer status;
    /** 完结方式：1 用户已解决 / 2 超时自动 / 3 客服强制。 */
    private Integer closeType;
    /** 当前处理客服账号 id。 */
    private Long agentId;
    /** 请求方未读消息数（按场景填充）。 */
    private Long unreadCount;
    /** 首条消息时间。 */
    private LocalDateTime firstMsgAt;
    /** 最后一条消息时间。 */
    private LocalDateTime lastMsgAt;
    /** 完结时间。 */
    private LocalDateTime closedAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getCloseType() {
        return closeType;
    }

    public void setCloseType(Integer closeType) {
        this.closeType = closeType;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public LocalDateTime getFirstMsgAt() {
        return firstMsgAt;
    }

    public void setFirstMsgAt(LocalDateTime firstMsgAt) {
        this.firstMsgAt = firstMsgAt;
    }

    public LocalDateTime getLastMsgAt() {
        return lastMsgAt;
    }

    public void setLastMsgAt(LocalDateTime lastMsgAt) {
        this.lastMsgAt = lastMsgAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
