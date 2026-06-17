package com.tsd.csm.modules.auth.domain.vo;

import com.tsd.csm.modules.account.domain.vo.MenuVO;

import java.util.List;
import java.util.Set;

/**
 * 登录 / 当前用户信息返回体。{@code token} 仅登录时下发，{@code /me} 接口为 null。
 */
public class LoginVO {

    /** 登录令牌（JWT）；仅登录接口下发，/me 接口为 null。 */
    private String token;
    /** 账号 id。 */
    private Long accountId;
    /** 所属租户。 */
    private String appId;
    /** 登录账号。 */
    private String username;
    /** 姓名。 */
    private String realName;
    /** 账号类型：1 平台超管 / 2 租户管理员 / 3 客服。 */
    private Integer accountType;
    /** 拥有的权限点集合（perm_code）。 */
    private Set<String> permCodes;
    /** 可见菜单树。 */
    private List<MenuVO> menus;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public Set<String> getPermCodes() {
        return permCodes;
    }

    public void setPermCodes(Set<String> permCodes) {
        this.permCodes = permCodes;
    }

    public List<MenuVO> getMenus() {
        return menus;
    }

    public void setMenus(List<MenuVO> menus) {
        this.menus = menus;
    }
}
