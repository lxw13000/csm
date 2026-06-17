package com.tsd.csm.core.common.exception;

import com.tsd.csm.core.common.result.ResultCode;

/**
 * 业务异常，由全局异常处理器统一转换为 {@link com.tsd.csm.core.common.result.R}。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BIZ_ERROR.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public int getCode() {
        return code;
    }

    public static BizException of(String message) {
        return new BizException(message);
    }
}
