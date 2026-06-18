-- ============================================================================
-- CSM 初始化数据
-- 说明：账号（含 bcrypt 密码）由后端启动时的 DataInitializer 幂等创建，
--      以保证密码哈希正确，此处仅初始化菜单字典、示例租户与租户配置、角色。
-- ============================================================================
USE csm;

-- ---- 菜单/权限点字典（全局，无 app_id）-------------------------------------
INSERT INTO csm_menu (id, parent_id, name, type, perm_code, path, sort) VALUES
  (100, 0,   '工作台',       2, 'dashboard:view',     '/dashboard',      1),
  (200, 0,   '租户接入',     2, 'tenant:list',        '/tenant',         2),
  (300, 0,   '账号管理',     1, NULL,                 NULL,              3),
  (301, 300, 'PC账号',       2, 'account:list',       '/account',        1),
  (302, 300, '客服账号',     2, 'account:agent:list', '/account/agent',  2),
  (400, 0,   'C端用户',      2, 'customer:list',      '/customer',       4),
  (500, 0,   '工单管理',     2, 'ticket:list',        '/ticket',         5),
  (600, 0,   'QA知识库',     2, 'qa:list',            '/qa',             6),
  (700, 0,   '统计分析',     1, NULL,                 NULL,              7),
  (701, 700, '工单统计',     2, 'stats:ticket',       '/stats/ticket',   1),
  (702, 700, '客服统计',     2, 'stats:agent',        '/stats/agent',    2),
  (800, 0,   '系统管理',     1, NULL,                 NULL,              8),
  (801, 800, '菜单管理',     2, 'system:menu:list',   '/system/menu',    1),
  (802, 800, '角色权限',     2, 'system:role:list',   '/system/role',    2),
  (900, 0,   '接入配置',     2, 'config:view',        '/config',         9),
  (1000,0,   '审计日志',     2, 'log:list',           '/log',            10)
AS new ON DUPLICATE KEY UPDATE name = new.name, path = new.path, sort = new.sort;

-- ---- 平台级角色 ------------------------------------------------------------
INSERT INTO csm_role (id, app_id, name, code, remark) VALUES
  (1, '_platform_', '平台超级管理员', 'PLATFORM_SUPER', '跨租户运营')
AS new ON DUPLICATE KEY UPDATE name = new.name;

-- ---- 示例租户（业务系统接入）----------------------------------------------
INSERT INTO csm_tenant (id, app_id, app_secret, name, credential_expire_minutes, status, remark) VALUES
  (1, 'biz_demo', 'demo_secret_please_change',
   '演示业务系统', 120,
   1, '内置演示租户：业务系统用 app_id+app_secret 调用 /api/integration/credential 换取通信凭证')
AS new ON DUPLICATE KEY UPDATE name = new.name,
   credential_expire_minutes = new.credential_expire_minutes;

-- ---- 示例租户的租户级配置 --------------------------------------------------
INSERT INTO csm_tenant_config (app_id, max_concurrent, auto_close_minutes, notify_sound) VALUES
  ('biz_demo', 5, 15, 1)
AS new ON DUPLICATE KEY UPDATE max_concurrent = new.max_concurrent;

-- ---- 示例租户的租户管理员角色 ----------------------------------------------
INSERT INTO csm_role (id, app_id, name, code, remark) VALUES
  (2, 'biz_demo', '租户管理员', 'TENANT_ADMIN', '管理本租户')
AS new ON DUPLICATE KEY UPDATE name = new.name;
