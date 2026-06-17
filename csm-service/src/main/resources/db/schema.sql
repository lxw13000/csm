-- ============================================================================
-- CSM 客服工单系统 · 表结构（MySQL 8.0+）
-- 配套 db-schema.md：除全局字典 csm_menu 外，每张业务表含隔离键 app_id，
-- 面向租户的复合索引一律以 app_id 打头，从存储层落实行级隔离。
-- ============================================================================
CREATE DATABASE IF NOT EXISTS csm DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE csm;

-- ---------------------------------------------------------------------------
-- 3.1 租户与接入
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS csm_tenant (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)   NOT NULL COMMENT '租户标识，对接业务系统唯一标识',
  app_secret    VARCHAR(128)  NOT NULL COMMENT '出站调用签名密钥',
  name          VARCHAR(128)  NOT NULL COMMENT '租户/业务系统名称',
  identity_api  VARCHAR(512)  NOT NULL COMMENT '业务系统①身份换取接口地址',
  user_info_api VARCHAR(512)  NOT NULL COMMENT '业务系统②按user_id查用户信息接口地址',
  ip_whitelist  VARCHAR(1024) DEFAULT NULL COMMENT 'IP白名单，逗号分隔，可空',
  status        TINYINT       NOT NULL DEFAULT 1 COMMENT '接入状态 1启用 0停用',
  remark        VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务系统(租户)接入表';

