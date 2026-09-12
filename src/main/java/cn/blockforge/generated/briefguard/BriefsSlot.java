package cn.blockforge.generated.briefguard;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 内裤栏实体槽:读写玩家身上的 {@link BriefsData} 数据(CustomData 持久化)。
 * 该槽同时注入玩家物品栏菜单(生存 E 界面)和创造模式 INVENTORY 标签页,
 * 数据随实体存档持久化;变更时由 {@link #afterChange(ItemStack)} 刷新属性并同步。
 */
public final class BriefsSlot extends Slot {
    /** 在玩家物品栏菜单中的槽位索引(附手栏 45 之后)。 */
    public static final int SLOT_INDEX = 46;

    private final Player owner;

    public BriefsSlot(Player owner, int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
        this.owner = owner;
    }

    @Override
    public ItemStack getItem() {
        return BriefsData.getStack(owner);
    }

    @Override
    public boolean hasItem() {
        return !BriefsData.getStack(owner).isEmpty();
    }

    @Override
    public void set(ItemStack stack) {
        ItemStack next = stack == null ? ItemStack.EMPTY : stack.copy();
        if (ItemStack.matches(next, BriefsData.getStack(owner))) return; // 菜单回显等无变化写入,直接跳过
        BriefsData.setStack(owner, next);
        afterChange(next);
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
        ItemStack next = stack == null ? ItemStack.EMPTY : stack.copy();
        if (ItemStack.matches(next, BriefsData.getStack(owner))) return;
        BriefsData.setStack(owner, next);
        afterChange(next);
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack current = getItem();
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = current.copy();
        removed.setCount(amount > 0 && removed.getCount() > amount ? amount : removed.getCount());
        BriefsData.setStack(owner, ItemStack.EMPTY);
        afterChange(ItemStack.EMPTY);
        return removed;
    }

    /**
     * 槽位被更改后同步属性与显示。
     * 服务端菜单(生存点击、右键装备):刷新属性并 S2C 同步;
     * 客户端创造模式 UI:创造界面不经过服务端容器流程,直接把改动 C2S 推给服务端。
     */
    private void afterChange(ItemStack next) {
        if (owner instanceof ServerPlayer serverPlayer) {
            BriefsEvents.refreshAttributes(serverPlayer);
            BriefsNetwork.sync(serverPlayer);
        } else if (owner.level().isClientSide() && owner.getAbilities().instabuild) {
            BriefsNetwork.notifyServerStackChange(next);
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BriefsArmorItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public Identifier getNoItemIcon() {
        return BriefGuardMod.id("container/slot/briefs");
    }
}