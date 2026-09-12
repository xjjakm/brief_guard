package cn.blockforge.generated.briefguard.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {

    /**
     * addSlot 声明在父类 AbstractContainerMenu 中(InventoryMenu 不重写它),
     * 因此 @Invoker 必须挂在 AbstractContainerMenu 上才能匹配到。
     */
    @Invoker("addSlot")
    Slot briefguard$invokeAddSlot(Slot slot);
}