package com.tsd.csm.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsd.csm.core.common.constant.CsmConst;
import com.tsd.csm.core.common.result.R;
import com.tsd.csm.core.common.result.ResultCode;
import com.tsd.csm.core.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录态拦截器：解析 token → 写入 {@link UserContext} 与 {@link TenantContext}。
 *
 * <p>平台超管可通过请求头 {@code X-App-Id} 指定「当前选中租户」作为隔离键；
 * 其余账号一律使用 token 内的 appId，不可越权。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return reject(response);
        }
        LoginUser user = jwtUtil.parse(token);
        if (user == null) {
            return reject(response);
        }
        UserContext.set(user);

        String effectiveAppId = user.getAppId();
        if (user.isPlatformSuper()) {
            String selected = request.getHeader(CsmConst.HEADER_APP_ID);
            if (StringUtils.hasText(selected)) {
                effectiveAppId = selected;
            }
        }
        TenantContext.setAppId(effectiveAppId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
        TenantContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(CsmConst.HEADER_TOKEN);
        if (StringUtils.hasText(header) && header.startsWith(CsmConst.TOKEN_PREFIX)) {
            return header.substring(CsmConst.TOKEN_PREFIX.length()).trim();
        }
        return header;
    }

    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(ResultCode.UNAUTHORIZED)));
        return false;
    }
}
