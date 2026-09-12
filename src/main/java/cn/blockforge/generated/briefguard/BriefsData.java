package cn.blockforge.generated.briefguard;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 26.2 中实体 setComponent 只处理 CUSTOM_NAME / CUSTOM_DATA 两个隐式组件,
 * 自定义 DataComponentType 写入实体会被静默丢弃。
 * 故改用内置 {@link DataComponents#CUSTOM_DATA}(CustomData) 存 NBT:
 * 会随实体存档持久化;客户端呈现用数据由 {@link BriefsNetwork} 的 S2C 包写进本地玩家。
 */
public final class BriefsData {
    /** customData 里存放内裤的键。 */
    private static final String KEY = "Underwear";

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("BriefGuard");

    private BriefsData() {}

    public static ItemStack getStack(Player player) {
        CustomData data = player.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return ItemStack.EMPTY;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(KEY)) return ItemStack.EMPTY;
        // 有 KEY 但解析失败说明存入的 NBT 异常,打印原始值帮助定位
        return ItemStack.CODEC.parse(registryOps(player), tag.get(KEY))
                .result().orElseGet(() -> {
                    LOGGER.warn("[BriefGuard] getStack PARSE FAILED! uuid={} raw={}", player.getUUID(), tag.get(KEY));
                    return ItemStack.EMPTY;
                });
    }

    public static void setStack(Player player, ItemStack stack) {
        String caller = Thread.currentThread().getStackTrace().length >= 3
                ? Thread.currentThread().getStackTrace()[2].getClassName() + "#" + Thread.currentThread().getStackTrace()[2].getMethodName()
                : "?";
        org.slf4j.LoggerFactory.getLogger("BriefGuard").info(
                "[BriefGuard] setStack caller={} stack={}", caller, stack);
        CompoundTag tag = CustomData.EMPTY.copyTag();
        if (stack != null && !stack.isEmpty()) {
            tag.put(KEY, ItemStack.CODEC.encodeStart(registryOps(player), stack).result().orElse(new CompoundTag()));
        }
        player.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static RegistryOps<Tag> registryOps(Player player) {
        return RegistryOps.create(NbtOps.INSTANCE, player.level().registryAccess());
    }

    public static void copy(Player from, Player to) {
        ItemStack stack = getStack(from);
        if (!stack.isEmpty()) {
            setStack(to, stack.copy());
        }
    }
}