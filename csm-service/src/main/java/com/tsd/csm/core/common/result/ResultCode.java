package com.tsd.csm.core.common.result;

/**
 * 统一返回码。
 */
public enum ResultCode {

    /** 成功。 */
    SUCCESS(0, "成功"),
    /** 业务处理失败（默认业务错误）。 */
    BIZ_ERROR(1000, "业务处理失败"),
    /** 参数校验失败。 */
    PARAM_ERROR(1001, "参数校验失败"),
    /** 未登录或登录已过期。 */
    UNAUTHORIZED(401, "未登录或登录已过期"),
    /** 无权限访问。 */
    FORBIDDEN(403, "无权限访问"),
    /** 资源不存在。 */
    NOT_FOUND(404, "资源不存在"),
    /** 越权访问其他租户数据。 */
    TENANT_FORBIDDEN(1003, "越权访问其他租户数据"),
    /** 系统异常。 */
    SYSTEM_ERROR(500, "系统异常");

    /** 返回码。 */
    private final int code;
    /** 返回消息。 */
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
