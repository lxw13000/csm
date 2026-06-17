package com.tsd.csm.core.util;

import com.tsd.csm.core.common.exception.BizException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 基于 Redis 的轻量分布式锁（SET NX PX + Lua 校验 token 释放）。
 * 用于派单等需跨节点串行化的临界区。
 */
@Component
public class RedisLock {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean tryLock(String key, String token, long ttlMillis) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, Duration.ofMillis(ttlMillis));
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String key, String token) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
    }

    /** 获取锁（带自旋等待）后执行，结束自动释放；等待超时抛业务异常。 */
    public <T> T executeLocked(String key, long ttlMillis, long maxWaitMillis, Supplier<T> action) {
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + maxWaitMillis;
        boolean locked = false;
        try {
            while (!(locked = tryLock(key, token, ttlMillis))) {
                if (System.currentTimeMillis() > deadline) {
                    throw new BizException("系统繁忙，请稍后重试");
                }
                Thread.sleep(50);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("操作被中断");
        } finally {
            if (locked) {
                unlock(key, token);
            }
        }
    }
}
