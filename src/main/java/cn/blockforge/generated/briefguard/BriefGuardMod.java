package cn.blockforge.generated.briefguard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

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

    // 扩展：十四种以机制为主的内裤材料(数值与上游 E:\mod 一致)
    public static final BriefsMaterial DRAGON_HEAD_MATERIAL = new BriefsMaterial("dragon_head", 8, 3.0F, 0.2F, 12, NETHERITE_EQUIP, Items.BLAZE_ROD);
    public static final BriefsMaterial CHASTITY_MATERIAL = new BriefsMaterial("chastity", 10, 4.0F, 0.6F, 15, IRON_EQUIP, Items.IRON_INGOT);
    public static final BriefsMaterial SLIME_MATERIAL = new BriefsMaterial("slime", 6, 0.0F, 0.0F, 8, LEATHER_EQUIP, Items.SLIME_BALL);
    public static final BriefsMaterial SPICY_MATERIAL = new BriefsMaterial("spicy", 3, 0.0F, 0.0F, 20, LEATHER_EQUIP, Items.SWEET_BERRIES);
    public static final BriefsMaterial POOP_MATERIAL = new BriefsMaterial("poop", 4, 0.0F, 0.0F, 5, SoundEvents.ARMOR_EQUIP_GENERIC, Items.DYE.pick(DyeColor.BROWN));
    public static final BriefsMaterial SILVERFISH_MATERIAL = new BriefsMaterial("silverfish", 5, 0.0F, 0.0F, 6, SoundEvents.ARMOR_EQUIP_GENERIC, Items.STONE);
    public static final BriefsMaterial TENTACLE_MATERIAL = new BriefsMaterial("tentacle", 6, 1.0F, 0.0F, 10, LEATHER_EQUIP, Items.KELP);
    public static final BriefsMaterial EDIBLE_MATERIAL = new BriefsMaterial("edible", 1, 0.0F, 0.0F, 30, SoundEvents.ARMOR_EQUIP_GENERIC, Items.WHEAT);
    public static final BriefsMaterial TRAPDOOR_MATERIAL = new BriefsMaterial("trapdoor", 7, 1.0F, 0.0F, 6, SoundEvents.ARMOR_EQUIP_GENERIC, Items.OAK_PLANKS);
    public static final BriefsMaterial PROMOTION_MATERIAL = new BriefsMaterial("promotion", 5, 1.0F, 0.0F, 18, DIAMOND_EQUIP, Items.EMERALD);
    public static final BriefsMaterial STICKY_PISTON_MATERIAL = new BriefsMaterial("sticky_piston", 7, 0.0F, 0.0F, 8, LEATHER_EQUIP, Items.SLIME_BALL);
    public static final BriefsMaterial GASEOUS_MATERIAL = new BriefsMaterial("gaseous", 3, 0.0F, 0.0F, 12, LEATHER_EQUIP, Items.PHANTOM_MEMBRANE);
    public static final BriefsMaterial SWORD_MATERIAL = new BriefsMaterial("sword", 6, 1.0F, 0.0F, 9, IRON_EQUIP, Items.IRON_INGOT);
    public static final BriefsMaterial SHIELD_MATERIAL = new BriefsMaterial("shield", 9, 2.0F, 0.3F, 11, IRON_EQUIP, Items.IRON_INGOT);

    // Items
    public static final Item LEATHER_BRIEFS = briefs("leather_briefs", LEATHER_MATERIAL, BriefsMaterialKind.LEATHER, 65, false);
    public static final Item COPPER_BRIEFS = briefs("copper_briefs", COPPER_MATERIAL, BriefsMaterialKind.COPPER, 91, false);
    public static final Item CHAIN_BRIEFS = briefs("chain_briefs", CHAIN_MATERIAL, BriefsMaterialKind.CHAIN, 156, false);
    public static final Item IRON_BRIEFS = briefs("iron_briefs", IRON_MATERIAL, BriefsMaterialKind.IRON, 195, false);
    public static final Item GOLD_BRIEFS = briefs("gold_briefs", GOLD_MATERIAL, BriefsMaterialKind.GOLD, 91, false);
    public static final Item DIAMOND_BRIEFS = briefs("diamond_briefs", DIAMOND_MATERIAL, BriefsMaterialKind.DIAMOND, 429, false);
    public static final Item NETHERITE_BRIEFS = briefs("netherite_briefs", NETHERITE_MATERIAL, BriefsMaterialKind.NETHERITE, 481, true);

    // 扩展：十四种以机制为主的内裤物品(耐久与上游 E:\mod 一致)
    public static final Item DRAGON_HEAD_BRIEFS = briefs("dragon_head_briefs", DRAGON_HEAD_MATERIAL, BriefsMaterialKind.DRAGON_HEAD, 429, true);
    public static final Item CHASTITY_BRIEFS = briefs("chastity_briefs", CHASTITY_MATERIAL, BriefsMaterialKind.CHASTITY, 481, false);
    public static final Item SLIME_BRIEFS = briefs("slime_briefs", SLIME_MATERIAL, BriefsMaterialKind.SLIME, 156, false);
    public static final Item SPICY_BRIEFS = briefs("spicy_briefs", SPICY_MATERIAL, BriefsMaterialKind.SPICY, 104, false);
    public static final Item POOP_BRIEFS = briefs("poop_briefs", POOP_MATERIAL, BriefsMaterialKind.POOP, 130, false);
    public static final Item SILVERFISH_BRIEFS = briefs("silverfish_briefs", SILVERFISH_MATERIAL, BriefsMaterialKind.SILVERFISH, 195, false);
    public static final Item TENTACLE_BRIEFS = briefs("tentacle_briefs", TENTACLE_MATERIAL, BriefsMaterialKind.TENTACLE, 260, false);
    public static final Item EDIBLE_BRIEFS = briefs("edible_briefs", EDIBLE_MATERIAL, BriefsMaterialKind.EDIBLE, 78, false);
    public static final Item TRAPDOOR_BRIEFS = briefs("trapdoor_briefs", TRAPDOOR_MATERIAL, BriefsMaterialKind.TRAPDOOR, 325, false);
    public static final Item PROMOTION_BRIEFS = briefs("promotion_briefs", PROMOTION_MATERIAL, BriefsMaterialKind.PROMOTION, 338, false);
    public static final Item STICKY_PISTON_BRIEFS = briefs("sticky_piston_briefs", STICKY_PISTON_MATERIAL, BriefsMaterialKind.STICKY_PISTON, 286, false);
    public static final Item GASEOUS_BRIEFS = briefs("gaseous_briefs", GASEOUS_MATERIAL, BriefsMaterialKind.GASEOUS, 130, false);
    public static final Item SWORD_BRIEFS = briefs("sword_briefs", SWORD_MATERIAL, BriefsMaterialKind.SWORD, 364, false);
    public static final Item SHIELD_BRIEFS = briefs("shield_briefs", SHIELD_MATERIAL, BriefsMaterialKind.SHIELD, 442, false);

    private static Item briefs(String name, BriefsMaterial material, BriefsMaterialKind kind, int durability, boolean fireResistant) {
        // 26.2: id 必须在构造 Item 前写入 Properties,否则 itemIdOrThrow() 抛 NPE
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        Item.Properties properties = new Item.Properties()
                .durability(durability)
                .enchantable(material.enchantmentValue())
                .setId(key)
                // 内裤单件不可堆叠:避免 placeItemBackInInventory 把旧内裤合并进背包里已有的同款堆
                .stacksTo(1);
        if (material.repairTag() != null) properties = properties.repairable(material.repairTag());
        else if (material.repairItem() != null) properties = properties.repairable(material.repairItem());
        if (fireResistant) properties = properties.fireResistant();
        // 皮革内裤默认穿在头上:配 EQUIPPABLE(HEAD) 使原版盔甲槽 mayPlace 放行(物品栏可拖入头部槽)。
        // 其它内裤走自定义内裤栏,不配 EQUIPPABLE,避免被拖进原版盔甲槽。
        if (kind == BriefsMaterialKind.LEATHER) properties = properties.equippable(EquipmentSlot.HEAD);
        return register(key, new BriefsArmorItem(material, kind, properties));
    }

    @SuppressWarnings("unused")
    @Override
    public void onInitialize() {
        // Networking
        BriefsNetwork.init();

        // Creative tab
        BriefsCreative.register();

        // Server tick (被动效果)
        ServerTickEvents.END_SERVER_TICK.register(BriefsEvents::onServerTick);

        // Player lifecycle
        ServerPlayerEvents.COPY_FROM.register(BriefsEvents::onPlayerClone);
        ServerPlayerEvents.AFTER_RESPAWN.register(BriefsEvents::onPlayerRespawn);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BriefsEvents.onPlayerJoin(handler.player));

        // Combat events
        AttackEntityCallback.EVENT.register(BriefsEvents::onAttackEntity);

        // Active skills: 空手右键(辣条喷火/可食用回血)
        // 注:Fabric 的 UseItemCallback 只在原版 ServerPlayerGameMode.useItem 注入,
        //    而原版不会对空手右键调用 useItem(空手右键走 useItemOn),所以改走 UseBlockCallback(useItemOn HEAD)。
        UseBlockCallback.EVENT.register(BriefsEvents::onUseBlockActive);

        // Damage events (完全取消型：火焰/闪电免疫 + 贞操带格挡)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !BriefsEvents.onLivingAttack(entity, source));
        ServerLivingEntityEvents.AFTER_DAMAGE.register(BriefsEvents::onLivingHurt);
        ServerLivingEntityEvents.AFTER_DEATH.register(BriefsEvents::onLivingDeath);
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !BriefsEvents.onLivingDeathCancel(entity, source));
    }

    private static Item register(ResourceKey<Item> key, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}