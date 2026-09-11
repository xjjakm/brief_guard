package cn.blockforge.generated.briefguard;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

/**
 * 所有游戏事件(从 Forge EventBus 移植到 Fabric 回调)。
 */
public final class BriefsEvents {
    private static final Identifier ATTACK_DAMAGE_ID = BriefGuardMod.id("briefs.attack_damage");
    private static final Identifier ATTACK_KNOCKBACK_ID = BriefGuardMod.id("briefs.attack_knockback");
    private static final Identifier LUCK_ID = BriefGuardMod.id("briefs.luck");
    private static final Identifier ARMOR_ID = BriefGuardMod.id("briefs.armor");
    private static final Identifier TOUGHNESS_ID = BriefGuardMod.id("briefs.armor_toughness");
    private static final Identifier KNOCKBACK_RESISTANCE_ID = BriefGuardMod.id("briefs.knockback_resistance");

    private BriefsEvents() {}

    /** 每个服务器 tick:被动效果。 */
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

    /** LivingAttackEvent:取消闪电(铜)和火焰(下界合金)伤害。 */
    public static boolean onLivingAttack(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!(entity instanceof Player player)) return false;
        BriefsMaterialKind kind = wornKind(player);
        return (kind == BriefsMaterialKind.COPPER && source.is(DamageTypeTags.IS_LIGHTNING))
                || (kind == BriefsMaterialKind.NETHERITE && source.is(DamageTypeTags.IS_FIRE));
    }

    /** AttackEntityCallback:手持内衣造成甜浆果伤害;铜内衣命中附加缓慢并进入冷却。 */
    @SuppressWarnings("unused")
    public static net.minecraft.world.InteractionResult onAttackEntity(Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.entity.Entity target, net.minecraft.world.phys.@Nullable EntityHitResult hitResult) {
        if (level.isClientSide() || !(target instanceof LivingEntity livingTarget)) return net.minecraft.world.InteractionResult.PASS;

        // Holding briefs in main hand: cancel normal attack, deal custom damage
        if (player.getMainHandItem().getItem() instanceof BriefsArmorItem) {
            float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * player.getAttackStrengthScale(0.5F));
            if (damage > 0.0F) {
                livingTarget.hurtServer((net.minecraft.server.level.ServerLevel) level, level.damageSources().sweetBerryBush(), damage);
            }
            return net.minecraft.world.InteractionResult.CONSUME;
        }

        // Copper briefs worn: slowness on hit with cooldown
        BriefsMaterialKind worn = wornKind(player);
        ItemStack copperStack = new ItemStack(BriefGuardMod.COPPER_BRIEFS);
        if (worn == BriefsMaterialKind.COPPER && !player.getCooldowns().isOnCooldown(copperStack)) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1), player);
            player.getCooldowns().addCooldown(copperStack, 60);
        }

        return net.minecraft.world.InteractionResult.PASS;
    }

    /** LivingDeathEvent:金内衣 → 35% 概率掉落金粒。 */
    public static boolean onLivingDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (source.getEntity() instanceof Player player && wornKind(player) == BriefsMaterialKind.GOLD) {
            if (player.getRandom().nextFloat() < 0.35F) {
                entity.level().addFreshEntity(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                        new ItemStack(Items.GOLD_NUGGET, 1 + player.getRandom().nextInt(3))));
            }
        }
        return false; // don't cancel death
    }

    /** Player clone (death/return from end):复制内衣数据。 */
    @SuppressWarnings("unused")
    public static void onPlayerClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        BriefsData.copy(oldPlayer, newPlayer);
        refreshAttributes(newPlayer);
    }

    /** Player respawn(Fabric 0.159 签名:oldPlayer, newPlayer, alive):刷新属性并同步。 */
    @SuppressWarnings("unused")
    public static void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        refreshAttributes(newPlayer);
        BriefsNetwork.sync(newPlayer);
    }

    /** Player join:刷新属性并同步。 */
    public static void onPlayerJoin(ServerPlayer player) {
        refreshAttributes(player);
        BriefsNetwork.sync(player);
    }

    @Nullable
    private static BriefsMaterialKind wornKind(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof BriefsArmorItem headBriefs && headBriefs.kind() == BriefsMaterialKind.LEATHER) {
            return BriefsMaterialKind.LEATHER;
        }
        ItemStack worn = BriefsData.getStack(player);
        return worn.getItem() instanceof BriefsArmorItem briefs ? briefs.kind() : null;
    }

    public static void refreshAttributes(Player player) {
        remove(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_ID);
        remove(player, Attributes.ATTACK_KNOCKBACK, ATTACK_KNOCKBACK_ID);
        remove(player, Attributes.LUCK, LUCK_ID);
        remove(player, Attributes.ARMOR, ARMOR_ID);
        remove(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID);
        remove(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_ID);

        BriefsMaterialKind kind = wornKind(player);
        if (kind == null) return;

        add(player, Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_ID, kind.attackDamage(), AttributeModifier.Operation.ADD_VALUE));
        add(player, Attributes.ATTACK_KNOCKBACK, new AttributeModifier(ATTACK_KNOCKBACK_ID, kind.attackKnockback(), AttributeModifier.Operation.ADD_VALUE));
        add(player, Attributes.LUCK, new AttributeModifier(LUCK_ID, kind.luck(), AttributeModifier.Operation.ADD_VALUE));

        if (kind != BriefsMaterialKind.LEATHER) {
            ItemStack worn = BriefsData.getStack(player);
            if (worn.getItem() instanceof BriefsArmorItem item) {
                BriefsMaterial material = item.material();
                add(player, Attributes.ARMOR, new AttributeModifier(ARMOR_ID, material.defense(), AttributeModifier.Operation.ADD_VALUE));
                add(player, Attributes.ARMOR_TOUGHNESS, new AttributeModifier(TOUGHNESS_ID, material.toughness(), AttributeModifier.Operation.ADD_VALUE));
                add(player, Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KNOCKBACK_RESISTANCE_ID, material.knockbackResistance(), AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    private static void add(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, AttributeModifier modifier) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && modifier.amount() != 0.0D) instance.addTransientModifier(modifier);
    }

    private static void remove(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, Identifier id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }
}