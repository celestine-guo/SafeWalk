# AGENTS.md

## 目标

这个文件为 Codex 提供 SafeWalk 仓库的项目级协作约定。请保持指令简洁、准确、可验证，优先做小而明确的改动，不要顺手做大范围重构。

## 仓库结构

- `app/`：Android 客户端，使用 Gradle Kotlin DSL。
- `app/src/main/java/com/celestine/safewalk/MainActivity.kt`：当前 Android 入口。
- `backend/`：FastAPI 后端服务。
- `backend/main.py`：后端应用入口。
- `backend/app/api/`：路由与中间件。
- `backend/app/services/`：服务层。
- `backend/app/data/`：数据库、ORM 模型、仓储层。
- `backend/app/models/`：领域模型与共享模型。
- `Docs/`：项目文档。

## 协作约定

- 用最小改动完成任务。
- 除非任务明确要求改写，否则保留现有命名、包结构和面向用户的中文文案。
- 不要修改生成物或本地环境文件，例如 `app/build/`、`.gradle/`、`.kotlin/`、`.idea/`、`local.properties`，除非任务本身就是处理这些内容。
- 除非任务明确需要，否则不要新增依赖或调整整体架构。
- 如果改动同时涉及 Android 和 backend，最终说明里要明确接口或数据契约是否受影响。

## Android 规则

- 从仓库根目录运行 Gradle。
- 推荐验证命令：
  - `./gradlew assembleDebug`
  - `./gradlew test`
  - `./gradlew connectedAndroidTest`
- 除非用户明确要求重命名，否则保持 Android namespace 和 application id 为 `com.celestine.safewalk`。
- 当前启动路径是 `MainActivity.kt` 中的 Compose UI。仓库里虽然有 `app/src/main/res/layout/activity_main.xml`，但它不是当前入口，不要默认 Compose 和 XML 两套实现同时生效。只有任务明确要求同步时才同时改两边。
- 修改 UI 时优先做局部、渐进式调整，不要顺手整体换风格。

## Backend 规则

- backend 需要 Python 3.9+。
- 必要环境变量来自 `backend/.env`，可用 `backend/.env.example` 作为模板。
- 推荐初始化流程：
  - `cp backend/.env.example backend/.env`
  - `pip install -r backend/requirements.txt`
- 推荐在仓库根目录启动：
  - `uvicorn backend.main:app --reload`
- 保持现有分层结构：router -> service -> data/model。
- 如果修改配置、数据库模型或 API 契约，要同步更新相关文档，或在最终说明里明确迁移/手动操作步骤。

## 测试与验证

- 始终运行与你改动最相关、范围最小的检查。
- 最低验证要求：
  - Android 代码或资源改动：条件允许时运行 `./gradlew assembleDebug`。
  - Android 逻辑改动：相关时再运行 `./gradlew test`。
  - Backend 改动：如果已有测试就跑针对性测试；如果没有，就做当前环境下最合适的导入或启动校验。
- 如果因为依赖、SDK、数据库或环境变量缺失而无法运行命令，要明确说明，不要把未执行的检查写成已通过。

## 完成标准

- 请求的行为已经实现。
- 只修改了与任务相关的文件。
- 没有手动编辑生成文件。
- 已运行相关检查，或已清楚说明阻塞原因。
- 最终说明应包含：
  - 改了什么
  - 验证了什么
  - 还有哪些假设、环境缺口或后续事项

## 后续扩展

- 如果某个子目录后续形成自己的工作流，应在该目录下新增更具体的 `AGENTS.md` 或 `AGENTS.override.md`，而不是持续把根目录文件写得越来越大。
