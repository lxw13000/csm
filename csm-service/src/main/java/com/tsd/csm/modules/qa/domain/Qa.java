package com.tsd.csm.modules.qa.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.TimedEntity;

/**
 * QA 问答对（按租户隔离）。对应 csm_qa。
 */
@TableName("csm_qa")
public class Qa extends TimedEntity {

    /** 所属租户，租户间完全隔离。 */
    private String appId;
    /** 标准问题。 */
    private String question;
    /** 答案。 */
    private String answer;
    /** 状态：1 启用 / 0 停用。 */
    private Integer status;
    /** 创建人账号 id。 */
    private Long createdBy;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
