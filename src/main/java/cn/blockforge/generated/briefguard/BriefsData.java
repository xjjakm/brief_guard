package cn.blockforge.generated.briefguard;

import net.fabricmc.fabric.api.entity.event.v1.EntityDataSaver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric replacement for the Forge Capability system.
 * Stores the worn underwear ItemStack in the player's persistent NBT via EntityDataSaver.
 */
public final class BriefsData {
    private static final String KEY = "BriefGuardUnderwear";

    private BriefsData() {}

    public static ItemStack getStack(Player player) {
        CompoundTag nbt = ((EntityDataSaver) player).getNbt();
        if (nbt.contains(KEY)) {
            return ItemStack.of(nbt.getCompound(KEY));
        }
        return ItemStack.EMPTY;
    }

    public static void setStack(Player player, ItemStack stack) {
        CompoundTag nbt = ((EntityDataSaver) player).getNbt();
        if (stack.isEmpty()) {
            nbt.remove(KEY);
        } else {
            CompoundTag itemTag = new CompoundTag();
            stack.save(itemTag);
            nbt.put(KEY, itemTag);
        }
    }

    public static void copy(Player from, Player to) {
        ItemStack stack = getStack(from);
        if (!stack.isEmpty()) {
            setStack(to, stack.copy());
        }
    }
}
