package cn.blockforge.generated.briefguard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import cn.blockforge.generated.briefguard.BriefsArmorItem;
import cn.blockforge.generated.briefguard.BriefsData;
import cn.blockforge.generated.briefguard.BriefsMaterialKind;
import cn.blockforge.generated.briefguard.BriefGuardMod;

/** Renders only the custom lower-body shell, leaving the player's skin and armor visible. */
public final class BriefsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final BriefsModel<AbstractClientPlayer> model;

    public BriefsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                       BriefsModel<AbstractClientPlayer> model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack stack = BriefsData.getStack(player);
        BriefsArmorItem briefs = stack.getItem() instanceof BriefsArmorItem item ? item : null;
        if (briefs == null) {
            stack = player.getItemBySlot(EquipmentSlot.HEAD);
            briefs = stack.getItem() instanceof BriefsArmorItem item ? item : null;
            if (briefs == null || briefs.kind() != BriefsMaterialKind.LEATHER) return;
        }

        PlayerModel<AbstractClientPlayer> parent = getParentModel();
        parent.copyPropertiesTo(model);
        model.head.copyFrom(parent.head);
        model.body.copyFrom(parent.body);
        model.rightArm.copyFrom(parent.rightArm);
        model.leftArm.copyFrom(parent.leftArm);
        model.rightLeg.copyFrom(parent.rightLeg);
        model.leftLeg.copyFrom(parent.leftLeg);
        model.hat.copyFrom(parent.hat);
        model.setAllVisible(false);
        model.body.visible = true;
        model.body.getChild("front_panel").visible = true;
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;

        ResourceLocation texture = texture(briefs.kind());
        VertexConsumer vertex = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, vertex, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation texture(BriefsMaterialKind kind) {
        return BriefGuardMod.id("textures/entity/briefs/" + kind.name().toLowerCase(java.util.Locale.ROOT) + ".png");
    }
}
