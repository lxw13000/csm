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
    public Message saveMessage(Long ticketId, int senderType, String senderId,
                               Integer contentType, String content, String clientMsgId) {
        Message duplicate = findByClientMsgId(ticketId, clientMsgId);
        if (duplicate != null) {
            return duplicate;
        }

        Message previous = latestMessage(ticketId);
        Message message = new Message();
        message.setTicketId(ticketId);
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

    public List<Message> history(Long ticketId, Long afterSeq) {
        return lambdaQuery()
                .eq(Message::getTicketId, ticketId)
                .gt(afterSeq != null, Message::getSeq, afterSeq)
                .orderByAsc(Message::getSeq)
                .list();
    }

    /** 已读高水位推进：每个 (工单, 阅读方) 仅一行，仅在更大序号时更新。 */
    public void markRead(Long ticketId, int readerType, String readerId, long seq) {
        MessageRead record = messageReadMapper.selectOne(new LambdaQueryWrapper<MessageRead>()
                .eq(MessageRead::getTicketId, ticketId)
                .eq(MessageRead::getReaderType, readerType)
                .eq(MessageRead::getReaderId, readerId));
        if (record == null) {
            record = new MessageRead();
            record.setTicketId(ticketId);
            record.setReaderType(readerType);
            record.setReaderId(readerId);
            record.setLastReadSeq(seq);
            record.setLastReadAt(LocalDateTime.now());
            messageReadMapper.insert(record);
        } else if (seq > record.getLastReadSeq()) {
            record.setLastReadSeq(seq);
            record.setLastReadAt(LocalDateTime.now());
            messageReadMapper.updateById(record);
        }
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

    private long watermark(Long ticketId, int readerType, String readerId) {
        MessageRead record = messageReadMapper.selectOne(new LambdaQueryWrapper<MessageRead>()
                .eq(MessageRead::getTicketId, ticketId)
                .eq(MessageRead::getReaderType, readerType)
                .eq(MessageRead::getReaderId, readerId));
        return record == null || record.getLastReadSeq() == null ? 0L : record.getLastReadSeq();
    }

    private Message findByClientMsgId(Long ticketId, String clientMsgId) {
        if (clientMsgId == null || clientMsgId.isBlank()) {
            return null;
        }
        return lambdaQuery()
                .eq(Message::getTicketId, ticketId)
                .eq(Message::getClientMsgId, clientMsgId)
                .one();
    }

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
