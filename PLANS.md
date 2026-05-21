# PLANS.md

本文件定义 SafeWalk 仓库内所有执行计划文档（ExecPlan）的编写与维护规则。ExecPlan 是一种可执行的实现说明：一个对本仓库完全陌生的人，只拿到当前工作树和某个计划文件，也应该能按文档把功能做出来、验证通过，并在中断后继续推进。

本文件补充官方 ExecPlan 规范，并与仓库根目录的 `AGENTS.md` 同时生效。如果两者有重叠，按“更具体的约束优先”处理；如果某个子目录未来有自己的 `AGENTS.md` 或 `AGENTS.override.md`，则还要同时遵守该目录中的更具体规则。

## 目标

编写 ExecPlan 时，要先说明用户能得到什么新能力、为什么这件事重要、以及如何亲眼看到它已经工作。计划不能只描述“改哪些代码”，必须描述“做完后怎样运行、会看到什么、怎么判断成功”。

每个 ExecPlan 都必须是活文档。实现过程中一旦有进展、发现、设计变更、阻塞、原型结论，都要回写到计划里。任何停止点都必须让下一个接手的人只靠该计划就能继续。

## SafeWalk 仓库上下文

每个 ExecPlan 都要把与当前任务相关的仓库上下文重新讲清楚，不能假设读者知道 SafeWalk 的结构。至少要根据任务覆盖以下事实中的相关部分：

- Android 客户端位于 `app/`，使用 Gradle Kotlin DSL。当前 Android 启动入口是 `app/src/main/java/com/celestine/safewalk/MainActivity.kt`，这里现在使用的是 Compose UI。
- `app/src/main/res/layout/activity_main.xml` 仍然存在，但它不是当前默认入口。除非任务明确要求同步两套实现，否则计划不能把 Compose 与 XML 都当成需要同时修改的对象。
- Android namespace 和 application id 当前都是 `com.celestine.safewalk`。除非任务明确要求，计划不应安排重命名。
- 后端位于 `backend/`，应用入口是 `backend/main.py`，使用 FastAPI。主要分层是 router -> service -> data/model，对应 `backend/app/api/`、`backend/app/services/`、`backend/app/data/` 与 `backend/app/models/`。
- 后端需要 Python 3.9+，环境变量来自 `backend/.env`，模板是 `backend/.env.example`。
- 如果一个任务同时涉及 Android 与 backend，计划必须明确写出接口、请求字段、响应字段、错误处理或数据契约是否变化；如果没有变化，也要明确写“无接口契约变更”。

不要只复述历史文档。仓库里的分析文档可能落后于代码；计划作者必须重新阅读当前源码，再把必要背景写进计划。

## 不可妥协的要求

每个 ExecPlan 都必须满足以下要求：

- 自包含。不能依赖“之前讨论过”“看另一篇文档就知道”“照旧处理”这类隐含上下文。
- 面向新手。出现术语时，必须立刻用普通语言解释它在这个仓库里对应什么文件、命令或行为。
- 结果可观察。完成后必须能通过运行命令、打开界面、访问接口、执行测试等方式证明行为真的生效。
- 文档持续更新。`Progress`、`Surprises & Discoveries`、`Decision Log`、`Outcomes & Retrospective` 四个章节必须一直保持最新状态。
- 以最小改动达成目标。计划不应顺手安排与任务无关的大范围重构，也不应要求修改生成物或本地环境文件，除非任务本身就是处理这些内容。
- 不依赖外部博客或未嵌入的参考资料。需要的背景知识要直接写进计划。

## 推荐的文件位置与命名

ExecPlan 推荐放在 `Docs/ExecPlans/` 下，文件名使用 `YYYY-MM-DD-<short-slug>.md`。如果用户明确指定了别的位置或文件名，按用户要求执行。

无论文件放在哪里，计划开头都应明确写出它必须遵守仓库根目录的 `PLANS.md`，并在正文中根据任务引用仓库根目录的 `AGENTS.md` 约束。

