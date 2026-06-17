package com.tsd.csm.core.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * 实体基类：自增主键。
 *
 * <p>时间字段不在此声明，因各表对 created_at/updated_at 的有无不一致
 * （如 csm_message 仅有 created_at、csm_agent_status 仅有 updated_at），
 * 需要二者的实体改继承 {@link TimedEntity}。
 */
public abstract class BaseEntity implements Serializable {

    /** 自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
