package com.tsd.csm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.tsd.csm.core.config.CsmProperties;

/**
 * CSM 独立客服工单系统 · 接口服务启动类。
 *
 * <p>根包 {@code com.tsd.csm}，分 {@code core}（核心公共）与 {@code modules}（业务模块）。
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CsmProperties.class)
@MapperScan("com.tsd.csm.modules.**.mapper")
public class CsmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsmApplication.class, args);
    }
}
