# 更新日志

> 这是整个仓库的更新日志

## 2.2.0-26.2（2026-09-19）

> 对比基线：2.1.0-26.2（6f0f202，新功能：内裤分类和真正的内裤槽）。涵盖 14 种机制内裤移植、耐久损耗、空手右键事件修复、史莱姆弹跳重构、气态内裤重力方案切换等。

### 新功能

- **14 种机制内裤（含独立材质、模型、配方、tooltip）**：龙首、守护带、史莱姆、辣条、泥沼、蠹虫、触手、可食用、活版门、晋升、粘性活塞、气态、剑型、盾牌。
  - 龙首：置身火/岩浆蓄能，满格时下次命中引爆；免疫火焰。
  - 守护带：12 秒未受伤叠一层守护，受击时消耗一层完全格挡。
  - 史莱姆：蹲下蓄力弹簧（spring 0~20），松开自动弹起（复刻原版跳跃提升药水加成公式，不显示药水 HUD）；免摔。
  - 辣条：辣度蓄能，越高攻击越烫；满格空手右键喷火。
  - 泥沼：周身恶臭光环使敌人中毒减速，坠落溅射。
  - 蠹虫：受击时几率召唤护卫反击攻击者。
  - 触手：水下呼吸；水下攻击把敌人拽近并减速。
  - 可食用：饥饿时自动进食积攒饱腹，空手右键释放回血。
  - 活版门：蹲下站定隐身藏身，潜行攻击造成背刺致盲。
  - 晋升：击杀累积军阶，满阶晋升：爆发增益并击退四周。
  - 粘性活塞：攻击强力击退，物品自动吸附；受击活塞击退攻击者。
  - 气态：重力钳到 0.01 复刻原版缓降药水（Mixin `getEffectiveGravity`，不显示药水 HUD）。
  - 剑型：极高的攻击伤害，命中几率触发横扫群敌。
  - 盾牌：潜行举盾大幅减伤并几率弹反，潜行攻击为盾击（`@ModifyVariable hurtServer` 改 damage）。
- **机制引擎**（`BriefsMechanic.java`）：集中处理 tick 蓄能、受伤格挡/减伤、坠落取消/溅射、攻击附加效果、击杀经验加成、空手右键主动技能。
- **内裤耐久损耗**：20 件自定义内裤栏装备在受伤时按原版盔甲损耗规则（每 4 伤害损 1 耐久，最低 1）自动磨损，耐久耗尽销毁并清空内裤栏 + 刷新属性 + S2C 同步。皮革内裤仍由原版 `hurtArmor` 自动损耗。
- **耐久耗尽销毁音效**：播放原版 `GENERIC_EXPLODE` 并清空玩家实体中的 CUSTOM_DATA 内裤栈。

### 修复

- **合成失败根因修复**（共 3 个叠加因素）：
  1. **配方目录错误**：26.2 配方数据包目录从 `recipes/`（复数）改为 `recipe/`（单数），旧配方在 `recipes/` 下被加载器静默忽略。全部 22 个配方已移至 `recipe/`。
  2. **配方 key 格式错误**：旧版 `{"X":{"id":"minecraft:diamond"}}` 对象格式，26.2 只认字符串 `"X":"minecraft:diamond"`。全部配方 key 已统一为字符串格式。
  3. **BOM 问题**：PowerShell `-Encoding UTF8` 写入带 BOM 前缀的配方 JSON，Gson 解析失败。已扫描并去除全部 35 个文件 BOM。
