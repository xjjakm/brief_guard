package cn.blockforge.generated.briefguard;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric 网络(26.2):S2C 同步穿着内衣,C2S 请求脱下内衣。
 * 包类型必须先通过 PayloadTypeRegistry 注册,再注册 handler。
 */
public final class BriefsNetwork {
    private BriefsNetwork() {}

    // ---- S2C: sync worn underwear to client ----
    public record SyncPayload(int entityId, ItemStack stack) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncPayload> TYPE =
                new CustomPacketPayload.Type<>(BriefGuardMod.id("sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                SyncPayload::entityId,
                ItemStack.OPTIONAL_STREAM_CODEC,
                SyncPayload::stack,
                SyncPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ---- C2S: client requests removal of worn underwear ----
    public record RemovePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RemovePayload> TYPE =
                new CustomPacketPayload.Type<>(BriefGuardMod.id("remove"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RemovePayload> CODEC = StreamCodec.unit(new RemovePayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SuppressWarnings("unused")
    public static void init() {
        // 注册包类型(必须在 handler 之前)
        PayloadTypeRegistry.serverboundPlay().register(RemovePayload.TYPE, RemovePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);

        // S2C receiver (client side)
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            Player local = context.client().player;
            if (local == null) return;
            Entity entity = local.level().getEntity(payload.entityId());
            if (entity instanceof Player target) {
                BriefsData.setStack(target, payload.stack());
            }
        }));

        // C2S receiver (server side)
        ServerPlayNetworking.registerGlobalReceiver(RemovePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                ItemStack worn = BriefsData.getStack(player);
                if (!worn.isEmpty()) {
                    BriefsData.setStack(player, ItemStack.EMPTY);
                    player.getInventory().placeItemBackInInventory(worn);
                    BriefsEvents.refreshAttributes(player);
                    sync(player);
                }
            });
        });
    }

    public static void sync(ServerPlayer player) {
        SyncPayload packet = new SyncPayload(player.getId(), BriefsData.getStack(player));
        // Send to tracking players and self
        for (ServerPlayer target : player.level().getServer().getPlayerList().getPlayers()) {
            if (target == player || hasLineOfSight(target, player)) {
                ServerPlayNetworking.send(target, packet);
            }
        }
    }

    private static boolean hasLineOfSight(ServerPlayer watcher, ServerPlayer target) {
        return watcher.level() == target.level()
                && watcher.distanceToSqr(target) < 64 * 64;
    }
}