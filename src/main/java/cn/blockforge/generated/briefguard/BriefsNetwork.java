package cn.blockforge.generated.briefguard;

import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric networking: S2C sync of worn underwear, and C2S request to remove underwear.
 */
public final class BriefsNetwork {
    private BriefsNetwork() {}

    // ---- S2C: sync worn underwear to client ----
    public record SyncPayload(int entityId, ItemStack stack) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BriefGuardMod.MOD_ID, "sync"));
        public static final StreamCodec<? super RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.of(
                SyncPayload::write, SyncPayload::new);

        public SyncPayload(RegistryFriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readItem());
        }

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeItem(stack);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ---- C2S: client requests removal of worn underwear ----
    public record RemovePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RemovePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BriefGuardMod.MOD_ID, "remove"));
        public static final StreamCodec<? super RegistryFriendlyByteBuf, RemovePayload> CODEC = StreamCodec.of(
                (buf, val) -> {}, buf -> new RemovePayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void init() {
        // S2C receiver (client side)
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Player local = context.client().player;
                if (local == null) return;
                net.minecraft.world.entity.Entity entity = local.level().getEntity(payload.entityId());
                if (entity instanceof Player target) {
                    BriefsData.setStack(target, payload.stack());
                }
            });
        });

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
        for (ServerPlayer target : player.server.getPlayerList().getPlayers()) {
            if (target == player || target.connection != null && hasLineOfSight(target, player)) {
                ServerPlayNetworking.send(target, packet);
            }
        }
    }

    private static boolean hasLineOfSight(ServerPlayer watcher, ServerPlayer target) {
        return watcher.level() == target.level()
                && watcher.distanceToSqr(target) < 64 * 64;
    }
}