## 格式规则

为了与官方规范保持一致，ExecPlan 的章节标题建议使用官方英文原名，正文可以写中文。这样既便于机器代理识别，也不影响团队阅读。

当你在聊天、评审说明或其他文档中展示一个 ExecPlan 时，整个计划必须包在一个单独的 `md` fenced code block 里，且内部不能再嵌套三反引号代码块。需要展示命令、日志、diff、代码片段时，使用缩进块。

当某个 Markdown 文件的全部内容就是那个 ExecPlan 时，不要在文件外层再加三反引号，直接写 Markdown 正文即可。

所有 ExecPlan 都应遵守以下格式约束：

- 标题清晰、动作导向，例如“Add backend health diagnostics page”而不是“Some notes”。
- 每个标题后空两行，再写正文。
- 叙述以 prose 为主，优先用完整句子讲清楚意图、步骤和结果。
- 除 `Progress` 外，避免把正文写成只有清单的形式。只有在不列点会明显变得难懂时才使用简短列表。
- `Progress` 章节必须使用复选框列表，并在每项前写时间戳，推荐 UTC，例如 `2026-03-25 09:30Z`。
- 不能在计划里写“见提交记录”或“看 PR 评论”。关键背景、决策和证据必须直接存在计划里。

## 每个 ExecPlan 必须包含的章节

以下章节是强制性的，标题建议与官方骨架保持一致：

- `## Purpose / Big Picture`
- `## Progress`
- `## Surprises & Discoveries`
- `## Decision Log`
- `## Outcomes & Retrospective`
- `## Context and Orientation`
- `## Plan of Work`
- `## Concrete Steps`
- `## Validation and Acceptance`
- `## Idempotence and Recovery`
- `## Artifacts and Notes`
- `## Interfaces and Dependencies`

如果任务足够复杂，可以加入 `## Milestone 1`, `## Milestone 2` 之类的里程碑章节，但它们是补充，不是对上述必选章节的替代。

## 各章节在 SafeWalk 中应如何写

### Purpose / Big Picture

先从用户视角解释价值。要回答：做完以后，Android 用户、后端调用方或项目维护者能做什么以前做不到的事？然后写出最直接的观察方式，例如“启动服务后访问 `/health` 能看到新增字段”或“安装 debug 包后进入主界面能看到新的安全状态卡片”。

### Progress

这是唯一必须使用 checklist 的章节。它不是待办列表，而是“真实状态面板”。每次停下工作时都要更新，明确哪些已经完成，哪些还没完成，哪些部分完成。

进度项必须写成可核对的动作，例如：

    - [x] (2026-03-25 09:30Z) Read current Android entry point and backend startup flow.
    - [ ] Define the API contract for emergency contact sync.
    - [ ] Partially completed: backend endpoint added; Android caller and validation still pending.

### Surprises & Discoveries

记录意料之外但会影响实现的事实，并附上简短证据。例如发现当前 Android 入口其实不是 XML，或发现 backend 启动时会自动建表，或发现没有现成后端测试可跑。证据可以是简短日志、接口响应、测试输出或源码定位说明。

### Decision Log

所有关键决策都要记录，格式固定为：

    - Decision: …
      Rationale: …
      Date/Author: …

如果中途改方案，要新增一条决策，而不是偷偷覆盖旧结论。决策日志的目标是让后来者知道“为什么这样做”，不是只知道“最后变成了什么”。

### Outcomes & Retrospective

在完成一个大里程碑或整个计划时更新。要总结实际实现了什么、还剩什么、哪些假设被证实或被推翻、下一次类似工作要注意什么。

### Context and Orientation

把读者当成第一次打开 SafeWalk 仓库的人。需要明确写出相关文件的仓库相对路径、入口函数、分层关系，以及任务会落在哪一层。

对于本仓库，常见上下文应尽量具体，例如：

