package cn.blockforge.generated.briefguard.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import cn.blockforge.generated.briefguard.BriefsNetwork;

public final class BriefsClient implements ClientModInitializer {
    private static boolean wasUsePressed = false;

    @Override
    public void onInitializeClient() {
        // Register briefs render layer on all player skins
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationProvider, context) -> {
            if (entityRenderer instanceof PlayerRenderer playerRenderer) {
                BriefsModel<AbstractClientPlayer> model = new BriefsModel<>(BriefsModel.createLayer().bakeRoot());
                registrationProvider.register(new BriefsLayer(playerRenderer, model));
            }
        });

        // Shift + right-click empty hand → remove underwear (C2S)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.options == null || client.options.keyUse == null) return;
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
