package com.tsd.csm.modules.file.domain.vo;

/**
 * 文件上传结果。
 */
public class UploadVO {

    /** 可访问的相对地址（经 /files/** 提供）。 */
    private String url;
    /** 原始文件名。 */
    private String name;
    /** 文件大小（字节）。 */
    private long size;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
