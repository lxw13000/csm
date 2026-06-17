package com.tsd.csm.core.security;

import com.tsd.csm.core.config.CsmProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发/解析内部账号登录态与 H5 会话凭证（统一 HS256，同一密钥）。
 */
@Component
public class JwtUtil {

    /** HS256 签名密钥。 */
    private final SecretKey key;
    /** 内部账号登录态有效期（毫秒）。 */
    private final long accountExpireMs;
    /** H5 会话凭证有效期（毫秒）。 */
    private final long sessionExpireMs;

    public JwtUtil(CsmProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.accountExpireMs = props.getJwt().getExpireMinutes() * 60_000L;
        this.sessionExpireMs = props.getSession().getExpireMinutes() * 60_000L;
    }

    /** 签发内部账号（PC/客服）登录态。 */
    public String generateAccountToken(LoginUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getAccountId()))
                .claim("appId", user.getAppId())
                .claim("accountType", user.getAccountType())
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("kind", "account")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accountExpireMs))
                .signWith(key)
                .compact();
    }

    /** 签发 C 端 H5 会话凭证。 */
    public String generateSessionToken(String appId, String userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim("appId", appId)
                .claim("userId", userId)
                .claim("kind", "session")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + sessionExpireMs))
                .signWith(key)
                .compact();
    }

    /** 解析 token 为登录主体；非法/过期返回 null。 */
    public LoginUser parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            String kind = c.get("kind", String.class);
            String appId = c.get("appId", String.class);
            if ("session".equals(kind)) {
                return LoginUser.ofCustomer(appId, c.get("userId", String.class));
            }
            Integer accountType = c.get("accountType", Integer.class);
            Long accountId = c.getSubject() == null ? null : Long.valueOf(c.getSubject());
            return LoginUser.ofAccount(appId, accountId, accountType,
                    c.get("username", String.class), c.get("realName", String.class));
        } catch (Exception e) {
            return null;
        }
    }
}
