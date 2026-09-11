package cn.blockforge.generated.briefguard.client;

import cn.blockforge.generated.briefguard.BriefGuardMod;
import cn.blockforge.generated.briefguard.BriefsArmorItem;
import cn.blockforge.generated.briefguard.BriefsData;
import cn.blockforge.generated.briefguard.BriefsMaterialKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 只渲染自定义的下体护甲外壳,保留玩家皮肤与盔甲。
 * 26.2 渲染管线:层实现 submit() 提交 SubmitNodeCollector;姿态通过 Fabric 混入的
 * FabricModel.copyTransforms() 从父玩家模型整树递归复制。
 */
public final class BriefsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final BriefsModel model;

    public BriefsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, BriefsModel model) {
        super(parent);
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       AvatarRenderState state, float yRot, float xRot) {
        ItemStack stack = dataStack(state);
        BriefsArmorItem briefs = stack.getItem() instanceof BriefsArmorItem item ? item : null;
        if (briefs == null) {
            stack = state.headEquipment;
            briefs = stack.getItem() instanceof BriefsArmorItem item ? item : null;
            if (briefs == null || briefs.kind() != BriefsMaterialKind.LEATHER) return;
        }

        // 继承父玩家模型姿态(复制整棵模型的变换与子部件)
        this.model.copyTransforms(this.getParentModel());
        this.model.head.visible = false;
        this.model.hat.visible = false;
        this.model.body.visible = true;
        this.model.rightArm.visible = false;
        this.model.leftArm.visible = false;
        this.model.rightLeg.visible = true;
        this.model.leftLeg.visible = true;
        this.model.body.getChild("front_panel").visible = true;

        submitNodeCollector.order(1)
                .submitModel(
                        this.model,
                        state,
                        poseStack,
                        RenderTypes.entityCutout(texture(briefs.kind())),
                        lightCoords,
                        LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                        -1,
                        null,
                        state.outlineColor,
                        null
                );
    }

    /** 优先读数据组件(自定义内衣槽);组件未同步到客户端时回退到头盔槽。 */
    private static ItemStack dataStack(AvatarRenderState state) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        Entity entity = level.getEntity(state.id);
        if (!(entity instanceof Player player)) return ItemStack.EMPTY;
        ItemStack stack = BriefsData.getStack(player);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    private static Identifier texture(BriefsMaterialKind kind) {
        return BriefGuardMod.id("textures/entity/briefs/" + kind.name().toLowerCase(java.util.Locale.ROOT) + ".png");
    }
}