-- ---------------------------------------------------------------------------
-- 3.2 账号与权限（RBAC）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS csm_account (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)  NOT NULL COMMENT '所属租户；平台超管用保留值_platform_',
  username      VARCHAR(64)  NOT NULL COMMENT '登录账号',
  password_hash VARCHAR(128) NOT NULL COMMENT '密码哈希(bcrypt/argon2)',
  real_name     VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
  account_type  TINYINT      NOT NULL COMMENT '账号类型 1平台超管 2租户管理员 3客服',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_username (app_id, username),
  KEY idx_app_type (app_id, account_type),
  CONSTRAINT chk_account_type CHECK (account_type IN (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内部账号表(PC后台+客服H5，客服单租户归属)';

CREATE TABLE IF NOT EXISTS csm_role (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id     VARCHAR(64)  NOT NULL COMMENT '所属租户；平台级角色用_platform_',
  name       VARCHAR(64)  NOT NULL COMMENT '角色名称',
  code       VARCHAR(64)  NOT NULL COMMENT '角色编码',
  remark     VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_code (app_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS csm_account_role (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id     VARCHAR(64)     NOT NULL COMMENT '所属租户(隔离冗余)',
  account_id BIGINT UNSIGNED NOT NULL COMMENT '账号id',
  role_id    BIGINT UNSIGNED NOT NULL COMMENT '角色id',
  PRIMARY KEY (id),
  UNIQUE KEY uk_account_role (account_id, role_id),
  KEY idx_app_role (app_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号-角色关联表';

CREATE TABLE IF NOT EXISTS csm_menu (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父菜单id，0为根',
  name       VARCHAR(64)  NOT NULL COMMENT '菜单/功能点名称',
  type       TINYINT      NOT NULL DEFAULT 1 COMMENT '1目录 2菜单 3按钮/权限点',
  perm_code  VARCHAR(128) DEFAULT NULL COMMENT '权限标识，如 ticket:list',
  path       VARCHAR(255) DEFAULT NULL COMMENT '前端路由',
  sort       INT          NOT NULL DEFAULT 0 COMMENT '排序',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_parent (parent_id),
  UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单/权限点(平台全局字典)';

CREATE TABLE IF NOT EXISTS csm_role_menu (
  id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id  VARCHAR(64)     NOT NULL COMMENT '所属租户(平台级为_platform_)',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色id',
  menu_id BIGINT UNSIGNED NOT NULL COMMENT '菜单id',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_id, menu_id),
  KEY idx_app (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-菜单关联表';

-- ---------------------------------------------------------------------------
-- 3.3 客服与租户配置
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS csm_tenant_config (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id             VARCHAR(64) NOT NULL COMMENT '所属租户',
  max_concurrent     INT     NOT NULL DEFAULT 0 COMMENT '单客服最大同时接入量，0=不限制',
  auto_close_minutes INT     NOT NULL DEFAULT 15 COMMENT '用户无操作自动完结倒计时(分钟)',
  notify_sound       TINYINT NOT NULL DEFAULT 1 COMMENT '页面声音提醒开关 1开 0关',
  ext                JSON    DEFAULT NULL COMMENT '扩展配置(提醒策略等)',
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户级配置表';

CREATE TABLE IF NOT EXISTS csm_agent_status (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id         VARCHAR(64)     NOT NULL COMMENT '所属租户',
  account_id     BIGINT UNSIGNED NOT NULL COMMENT '客服账号id(csm_account.id)',
  online_status  TINYINT NOT NULL DEFAULT 0 COMMENT '在线状态 0离线 1在线',
  current_load   INT     NOT NULL DEFAULT 0 COMMENT '当前处理中工单数',
  last_online_at DATETIME DEFAULT NULL COMMENT '最近上线时间',
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_account (account_id),
  KEY idx_dispatch (app_id, online_status, current_load)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服在线状态/负载表';

-- ---------------------------------------------------------------------------
-- 3.4 C 端用户（缓存）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS csm_customer (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)  NOT NULL COMMENT '所属租户',
  user_id       VARCHAR(64)  NOT NULL COMMENT '业务系统用户id',
  nickname      VARCHAR(128) DEFAULT NULL COMMENT '昵称(缓存)',
  avatar        VARCHAR(512) DEFAULT NULL COMMENT '头像URL(缓存)',
  user_level    VARCHAR(32)  DEFAULT NULL COMMENT '用户等级(缓存)',
  masked_phone  VARCHAR(32)  DEFAULT NULL COMMENT '脱敏手机号(缓存)',
  register_time DATETIME     DEFAULT NULL COMMENT '注册时间(缓存)',
  last_sync_at  DATETIME     DEFAULT NULL COMMENT '最近一次同步业务系统信息的时间',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次接入时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_user (app_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='C端用户展示信息缓存表(权威源为业务系统)';

-- ---------------------------------------------------------------------------
-- 3.5 QA 知识库
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS csm_qa (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id     VARCHAR(64)  NOT NULL COMMENT '所属租户，租户间完全隔离',
  question   VARCHAR(512) NOT NULL COMMENT '标准问题',
  answer     TEXT         NOT NULL COMMENT '答案',
  status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人账号id',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_app_status (app_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='QA问答对表(按租户隔离)';

CREATE TABLE IF NOT EXISTS csm_qa_keyword (
  id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id  VARCHAR(64)     NOT NULL COMMENT '所属租户',
  qa_id   BIGINT UNSIGNED NOT NULL COMMENT 'QA问答对id',
  keyword VARCHAR(64)     NOT NULL COMMENT '关联关键词',
  PRIMARY KEY (id),
  KEY idx_app_keyword (app_id, keyword),
  KEY idx_qa (qa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='QA关键词关联表';

-- ---------------------------------------------------------------------------
-- 3.6 工单与会话
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS csm_ticket (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id       VARCHAR(64)     NOT NULL COMMENT '所属租户',
  user_id      VARCHAR(64)     NOT NULL COMMENT '所属C端用户(业务系统user_id)',
  status       TINYINT         NOT NULL COMMENT '状态 1智能问答 2人工转接中 3处理中 4已完结',
  close_type   TINYINT         DEFAULT NULL COMMENT '完结方式 1用户已解决 2超时自动 3客服强制',
  agent_id     BIGINT UNSIGNED DEFAULT NULL COMMENT '当前处理客服账号id',
  first_msg_at DATETIME(3)     DEFAULT NULL COMMENT '首条消息时间',
  assigned_at  DATETIME(3)     DEFAULT NULL COMMENT '派单时间',
  last_msg_at  DATETIME(3)     DEFAULT NULL COMMENT '最后一条消息时间',
  closed_at    DATETIME(3)     DEFAULT NULL COMMENT '完结时间',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_app_user (app_id, user_id),
  KEY idx_app_status (app_id, status),
  KEY idx_app_agent (app_id, agent_id, status),
  CONSTRAINT chk_ticket_status CHECK (status IN (1,2,3,4)),
  CONSTRAINT chk_ticket_close  CHECK (close_type IN (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单表';

CREATE TABLE IF NOT EXISTS csm_message (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)     NOT NULL COMMENT '所属租户',
  ticket_id     BIGINT UNSIGNED NOT NULL COMMENT '工单id',
  client_msg_id VARCHAR(64)     DEFAULT NULL COMMENT '客户端生成唯一id，用于去重(4.5)',
  seq           BIGINT          NOT NULL DEFAULT 0 COMMENT '会话内递增序号，用于排序与断线增量',
  sender_type   TINYINT         NOT NULL COMMENT '发送方 1用户 2客服 3系统/机器人',
  sender_id     VARCHAR(64)     DEFAULT NULL COMMENT '发送方标识(user_id或客服account_id)',
  content_type  TINYINT         NOT NULL DEFAULT 1 COMMENT '内容类型 1文本 2图片 3其他多媒体',
  content       TEXT            DEFAULT NULL COMMENT '文本内容或媒体引用URL',
  response_cost INT             DEFAULT NULL COMMENT '响应耗时(秒)，仅客服回复且前一条为用户消息时记录',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间(毫秒)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_clientmsg (ticket_id, client_msg_id),
  KEY idx_app_ticket_seq (app_id, ticket_id, seq),
  CONSTRAINT chk_sender_type  CHECK (sender_type IN (1,2,3)),
  CONSTRAINT chk_content_type CHECK (content_type IN (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话消息表';

CREATE TABLE IF NOT EXISTS csm_ticket_transfer (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)     NOT NULL COMMENT '所属租户',
  ticket_id     BIGINT UNSIGNED NOT NULL COMMENT '工单id',
  from_agent_id BIGINT UNSIGNED DEFAULT NULL COMMENT '转出客服账号id',
  to_agent_id   BIGINT UNSIGNED NOT NULL COMMENT '转入客服账号id',
  reason        VARCHAR(255)    DEFAULT NULL COMMENT '转接原因',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '转接时间',
  PRIMARY KEY (id),
  KEY idx_app_ticket (app_id, ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单转接记录表';

CREATE TABLE IF NOT EXISTS csm_ticket_evaluation (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id     VARCHAR(64)     NOT NULL COMMENT '所属租户',
  ticket_id  BIGINT UNSIGNED NOT NULL COMMENT '工单id',
  resolved   TINYINT         DEFAULT NULL COMMENT '是否已解决 1已解决 0未解决',
  rating     TINYINT         DEFAULT NULL COMMENT '满意度评分 1-5',
  remark     VARCHAR(512)    DEFAULT NULL COMMENT '评价文字',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket (ticket_id),
  KEY idx_app (app_id),
  CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务评价表';

-- ---------------------------------------------------------------------------
-- 3.7 运营与统计辅助表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS csm_message_read (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)     NOT NULL COMMENT '所属租户',
  ticket_id     BIGINT UNSIGNED NOT NULL COMMENT '工单id',
  reader_type   TINYINT         NOT NULL COMMENT '阅读方 1用户 2客服',
  reader_id     VARCHAR(64)     NOT NULL COMMENT '阅读方标识(user_id或客服account_id)',
  last_read_seq BIGINT          NOT NULL DEFAULT 0 COMMENT '已读到的最大消息序号(对应csm_message.seq)',
  last_read_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近已读时间',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_reader (ticket_id, reader_type, reader_id),
  KEY idx_app_ticket (app_id, ticket_id),
  CONSTRAINT chk_reader_type CHECK (reader_type IN (1,2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息已读水位表(read回执)';

CREATE TABLE IF NOT EXISTS csm_operation_log (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)     NOT NULL COMMENT '操作所属租户；平台级操作为_platform_',
  operator_id   BIGINT UNSIGNED NOT NULL COMMENT '操作人账号id(csm_account.id)',
  operator_name VARCHAR(64)     DEFAULT NULL COMMENT '操作人姓名(快照)',
  operator_type TINYINT         NOT NULL DEFAULT 0 COMMENT '操作人类型 1平台超管 2租户管理员 3客服',
  module        VARCHAR(32)     NOT NULL COMMENT '操作模块 如 ticket/qa/account/tenant/config/auth',
  action        VARCHAR(32)     NOT NULL COMMENT '操作动作 如 create/update/delete/close/transfer/login',
  target_type   VARCHAR(32)     DEFAULT NULL COMMENT '目标对象类型',
  target_id     VARCHAR(64)     DEFAULT NULL COMMENT '目标对象id',
  detail        JSON            DEFAULT NULL COMMENT '操作详情(请求参数/变更前后快照)',
  client_ip     VARCHAR(45)     DEFAULT NULL COMMENT '操作来源IP(兼容IPv6)',
  user_agent    VARCHAR(512)    DEFAULT NULL COMMENT '客户端User-Agent',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_app_time (app_id, created_at),
  KEY idx_app_operator (app_id, operator_id, created_at),
  KEY idx_app_module (app_id, module, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台操作审计日志表';

CREATE TABLE IF NOT EXISTS csm_agent_work_daily (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id            VARCHAR(64)     NOT NULL COMMENT '所属租户',
  agent_id          BIGINT UNSIGNED NOT NULL COMMENT '客服账号id(csm_account.id)',
  stat_date         DATE            NOT NULL COMMENT '统计日期',
  online_seconds    INT             NOT NULL DEFAULT 0 COMMENT '在线时长(秒)',
  ticket_count      INT             NOT NULL DEFAULT 0 COMMENT '接待工单数',
  reply_count       INT             NOT NULL DEFAULT 0 COMMENT '回复消息数',
  avg_response_cost INT             NOT NULL DEFAULT 0 COMMENT '平均响应耗时(秒)',
  force_close_count INT             NOT NULL DEFAULT 0 COMMENT '强制关闭工单数',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_agent_date (app_id, agent_id, stat_date),
  KEY idx_app_date (app_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服工作情况日汇总表';
