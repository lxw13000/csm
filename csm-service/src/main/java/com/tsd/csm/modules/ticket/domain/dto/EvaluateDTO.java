package com.tsd.csm.modules.ticket.domain.dto;

/**
 * 服务评价入参。
 */
public class EvaluateDTO {

    /** 是否已解决：1 已解决 / 0 未解决。 */
    private Integer resolved;

    /** 满意度评分 1-5。 */
    private Integer rating;

    /** 评价文字。 */
    private String remark;

    public Integer getResolved() {
        return resolved;
    }

    public void setResolved(Integer resolved) {
        this.resolved = resolved;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
