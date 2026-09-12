package cn.blockforge.generated.briefguard.mixin;

import cn.blockforge.generated.briefguard.BriefsSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在玩家物品栏菜单末尾(附手栏 45 之后)追加内裤槽(索引 46)。
 * 生存模式 E 界面直接使用菜单坐标渲染,槽位位于副手栏正上方 (77,44)。
 * 创造模式 INVENTORY 标签页复用此菜单的槽(点击走 inventoryMenu.clicked),见 {@link MixinCreativeModeInventoryScreen}。
 */
@Mixin(InventoryMenu.class)
public abstract class MixinInventoryMenu {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void briefguard$addUnderwearSlot(net.minecraft.world.entity.player.Inventory inventory,
                                             boolean active,
                                             net.minecraft.world.entity.player.Player owner,
                                             CallbackInfo ci) {
        Slot briefs = new BriefsSlot(owner, 77, 44);
        ((AbstractContainerMenuAccessor) (Object) this).briefguard$invokeAddSlot(briefs);
    }
}