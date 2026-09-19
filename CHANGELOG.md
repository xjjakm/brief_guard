# 更新日志

> 这是整个仓库的更新日志

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