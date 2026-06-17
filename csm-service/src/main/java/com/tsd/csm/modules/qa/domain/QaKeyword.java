package com.tsd.csm.modules.qa.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tsd.csm.core.mybatis.BaseEntity;

/**
 * QA 关键词关联。对应 csm_qa_keyword。
 */
@TableName("csm_qa_keyword")
public class QaKeyword extends BaseEntity {

    /** 所属租户。 */
    private String appId;
    /** QA 问答对 id。 */
    private Long qaId;
    /** 关联关键词。 */
    private String keyword;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Long getQaId() {
        return qaId;
    }

    public void setQaId(Long qaId) {
        this.qaId = qaId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
