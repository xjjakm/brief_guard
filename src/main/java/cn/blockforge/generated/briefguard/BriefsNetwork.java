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
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("BriefGuard");

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

    // ---- C2S: client pushes worn-underwear change made in the creative UI ----
    public record SetStackPayload(ItemStack stack) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetStackPayload> TYPE =
                new CustomPacketPayload.Type<>(BriefGuardMod.id("set_stack"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetStackPayload> CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC,
                SetStackPayload::stack,
                SetStackPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SuppressWarnings("unused")
    public static void init() {
        // 注册包类型(必须在 handler 之前)
        PayloadTypeRegistry.serverboundPlay().register(SetStackPayload.TYPE, SetStackPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);

        // S2C receiver (client side)
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            Player local = context.client().player;
            if (local == null) return;
            Entity entity = local.level().getEntity(payload.entityId());
            LOGGER.info("[BriefGuard] S2C sync received entityId={} stack={}", payload.entityId(), payload.stack());
            if (entity instanceof Player target) {
                BriefsData.setStack(target, payload.stack());
            }
        }));

        // C2S receiver:创造 UI 的内裤槽改动(客户端无服务器容器流程)。
        // 只在创造模式接受,且只接受空栈或单件内裤,防止生存模式走私物品。
        ServerPlayNetworking.registerGlobalReceiver(SetStackPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                ItemStack stack = payload.stack();
                LOGGER.info("[BriefGuard] C2S set_stack received uuid={} instabuild={} stack={}",
                        player.getUUID(), player.getAbilities().instabuild, stack);
                if (!player.getAbilities().instabuild) return;
                if (!stack.isEmpty() && (stack.getCount() != 1 || !(stack.getItem() instanceof BriefsArmorItem))) return;
                BriefsData.setStack(player, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
                BriefsEvents.refreshAttributes(player);
                sync(player);
            });
        });
    }

    /**
     * 客户端在创造模式 UI 中改动内裤槽后,把改动推给服务端。
     * 方法内按环境守卫:专属服务器不会执行到 ClientPlayNetworking,不会加载客户端类。
     */
    public static void notifyServerStackChange(ItemStack stack) {
        if (net.fabricmc.api.EnvType.CLIENT == net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()) {
            ClientPlayNetworking.send(new SetStackPayload(stack));
        }
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