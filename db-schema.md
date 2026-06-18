# 客服工单系统 · 数据库表结构设计（MySQL 8.0+）

> 配套需求文档 `xuqiu.md`（V2）。核心原则：**贯彻 2.6 租户级数据隔离**——除全局字典表外，每张业务表均含隔离键 `app_id`，且面向租户的复合索引一律以 `app_id` 打头，从存储层落实行级隔离。

---

## 一、通用设计约定

* **引擎与字符集**：所有表 `ENGINE=InnoDB`、`DEFAULT CHARSET=utf8mb4`、`COLLATE=utf8mb4_0900_ai_ci`（MySQL 8.0 默认排序规则，支持 emoji 与多语言聊天内容）。
* **主键**：统一 `id BIGINT UNSIGNED AUTO_INCREMENT`；如后续分库分表，可平滑替换为分布式 ID（雪花等），业务层无感。
* **租户隔离键**：业务表均含 `app_id VARCHAR(64)`。**平台级**账号/角色/角色菜单等使用保留值 `'_platform_'`，使 `(app_id, …)` 唯一约束在平台与租户两侧统一生效。
* **时间字段**：`created_at` / `updated_at` 用 `DATETIME` + `CURRENT_TIMESTAMP`（`updated_at` 带 `ON UPDATE`）；**消息表用 `DATETIME(3)` 毫秒精度**，保证消息排序与响应耗时计算准确（呼应 4.5）。
* **枚举**：用 `TINYINT` + 列 `COMMENT` 标注取值；关键状态/类型字段附 `CHECK` 约束（MySQL 8.0.16+ 强制生效）。
* **扩展字段**：易变配置用 `JSON` 列（MySQL 8.0 原生 JSON）承载，避免频繁加列。
* **软删除**：按需在表上加 `deleted TINYINT NOT NULL DEFAULT 0`（下方 DDL 默认未列，按治理需要启用）。
* **敏感信息**：C 端用户昵称/头像在业务系统换取通信凭证时同步缓存（见 `csm_customer`）；其余字段（手机号等）为预留缓存列，权威数据不落库，以业务系统为准（呼应 2.5）。

---

## 二、主要表清单

| # | 表名 | 中文名 | 所属域 | 隔离键 | 关键说明 |
| --- | --- | --- | --- | --- | --- |
| 1 | `csm_tenant` | 业务系统(租户) | 租户接入 | app_id(PK业务键) | app_id/app_secret/凭证有效期/状态 |
| 2 | `csm_account` | 内部账号 | 账号权限 | app_id | PC后台+客服H5账号，type区分超管/租户管理员/客服 |
| 3 | `csm_role` | 角色 | 账号权限 | app_id | RBAC 角色 |
| 4 | `csm_account_role` | 账号-角色 | 账号权限 | app_id | 多对多 |
| 5 | `csm_menu` | 菜单/权限点 | 账号权限 | 全局 | 平台级功能字典，无 app_id |
| 6 | `csm_role_menu` | 角色-菜单 | 账号权限 | app_id | 角色授权 |
| 7 | `csm_tenant_config` | 租户配置 | 客服配置 | app_id | 接入阈值、自动完结时长、提醒策略 |
| 8 | `csm_agent_status` | 客服在线状态 | 客服配置 | app_id | 在线状态+当前负载，派单依据 |
| 9 | `csm_customer` | C端用户(缓存) | 用户 | app_id | app_id+user_id 联合唯一，展示用缓存 |
| 10 | `csm_qa` | QA问答对 | QA库 | app_id | 租户独立、完全隔离 |
| 11 | `csm_qa_keyword` | QA关键词 | QA库 | app_id | 关键词关联 |
| 12 | `csm_ticket` | 工单 | 工单会话 | app_id | 状态/完结方式/归属客服 |
| 13 | `csm_ticket_message` | 工单会话消息 | 工单会话 | app_id | 毫秒时间、user_id冗余按人查历史、client_msg_id去重、响应耗时 |
| 14 | `csm_ticket_transfer` | 工单转接记录 | 工单会话 | app_id | 转接来源/目标客服 |
| 15 | `csm_ticket_evaluation` | 服务评价 | 工单会话 | app_id | 已解决/未解决、评分 |
| 16 | `csm_ticket_message_read` | 消息已读水位 | 运营辅助(可选) | app_id | `read` 回执、未读数计算 |
| 17 | `csm_operation_log` | 操作审计日志 | 运营辅助(可选) | app_id | 后台/客服端操作留痕 |
| 18 | `csm_agent_work_daily` | 客服工作日汇总 | 运营辅助(可选) | app_id | 按日预聚合，加速统计 |

