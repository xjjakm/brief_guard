package cn.blockforge.generated.briefguard.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** 下体护甲外壳,由玩家渲染层使用。26.2 模型统一以 EntityRenderState 为类型参数。 */
public final class BriefsModel extends HumanoidModel<AvatarRenderState> {
    public BriefsModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 26.2: HumanoidModel 构造从 head.getChild("hat") 取帽子,hat 必须挂在 head 下
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.15F, 0.0F, -2.25F, 8.3F, 4.2F, 4.5F, new CubeDeformation(0.16F)), PartPose.ZERO);
        body.addOrReplaceChild("front_panel", CubeListBuilder.create()
                .texOffs(16, 0)
                .addBox(-3.55F, 0.9F, -2.48F, 7.1F, 3.1F, 0.34F, new CubeDeformation(0.04F)), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-2.06F, 0.0F, -2.06F, 4.12F, 4.2F, 4.12F, new CubeDeformation(0.14F));
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-2.06F, 0.0F, -2.06F, 4.12F, 4.2F, 4.12F, new CubeDeformation(0.14F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}