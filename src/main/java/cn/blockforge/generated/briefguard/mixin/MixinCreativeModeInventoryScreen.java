package cn.blockforge.generated.briefguard.mixin;

import cn.blockforge.generated.briefguard.BriefsSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 创造模式 INVENTORY 标签页(带玩家模型的“生存物品栏”页)在 selectTab 时
 * 会把玩家物品栏菜单的所有槽用 SlotWrapper(私有类,无法在 Java 层直接引用/继承)
 * 重建进菜单槽列表,内裤槽默认落在网格坐标 (27,126)。
 *
 * <p>这里直接改写 SlotWrapper 构造参数:把内裤槽(索引 46)的坐标改为护腿槽 (108,6)
 * 正右边一格的 (126,6)。槽位对象仍是原版 SlotWrapper,槽点击走
 * player.inventoryMenu.clicked(46) 的正常链路,不会出现类型转换崩溃。
 * 客户端点击后由 {@link BriefsSlot#afterChange()} 把改动推到服务端。
 */
@Mixin(net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class)
public abstract class MixinCreativeModeInventoryScreen {

    @ModifyArg(
            method = "selectTab",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"),
            index = 2)
    private int briefguard$briefsSlotX(Slot target, int index, int x, int y) {
        return target.index == BriefsSlot.SLOT_INDEX ? 126 : x;
    }

    @ModifyArg(
            method = "selectTab",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"),
            index = 3)
    private int briefguard$briefsSlotY(Slot target, int index, int x, int y) {
        return target.index == BriefsSlot.SLOT_INDEX ? 6 : y;
    }
}