# 内裤防具与装备系统（Brief Guard）

一个面向 **Minecraft 26.2** 的 Fabric 模组：添加七种可穿戴的内裤防具，包含独立内裤栏、材质专属被动效果、三角形合成配方与自定义穿戴模型。
这个项目是鲸鱼（deepseek）写的，原项目也是

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | >= 0.19.5 |
| Fabric API | 0.159.0+26.2 |
| Java | >= 25 |

## 内容

七种内裤，每种由「材质」（防御/特攻等数值，见 `BriefsMaterial`）与「种类」（攻击属性与被动效果，见 `BriefsMaterialKind`）构成：

| 内裤 | 护甲 | 韧性 | 击退抗性 | 附魔能力 | 耐久 | 手持攻击力 | 手持击退 | 手持幸运 | 耐火 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 皮革 | 2 | 0 | 0 | 15 | 65 | 1.0 | 0 | 0 | - |
| 氧化铜 | 4 | 1.0 | 0 | 10 | 91 | 2.0 | 0.5 | 0 | - |
| 锁链 | 5 | 1.0 | 0 | 12 | 156 | 3.0 | 0 | 0 | - |
| 铁 | 6 | 2.0 | 0 | 9 | 195 | 4.0 | 0 | 0 | - |
| 黄金 | 5 | 0 | 0 | 25 | 91 | 3.0 | 0 | 1.0 | - |
| 钻石 | 8 | 3.0 | 0.1 | 10 | 429 | 5.0 | 0.5 | 0 | - |
| 下界合金 | 10 | 4.0 | 0.9 | 15 | 481 | 7.0 | 1.0 | 0 | 是 |

### 装备方式

- **皮革内裤**：装备在**头盔栏**（作为兜帽头饰）。右键穿戴；因带有 `EQUIPPABLE(HEAD)` 组件，也支持直接从物品栏拖入头部盔甲槽。
- **其他内裤**：装备在**独立内裤栏**，不占用胸甲栏。
  - 生存模式：内裤栏位于物品栏中**副手栏上方**；创造模式：位于**护腿槽旁**。
  - 右键内裤（或从物品栏拖入内裤栏）穿戴，点按内裤栏取出脱下。
  - 内裤栏数据随玩家存档经 `CUSTOM_DATA` 持久化，客户端/服务端自动同步。
  - 创造模式物品栏仅同步客户端显示，内裤栏的增删通过自定义 C2S 网络包推给服务端刷新属性。
  - 空槽位显示内裤像素图标 + 原版风格灰色槽位底板。

### 被动效果（穿戴时）

| 材质 | 效果 |
| --- | --- |
| 氧化铜 | 免疫闪电伤害；命中敌人附加缓慢 II（1.5 秒，冷却 3 秒） |
| 黄金 | 村民交易折扣；击杀生物时 35% 概率掉落金粒 |
| 下界合金 | 免疫火焰伤害；可在岩浆中游泳（下界合金外套风格） |

### 手持效果

手持任意内裤时，空手攻击改为使用**甜浆果丛伤害类型**，伤害随攻击力与攻击蓄力缩放；同时获得手持攻击力 / 击退 / 幸运属性加成。

## 合成

- 三角形合成（2×2 倒三角）：左上、右上、下中各放一块对应材料，如 `皮革` → `皮革内裤`。
- **下界合金内裤**：锻造台升级 —— 钻石内裤 + 下界合金升级锻造模板 + 下界合金锭。

## 开发构建

```bash
./gradlew build
```

产物位于 `build/libs/`。不需要额外数据包，`数据组件（Data Components）` 负责属性、耐久与内裤栏存储，客户端/服务端自动同步。

## 目录结构

```
src/main/java/cn/blockforge/generated/briefguard
├── BriefGuardMod.java        # 入口：材质/物品注册、事件绑定、用 EQUIPPABLE 组件放行皮革头部槽
├── BriefsArmorItem.java      # 物品：普通 Item + DataComponents(属性/装备组件)
├── BriefsMaterial.java       # 材质数据记录
├── BriefsMaterialKind.java   # 种类：攻击属性与被动效果
├── BriefsData.java           # 独立内裤栏：CUSTOM_DATA 持久化（RegistryOps + ItemStack.CODEC）
├── BriefsSlot.java           # 内裤栏槽位：读写玩家内裤数据、刷新属性、触发网络同步
├── BriefsEvents.java         # 被动效果/战斗/掉落事件
├── BriefsNetwork.java        # C2S/S2C 内裤栏变更同步
├── BriefsCreative.java       # 「内裤守卫」创意分类标签页（图标：钻石内裤）
├── mixin/
│   ├── MixinInventoryMenu.java            # 生存菜单注入内裤栏槽位
│   ├── MixinCreativeModeInventoryScreen.java # 创造模式槽位坐标调整
│   ├── MixinAbstractContainerScreen.java  # 渲染时补画原版槽位底板
│   └── AbstractContainerMenuAccessor.java # 暴露父类 addSlot 供菜单注入
└── client/
    ├── BriefsClient.java     # 渲染层注册
    ├── BriefsLayer.java      # 穿戴模型渲染层（submit 节点提交）
    └── BriefsModel.java      # 下体护甲外壳模型（挂接 HumanoidModel 骨架）
```

## 许可证

原项目使用ARR许可证，这个项目使用GNU通用公共许可证v3

![image](https://www.gnu.org/graphics/gplv3-127x51.png)