package com.tsd.csm.modules.qa.domain.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QA 问答对展示 VO（含关联关键词）。
 */
public class QaVO {

    /** QA id。 */
    private Long id;
    /** 标准问题。 */
    private String question;
    /** 答案。 */
    private String answer;
    /** 状态：1 启用 / 0 停用。 */
    private Integer status;
    /** 关联关键词列表。 */
    private List<String> keywords;
    /** 创建时间。 */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
