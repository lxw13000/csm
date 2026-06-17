package com.tsd.csm;

import java.io.File;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.system.ApplicationHome;
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
        // 日志目录默认与 jar 同级的 logs/，由 logback-spring-{prod,test}.xml 通过 ${LOG_HOME} 引用。
        // 在 SpringApplication.run 之前设置，确保日志系统初始化时即可读取；无需依赖 CSM_LOG_DIR 环境变量。
        ApplicationHome home = new ApplicationHome(CsmApplication.class);
        System.setProperty("LOG_HOME", new File(home.getDir(), "logs").getAbsolutePath());
        SpringApplication.run(CsmApplication.class, args);
    }
}
