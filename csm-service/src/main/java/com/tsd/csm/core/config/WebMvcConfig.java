package com.tsd.csm.core.config;

import com.tsd.csm.core.security.AuthInterceptor;
import com.tsd.csm.modules.file.service.FileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置：注册登录态拦截器，放行登录、颁发通信凭证、健康检查与静态资源。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final FileStorageService fileStorageService;

    public WebMvcConfig(AuthInterceptor authInterceptor, FileStorageService fileStorageService) {
        this.authInterceptor = authInterceptor;
        this.fileStorageService = fileStorageService;
    }

    /** 静态资源：上传文件经 /files/** 对外提供（不经 AuthInterceptor）。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = fileStorageService.getBaseDir().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/common/**",
                        "/api/admin/auth/login",
                        "/api/agent/auth/login",
                        "/api/integration/credential"
                );
    }
}
