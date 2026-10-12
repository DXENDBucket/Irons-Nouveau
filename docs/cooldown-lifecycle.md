# 并行与冷却生命周期（2026-10-13）

版本 0.17.20；独立工作树 `parallel-workspaces/cross-audit-irons-nouveau/Irons-Nouveau`，分支 `fix/cooldown-lifecycle`。固定 Core `21fb59af5ad4c1be96ed86e67b7dfc8fbc8bc59d`（0.1.23），源码副本只读。

绑定工具不会因持续吐息或非体术活动而拒绝施法；吟唱准备与冷却照常生效。

非体术效果允许并行；体术模拟仍可按实际执行实体替换。普通冷却、吟唱准备、资源支付及原生资格不因此取消。

客户端视觉与多人观察未人工验证。未推送或公开发布；构建、定向检查和哈希在本批收尾补充。

## 验证与产物

NeoForge / Forge 各 1 项绑定工具并行用例通过；原独占体术可替换，shared channel 和 ACTIVITY 均不阻止再次使用。日志 `lifecycle-tests.log` / `forge/lifecycle-tests.log`，Forge 发行包已重混淆。

- `irons-nouveau-0.17.20-mc1.21.1-neoforge.jar`：`f1e86cfa57f54d70cc8aed62f8fd88040678d975bb72fb9c26654c45c86edd1c`
- `irons-nouveau-0.17.20-mc1.20.1-forge.jar`：`1eafec8b297979e453b8a9cd4dea1e21ff1ed532361a5e59647833b0516bc793`