---

## 三、建表语句（DDL）

### 3.1 租户与接入

#### csm_tenant 业务系统(租户)接入表
```sql
CREATE TABLE csm_tenant (
  id                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id                   VARCHAR(64)   NOT NULL COMMENT '租户标识，对接业务系统唯一标识',
  app_secret               VARCHAR(128)  NOT NULL COMMENT '换取通信凭证的鉴权密钥',
  name                     VARCHAR(128)  NOT NULL COMMENT '租户/业务系统名称',
  credential_expire_minutes INT          NOT NULL DEFAULT 120 COMMENT '颁发给业务系统的通信凭证有效期(分钟)',
  ip_whitelist             VARCHAR(1024) DEFAULT NULL COMMENT 'IP白名单，逗号分隔，可空',
  status                   TINYINT       NOT NULL DEFAULT 1 COMMENT '接入状态 1启用 0停用',
  remark                   VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  created_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务系统(租户)接入表';
```

### 3.2 账号与权限（RBAC）

#### csm_account 内部账号表（PC 后台 + 客服端 H5）
```sql
CREATE TABLE csm_account (
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
```

#### csm_role 角色表
```sql
CREATE TABLE csm_role (
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
```

#### csm_account_role 账号-角色关联表
```sql
CREATE TABLE csm_account_role (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id     VARCHAR(64)     NOT NULL COMMENT '所属租户(隔离冗余)',
  account_id BIGINT UNSIGNED NOT NULL COMMENT '账号id',
  role_id    BIGINT UNSIGNED NOT NULL COMMENT '角色id',
  PRIMARY KEY (id),
  UNIQUE KEY uk_account_role (account_id, role_id),
  KEY idx_app_role (app_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号-角色关联表';
```

#### csm_menu 菜单/权限点（平台全局字典，无 app_id）
```sql
CREATE TABLE csm_menu (
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
```

#### csm_role_menu 角色-菜单关联表
```sql
CREATE TABLE csm_role_menu (
  id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id  VARCHAR(64)     NOT NULL COMMENT '所属租户(平台级为_platform_)',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色id',
  menu_id BIGINT UNSIGNED NOT NULL COMMENT '菜单id',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_id, menu_id),
  KEY idx_app (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-菜单关联表';
```

### 3.3 客服与租户配置

#### csm_tenant_config 租户配置表（每租户一行）
```sql
CREATE TABLE csm_tenant_config (
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
```

#### csm_agent_status 客服在线状态表（派单依据）
```sql
CREATE TABLE csm_agent_status (
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
```
> `idx_dispatch (app_id, online_status, current_load)` 直接支撑 5.4 派单：同租户 + 在线 + 负载未达阈值 + 取负载最小者。在线状态等热点字段可同时镜像至 Redis，本表作权威与统计来源。

### 3.4 C 端用户（缓存）

#### csm_customer C 端用户缓存表
```sql
CREATE TABLE csm_customer (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)  NOT NULL COMMENT '所属租户',
  user_id       VARCHAR(64)  NOT NULL COMMENT '业务系统用户id',
  nickname      VARCHAR(128) DEFAULT NULL COMMENT '昵称(换取凭证时同步缓存)',
  avatar        VARCHAR(512) DEFAULT NULL COMMENT '头像URL(换取凭证时同步缓存)',
  user_level    VARCHAR(32)  DEFAULT NULL COMMENT '用户等级(预留缓存)',
  masked_phone  VARCHAR(32)  DEFAULT NULL COMMENT '脱敏手机号(预留缓存)',
  register_time DATETIME     DEFAULT NULL COMMENT '注册时间(预留缓存)',
  last_sync_at  DATETIME     DEFAULT NULL COMMENT '最近一次同步业务系统信息的时间',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次接入时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_user (app_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='C端用户展示信息缓存表(权威源为业务系统)';
```

### 3.5 QA 知识库

