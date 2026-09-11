package cn.blockforge.generated.briefguard;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * All gameplay events ported from Forge EventBus to Fabric event callbacks.
 */
public final class BriefsEvents {
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("1b7e99d2-1ef0-4e5f-8e1e-4a6fced8a001");
    private static final UUID ATTACK_KNOCKBACK_UUID = UUID.fromString("1b7e99d2-1ef0-4e5f-8e1e-4a6fced8a002");
    private static final UUID LUCK_UUID = UUID.fromString("1b7e99d2-1ef0-4e5f-8e1e-4a6fced8a003");
    private static final UUID ARMOR_UUID = UUID.fromString("1b7e99d2-1ef0-4e5f-8e1e-4a6fced8a004");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("1b7e99d2-1ef0-4e5f-8e1e-4a6fced8a005");
    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("1b7e99d2-1ef0-4e5f-8e1e-4a6fced8a006");

    private BriefsEvents() {}

    /** Called every server tick: handles passive effects and attribute sync. */
    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BriefsMaterialKind kind = wornKind(player);

            // Netherite: lava swim + fire particles
            if (kind == BriefsMaterialKind.NETHERITE && player.isInLava()) {
                player.clearFire();
                if (player.getDeltaMovement().y < 0.05D) {
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.05D, player.getDeltaMovement().z);
                }
                if (player.tickCount % 4 == 0) {
                    player.level().addParticle(ParticleTypes.FLAME,
                            player.getX() + (player.getRandom().nextDouble() - 0.5D), player.getY() + 0.3D,
                            player.getZ() + (player.getRandom().nextDouble() - 0.5D), 0.0D, 0.03D, 0.0D);
                }
            }

            // Gold: villager discount
            if (kind == BriefsMaterialKind.GOLD && player.containerMenu instanceof MerchantMenu menu) {
                for (MerchantOffer offer : menu.getOffers()) {
                    offer.setSpecialPriceDiff(Math.min(offer.getSpecialPriceDiff(), -3));
                }
            }
        }
    }

    /** LivingAttackEvent: cancel lightning (copper) and fire (netherite) damage. */
    public static boolean onLivingAttack(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!(entity instanceof Player player)) return false;
        BriefsMaterialKind kind = wornKind(player);
        if (kind == BriefsMaterialKind.COPPER && source.is(DamageTypeTags.IS_LIGHTNING)) return true;
        if (kind == BriefsMaterialKind.NETHERITE && source.is(DamageTypeTags.IS_FIRE)) return true;
        return false;
    }

    /** LivingHurtEvent: modify projectile (chain), fire/explosion (netherite) damage. */
    public static float onLivingHurt(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!(entity instanceof Player player)) return amount;
        BriefsMaterialKind kind = wornKind(player);
        if (kind == BriefsMaterialKind.CHAIN && source.is(DamageTypeTags.IS_PROJECTILE)) {
            amount *= 0.25F;
        }
        if (kind == BriefsMaterialKind.NETHERITE) {
            if (source.is(DamageTypeTags.IS_FIRE)) return 0.0F;
            if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;
        }
        return amount;
    }

    /** AttackEntityCallback: holding briefs deals sweet berry damage; copper applies slowness. */
    public static net.minecraft.world.InteractionResult onAttackEntity(Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.entity.Entity target, net.minecraft.world.phys.EntityHitResult hitResult) {
        if (level.isClientSide() || !(target instanceof LivingEntity livingTarget)) return net.minecraft.world.InteractionResult.PASS;

        // Holding briefs in main hand: cancel normal attack, deal custom damage
        if (player.getMainHandItem().getItem() instanceof BriefsArmorItem) {
            float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * player.getAttackStrengthScale(0.5F));
            if (damage > 0.0F) {
                livingTarget.hurt(level.damageSources().sweetBerryBush(), damage);
            }
            return net.minecraft.world.InteractionResult.CONSUME;
        }

        // Copper briefs worn: slowness on hit with cooldown
        BriefsMaterialKind worn = wornKind(player);
        if (worn == BriefsMaterialKind.COPPER && !player.getCooldowns().isOnCooldown(BriefGuardMod.COPPER_BRIEFS)) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), player);
            player.getCooldowns().addCooldown(BriefGuardMod.COPPER_BRIEFS, 60);
        }

        return net.minecraft.world.InteractionResult.PASS;
    }

    /** LivingDeathEvent: gold briefs → 35% chance to drop gold nuggets. */
    public static boolean onLivingDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && wornKind(player) == BriefsMaterialKind.GOLD) {
            if (player.getRandom().nextFloat() < 0.35F) {
                entity.level().addFreshEntity(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                        new ItemStack(Items.GOLD_NUGGET, 1 + player.getRandom().nextInt(3))));
            }
        }
        return false; // don't cancel death
    }

    /** Player clone (death/return from end): copy underwear data. */
    public static void onPlayerClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        BriefsData.copy(oldPlayer, newPlayer);
        refreshAttributes(newPlayer);
    }

    /** Player respawn: refresh attributes and sync. */
    public static void onPlayerRespawn(ServerPlayer player, boolean alive) {
        refreshAttributes(player);
        BriefsNetwork.sync(player);
    }

    /** Player join: refresh attributes and sync. */
    public static void onPlayerJoin(ServerPlayer player) {
        refreshAttributes(player);
        BriefsNetwork.sync(player);
    }

    private static BriefsMaterialKind wornKind(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof BriefsArmorItem headBriefs && headBriefs.kind() == BriefsMaterialKind.LEATHER) {
            return BriefsMaterialKind.LEATHER;
        }
        ItemStack worn = BriefsData.getStack(player);
        return worn.getItem() instanceof BriefsArmorItem briefs ? briefs.kind() : null;
    }

    public static void refreshAttributes(Player player) {
        remove(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_UUID);
        remove(player, Attributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK_UUID);
        remove(player, Attributes.LUCK, LUCK_UUID);
        remove(player, Attributes.ARMOR, ARMOR_UUID);
        remove(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_UUID);
        remove(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_UUID);

        BriefsMaterialKind kind = wornKind(player);
        if (kind == null) return;

        add(player, Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_UUID, "Briefs attack damage", kind.attackDamage(), AttributeModifier.Operation.ADDITION));
        add(player, Attributes.ATTACK_KNOCKBACK, new AttributeModifier(ATTACK_KNOCKBACK_UUID, "Briefs attack knockback", kind.attackKnockback(), AttributeModifier.Operation.ADDITION));
        add(player, Attributes.LUCK, new AttributeModifier(LUCK_UUID, "Gold briefs luck", kind.luck(), AttributeModifier.Operation.ADDITION));

        if (kind != BriefsMaterialKind.LEATHER) {
            ItemStack worn = BriefsData.getStack(player);
            if (worn.getItem() instanceof BriefsArmorItem item) {
                add(player, Attributes.ARMOR, new AttributeModifier(ARMOR_UUID, "Briefs armor", item.getMaterial().getDefenseForType(item.getType()), AttributeModifier.Operation.ADDITION));
                add(player, Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGHNESS_UUID, "Briefs toughness", item.getMaterial().getToughness(), AttributeModifier.Operation.ADDITION));
                add(player, Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "Briefs knockback resistance", item.getMaterial().getKnockbackResistance(), AttributeModifier.Operation.ADDITION));
            }
        }
    }

    private static void add(Player player, Attribute attribute, AttributeModifier modifier) {
        var instance = player.getAttribute(attribute);
        if (instance != null && modifier.getAmount() != 0.0D) instance.addTransientModifier(modifier);
    }

    private static void remove(Player player, Attribute attribute, UUID id) {
        var instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }
}
