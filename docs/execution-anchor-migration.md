# 吐息与血吸光束执行锚点迁移

0.17.17，依赖Conflux 0.1.20。BreathPose仍保留原公开数据和客户端偏移协议，采样与有效性检查委托Core ExecutionAnchor；实体的眼部偏移、原生范围、法强、等级、计费节拍、固定时长和玩家移动限制仍由Iron适配负责。

实体锚点使用统一Allies关系，只授权原施法者的自身／友军，不因伤害归属变化取得控制实体的权限。生命、旁观者、维度与友军关系持续复查；丢失关系停止并清理。位置命中保持固定位置和入射方向，不能把MISS视为位置。

实体吐息和血吸光束使用共享CHANNELING声明，实际喷吐者占用该通道，原付款者不被错误标记；多个持续输出保留并存。位置输出无实体声明。原有其他驱动的活动标记保持非独占兼容语义。移动限制仍只给实际玩家执行者，不新增生物减速。

RayGeometry按真实start和方块裁剪后的end圈选实体，复用Iron Utils.checkEntityIntersecting。血吸服务端、客户端以及冰霜射线使用同一查询；固定位置不借用观察玩家的碰撞箱或排除观察玩家来决定端点。Forge的空实体ClipContext合法，NeoForge则显式使用CollisionContext.empty，差异留在RayClips平台类。没有创建假实体或移动施法者来配合射线。

## 验证

- NeoForge `build-anchor-migration-final.log`：13项required通过，包括远端血吸伤害及单次收费、独立伤害归属、provider-only友军与解除关系、固定点及墙体、真实NEG改自身／脚下和原生Ars延迟，以及既有吐息／射线回归。
- Forge `forge/build-anchor-migration.log`：9项required通过；最后Core兼容基线下 `forge/build-anchor-release-final.log` 又验证4项锚点／授权用例。没有强行将未安装NEG的Forge称为已验证NEG改形态。
- 第一轮发现NeoForge空实体ClipContext不可用，改为平台入口后通过；旧玩家头部吐息fixture漏写友军关系，明确登记后继续验证四种吐息的头部／蹲伏位置。没有放宽运行时授权规则来使测试通过。
- 灾变的独立可选共存用例加载真实Iron 0.17.17，验证同一友军可同时接受Iron吐息与剑舞、由共同Core分通道记录、解除友军后双方及原生视觉均清理。
- 本批没有启动图形客户端。客户端查询与服务端共用几何，并经过Java编译及既有渲染注入点检查；实际画面仍需人工观察。

此迁移不改变原生法术的Mana、冷却、吟唱或等级，不重做其他所有能力的锚点语义。Forge 0.17.16的渲染注入修复继续保留。