#### csm_qa QA 问答对表
```sql
CREATE TABLE csm_qa (
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
```
> 中文相似问匹配可选启用全文索引（MySQL 8.0 ngram 分词）：
> `ALTER TABLE csm_qa ADD FULLTEXT KEY ft_question (question) WITH PARSER ngram;`
> 查询时务必附加 `app_id = ?` 以保持租户隔离。

#### csm_qa_keyword QA 关键词关联表
```sql
CREATE TABLE csm_qa_keyword (
  id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id  VARCHAR(64)     NOT NULL COMMENT '所属租户',
  qa_id   BIGINT UNSIGNED NOT NULL COMMENT 'QA问答对id',
  keyword VARCHAR(64)     NOT NULL COMMENT '关联关键词',
  PRIMARY KEY (id),
  KEY idx_app_keyword (app_id, keyword),
  KEY idx_qa (qa_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='QA关键词关联表';
```

### 3.6 工单与会话

#### csm_ticket 工单表
```sql
CREATE TABLE csm_ticket (
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
```

#### csm_ticket_message 工单会话消息表
```sql
CREATE TABLE csm_ticket_message (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)     NOT NULL COMMENT '所属租户',
  ticket_id     BIGINT UNSIGNED NOT NULL COMMENT '工单id',
  user_id       VARCHAR(64)     NOT NULL COMMENT '所属C端用户(业务系统user_id)，无论发送方是谁均冗余记录，便于按人查历史',
  client_msg_id VARCHAR(64)     DEFAULT NULL COMMENT '客户端生成唯一id，用于去重(4.5)',
  seq           BIGINT          NOT NULL DEFAULT 0 COMMENT '会话内递增序号，用于工单内排序与已读水位',
  sender_type   TINYINT         NOT NULL COMMENT '发送方 1用户 2客服 3系统/机器人',
  sender_id     VARCHAR(64)     DEFAULT NULL COMMENT '发送方标识(user_id或客服account_id)',
  content_type  TINYINT         NOT NULL DEFAULT 1 COMMENT '内容类型 1文本 2图片 3其他多媒体',
  content       TEXT            DEFAULT NULL COMMENT '文本内容或媒体引用URL',
  response_cost INT             DEFAULT NULL COMMENT '响应耗时(秒)，仅客服回复且前一条为用户消息时记录',
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间(毫秒)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_clientmsg (ticket_id, client_msg_id),
  KEY idx_app_ticket_seq (app_id, ticket_id, seq),
  KEY idx_app_user_id (app_id, user_id, id),
  CONSTRAINT chk_sender_type  CHECK (sender_type IN (1,2,3)),
  CONSTRAINT chk_content_type CHECK (content_type IN (1,2,3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单会话消息表';
```
> `uk_ticket_clientmsg` 实现幂等去重；`response_cost` 由服务端在落库时按「本条客服消息时间 − 上一条用户消息时间」计算（呼应 4.2/5.3）。
> `user_id` 冗余记录消息所属 C 端用户，配合 `idx_app_user_id (app_id, user_id, id)` 支撑**实时聊天按人查全量历史**（跨工单，按主键 id 即时间倒序分页），无需与 `csm_ticket` 联查；`seq` 仍按工单维度用于已读水位。消息量大时可按 `created_at` 做分区或归档。

#### csm_ticket_transfer 工单转接记录表
```sql
CREATE TABLE csm_ticket_transfer (
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
```

#### csm_ticket_evaluation 服务评价表
```sql
CREATE TABLE csm_ticket_evaluation (
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
```

### 3.7 运营与统计辅助表（可选启用）

> 以下三张为运营/统计辅助表，可按需启用；均含 `app_id` 且索引以 `app_id` 打头，遵循 §2.6 隔离约定。

#### csm_ticket_message_read 消息已读水位表
```sql
CREATE TABLE csm_ticket_message_read (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  app_id        VARCHAR(64)     NOT NULL COMMENT '所属租户',
  ticket_id     BIGINT UNSIGNED NOT NULL COMMENT '工单id',
  reader_type   TINYINT         NOT NULL COMMENT '阅读方 1用户 2客服',
  reader_id     VARCHAR(64)     NOT NULL COMMENT '阅读方标识(user_id或客服account_id)',
  last_read_seq BIGINT          NOT NULL DEFAULT 0 COMMENT '已读到的最大消息序号(对应csm_ticket_message.seq)',
  last_read_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近已读时间',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_reader (ticket_id, reader_type, reader_id),
  KEY idx_app_ticket (app_id, ticket_id),
  CONSTRAINT chk_reader_type CHECK (reader_type IN (1,2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息已读水位表(read回执)';
```
> 采用「高水位」设计：每个 `(工单, 阅读方)` 仅一行，随阅读推进更新 `last_read_seq`；某方未读数 = 该工单中对端发送、且 `seq > last_read_seq` 的消息数，无需逐条记录已读。