- **空手右键主动技能不生效**：Fabric `UseItemCallback` 只注入原版 `ServerPlayerGameMode.useItem`，而空手右键走的是 `useItemOn` 链路。改用 `UseBlockCallback.EVENT`（注入 `useItemOn` HEAD），对所有右键（含空手瞄准空气）都触发，守卫 `!mainHand.isEmpty()` 只让空手触发。
- **史莱姆弹跳三次重构**：
  1. 最初方案 `END_SERVER_TICK` 里 `setDeltaMovement` 太晚，玩家 gravity 已先作用。
  2. 改用 `START_SERVER_TICK` flag 兑现，但仍有客户端预测覆盖问题。
  3. 最终方案：Mixin `travel HEAD` 触发，复刻原版 `jumpFromGround()` + 手动 `addDeltaMovement(0, 0.1F*spring, 0)` 加分（跳跃提升药水公式），不走药水 HUD。同时去掉 `isClientSide` 守卫让客户端 LocalPlayer 也能跑。
- **Mixin `@Inject at=RETURN` setReturnValue 崩溃**：原版 `getJumpBoostPower()` 是 non-cancellable 方法，`setReturnValue()` 内部先 cancel → `CancellationException`。已删除该注入，改为 jumpFromGround 后手动加 bonus。
- **`SoundEvents` 字段类型适配**：26.2 部分音效字段改为 `Holder<SoundEvent>` 类型，使用 `.value()` 解引用（`SHIELD_BLOCK`、`GENERIC_EXPLODE`、`GENERIC_EAT`、`ARMOR_EQUIP_CHAIN`）。
- **`CompoundTag.getInt` 返回 `Optional`**：改为 `getIntOr(key, 0)`。
- **`Items.BROWN_DYE` 移除**：改为 `Items.DYE.pick(DyeColor.BROWN)`。
- **`Entity.moveTo(5参)` 移除**：改为 `setPos` + `setYRot`。
- **`hurt()` 过时**：改为 `hurtServer(ServerLevel, ...)`。
- **游戏音效自己听不到**：原版 `PlayerList.broadcast` 把自己作为 `except` 传入，导致穿内裤的玩家永远听不到自己触发的机制音效。新增 `sound()` 广播辅助方法（`playSound(null, ...)` except=null，所有人都能听到）。

### 变更

- **删除 4 处临时诊断日志**：`BriefsMechanic.tick` 每 5 秒日志、SLIME 满蓄/蓄力/弹起日志、`MixinLivingEntity.fall` 诊断、`MixinLivingEntity.modifyDamage` 诊断。同步移除两个类的 `LOGGER` 字段。
- **删除 START_SERVER_TICK 注册**：史莱姆弹跳不再需要 tick 开头 flag 兑现。
- **配方 result 字段**：从上游 Forge 的 `item` 改为 26.2 的 `id`（`ItemStackTemplate.CODEC` 使用）。
- **MixinLivingEntity 新增两个注入点**：
  - `travel HEAD`：SLIME 蓄力触发跳跃（两端都跑）。
  - `getEffectiveGravity HEAD`：气态内裤重力钳制（不显示药水 HUD）。
- **MixinLivingEntity 新增两个注入点（已删除）**：
  - `getJumpBoostPower RETURN`：因 non-cancellable 崩溃，删除后改为 jumpFromGround 后手动加分。
  - `hurtServer HEAD`：保留 SHIELD 减伤逻辑。
- **BriefsData 新增 `setStackQuiet`**：机制 tick 高频写回玩家实体的无日志变体，避免刷屏。
- **版本号**：提升至 `2.2.0-26.2`。

### 新增文件清单（相对 6f0f202）

**Java 源（2）**
- `src/main/java/cn/blockforge/generated/briefguard/BriefsMechanic.java`
- `src/main/java/cn/blockforge/generated/briefguard/mixin/MixinLivingEntity.java`

**配方（22，已移入 `recipe/` 单数目录）**
- 14 个机制内裤三角配方 + 7 个基础内裤三角配方 + 1 个下界合金锻造配方

**资源（14 套 × 4 类）**
- `assets/brief_guard/textures/item/*.png` × 14
- `assets/brief_guard/textures/entity/briefs/*.png` × 14
- `assets/brief_guard/models/item/*.json` × 14
- `assets/brief_guard/items/*.json` × 14（26.2 新路径）

**语言文件**
- `zh_cn.json` +29 条（物品名 + 机制描述）
- `en_us.json` +29 条（同上）

