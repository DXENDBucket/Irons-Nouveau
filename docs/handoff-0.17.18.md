# 身体行动期间的绑定武器入口

2026-10-13，Iron0.17.18，代码 `20f5a19e55f7c351f06334be51fba0b75f951261`，本地标签 `v0.17.18`，工作树 `parallel-workspaces/iron-action-interruption/Irons-Nouveau`。固定Core `884fd49b81b8d30e56432cbdef93060705bc1613`（0.1.21），受控副本干净；两平台最低Core升为0.1.21。

绑定武器忽略单纯的独占ENTITY_ACTION忙碌标记，让真正触发的效果处理动作交接；吟唱、共享引导和其他活动仍限制该入口。Iron旧CastExecution的活动标记同步使用Core ACTIVITY，资源、权限、原生冷却与持续法术不改。本附属不依赖灾变，也不自行取消灾变动作。

- Neo `build-body-entry.log`、Forge `forge/build-body-entry-forge.log`：各5项required通过，覆盖身体动作入口、其他活动／引导仍忙碌、实际执行者、原有受益目标和连发冷却规则。两平台发行与源码构建成功。
- 灾变加载真实本包后，反射调用绑定API，以绑定铁剑释放自身→跃砸，旧剑舞被关闭、新动作实际开始；共享引导仍拦住入口且保留会话。仅测试运行时引用，无附属之间硬依赖。
- Neo二进制SHA256 `86dedf964517073d90e8617a771afdf766ba9cf0c560ac9f1d07c48e0a426f45`；源码 `39136490955303ad7356ca1180765d97272772a10d6098aa8876fef413187b76`。
- Forge二进制SHA256 `e082067bae15a3bd1a10f65b0001b279276d3940593dbf5820347cbbf90ed9fa`；源码 `ca703b96a58f7a38e8fbe15dc8cdc93936138619939199b89c1583b5cbfa84a4`。
- 许可、测试类排除、最低依赖与Forge Jar-in-Jar范围检查通过；未改已有原生射线渲染注入。产物在build/libs及forge/build/libs。
- 串行部署万法归一及1.20.1测试实例，旧包位于各实例 `mod-backups/2026-10-13-action-interruption`，复制哈希一致。没有启动图形客户端，未推送或发布。
