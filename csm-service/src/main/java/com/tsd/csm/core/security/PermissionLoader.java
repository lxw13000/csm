package com.tsd.csm.core.security;

import java.util.Set;

/**
 * 权限点加载器。由 account 模块实现（避免 core 反向依赖 modules）。
 */
public interface PermissionLoader {

    /** 加载某账号在其租户下拥有的全部权限点（perm_code）。 */
    Set<String> loadPermCodes(Long accountId, String appId);
}
