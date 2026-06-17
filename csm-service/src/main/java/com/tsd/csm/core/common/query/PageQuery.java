package com.tsd.csm.core.common.query;

/**
 * 分页查询基类。
 */
public class PageQuery {

    /** 当前页码，从 1 开始，默认 1。 */
    private long current = 1;
    /** 每页条数，默认 10。 */
    private long size = 10;

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
