package cn.blockforge.generated.briefguard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public final class BriefGuardMod implements ModInitializer {
    public static final String MOD_ID = "brief_guard";

    // Armor materials
    public static final ArmorMaterial LEATHER_MATERIAL = new BriefsMaterial("leather", 5, 2, 0.0F, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, Ingredient.of(Items.LEATHER));
    public static final ArmorMaterial COPPER_MATERIAL = new BriefsMaterial("copper", 7, 4, 1.0F, 10, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, Ingredient.of(Items.COPPER_INGOT));
    public static final ArmorMaterial CHAIN_MATERIAL = new BriefsMaterial("chain", 12, 5, 1.0F, 12, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, Ingredient.of(Items.IRON_NUGGET));
    public static final ArmorMaterial IRON_MATERIAL = new BriefsMaterial("iron", 15, 6, 2.0F, 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, Ingredient.of(Items.IRON_INGOT));
    public static final ArmorMaterial GOLD_MATERIAL = new BriefsMaterial("gold", 7, 5, 0.0F, 25, SoundEvents.ARMOR_EQUIP_GOLD, 0.0F, Ingredient.of(Items.GOLD_INGOT));
    public static final ArmorMaterial DIAMOND_MATERIAL = new BriefsMaterial("diamond", 33, 8, 3.0F, 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 0.1F, Ingredient.of(Items.DIAMOND));
    public static final ArmorMaterial NETHERITE_MATERIAL = new BriefsMaterial("netherite", 37, 10, 4.0F, 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 0.9F, Ingredient.of(Items.NETHERITE_INGOT));

    // Items
    public static final Item LEATHER_BRIEFS = register("leather_briefs", new BriefsArmorItem(LEATHER_MATERIAL, ArmorItem.Type.HELMET, BriefsMaterialKind.LEATHER, new Item.Properties().durability(65)));
    public static final Item COPPER_BRIEFS = register("copper_briefs", new BriefsArmorItem(COPPER_MATERIAL, ArmorItem.Type.CHESTPLATE, BriefsMaterialKind.COPPER, new Item.Properties().durability(91)));
    public static final Item CHAIN_BRIEFS = register("chain_briefs", new BriefsArmorItem(CHAIN_MATERIAL, ArmorItem.Type.CHESTPLATE, BriefsMaterialKind.CHAIN, new Item.Properties().durability(156)));
    public static final Item IRON_BRIEFS = register("iron_briefs", new BriefsArmorItem(IRON_MATERIAL, ArmorItem.Type.CHESTPLATE, BriefsMaterialKind.IRON, new Item.Properties().durability(195)));
    public static final Item GOLD_BRIEFS = register("gold_briefs", new BriefsArmorItem(GOLD_MATERIAL, ArmorItem.Type.CHESTPLATE, BriefsMaterialKind.GOLD, new Item.Properties().durability(91)));
    public static final Item DIAMOND_BRIEFS = register("diamond_briefs", new BriefsArmorItem(DIAMOND_MATERIAL, ArmorItem.Type.CHESTPLATE, BriefsMaterialKind.DIAMOND, new Item.Properties().durability(429)));
    public static final Item NETHERITE_BRIEFS = register("netherite_briefs", new BriefsArmorItem(NETHERITE_MATERIAL, ArmorItem.Type.CHESTPLATE, BriefsMaterialKind.NETHERITE, new Item.Properties().durability(481).fireResistant()));

    @Override
    public void onInitialize() {
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

        // Damage events (Fabric API ServerLivingEntityEvents)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !BriefsEvents.onLivingAttack(entity, source, amount));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !BriefsEvents.onLivingDeath(entity, source, amount));

        // Note: LivingHurtEvent (damage modification) is handled via ServerLivingEntityEvents
        // if available; otherwise damage reduction effects may need a mixin.
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
