package cn.blockforge.generated.briefguard.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.Player;
import cn.blockforge.generated.briefguard.BriefsNetwork;

public final class BriefsClient implements ClientModInitializer {
    private static boolean wasUsePressed = false;

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

        // Shift + right-click empty hand → remove underwear (C2S)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            boolean pressed = client.options.keyUse.isDown();
            if (pressed && !wasUsePressed) {
                wasUsePressed = true;
                Player player = client.player;
                if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
                    ClientPlayNetworking.send(new BriefsNetwork.RemovePayload());
                }
            } else if (!pressed) {
                wasUsePressed = false;
            }
        });
    }
}
