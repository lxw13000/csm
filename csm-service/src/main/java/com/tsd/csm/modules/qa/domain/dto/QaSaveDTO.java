package com.tsd.csm.modules.qa.domain.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * QA 问答对新增/编辑入参。
 */
public class QaSaveDTO {

    /** 标准问题。 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 答案。 */
    @NotBlank(message = "答案不能为空")
    private String answer;

    /** 状态：1 启用 / 0 停用，默认启用。 */
    private Integer status;

    /** 关联关键词列表（用于智能问答命中匹配）。 */
    private List<String> keywords;

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
}
