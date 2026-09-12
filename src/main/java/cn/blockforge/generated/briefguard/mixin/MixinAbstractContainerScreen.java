package cn.blockforge.generated.briefguard.mixin;

import cn.blockforge.generated.briefguard.BriefGuardMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 内裤槽位于各界面背景贴图的空白区,补画原版 18x18 的槽位背景底板
 * (container/slot),多用于统计页/马背包等动态槽位。创造模式 INVENTORY
 * 标签页中内裤槽被 SlotWrapper 包装,需识别其 target。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {
    private static final Identifier SLOT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/slot");

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void briefguard$briefsSlotBackground(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (isBriefsSlot(slot)) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND_SPRITE, slot.x - 1, slot.y - 1, 18, 18);
        }
    }

    /** 通过空槽图标 ID 识别内裤槽(创造模式 SlotWrapper 是 private,但仍委托 getNoItemIcon)。 */
    private static boolean isBriefsSlot(Slot slot) {
        return BriefGuardMod.id("container/slot/briefs").equals(slot.getNoItemIcon());
    }
}