- Android UI 改动通常从 `app/src/main/java/com/celestine/safewalk/MainActivity.kt` 的 Compose 组件开始描述。
- Android 资源改动要说明是否只影响 `app/src/main/res/values/` 或 `drawable/`，以及是否会影响当前 Compose 入口。
- backend 路由改动要指出对应 router 文件、service 文件、repository 或 model 文件分别在哪。
- 若任务与数据库相关，要说明 `backend/main.py` 启动时会调用 `create_tables()` 自动创建表，这会影响本地验证方式。

### Plan of Work

按叙述顺序说明要改什么文件、在什么函数或模块附近改、为什么这样改。写法要具体到“在哪个文件的哪个位置插入/调整什么逻辑”，但不要变成大段粘贴代码。

计划应优先安排小而可验证的增量步骤。对于风险较高的工作，可以先安排原型型里程碑，用一个可运行的小实验确认方向，再推进正式实现。

### Concrete Steps

列出精确命令，并写明工作目录。命令必须可直接执行，不能只写“运行测试”或“启动应用”。

对本仓库常见命令的表达方式应类似下面这样：

    在仓库根目录 `/Users/xiaiguo/AndroidStudioProjects/SafeWalk` 运行：
      ./gradlew assembleDebug

    在仓库根目录 `/Users/xiaiguo/AndroidStudioProjects/SafeWalk` 运行：
      ./gradlew test

    在仓库根目录 `/Users/xiaiguo/AndroidStudioProjects/SafeWalk` 运行：
      uvicorn backend.main:app --reload

如果命令依赖前置条件，例如 Android SDK、模拟器、数据库、`backend/.env`，必须在计划里写清楚。如果环境不完整，要写替代验证方案。

### Validation and Acceptance

这是计划最关键的章节之一。验收必须描述为“人能观察到的行为”，而不是内部实现细节。

针对 SafeWalk，常见的验收写法应覆盖以下类型：

- Android 变更：从仓库根目录运行 `./gradlew assembleDebug`；如果任务涉及逻辑，再运行 `./gradlew test`；如果任务涉及设备交互，再根据需要运行 `./gradlew connectedAndroidTest`。除了构建成功外，还要写出安装并打开应用后应看到什么。
- backend 变更：优先运行最相关的测试。如果仓库中仍然没有对应测试，至少提供导入校验、启动校验或 HTTP 访问校验。当前仓库里没有现成的 backend 测试目录，因此计划通常需要写出如何启动 `uvicorn backend.main:app --reload`，以及访问 `http://127.0.0.1:8000/health` 或其他接口后应看到的 JSON 响应。
- 跨端变更：同时验证 backend 接口行为和 Android 端的调用结果，并明确请求字段、响应字段、空值处理、失败提示是否发生变化。

如果某项验证因为环境缺失无法执行，计划必须要求记录真实阻塞原因，不能把“未执行”写成“已通过”。

### Idempotence and Recovery

说明计划步骤重复执行是否安全，以及失败后如何恢复。SafeWalk 中常见的恢复说明包括：

- `./gradlew assembleDebug`、`./gradlew test`、`uvicorn backend.main:app --reload` 都是可重复执行的。
- 如果 backend 依赖的 `.env` 不存在，可从 `backend/.env.example` 重新复制生成。
- 如果某一步会写数据库或修改本地配置，计划必须提前写出如何备份、如何回滚、以及如何避免污染开发环境。

### Artifacts and Notes

只保留最能证明结论的简短证据，例如一段 `/health` 响应、一个关键测试通过输出、一个非常小的 diff 摘要。不要把整份长日志贴进去。

### Interfaces and Dependencies

要明确最终必须存在的接口、类型、函数签名、路由路径、请求响应结构，以及为什么使用现有模块而不是新建大块架构。除非任务明确要求，否则计划不应默认引入新依赖。

如果任务涉及 Android 和 backend 的联动，这一节必须把契约写完整，例如：

- Android 发给 backend 的 URL、HTTP 方法、JSON 字段名。
- backend 返回的状态码和 JSON 结构。
- 失败时 Android 如何展示，是否复用现有中文文案。

