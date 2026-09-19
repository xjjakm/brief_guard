package cn.blockforge.generated.briefguard;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 所有游戏事件(从 Forge EventBus 移植到 Fabric 回调)。
 *
 * <p>26.2 事件分工：
 * <ul>
 *   <li>{@code ALLOW_DAMAGE}　：完全取消型（火焰/闪电免疫、贞操带格挡）</li>
 *   <li>{@code AFTER_DAMAGE}　：受击反应（粪/蠹虫/活塞/气体）</li>
 *   <li>{@code AFTER_DEATH}　：击杀效果（金粒掉落、晋升经验）</li>
 *   <li>{@code MixinLivingEntity}：改伤害数值（SHIELD 减伤）、坠落（SLIME/POOP）</li>
 *   <li>{@code UseItemCallback}：空手右键主动技能（辣条喷火、可食用回血）</li>
 * </ul>
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
    @SuppressWarnings("unused")
    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // 机制引擎每玩家 tick(含 NETHERITE 岩浆漂浮)
            BriefsMechanic.tick(player);

            // Gold: villager discount
            if (wornKind(player) == BriefsMaterialKind.GOLD && player.containerMenu instanceof MerchantMenu menu) {
                for (MerchantOffer offer : menu.getOffers()) {
                    offer.setSpecialPriceDiff(Math.min(offer.getSpecialPriceDiff(), -3));
                }
            }
        }
    }

    /** ALLOW_DAMAGE:返回 true 表示「应取消本次伤害」。 */
    public static boolean onLivingAttack(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Player player)) return false;
        BriefsMaterialKind kind = wornKind(player);
        if (kind == null) return false;
        // 基础款：铜免闪电、下界合金/龙首免火焰。
        if (kind == BriefsMaterialKind.COPPER && source.is(DamageTypeTags.IS_LIGHTNING)) return true;
        if ((kind == BriefsMaterialKind.NETHERITE || kind == BriefsMaterialKind.DRAGON_HEAD)
                && source.is(DamageTypeTags.IS_FIRE)) return true;
        // 贞操带：守护层完全格挡。
        return BriefsMechanic.tryBlockWithGuard(player);
    }

    /** AFTER_DAMAGE:受击反应(粪/蠹虫/活塞/气体)。 */
    @SuppressWarnings("unused")
    public static void onLivingHurt(LivingEntity entity, DamageSource source,
                                    float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof Player player)) return;
        BriefsMechanic.damageWorn(player, baseDamageTaken); // 内裤栏耐久损耗(对齐原版 max(1, damage/4))
        BriefsMechanic.onHurt(player, source);
    }

    /** AFTER_DEATH:实体死亡后触发(金粒掉落 + 晋升经验)。 */
    @SuppressWarnings("unused")
    public static void onLivingDeath(LivingEntity entity, DamageSource source) {
        if (!(source.getEntity() instanceof Player player)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        ItemStack worn = wornStack(player);
        if (!(worn.getItem() instanceof BriefsArmorItem item)) return;
        if (item.kind() == BriefsMaterialKind.GOLD && player.getRandom().nextFloat() < 0.35F) {
            entity.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                    new ItemStack(Items.GOLD_NUGGET, 1 + player.getRandom().nextInt(3))));
        } else if (item.kind() == BriefsMaterialKind.PROMOTION) {
            // 晋升：击杀额外掉落 1.5 倍经验 + 1，累积军阶。
            int xp = entity.getExperienceReward(serverLevel, player);
            ExperienceOrb.award(serverLevel, entity.position(), (int) (xp * 1.5F) + 1);
            BriefsMechanic.onKill(player, worn);
        }
    }

    /** ALLOW_DEATH 兜底：本项目不取消死亡。 */
    public static boolean onLivingDeathCancel(LivingEntity entity, DamageSource source) {
        return false;
    }

    /** AttackEntityCallback:手持内衣造成甜浆果伤害;机制攻击 + 铜内衣缓慢附加。 */
    @SuppressWarnings("unused")
    public static InteractionResult onAttackEntity(Player player, Level level, InteractionHand hand,
                                                   Entity target, @Nullable EntityHitResult hitResult) {
        if (level.isClientSide() || !(target instanceof LivingEntity livingTarget)) return InteractionResult.PASS;

        // Holding briefs in main hand: cancel normal attack, deal custom damage
        boolean held = player.getMainHandItem().getItem() instanceof BriefsArmorItem;
        if (held) {
            float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * player.getAttackStrengthScale(0.5F));
            if (damage > 0.0F) {
                livingTarget.hurtServer((ServerLevel) level, level.damageSources().sweetBerryBush(), damage);
            }
            return InteractionResult.CONSUME;
        }

        // Copper briefs worn: slowness on hit with cooldown
        BriefsMaterialKind worn = wornKind(player);
        ItemStack copperStack = new ItemStack(BriefGuardMod.COPPER_BRIEFS);
        if (worn == BriefsMaterialKind.COPPER && !player.getCooldowns().isOnCooldown(copperStack)) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1), player);
            player.getCooldowns().addCooldown(copperStack, 60);
        }

        // 机制攻击(龙首爆燃/辣条引燃/剑型旋风/盾击/活塞击退/活版门背刺/触手拉拽)
        BriefsMechanic.onAttack(player, livingTarget);

        return InteractionResult.PASS;
    }

    /** UseBlockCallback:空主手右键触发主动技能(辣条喷火/可食用回血)。
     * 走 useItemOn HEAD 注入,Fabric 对所有右键(含空手瞄准空气)都触发此回调。 */
    @SuppressWarnings("unused")
    public static InteractionResult onUseBlockActive(Player player, Level level, InteractionHand hand,
                                                     net.minecraft.world.phys.BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        BriefsMechanic.activeRightClick(player);
        return InteractionResult.PASS;
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
    static ItemStack wornStack(Player player) {
        ItemStack head = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (head.getItem() instanceof BriefsArmorItem headBriefs && headBriefs.kind() == BriefsMaterialKind.LEATHER) {
            return head;
        }
        return BriefsData.getStack(player);
    }

    @Nullable
    public static BriefsMaterialKind wornKind(Player player) {
        ItemStack worn = wornStack(player);
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