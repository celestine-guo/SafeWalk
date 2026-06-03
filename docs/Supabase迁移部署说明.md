# SafeWalk Supabase 迁移部署说明

## 1. Supabase 准备

1. Supabase 项目：`safewalk-prod`
   - Project ID / ref：`zmaxcsvwmeboegezsmxe`
   - Project URL：`https://zmaxcsvwmeboegezsmxe.supabase.co`
   - Region：`ap-southeast-1`
2. 当前迁移保留 **FastAPI + Clerk + Supabase PostgreSQL** 架构：Android 不直接访问 Supabase 业务表，Android 继续调用公网 FastAPI，FastAPI 校验 Clerk 会话并写入 Supabase 数据库。
3. 打开 SQL Editor，按顺序执行：
   - `backend/sql/supabase_schema.sql`
   - `backend/sql/supabase_security.sql`
4. 在 **Connect** 面板获取 Session Pooler 连接串（`5432`）。

## 2. 后端环境变量

在仓库根目录创建 `backend/.env`（可从 `backend/.env.example` 复制），至少配置：

- `DATABASE_URL=postgresql+asyncpg://...pooler.supabase.com:5432/postgres?ssl=require`
- `CLERK_SECRET_KEY=...`
- `CLERK_JWT_KEY=...`（可选）
- `CLERK_AUTHORIZED_PARTIES=[...]`（可选）
- `CLERK_PUBLISHABLE_KEY=...`（**Android 构建配置使用**，非 backend 运行时必需）

## 3. 启动后端

前置条件（在仓库根目录执行）：

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r backend/requirements.txt
```

启动命令（仍在仓库根目录）：

```bash
uvicorn backend.main:app --reload
```

## 4. Render 托管后端

仓库根目录的 `render.yaml` 已配置 Render Blueprint。创建 Blueprint 时使用：

- Service name：`safewalk-backend`
- Runtime：Python
- Region：Singapore
- Root Directory：留空，使用仓库根目录
- Build Command：`pip install -r backend/requirements.txt`
- Start Command：`uvicorn backend.main:app --host 0.0.0.0 --port $PORT`
- Health Check Path：`/health`

Render 会在创建 Blueprint 时提示填写 `sync: false` 的 secret 环境变量。至少需要：

- `DATABASE_URL`
- `CLERK_SECRET_KEY`
- `CLERK_PUBLISHABLE_KEY`

可选：

- `CLERK_JWT_KEY`
- `QWEN_API_KEY`

部署成功后先访问：

```text
https://<render-service>.onrender.com/health
```

看到 `{"status":"healthy","service":"SafeWalk Backend"}` 后，再更新 Android 的后端地址。

## 5. Android API 地址配置

`SAFEWALK_BACKEND_BASE_URL` 需指向后端 API 前缀（包含 `/api`）。真机体验和分发版本必须使用公网 HTTPS 后端地址，不能使用 `localhost`、`127.0.0.1` 或只在模拟器可用的 `10.0.2.2`。

例如：

- 模拟器：`http://10.0.2.2:8000/api`
- 真机同网段：`http://<你的电脑局域网IP>:8000/api`
- 真机分发：`https://<你的后端域名>/api`
- Render：`https://<render-service>.onrender.com/api`

可通过以下任一方式注入：

1. `local.properties`
2. 环境变量 `SAFEWALK_BACKEND_BASE_URL`
3. Gradle 参数 `-PSAFEWALK_BACKEND_BASE_URL=...`

## 6. 当前接口契约增量（仅本次迁移变更）

以下仅列出相对迁移前的差异，未列出的接口契约保持不变：
- 全部业务接口需要 `Authorization: Bearer <clerk_session_token>`
- `POST /api/trips/` 不再接受 `user_id`
- `POST /api/emergencies/` 不再接受 `user_id`
- 新增：
  - `GET/POST /api/guardians/`
  - `GET/POST /api/chats/`（`GET` 为 `/api/chats/trip/{trip_id}`）
  - `GET/POST /api/trip-events/`

## 7. 说明

- 这次迁移目标是“切到 Supabase PostgreSQL 云端库 + Clerk 鉴权闭环”。
- Android 端不需要 Supabase `service_role` key；当前架构下也不需要 Supabase publishable key。业务数据权限由 FastAPI + Clerk + 数据库最小权限/RLS 基线共同保护。
- 未提供自动化的 SQLite 历史数据搬迁脚本；如需保留历史本地数据，请单独执行一次性导入流程。

## 8. 发布与回滚操作（Playbook）

### 8.1 发布顺序

1. **先执行 Supabase SQL**：在 SQL Editor 按顺序执行 `backend/sql/supabase_schema.sql` 与 `backend/sql/supabase_security.sql`，确认无报错。
2. **再部署 backend**：发布后端服务，并用一次健康检查 + 关键接口冒烟（如 trip/emergency 写入）确认可用。
3. **最后发布 Android**：在后端稳定后再发版 Android，避免客户端先于服务端协议生效。

### 8.2 回滚策略

1. **Backend 回滚**：保留上一个稳定版本镜像/构建；出现异常时优先回滚后端版本并恢复上一版路由逻辑。
2. **数据库回滚策略**：不做破坏性数据回滚（不直接删表/删列回退历史数据）；优先使用功能开关、路由下线或权限收紧作为兜底。
3. **Android 回滚**：通过分阶段发布暂停、下架问题版本，必要时回退到上一稳定包；同时保留服务端兼容窗口，避免旧包不可用。

### 8.3 监控清单（发布后重点观察）

- **5xx ratio（5 分钟窗口）**：发布后前 30 分钟每 5 分钟观察一次，目标 `< 1%`；连续 2 个窗口 `>= 1%` 立即排查/回滚。
- **DB connections（1 分钟窗口）**：连接使用率应 `< 80%`；连续 10 分钟 `>= 80%` 先限流并排查慢查询/连接泄漏。
- **trip/location/emergency 写入成功率（5 分钟窗口）**：三类接口成功率应 `>= 99%`；任一接口连续 2 个窗口 `< 99%` 触发告警。
- **auth 401 ratio（5 分钟窗口）**：发布后前 30 分钟目标 `< 5%`；若 `>= 5%` 且持续 10 分钟，优先检查 token 传递与 Clerk 配置。
