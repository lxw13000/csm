# CSM · 独立客服工单系统（多租户 SaaS）

基于 `xuqiu.md`（需求 V2）与 `db-schema.md`（数据库设计）实现的独立客服工单系统。以 `app_id` 为唯一隔离键实现全量业务数据租户级隔离；单条 WebSocket + 逻辑通道承载实时通信。

## 工程结构（monorepo）

| 目录 | 端 | 技术栈 | 说明 |
| --- | --- | --- | --- |
| `csm-service` | 接口服务 | Java 21 · Spring Boot 3.5.14 · MyBatis-Plus 3.5.16 · Maven · MySQL 8 · Redis | 后端核心：多租户、RBAC、工单/会话、派单、WebSocket、统计 |
| `csm-admin` | PC 管理端 | Vue 3 · Element Plus · Vite · TypeScript | SaaS 多租户分级权限后台 |
| `csm-agent` | 客服端 H5 | Vue 3 · Vant · Vite | 独立账号登录、上下线接单、会话与聊天 |
| `csm-user` | 用户端 H5 | Vue 3 · Vant · Vite | 内嵌业务 App WebView，token 换取身份、智能问答、聊天 |

后端根包 `com.tsd.csm`，分 `core`（核心公共）与 `modules`（业务模块，每模块含 domain/mapper/service/controller）。

## 快速开始

### 1. 启动依赖（MySQL + Redis）

```bash
docker-compose up -d
```

MySQL 暴露 `3306`（库 `csm`，账号 `root/csm123456`），Redis 暴露 `6379`。

### 2. 初始化数据库

容器首次启动会自动执行 `csm-service/src/main/resources/db/schema.sql` 与 `data.sql`（通过挂载到 MySQL 的初始化目录）。如需手动执行：

```bash
mysql -h127.0.0.1 -uroot -pcsm123456 csm < csm-service/src/main/resources/db/schema.sql
mysql -h127.0.0.1 -uroot -pcsm123456 csm < csm-service/src/main/resources/db/data.sql
```

由后端启动类 `DataInitializer` 幂等创建的演示账号：

| 入口 | app_id | 账号 | 密码 | 说明 |
| --- | --- | --- | --- | --- |
| 管理后台 | `_platform_` | `admin` | `admin123` | 平台超级管理员（跨租户） |
| 管理后台 | `biz_demo` | `admin` | `admin123` | 演示租户管理员 |
| 客服端 | `biz_demo` | `agent1` / `agent2` | `agent123` | 演示客服 |
| 用户端 | `biz_demo` | — | — | 经 `?app_id=biz_demo&token=任意串` 接入（演示 mock 据 token 生成 user_id） |

### 3. 启动后端

```bash
cd csm-service
mvn spring-boot:run
```

默认端口 `8081`（见 `application.yml` 的 `server.port`），上下文 `/`。健康检查：`GET http://localhost:8081/api/common/health`。

### 4. 启动前端（任选其一）

```bash
cd csm-admin && pnpm i && pnpm dev   # PC 管理端  http://localhost:5173
cd csm-agent && pnpm i && pnpm dev   # 客服端 H5  http://localhost:5174
cd csm-user  && pnpm i && pnpm dev   # 用户端 H5  http://localhost:5175
```

各前端通过 Vite 代理将 `/api`、`/ws` 转发到后端 `8081`。

> 未安装 pnpm 可改用 npm：`npm i && npm run dev`；生产构建 `npm run build`（含 `vue-tsc` 类型检查）。三个前端均为独立 Vite 工程，分别 `npm i`。

## API 路径约定

| 前缀 | 面向 |
| --- | --- |
| `/api/admin/**` | PC 管理端 |
| `/api/agent/**` | 客服端 H5 |
| `/api/h5/**` | 用户端 H5 |
| `/ws` | WebSocket（单连接，逻辑通道按消息 type 区分） |

统一响应体 `R<T>`：`{ code, msg, data }`，`code=0` 为成功。

## 租户隔离

- 除全局字典 `csm_menu` 外，所有业务表含 `app_id`；MyBatis-Plus `TenantLineInnerInterceptor` 自动追加 `app_id = ?`。
- 平台超管以请求头 `X-App-Id` 指定「当前选中租户」；跨租户只读统计走显式忽略路径。
- 平台级账号/角色使用保留 `app_id = '_platform_'`。

详见 `xuqiu.md` 2.6 与 `db-schema.md` 四章。