## SafeWalk 专项约束

所有 ExecPlan 都必须显式遵守以下仓库特定规则：

- 优先最小改动，不把顺手重构写进计划。
- 保留现有命名、包结构和面向用户的中文文案，除非任务明确要求改写。
- 不要把 `app/build/`、`.gradle/`、`.kotlin/`、`.idea/`、`local.properties` 这类生成物或本地环境文件写进计划的实施步骤，除非任务本身就是处理这些内容。
- Android 相关命令从仓库根目录运行，而不是进入 `app/` 后运行。
- backend 仍需保持现有分层：router -> service -> data/model。计划不应直接跨层塞逻辑而不说明原因。
- 如果计划修改配置、数据库模型或 API 契约，必须同时安排文档更新步骤，或者在验收和回顾里明确写出需要的手动迁移与后续动作。

## 里程碑写法

复杂任务建议拆成可独立验证的里程碑。每个里程碑都要讲清楚“本阶段结束后会多出什么能力、要运行什么命令、应该看到什么结果”。不要把里程碑写成纯管理动作，例如“完成开发”“联调一下”；它们必须对应真实的可运行成果。

对于高风险探索，可以加入明确标注为“prototyping”的里程碑，先做一个范围可控的原型验证可行性，再决定是否纳入正式实现。原型也必须可运行、可观察，并在 `Decision Log` 中说明是否保留或废弃。

## 修订要求

每次修改一个 ExecPlan，都要同步检查整份文档是否仍然自包含，不能只改一个小段而让其他章节过期。特别是当设计改变时，至少要同时更新：

- `Progress`
- `Decision Log`
- `Plan of Work`
- `Concrete Steps`
- `Validation and Acceptance`
- 文末修订说明

每次修订后，都要在计划底部追加一条简短说明，写明“改了什么、为什么改”。这条说明的作用是让后来者快速理解本次更新的意图。

## 推荐骨架

下面是 SafeWalk 仓库推荐使用的 ExecPlan 骨架。标题建议保持不变，正文可按任务改写：

    # <Short, action-oriented description>

    This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

    This document must be maintained in accordance with `/PLANS.md` and `/AGENTS.md` from the repository root.

    ## Purpose / Big Picture

    先说明用户会得到什么新能力，以及如何亲眼看到它工作。

    ## Progress

    - [ ] 用带时间戳的复选框持续记录真实进度。

    ## Surprises & Discoveries

    - Observation: …
      Evidence: …

    ## Decision Log

    - Decision: …
      Rationale: …
      Date/Author: …

    ## Outcomes & Retrospective

    总结阶段结果、剩余问题与经验。

    ## Context and Orientation

    用仓库相对路径解释当前相关模块和入口。

    ## Plan of Work

    用 prose 说明要改哪些文件、为什么这样安排。

    ## Concrete Steps

    写清楚工作目录、命令、预期输出。

    ## Validation and Acceptance

    写清楚如何证明行为生效，而不是只证明代码存在。

    ## Idempotence and Recovery

    说明如何安全重试、如何回滚或恢复环境。

    ## Artifacts and Notes

    放最小但足够的证据片段。

    ## Interfaces and Dependencies

    写出最终必须存在的接口、函数签名、依赖和契约。

## 最后检查

在提交一个 ExecPlan 前，作者应自行确认以下问题的答案都是“是”：

- 只看这一个计划文件和当前工作树，新手能否从零开始做完？
- 计划是否明确写出了用户可见的结果，而不是只写内部改动？
- 所有命令是否给出了工作目录、前置条件和成功标志？
- 当前仓库的真实入口、分层和验证方式是否已经写清，而不是沿用过时描述？
- 如果任务跨 Android 与 backend，接口契约是否已经写清？
- 计划是否要求在每个停顿点更新 `Progress`、`Decision Log`、`Surprises & Discoveries` 和 `Outcomes & Retrospective`？

如果上述任一问题答案是否定的，就先修计划，再开始实现。
