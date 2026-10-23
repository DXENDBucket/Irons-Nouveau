# Ars 'n Spells 共装提示：0.17.24

2026-10-23，用户授权加入新进入世界提示并发布 CurseForge，NeoForge 1.21.1 与 Forge 1.20.1 共用版本0.17.24。

## 行为

两个平台在已有服务端 PlayerLoggedInEvent 中检测 modid `ars_n_spells`，调用同一个 common 提示实现。不引用 ANS 类、不读取其配置、不添加硬依赖。未装 ANS 时不发消息，也不写已读标记；以后安装仍可收到提示。

每个玩家在每个世界仅提示一次。已读标记 `irons_nouveau:ans_overlap_notice_v1` 放在 Forge/NeoForge 跨死亡保留的 PlayerPersisted 内，随该世界的玩家存档保存；重连、死亡复活与换维度不重复。已有世界升级后第一次登录也会显示。三个服务端聊天组件由客户端按其语言显示，提供英文和简体中文。

文本明确区分版本：ANS3.4.0起的 Iron 魔符存在部分功能重叠，旧版本可忽略。若选择保留本模组的 Iron 魔符，可由服务器管理员在 `<world>/serverconfig/ars_n_spells-server.toml` 的 `["Imported Iron's Glyphs"]` 下设置 `enable_iron_glyphs = false` 和 `enable_glyph_press = false`，仍可保留 ANS 的魔力/属性联动。本模组只提供提示，不强制选择实现，也不自动改动任何配置。

配置名和版本来源：ANS官方 [3.4.0配置文档](https://github.com/otectus/ars-n-spells/blob/main/docs/3.4.0/config-reference.md) 与 [发行说明](https://www.curseforge.com/minecraft/mc-mods/ars-n-spells/files/9280543)。本地现有 ANS3.3.5 JAR 元数据确认 modid 为 `ars_n_spells`。

## 隔离与验证

工作树 `C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/iron-ans-notice-01724-20261023/Irons-Nouveau`，分支 `feature/ans-overlap-notice`，起点 `a64250a370eb5d2d8cbbd6a0d6bf9f24e208c41a`。Core固定 `ed44ebb86bd087f3546a1bc6a315d2692f70262c`，受跟踪文件只读，不修改或升级共享Core。

本次仅登录消息与已读标记，收拢后验证两版编译、资源处理、Neo JAR、Forge重混淆Jar-in-Jar与源码包，静态检查两端事件接线、翻译键和可选依赖边界，不重跑战斗GameTest。没有实际客户端登录/死亡重连人工测试；持久化语义沿用加载器的 PlayerPersisted。构建与发布结果记在本文件后续段落。

验证完成：NeoForge编译/JAR/源码包与Forge编译/重混淆Jar-in-Jar/源码包全部通过。核对发行包中的公共提示类、两种语言与MIT许可；没有新增ANS硬依赖或打包ANS资源/实现。Forge内嵌MixinExtras范围仍是 `[0.4.1,)`。哈希见 `ans-coexistence-build.json`。