---

## 2.1.0-26.2（2026-09-12）

> 对比基线：2.0.0-26.2（c1e842d）。包含 `6f0f202`（新功能：内裤分类和真正的内裤槽）与 `829b131`（修复：内裤怎么飞了！）。

### 新功能

- **「内裤」创意分类标签页**：七种内裤归入独立分类，图标为钻石内裤；同时保留战斗分类中的原有显示。
  - 中英文标签页标题：`itemGroup.brief_guard`（中文：内裤 / 英文：Brief Guard）。
- **真正的独立内裤栏（实体槽位）**：
  - 生存模式：位于玩家物品栏菜单**副手栏上方**（46 号槽）。
  - 创造模式：位于**护腿槽旁**，通过改写原版 `SlotWrapper` 坐标实现，槽位点击仍走 `inventoryMenu.clicked(46)` 正常链路。
  - 内裤栏数据随玩家存档经 `CUSTOM_DATA` 持久化，客户端/服务端自动同步。
  - 空槽位显示内裤像素图标 + 原版风格灰色槽位底板（`container/slot`）。
- **皮革内裤支持拖入头部盔甲槽**：新增 `EQUIPPABLE(HEAD)` 组件放行原版盔甲槽 `mayPlace`；非皮革内裤不配置该组件，避免被拖入原版盔甲槽。装备提示更新为「头盔栏或者内裤栏」。
- **内裤单件不可堆叠**：物品属性新增 `stacksTo(1)`，避免放回背包时与同款堆叠合并。

### 修复

- **修复右键换装导致的内裤丢失**：`use()` 改为完全对齐原版 `Equippable.swapWithEquipmentSlot` 交换语义——换下的旧内裤通过 `SUCCESS.heldItemTransformedTo(worn.copy())` 干净地回到手持选中槽，不再手动 `placeItemBackInInventory` 造成 `useItem` 状态回写混乱。
- 右键穿戴对手持**副手**返回 `PASS`；穿戴**同款**内裤返回 `PASS`，避免凭空消耗。
- 修复实体数据持久化：26.2 中自定义 `DataComponentType` 写入实体会被静默丢弃，改为使用内置 `CUSTOM_DATA`（`CustomData`）NBT 存储内裤 `ItemStack`（键值 `Underwear`，`RegistryOps` + `ItemStack.CODEC` 编解码）。

### 变更

- **移除 Shift + 右键空手卸载内裤**：改为点按内裤栏取出，删除 C2S `RemovePayload` 与其客户端轮询代码（`BriefsClient` 不再监听 `keyUse`）。
- **新增创造模式 C2S 同步包** `SetStackPayload`：创造 UI 内裤栏改动推给服务端刷新属性（仅接受创造模式、空栈或单件内裤，防止生存模式走私物品）。
- **新增 Mixin 四件套**（注册于 `briefguard.mixins.json`，已接入 `fabric.mod.json`）：
  - `MixinInventoryMenu`：物品栏菜单末尾注入内裤槽。
  - `MixinCreativeModeInventoryScreen`：创造 INVENTORY 标签页调整内裤槽坐标到护腿槽旁。
  - `MixinAbstractContainerScreen`：补画内裤槽的原版槽位底板（通过空槽图标 ID 识别，兼容创造模式 `SlotWrapper`）。
  - `AbstractContainerMenuAccessor`：暴露父类 `addSlot` 供菜单注入。
- **右键穿戴流程重构**（对齐原版语义，含客户端预测分支）：
  - 皮革内裤：`setItemSlot(HEAD)` + 旧头盔经 `heldItemTransformedTo` 回手持。
  - 其他内裤：`BriefsData.setStack` + 旧内裤经 `heldItemTransformedTo` 回手持。
  - 穿戴完成在服务端刷新属性、播放装备音效并 S2C 同步。
- 版本号提升至 `2.1.0-26.2`；`API_NOTES_26.2.md` 移出版本控制。
