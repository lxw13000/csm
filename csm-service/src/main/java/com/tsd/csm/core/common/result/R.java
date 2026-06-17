package com.tsd.csm.core.common.result;

import java.io.Serializable;

/**
 * 统一响应体。{@code code = 0} 表示成功，非 0 为业务/系统错误码。
 *
 * @param <T> 业务数据类型
 */
public class R<T> implements Serializable {

    /** 返回码：0 成功，非 0 为业务/系统错误码（见 {@link ResultCode}）。 */
    private int code;
    /** 提示信息。 */
    private String msg;
    /** 业务数据载荷。 */
    private T data;

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功，无数据。 */
    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    /** 成功，携带数据。 */
    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /** 失败，默认业务错误码，自定义消息。 */
    public static <T> R<T> fail(String msg) {
        return new R<>(ResultCode.BIZ_ERROR.getCode(), msg, null);
    }

    /** 失败，自定义错误码与消息。 */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    /** 失败，使用预定义的返回码。 */
    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMsg(), null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
