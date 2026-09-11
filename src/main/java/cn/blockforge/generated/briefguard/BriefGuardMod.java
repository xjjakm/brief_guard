package cn.blockforge.generated.briefguard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;

public final class BriefGuardMod implements ModInitializer {
    public static final String MOD_ID = "brief_guard";

    // 26.2: SoundEvents.* 本身就是 Holder<SoundEvent>,无需再包装
    private static final Holder<SoundEvent> LEATHER_EQUIP = SoundEvents.ARMOR_EQUIP_LEATHER;
    private static final Holder<SoundEvent> IRON_EQUIP = SoundEvents.ARMOR_EQUIP_IRON;
    private static final Holder<SoundEvent> CHAIN_EQUIP = SoundEvents.ARMOR_EQUIP_CHAIN;
    private static final Holder<SoundEvent> GOLD_EQUIP = SoundEvents.ARMOR_EQUIP_GOLD;
    private static final Holder<SoundEvent> DIAMOND_EQUIP = SoundEvents.ARMOR_EQUIP_DIAMOND;
    private static final Holder<SoundEvent> NETHERITE_EQUIP = SoundEvents.ARMOR_EQUIP_NETHERITE;

    // Armor materials (纯数据,见 BriefsMaterial)
    public static final BriefsMaterial LEATHER_MATERIAL = new BriefsMaterial("leather", 2, 0.0F, 0.0F, 15, LEATHER_EQUIP, ItemTags.REPAIRS_LEATHER_ARMOR);
    public static final BriefsMaterial COPPER_MATERIAL = new BriefsMaterial("copper", 4, 1.0F, 0.0F, 10, IRON_EQUIP, ItemTags.REPAIRS_COPPER_ARMOR);
    public static final BriefsMaterial CHAIN_MATERIAL = new BriefsMaterial("chain", 5, 1.0F, 0.0F, 12, CHAIN_EQUIP, ItemTags.REPAIRS_CHAIN_ARMOR);
    public static final BriefsMaterial IRON_MATERIAL = new BriefsMaterial("iron", 6, 2.0F, 0.0F, 9, IRON_EQUIP, ItemTags.REPAIRS_IRON_ARMOR);
    public static final BriefsMaterial GOLD_MATERIAL = new BriefsMaterial("gold", 5, 0.0F, 0.0F, 25, GOLD_EQUIP, ItemTags.REPAIRS_GOLD_ARMOR);
    public static final BriefsMaterial DIAMOND_MATERIAL = new BriefsMaterial("diamond", 8, 3.0F, 0.1F, 10, DIAMOND_EQUIP, ItemTags.REPAIRS_DIAMOND_ARMOR);
    public static final BriefsMaterial NETHERITE_MATERIAL = new BriefsMaterial("netherite", 10, 4.0F, 0.9F, 15, NETHERITE_EQUIP, ItemTags.REPAIRS_NETHERITE_ARMOR);

    // Items
    public static final Item LEATHER_BRIEFS = briefs("leather_briefs", LEATHER_MATERIAL, BriefsMaterialKind.LEATHER, 65, false);
    public static final Item COPPER_BRIEFS = briefs("copper_briefs", COPPER_MATERIAL, BriefsMaterialKind.COPPER, 91, false);
    public static final Item CHAIN_BRIEFS = briefs("chain_briefs", CHAIN_MATERIAL, BriefsMaterialKind.CHAIN, 156, false);
    public static final Item IRON_BRIEFS = briefs("iron_briefs", IRON_MATERIAL, BriefsMaterialKind.IRON, 195, false);
    public static final Item GOLD_BRIEFS = briefs("gold_briefs", GOLD_MATERIAL, BriefsMaterialKind.GOLD, 91, false);
    public static final Item DIAMOND_BRIEFS = briefs("diamond_briefs", DIAMOND_MATERIAL, BriefsMaterialKind.DIAMOND, 429, false);
    public static final Item NETHERITE_BRIEFS = briefs("netherite_briefs", NETHERITE_MATERIAL, BriefsMaterialKind.NETHERITE, 481, true);

    private static Item briefs(String name, BriefsMaterial material, BriefsMaterialKind kind, int durability, boolean fireResistant) {
        // 26.2: id 必须在构造 Item 前写入 Properties,否则 itemIdOrThrow() 抛 NPE
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        Item.Properties properties = new Item.Properties()
                .durability(durability)
                .enchantable(material.enchantmentValue())
                .setId(key);
        if (material.repairTag() != null) properties = properties.repairable(material.repairTag());
        if (fireResistant) properties = properties.fireResistant();
        return register(key, new BriefsArmorItem(material, kind, properties));
    }

    @SuppressWarnings("unused")
    @Override
    public void onInitialize() {
        // 实体数据组件注册(替代已删除的 Fabric EntityDataSaver)
        BriefsData.register();

        // Networking
        BriefsNetwork.init();

        // Creative tab
        BriefsCreative.register();

        // Server tick (passive effects)
        ServerTickEvents.END_SERVER_TICK.register(BriefsEvents::onServerTick);

        // Player lifecycle
        ServerPlayerEvents.COPY_FROM.register(BriefsEvents::onPlayerClone);
        ServerPlayerEvents.AFTER_RESPAWN.register(BriefsEvents::onPlayerRespawn);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BriefsEvents.onPlayerJoin(handler.player));

        // Combat events
        AttackEntityCallback.EVENT.register(BriefsEvents::onAttackEntity);

        // Damage events
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !BriefsEvents.onLivingAttack(entity, source));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !BriefsEvents.onLivingDeath(entity, source));
    }

    private static Item register(ResourceKey<Item> key, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}