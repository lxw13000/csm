package com.tsd.csm.modules.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tsd.csm.core.common.constant.CsmConst;
import com.tsd.csm.core.common.enums.ReaderType;
import com.tsd.csm.core.common.enums.SenderType;
import com.tsd.csm.modules.ticket.domain.Message;
import com.tsd.csm.modules.ticket.domain.MessageRead;
import com.tsd.csm.modules.ticket.domain.Ticket;
import com.tsd.csm.modules.ticket.domain.vo.MessageVO;
import com.tsd.csm.modules.ticket.mapper.MessageMapper;
import com.tsd.csm.modules.ticket.mapper.MessageReadMapper;
import com.tsd.csm.modules.ticket.mapper.TicketMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话消息服务：序号生成（Redis INCR）、客户端去重、响应耗时计算、已读高水位与未读数。
 */
@Service
public class MessageService extends ServiceImpl<MessageMapper, Message> {

    private final MessageReadMapper messageReadMapper;
    private final TicketMapper ticketMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public MessageService(MessageReadMapper messageReadMapper, TicketMapper ticketMapper,
                          StringRedisTemplate stringRedisTemplate) {
        this.messageReadMapper = messageReadMapper;
        this.ticketMapper = ticketMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 落库一条消息：按 client_msg_id 幂等去重，分配会话内递增 seq，
     * 客服消息且上一条为用户消息时记录响应耗时，并刷新工单首条/最后消息时间。
     */
    @Transactional(rollbackFor = Exception.class)
    public Message saveMessage(Long ticketId, String userId, int senderType, String senderId,
                               Integer contentType, String content, String clientMsgId) {
        Message duplicate = findByClientMsgId(ticketId, clientMsgId);
        if (duplicate != null) {
            return duplicate;
        }

        Message previous = latestMessage(ticketId);
        Message message = new Message();
        message.setTicketId(ticketId);
        message.setUserId(userId);
        message.setClientMsgId(clientMsgId);
        message.setSeq(nextSeq(ticketId));
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setContentType(contentType == null ? 1 : contentType);
        message.setContent(content);
        if (senderType == SenderType.AGENT.getCode() && previous != null
                && previous.getSenderType() != null
                && previous.getSenderType() == SenderType.USER.getCode()
                && previous.getCreatedAt() != null) {
            long seconds = Duration.between(previous.getCreatedAt(), LocalDateTime.now()).getSeconds();
            message.setResponseCost((int) Math.max(0, seconds));
        }
        try {
            save(message);
        } catch (DuplicateKeyException e) {
            // 并发同 client_msg_id：回查已落库的那条
            Message existing = findByClientMsgId(ticketId, clientMsgId);
            if (existing != null) {
                return existing;
            }
            throw e;
        }
        touchTicket(ticketId);
        return message;
    }

    /**
     * 拉取单个工单消息（按 seq 升序），供管理端查看某工单聊天记录。
     * @param ticketId 工单 id
     * @param afterSeq 增量游标：仅返回 seq 大于它的消息，null 表示全量
     * @return 消息列表
     */
    public List<Message> history(Long ticketId, Long afterSeq) {
        return lambdaQuery()
                .eq(Message::getTicketId, ticketId)
                .gt(afterSeq != null, Message::getSeq, afterSeq)
                .orderByAsc(Message::getSeq)
                .list();
    }

    /**
     * 拉取某 C 端用户的全量历史消息（跨工单，按主键 id 即时间升序），用于断线增量恢复。
     * @param userId 业务系统用户 id
     * @param afterId 增量游标：仅返回 id 大于它的消息，null 表示全量
     * @return 按 id 升序的消息列表
     */
    public List<Message> userHistory(String userId, Long afterId) {
        return lambdaQuery()
                .eq(Message::getUserId, userId)
                .gt(afterId != null, Message::getId, afterId)
                .orderByAsc(Message::getId)
                .list();
    }

    /**
     * 拉取某 C 端用户最近 limit 条历史消息（跨工单，按 id 倒序取再正序返回），
     * 用于实时聊天初次加载与向上滚动加载更早历史。
     * @param userId 业务系统用户 id
     * @param beforeId 向上翻页游标：仅取 id 小于它的消息，null 表示取最新
     * @param limit 返回条数上限（1~100）
     * @return 按 id 升序排列的消息列表
     */
    public List<Message> userHistoryBefore(String userId, Long beforeId, int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        List<Message> desc = lambdaQuery()
                .eq(Message::getUserId, userId)
                .lt(beforeId != null, Message::getId, beforeId)
                .orderByDesc(Message::getId)
                .last("limit " + size)
                .list();
        List<Message> ordered = new ArrayList<>(desc);
        Collections.reverse(ordered);
        return ordered;
    }

    /**
     * 已读高水位推进：每个 (工单, 阅读方) 仅一行，仅在更大序号时更新。
     * 并发首次上报可能同时插入，捕获唯一键冲突后转为推进高水位（幂等）。
     */
    public void markRead(Long ticketId, int readerType, String readerId, long seq) {
        MessageRead record = findRead(ticketId, readerType, readerId);
        if (record != null) {
            bumpWatermark(record, seq);
            return;
        }
        MessageRead created = new MessageRead();
        created.setTicketId(ticketId);
        created.setReaderType(readerType);
        created.setReaderId(readerId);
        created.setLastReadSeq(seq);
        created.setLastReadAt(LocalDateTime.now());
        try {
            messageReadMapper.insert(created);
        } catch (DuplicateKeyException e) {
            // 并发已插入同一 (工单,阅读方)：回查后推进高水位
            bumpWatermark(findRead(ticketId, readerType, readerId), seq);
        }
    }

    private MessageRead findRead(Long ticketId, int readerType, String readerId) {
        return messageReadMapper.selectOne(new LambdaQueryWrapper<MessageRead>()
                .eq(MessageRead::getTicketId, ticketId)
                .eq(MessageRead::getReaderType, readerType)
                .eq(MessageRead::getReaderId, readerId));
    }

    private void bumpWatermark(MessageRead record, long seq) {
        if (record == null || (record.getLastReadSeq() != null && seq <= record.getLastReadSeq())) {
            return;
        }
        record.setLastReadSeq(seq);
        record.setLastReadAt(LocalDateTime.now());
        messageReadMapper.updateById(record);
    }

    /** 某阅读方在某工单的未读数 = 对端发送且 seq 大于已读水位的消息数。 */
    public long unreadCount(Long ticketId, int readerType, String readerId) {
        long watermark = watermark(ticketId, readerType, readerId);
        int ownSenderType = readerType == ReaderType.USER.getCode()
                ? SenderType.USER.getCode() : SenderType.AGENT.getCode();
        return lambdaQuery()
                .eq(Message::getTicketId, ticketId)
                .gt(Message::getSeq, watermark)
                .ne(Message::getSenderType, ownSenderType)
                .count();
    }

    /**
     * 消息实体转展示 VO。
     * @param message 消息实体
     * @return 消息 VO
     */
    public MessageVO toVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setTicketId(message.getTicketId());
        vo.setSeq(message.getSeq());
        vo.setSenderType(message.getSenderType());
        vo.setSenderId(message.getSenderId());
        vo.setContentType(message.getContentType());
        vo.setContent(message.getContent());
        vo.setResponseCost(message.getResponseCost());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    /** 取某阅读方在该工单的已读高水位 seq（无记录为 0）。 */
    private long watermark(Long ticketId, int readerType, String readerId) {
        MessageRead record = messageReadMapper.selectOne(new LambdaQueryWrapper<MessageRead>()
                .eq(MessageRead::getTicketId, ticketId)
                .eq(MessageRead::getReaderType, readerType)
                .eq(MessageRead::getReaderId, readerId));
        return record == null || record.getLastReadSeq() == null ? 0L : record.getLastReadSeq();
    }

    /** 按 client_msg_id 查已落库消息（幂等去重用），为空或未找到返回 null。 */
    private Message findByClientMsgId(Long ticketId, String clientMsgId) {
        if (clientMsgId == null || clientMsgId.isBlank()) {
            return null;
        }
        return lambdaQuery()
                .eq(Message::getTicketId, ticketId)
                .eq(Message::getClientMsgId, clientMsgId)
                .one();
    }

    /** 取工单内 seq 最大的消息（最后一条），无则返回 null。 */
    private Message latestMessage(Long ticketId) {
        return lambdaQuery()
                .eq(Message::getTicketId, ticketId)
                .orderByDesc(Message::getSeq)
                .last("limit 1")
                .one();
    }

    /** 会话内递增序号：Redis INCR，key 首次出现时以库内当前最大 seq 初始化。 */
    private long nextSeq(Long ticketId) {
        String key = CsmConst.REDIS_TICKET_SEQ_PREFIX + ticketId;
        Boolean exists = stringRedisTemplate.hasKey(key);
        if (!Boolean.TRUE.equals(exists)) {
            Message latest = latestMessage(ticketId);
            long base = latest == null || latest.getSeq() == null ? 0L : latest.getSeq();
            stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(base));
        }
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        return seq == null ? 1L : seq;
    }

    /** 刷新工单的首条消息时间（仅首次）与最后消息时间。 */
    private void touchTicket(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (ticket.getFirstMsgAt() == null) {
            ticket.setFirstMsgAt(now);
        }
        ticket.setLastMsgAt(now);
        ticketMapper.updateById(ticket);
    }
}
