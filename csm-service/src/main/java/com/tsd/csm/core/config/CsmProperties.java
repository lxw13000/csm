package com.tsd.csm.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CSM 自定义配置项，绑定 {@code csm.*}。
 */
@ConfigurationProperties(prefix = "csm")
public class CsmProperties {

    /** JWT 登录态配置（csm.jwt.*）。 */
    private final Jwt jwt = new Jwt();
    /** H5 会话凭证配置（csm.session.*）。 */
    private final Session session = new Session();
    /** 初始化数据配置（csm.init.*）。 */
    private final Init init = new Init();
    /** 文件上传配置（csm.upload.*）。 */
    private final Upload upload = new Upload();

    public Jwt getJwt() {
        return jwt;
    }

    public Session getSession() {
        return session;
    }

    public Init getInit() {
        return init;
    }

    public Upload getUpload() {
        return upload;
    }

    /** JWT 登录态配置。 */
    public static class Jwt {
        /** 签名密钥（长度需 >= 32 字节以满足 HS256）。 */
        private String secret;
        /** 登录态有效期（分钟）。 */
        private long expireMinutes = 720;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpireMinutes() {
            return expireMinutes;
        }

        public void setExpireMinutes(long expireMinutes) {
            this.expireMinutes = expireMinutes;
        }
    }

    /** H5 会话凭证配置。 */
    public static class Session {
        /** H5 会话凭证有效期（分钟）。 */
        private long expireMinutes = 120;

        public long getExpireMinutes() {
            return expireMinutes;
        }

        public void setExpireMinutes(long expireMinutes) {
            this.expireMinutes = expireMinutes;
        }
    }

    /** 初始化数据配置。 */
    public static class Init {
        /** 内置账号初始化默认密码。 */
        private String defaultPassword = "admin123";

        public String getDefaultPassword() {
            return defaultPassword;
        }

        public void setDefaultPassword(String defaultPassword) {
            this.defaultPassword = defaultPassword;
        }
    }

    /** 文件上传配置。 */
    public static class Upload {
        /** 上传文件存储根目录（默认相对路径 uploads，对外经 /files/** 访问）。 */
        private String dir = "uploads";

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }
    }
}
