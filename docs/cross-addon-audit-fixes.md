# 横向审计修复（2026-10-13）

版本 0.17.19；固定 Core 0.1.22 / `a8a2e26392cd95532375b7e23b90e9740bf051b1`。工作树 `parallel-workspaces/cross-audit-irons-nouveau/Irons-Nouveau`，分支 `fix/cross-addon-audit`。Core 副本受版本控制文件只读。

局部 Iron 资源账户不再污染同编排的其他附属；混合吟唱按最严格倍率合成移动限制；冷却反馈交给 Core。

本次保留原生冷却存储、创造模式策略和能力范围。Hex 主动入口可通过 Core 的两个独立配置控制吟唱和冷却；资源支付不因关闭这些配置而改变。

客户端画面、移动手感及多人观察未人工验收。构建/定向验证与产物哈希在完成后补充。没有推送或公开发布。

## 验证

NeoForge 最终发行包和源码包已构建，日志 `audit-pack-neo.log`。 同批定向服务端用例 3 项通过，日志 `audit-final-neo.log`。 分池验证使用 `-Pstandalone=true`，不加载会合并魔力池的 Ars ’n Spells。

## 发行包 SHA-256

- `irons-nouveau-0.17.19-mc1.21.1-neoforge.jar`：`26aee5471b9e6e5346d901642d0a80be92df24e80d39f08cb196c8fd77d5ef4a`
- `irons-nouveau-0.17.19-mc1.20.1-forge.jar`：`670c18d5acb3e363f1015d6de77c153cfbc45775fa23d65152528d352d1c9131`

两端发行包及源码包许可和打包内容已检查。

Forge 原生分池/外部账户用例 1 项通过，日志 `forge/audit-account-forge.log`。发行包已重混淆；Core 与 Iron 的实际 JarJarSelector 检查通过，同时验证最低 0.4.1 和其他模组提供 0.5.5。
