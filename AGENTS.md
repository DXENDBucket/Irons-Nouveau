# Irons-Nouveau 工作约定

先读本仓库 `README.md`、`VERSION` 和构建配置，再读相邻 Core 的 [共享流程](../Ars-Conflux/docs/development-workflow.md)。非相邻检出目录按 `confluxCheckout`/`workspace.json` 定位；固定 Core 太旧没有该文档时，读取 `workflow_file` 或由协调者提供，不升级 Core。

- 本附属开发仅修改本任务指定的工作树。Conflux 源码、资源、版本及构建配置不在范围内，API 扩展需求交给独立 Core 任务。
- 并行任务使用独立附属和固定 Core worktree，避免共用构建/测试输出。不写共享人工测试实例，由协调者串行部署。
- 共享代码放 `common/`，平台差异留在各平台入口/适配。已有 Forge 的项目共享根目录 `VERSION`；不宣称没实现的平台也已支持。
- 保持 Iron 本体的施法语义、等级、资源和装备修饰；核心 API 不持有 Iron 类引用。当前 Forge 维护入口是本仓库 `forge/`，不是旧独立工作树。
- 使用原模组实际发布 JAR 核对行为，完整考虑服务端、客户端、伤害来源、费用、重复触发和清理。能力类别及排序复用 Core，不给其他附属逐对加兼容。
- 按变化选最小必要验证，收拢后测试；不要频繁重复全矩阵。未做客户端人工验证就明确标注。
- 只提交自己的文件。交接固定 Core SHA、提交、JAR 哈希、验证证据和未验证项，参考 [交接模板](../Ars-Conflux/docs/agent-handoff-template.md)。此文件不自动授权推送或发布。