#### csm_operation_log 后台操作审计日志表
```sql
CREATE TABLE csm_operation_log (
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
```
> 追加写、增长快：建议按 `created_at` 做分区或定期归档冷数据；`detail` 用 JSON 存请求参数与变更前后快照，便于合规追溯。

#### csm_agent_work_daily 客服工作情况日汇总表
```sql
CREATE TABLE csm_agent_work_daily (
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
```
> 由定时任务每日凌晨按 `csm_ticket` / `csm_ticket_message` / 在线状态明细预聚合写入；报表查询直接读本表，避免实时扫描大表（呼应 4.3.2 客服工作情况统计）。

---

## 四、关键关系与隔离说明

### 4.1 实体关系（简版）
```text
csm_tenant(app_id) ─┬─< csm_account ─< csm_account_role >─ csm_role ─< csm_role_menu >─ csm_menu(全局)
                  ├─< csm_tenant_config (1:1)
                  ├─< csm_agent_status (1:1 客服)
                  ├─< csm_customer (app_id+user_id)
                  ├─< csm_qa ─< csm_qa_keyword
                  └─< csm_ticket ─┬─< csm_ticket_message
                                ├─< csm_ticket_message_read
                                ├─< csm_ticket_transfer
                                └─< csm_ticket_evaluation (1:1)
csm_account ─< csm_agent_work_daily（按日预聚合）     csm_operation_log（操作审计，按 app_id 记录）
```

### 4.2 租户隔离落地要点
* **全表带 `app_id`**：除全局字典 `csm_menu` 外，所有表均含 `app_id`，复合唯一键与查询索引一律 `app_id` 打头（如 `uk_app_user`、`idx_app_status`、`idx_dispatch`），既支撑隔离又利于走索引。
* **统一注入**：数据访问层（MyBatis 拦截器 / ORM 全局 scope）对所有面向租户的 SQL 强制追加 `app_id = :ctxTenant`；平台超管以「当前选中租户」注入，跨租户统计单独走带 `app_id` 维度聚合的只读路径。
* **不依赖物理外键**：为便于分库分表与高并发写入，表间关系以逻辑外键（`xxx_id` + 索引）维护，不建 `FOREIGN KEY` 约束；一致性由应用层与事务保证。
* **派单一致性**：`csm_agent_status.idx_dispatch` 与 `csm_ticket.idx_app_agent` 保证 5.4 的「同租户 + 在线 + 未达阈值 + 负载最小」匹配高效且不跨租户。

### 4.3 会话凭证与实时层（不落主表）
* H5 会话凭证（session token）、WebSocket 连接态、未 `ack` 重发队列、消息序号水位等**置于 Redis 等缓存**，不进上述主表；token 绑定 `app_id + user_id`（或客服账号），连接按 `app_id` 隔离，防止跨租户串话（呼应 4.5、2.6③）。

### 4.4 运营与统计辅助表的启用说明
`csm_ticket_message_read`、`csm_operation_log`、`csm_agent_work_daily` 三张表的完整结构见 **3.7**，均带 `app_id` 且索引以 `app_id` 打头，遵循统一隔离约定。启用建议：

| 表 | 何时启用 | 写入方式 | 注意 |
| --- | --- | --- | --- |
| `csm_ticket_message_read` | 上线聊天「已读/未读」能力时 | 阅读时高水位单行 upsert | 成本低，可常驻启用 |
| `csm_operation_log` | 有安全审计/合规留痕需求时 | 操作切面追加写 | 量大，按 `created_at` 归档或分区 |
| `csm_agent_work_daily` | 客服报表数据量增大后 | 定时任务每日预聚合 | 明细仍以 `csm_ticket`/`csm_ticket_message` 为准
