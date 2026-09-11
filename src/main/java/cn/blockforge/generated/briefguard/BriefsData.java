package cn.blockforge.generated.briefguard;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 26.2 中 Fabric 的 EntityDataSaver 已删除。
 * 改为注册实体 DataComponent(随玩家存档持久化,天然支持 ItemStack codec)。
 * 客户端呈现用的数据由 {@link BriefsNetwork} 的 S2C 包写进本地玩家。
 */
public final class BriefsData {
    public static final DataComponentType<ItemStack> UNDERWEAR = DataComponentType.<ItemStack>builder()
            .persistent(ItemStack.CODEC)
            .cacheEncoding()
            .build();

    private BriefsData() {}

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, BriefGuardMod.id("underwear"), UNDERWEAR);
    }

    public static ItemStack getStack(Player player) {
        ItemStack stack = player.get(UNDERWEAR);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static void setStack(Player player, ItemStack stack) {
        player.setComponent(UNDERWEAR, stack == null ? ItemStack.EMPTY : stack);
    }

    public static void copy(Player from, Player to) {
        ItemStack stack = getStack(from);
        if (!stack.isEmpty()) {
            setStack(to, stack.copy());
        }
    }
}