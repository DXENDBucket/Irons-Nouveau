# 隐含形态的工具抄写模板

Iron & Nouveau 0.17.22，NeoForge 1.21.1 与 Forge 1.20.1 共享修改。

原 Iron 校验要求每个替换增强都依附已注册的弹射物形态，因而拒绝以替换开头的无形态编排。Ars 原生魔杖恰恰要求抄写模板不包含形态，并在抄写时添加“弹射物 → 加速”；附魔剑同样接收无形态模板，但补的是触摸。两种工具都应使用原生行为，不能在编辑阶段替玩家假定同一种形态。

`NativeCasting.validate` 对动作尚为空的首段只执行不依赖形态的检查：单个 Iron 替换和启用状态。Core 的跨提供者单载荷限制保持不变。工具补完后的编排依旧检查实际形态、载体、增强适用性及所在位置。Ars 的实际施法验证仍要求形态开头，无形态模板不会变成可直接施法的法术书编排。

只修改 Iron，不修改 Conflux 或其他替换提供者。现有法术数据、ID、费用、等级、吟唱、冷却与学习方式不变。

## 验证

隔离工作区：`parallel-workspaces/iron-implicit-forms/Irons-Nouveau`。固定 Core 提交 `15a20d6ec33c4dae8d154dc841c15b0d2403a908`，Core 受版本控制文件未修改。

针对性 GameTest 逐个检查所有已注册 Iron 替换：无形态编辑允许；无形态直接施法拒绝；调用各平台真实的原生魔杖抄写转换后允许；触摸剑转换后拒绝；来源模板不被修改。额外检查重复替换、延后检查不支持的穿透、等级增强，以及实际 Ars 施法生成原生 Iron 弹体。

Neo 使用原生 `scribeModifiedSpell`，Forge 使用原生 `setSpell`，没有模拟工具的配方补完代码。本次验证是服务端原生转换与生成检查，没有额外启动客户端人工操作书页或抄写台。

两版本各 1 项集中 GameTest 通过，随后完成 Neo 编译/打包和 Forge 重混淆打包。Forge 测试使用有真实魔力能力的测试玩家，避免将 Ars 5 的测试账户接口误用于 Ars 4。两个最终包及源码包已检查 MIT 许可、版本元数据和测试代码排除。

- build\libs\irons-nouveau-0.17.22-mc1.21.1-neoforge.jar SHA-256：`e2c3ec3cb299e58ee5cd1fcc8e5dea9a1cfb2140caf80f18df6311df64c4b5c0`。
- build\libs\irons-nouveau-0.17.22-sources.jar SHA-256：`de61f41cddb1e0a7eb1f495176b3f563ad0e05e4cb35acc18c751152935fa2c8`。
- forge\build\libs\irons-nouveau-0.17.22-mc1.20.1-forge.jar SHA-256：`8144b5c96d275f13cca09f97a1a3a3229cf069cf5b92d2bb4a7d98277c8611ce`。
- forge\build\libs\irons-nouveau-0.17.22-sources.jar SHA-256：`1576072ddaac2041684253c7a398a0a35e7695218ff8c245e0fb79d2bb5638ce`。

实现提交 `db239ee`，标签 `v0.17.22`。产物与源码包已复制主检出目录，并分别部署“万法归一”和“1.20.1-模组测试”；每个 Iron modid 唯一，部署哈希匹配。旧包备份在实例 `.codex-backups/iron-implicit-forms-20261013/`，Core 与其他附属不变。本任务测试进程已结束。没有上传或推送。
