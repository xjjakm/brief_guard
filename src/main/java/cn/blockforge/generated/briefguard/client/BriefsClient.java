package cn.blockforge.generated.briefguard.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public final class BriefsClient implements ClientModInitializer {

    @SuppressWarnings("unused")
    @Override
    public void onInitializeClient() {
        // Register briefs render layer on all player renderers (slim/wide avatars)
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof AvatarRenderer<?> avatarRenderer) {
                BriefsModel model = new BriefsModel(BriefsModel.createLayer().bakeRoot());
                registrationHelper.register(new BriefsLayer(avatarRenderer, model));
            }
        });

        // 内裤已改为实体物品栏槽位(玩家物品栏菜单 46 号槽),不再使用 Shift+右键空手卸载。
    }
}