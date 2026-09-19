package cn.blockforge.generated.briefguard;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 复杂机制引擎：十四种扩展内裤的能力统一在这里实现。
 *
 * <p>设计原则：
 * <ul>
 *   <li>不使用持续性的简单增益（移速、跳跃、呼吸、隐身等 potion 常驻）；改为有起手、有反馈、
 *       需要互动的事件式机制。</li>
 *   <li>机制状态（蓄力、层数、计数）挂在穿着的内裤 ItemStack 的 CUSTOM_DATA 组件上，随穿戴
 *       持久化，死亡/换存档/脱下重穿都不丢。</li>
 *   <li>所有读取穿着的入口统一走 {@link #wornStack}/{@link #wornKind}，内部用
 *       {@link BriefsData#getStack} 的安全兜底，重生过渡窗口也不会抛异常。</li>
 * </ul>
 *
 * <p>26.2 适配：内裤栈是 {@link BriefsData#getStack} 解码出的副本实例，改它的组件不会影响
 * 玩家实体里的原件，所以任何状态写入都必须先改栈的组件、再经
 * {@link BriefsData#setStackQuiet} 回写玩家实体（无日志变体，机制 tick 高频写不刷屏）。
 */
public final class BriefsMechanic {
    public static final int FIRE_MAX = 10;
    public static final int GUARD_MAX = 3;
    public static final int SPRING_MAX = 20;
    public static final int HEAT_MAX = 10;
    public static final int FULL_MAX = 5;
    public static final int RANK_MAX = 3;

    private BriefsMechanic() {}

    // ---------------------------------------------------------------- 状态存取
    private static int tag(ItemStack stack, String key) {
        if (stack == null || stack.isEmpty()) return 0;
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getIntOr(key, 0);
    }

    /** 写入内裤栈组件并回写玩家实体；值未变化时跳过写回，避免机制 tick 高频编解码。 */
    private static void setTag(Player player, ItemStack stack, String key, int value) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.getIntOr(key, 0) == value) return;
        tag.putInt(key, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        BriefsData.setStackQuiet(player, stack);
    }

    private static void addTag(Player player, ItemStack stack, String key, int delta, int min, int max) {
        int current = tag(stack, key);
        int next = Math.max(min, Math.min(max, current + delta));
        if (next != current) setTag(player, stack, key, next);
    }

    // ---------------------------------------------------------------- 穿着查询
    static ItemStack wornStack(Player player) {
        return BriefsEvents.wornStack(player);
    }

    static BriefsMaterialKind wornKind(Player player) {
        ItemStack stack = wornStack(player);
        return stack.getItem() instanceof BriefsArmorItem item ? item.kind() : null;
    }

    // ---------------------------------------------------------------- 音效
    /**
     * 音效广播：以 except=null 经 ServerLevel 广播,使触发者本人也能听到。
     * 直接用 Player.playSound 会把玩家自己作为 except 传给 PlayerList.broadcast 而被排除,导致穿内裤的玩家听不到任何机制音效。
     */
    private static void sound(Player player, SoundEvent sound, float volume, float pitch) {
        if (player == null) return;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), volume, pitch);
    }

    // ---------------------------------------------------------------- 服务端 tick
    static void tick(Player player) {
        BriefsMaterialKind kind = wornKind(player);
        ItemStack worn = wornStack(player);
        if (kind == null) return;
        if (worn.isEmpty() && kind != BriefsMaterialKind.LEATHER) return;
        switch (kind) {
            case NETHERITE -> netheriteTick(player);
            case DRAGON_HEAD -> dragonHeadTick(player, worn);
            case CHASTITY -> chastityTick(player, worn);
            case SLIME -> slimeTick(player, worn);
            case SPICY -> spicyTick(player, worn);
            case POOP -> poopTick(player);
            case TENTACLE -> tentacleTick(player);
            case EDIBLE -> edibleTick(player, worn);
            case TRAPDOOR -> trapdoorTick(player);
            case PROMOTION -> promotionTick(player, worn);
            case STICKY_PISTON -> stickyPistonTick(player);
            case GASEOUS -> gaseousTick(player);
            default -> { }
        }
    }

    private static void netheriteTick(Player player) {
        // 基础款：岩浆漂浮，保留。
        if (player.isInLava()) {
            player.clearFire();
            if (player.getDeltaMovement().y < 0.05D) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.05D, player.getDeltaMovement().z);
            }
        }
    }

    private static void dragonHeadTick(Player player, ItemStack worn) {
        // 岩浆/火焰漂浮 + 火焰免疫（免疫在 BriefsEvents 的 attack/hurt 里集中处理）。
        if (player.isInLava()) {
            player.clearFire();
            if (player.getDeltaMovement().y < 0.05D) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.05D, player.getDeltaMovement().z);
            }
        }
        // 龙息蓄力：待在火/岩浆里持续充能，满格时音效+岩浆粒子雨提示；离开后缓慢泄能。
        boolean hot = player.isInLava() || player.isOnFire();
        int fire = tag(worn, "fire");
        if (hot) {
            if (fire < FIRE_MAX) {
                fire++;
                setTag(player, worn, "fire", fire);
                if (fire == FIRE_MAX) {
                    sound(player,SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
                    for (int i = 0; i < 6; i++) {
                        player.level().addParticle(ParticleTypes.LAVA,
                                player.getX() + (player.getRandom().nextDouble() - 0.5D) * 1.2D, player.getY() + 0.6D,
                                player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 1.2D, 0.0D, 0.12D, 0.0D);
                    }
                }
            }
            if (player.tickCount % 4 == 0) {
                player.level().addParticle(ParticleTypes.FLAME,
                        player.getX() + (player.getRandom().nextDouble() - 0.5D), player.getY() + 0.3D,
                        player.getZ() + (player.getRandom().nextDouble() - 0.5D), 0.0D, 0.03D, 0.0D);
            }
        } else {
            addTag(player, worn, "fire", -1, 0, FIRE_MAX);
        }
    }

    private static void chastityTick(Player player, ItemStack worn) {
        // 守护层：12 秒(240 tick)未受伤则积一层，上限 3 层；受击在 tryBlockWithGuard 里消耗。
        int lastHit = tag(worn, "lastHit");
        if (lastHit == 0) {
            // 首次穿上：以当前 tick 作为计时起点，避免瞬间叠满。
            setTag(player, worn, "lastHit", player.tickCount);
            return;
        }
        int guard = tag(worn, "guard");
        if (guard < GUARD_MAX && player.tickCount - lastHit >= 240) {
            setTag(player, worn, "guard", guard + 1);
            sound(player,SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.6F, 1.3F);
        }
    }

    private static void slimeTick(Player player, ItemStack worn) {
        // 史莱姆弹跳:蹲下站定蓄力(spring 0~20),松开触发原版 LEVITATION 漂浮。
        // 蓄力等级 → amplifier=spring/5、持续时间=20+spring*4 tick,
        // 原版在 travelInAir 里自然处理重力(LEVITATION 让 y 速度平滑趋近 0.05*(amplifier+1))。
        int spring = tag(worn, "spring");
        if (player.isShiftKeyDown() && player.onGround()) {
            if (spring < SPRING_MAX) {
                spring++;
                setTag(player, worn, "spring", spring);
                if (spring == SPRING_MAX) {
                    sound(player,SoundEvents.SLIME_JUMP_SMALL, 0.9F, 2.0F);
                    for (int i = 0; i < 8; i++) {
                        player.level().addParticle(ParticleTypes.ITEM_SLIME,
                                player.getX() + (player.getRandom().nextDouble() - 0.5D), player.getY() + 0.4D,
                                player.getZ() + (player.getRandom().nextDouble() - 0.5D),
                                (player.getRandom().nextDouble() - 0.5D) * 0.1D, 0.15D, (player.getRandom().nextDouble() - 0.5D) * 0.1D);
                    }
                }
            }
            if (spring % 4 == 0) {
                player.level().addParticle(ParticleTypes.ITEM_SLIME,
                        player.getX() + (player.getRandom().nextDouble() - 0.5D), player.getY() + 0.2D,
                        player.getZ() + (player.getRandom().nextDouble() - 0.5D), 0.0D, -0.05D, 0.0D);
            }
        } else if (spring > 0 && player.onGround()) {
            // 存负 spring 作"待跳"flag,等 Mixin travel HEAD 时机触发 jumpFromGround。
            setTag(player, worn, "spring", -spring);
        }
    }

    /** Mixin 调:读 spring 待跳 flag,返回恢复后的 spring 等级并清 0。 */
    public static int consumeSlimeSpringFlag(Player player) {
        BriefsMaterialKind kind = wornKind(player);
        if (kind != BriefsMaterialKind.SLIME) return 0;
        ItemStack worn = wornStack(player);
        int spring = tag(worn, "spring");
        if (spring < 0) {
            setTag(player, worn, "spring", 0);
            return -spring;
        }
        return 0;
    }

    private static void spicyTick(Player player, ItemStack worn) {
        // 辣度蓄能：待火/岩浆快速升温，平时缓慢累积；高处辣度引爆敌人+喷火，见 onAttack/activeRightClick。
        int heat = tag(worn, "heat");
        if (player.isOnFire() || player.isInLava()) {
            heat = Math.min(HEAT_MAX, heat + 2);
        } else if (player.tickCount % 10 == 0) {
            heat = Math.min(HEAT_MAX, heat + 1);
        }
        setTag(player, worn, "heat", heat);
        if (heat >= 7 && player.tickCount % 8 == 0) {
            player.level().addParticle(ParticleTypes.FLAME,
                    player.getX() + (player.getRandom().nextDouble() - 0.5D), player.getY() + 0.8D,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5D), 0.0D, 0.04D, 0.0D);
        }
        if (heat >= HEAT_MAX && player.tickCount % 40 == 0) {
            // 满辣度：周期性提示已可空手右键喷火
            sound(player,SoundEvents.FIRECHARGE_USE, 0.6F, 1.6F);
        }
    }

    private static void poopTick(Player player) {
        // 粪臭光环：周期性地让附近生物中毒+减速。
        if (player.tickCount % 20 != 0) return;
        AABB box = player.getBoundingBox().inflate(3.5D);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && e instanceof Enemy)) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), player);
        }
        if (player.tickCount % 60 == 0) {
            player.level().addParticle(ParticleTypes.SMOKE,
                    player.getX() + (player.getRandom().nextDouble() - 0.5D), player.getY() + 0.4D,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5D), 0.0D, 0.05D, 0.0D);
        }
    }

    private static void tentacleTick(Player player) {
        // 水下呼吸，其余交给 onAttack 的触手抓取。
        if (player.isInWater()) {
            player.setAirSupply(player.getMaxAirSupply());
        }
    }

    private static void edibleTick(Player player, ItemStack worn) {
        // 饥饿时自动进食，每次累计一层"饱腹"，可在需要时释放回血，见 activeRightClick。
        if (player.getFoodData().getFoodLevel() < 18 && player.tickCount % 60 == 0) {
            addTag(player, worn, "full", 1, 0, FULL_MAX);
            player.getFoodData().eat(4, 0.5F);
            sound(player,SoundEvents.GENERIC_EAT.value(), 0.8F, 1.0F);
        }
    }

    private static void trapdoorTick(Player player) {
        // 潜伏：蹲下且几乎不移动时隐身（原地藏身），攻击触发背刺见 onAttack。
        boolean hidden = player.isShiftKeyDown() && player.getDeltaMovement().horizontalDistanceSqr() < 0.01D;
        if (hidden) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
        }
    }

    private static void promotionTick(Player player, ItemStack worn) {
        // 晋升冲击：军阶攒满自动"晋升"，爆发一次强力 buff 并击退周围敌人；军阶在击杀时累积。
        if (tag(worn, "rank") >= RANK_MAX) {
            setTag(player, worn, "rank", 0);
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 100, 0, false, false));
            AABB box = player.getBoundingBox().inflate(4.0D);
            for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive() && e instanceof Enemy)) {
                entity.knockback(1.2D, player.getX() - entity.getX(), player.getZ() - entity.getZ(),
                        player.level().damageSources().generic(), 0.0F);
            }
            sound(player,SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
            player.level().addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 0.0D, 0.5D, 0.0D);
        }
    }

    private static void stickyPistonTick(Player player) {
        // 磁力吸附：把附近的掉落物吸到自己身边。
        AABB box = player.getBoundingBox().inflate(4.0D);
        Vec3 center = player.position().add(0.0D, 0.5D, 0.0D);
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, box)) {
            Vec3 diff = center.subtract(item.position());
            double len = diff.lengthSqr();
            if (len > 0.05D) {
                item.setDeltaMovement(diff.normalize().scale(0.18D).add(0.0D, 0.08D, 0.0D));
                item.setNoPickUpDelay();
            }
        }
    }

    private static void gaseousTick(Player player) {
        // 气态操控:重力修改已由 MixinLivingEntity.getEffectiveGravity 注入处理(返回 0.01,复刻 SLOW_FALLING)。
        // 这里只负责视觉反馈:脚部持续云雾粒子让生效可见;蹲下解除重力钳制。
        if (player.tickCount % 8 == 0) {
            player.level().addParticle(ParticleTypes.CLOUD,
                    player.getX() + (player.getRandom().nextDouble() - 0.5D) * 0.6D, player.getY() + 0.1D,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 0.6D, 0.0D, 0.03D, 0.0D);
        }
    }

    // ---------------------------------------------------------------- 受伤（数值修改）
    /**
     * hurtServer 里 @ModifyVariable 调用的伤害数值修改器。
     * 返回修改后的伤害量（如 SHIELD 举盾减伤 70%）。
     */
    public static float modifyHurtDamage(Player player, DamageSource source, float damage) {
        BriefsMaterialKind kind = wornKind(player);
        if (kind == null) return damage;
        // 基础款：链甲远程减伤、下界合金爆炸减半（火焰取消在 ALLOW_DAMAGE）。
        if (kind == BriefsMaterialKind.CHAIN && source.is(DamageTypeTags.IS_PROJECTILE)) {
            damage *= 0.25F;
        } else if (kind == BriefsMaterialKind.NETHERITE && source.is(DamageTypeTags.IS_EXPLOSION)) {
            damage *= 0.5F;
        } else if (kind == BriefsMaterialKind.SHIELD && player.isShiftKeyDown()) {
            // 举盾架势：大幅减伤，且几率招架弹反。
            damage *= 0.3F;
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
            if (attacker != null && player.getRandom().nextFloat() < 0.35F) {
                attacker.knockback(0.8D, player.getX() - attacker.getX(), player.getZ() - attacker.getZ(),
                        player.level().damageSources().generic(), 0.0F);
                sound(player,SoundEvents.SHIELD_BLOCK.value(), 0.9F, 1.2F);
            }
        }
        return damage;
    }

    /** CHASTITY 完全格挡：guard>0 时消耗并返回 true（调用方应取消本次伤害）。 */
    static boolean tryBlockWithGuard(Player player) {
        ItemStack worn = wornStack(player);
        if (!(worn.getItem() instanceof BriefsArmorItem item) || item.kind() != BriefsMaterialKind.CHASTITY) {
            return false;
        }
        int guard = tag(worn, "guard");
        if (guard > 0) {
            setTag(player, worn, "guard", guard - 1);
            setTag(player, worn, "lastHit", player.tickCount);
            sound(player,SoundEvents.SHIELD_BLOCK.value(), 0.8F, 1.2F);
            player.level().addParticle(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 0.0D, 0.0D, 0.0D);
            return true;
        }
        setTag(player, worn, "lastHit", player.tickCount);
        return false;
    }

    /** AFTER_DAMAGE 受击反应（POOP/SILVERFISH/STICKY_PISTON/GASEOUS）。 */
    static void onHurt(Player player, DamageSource source) {
        BriefsMaterialKind kind = wornKind(player);
        if (kind == null) return;
        switch (kind) {
            case POOP -> {
                LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
                if (attacker != null) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
                    attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), player);
                    player.level().addParticle(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 0.0D, 0.2D, 0.0D);
                }
            }
            case SILVERFISH -> {
                LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
                if (attacker != null && player.getRandom().nextFloat() < 0.30F) {
                    spawnSilverfishPlayer(player, attacker);
                }
            }
            case STICKY_PISTON -> {
                LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
                if (attacker != null) {
                    attacker.knockback(1.6D, player.getX() - attacker.getX(), player.getZ() - attacker.getZ(),
                            player.level().damageSources().generic(), 0.0F);
                    sound(player,SoundEvents.PISTON_EXTEND, 0.9F, 1.0F);
                }
            }
            case GASEOUS -> {
                // 气体爆风：受击时炸开，击退四周敌人。
                AABB box = player.getBoundingBox().inflate(3.0D);
                for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != player && e.isAlive() && e instanceof Enemy)) {
                    entity.knockback(1.0D, entity.getX() - player.getX(), entity.getZ() - player.getZ(),
                            player.level().damageSources().generic(), 0.0F);
                }
                player.level().addParticle(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 0.5D, player.getZ(), 0.0D, 0.2D, 0.0D);
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- 耐久损耗
    /** 受伤时按原版盔甲损耗规则(损耗 = max(1, 伤害/4))磨损内裤栏物品,耐久耗尽则销毁并清空内裤栏。 */
    public static void damageWorn(Player player, float baseDamage) {
        if (player == null || player.level().isClientSide() || baseDamage <= 0.0F) return;
        ItemStack worn = BriefsData.getStack(player);
        if (worn.isEmpty() || !worn.isDamageableItem()) return;
        int durabilityDamage = (int) Math.max(1.0F, baseDamage / 4.0F);
        worn.hurtAndBreak(durabilityDamage, player, EquipmentSlot.CHEST);
        if (worn.isEmpty()) {
            BriefsData.setStack(player, ItemStack.EMPTY);
            BriefsEvents.refreshAttributes(player);
            if (player instanceof ServerPlayer serverPlayer) BriefsNetwork.sync(serverPlayer);
        } else {
            BriefsData.setStackQuiet(player, worn);
        }
    }

    // ---------------------------------------------------------------- 坠落反应
    /** checkFallDamage HEAD 调用；返回 true 表示已处理（应 cancel 原版坠落伤害）。 */
    public static boolean onFall(Player player, double distance) {
        BriefsMaterialKind kind = wornKind(player);
        if (kind == null) return false;
        if (kind == BriefsMaterialKind.SLIME) {
            if (distance > 2.0F) {
                double power = Math.min(0.85D, 0.2D + distance * 0.04D);
                player.setDeltaMovement(player.getDeltaMovement().x, power, player.getDeltaMovement().z);
            }
            if (player.tickCount % 2 == 0) {
                player.level().addParticle(ParticleTypes.ITEM_SLIME,
                        player.getX() + (player.getRandom().nextDouble() - 0.5D), player.getY() + 0.2D,
                        player.getZ() + (player.getRandom().nextDouble() - 0.5D), 0.0D, -0.05D, 0.0D);
            }
            return true;
        } else if (kind == BriefsMaterialKind.POOP) {
            if (distance > 2.0F) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.25D, player.getDeltaMovement().z);
                AABB box = player.getBoundingBox().inflate(2.0D);
                for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != player && e.isAlive())) {
                    entity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), player);
                }
                player.level().addParticle(ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 0.5D, player.getZ(), 0.0D, 0.3D, 0.0D);
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- 攻击反应
    static void onAttack(Player player, LivingEntity target) {
        BriefsMaterialKind kind = wornKind(player);
        if (kind == null) return;
        ItemStack worn = wornStack(player);
        switch (kind) {
            case DRAGON_HEAD -> {
                int fire = tag(worn, "fire");
                if (fire >= FIRE_MAX) {
                    setTag(player, worn, "fire", 0);
                    target.igniteForTicks(100);
                    target.hurtServer((ServerLevel) player.level(), player.level().damageSources().explosion(player, player), 4.0F);
                    target.knockback(1.0D, player.getX() - target.getX(), player.getZ() - target.getZ(),
                            player.level().damageSources().explosion(player, player), 4.0F);
                    player.level().addParticle(ParticleTypes.EXPLOSION_EMITTER,
                            target.getX(), target.getY() + 0.5D, target.getZ(), 0.0D, 0.0D, 0.0D);
                    sound(player,SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.0F);
                } else if (fire > 0) {
                    target.igniteForTicks(40);
                }
            }
            case SPICY -> {
                int heat = tag(worn, "heat");
                if (heat >= 5) {
                    target.igniteForTicks(60);
                    target.knockback(0.6D, player.getX() - target.getX(), player.getZ() - target.getZ(),
                            player.level().damageSources().generic(), 0.0F);
                    setTag(player, worn, "heat", heat - 2);
                }
            }
            case TENTACLE -> {
                if (player.isInWater()) {
                    Vec3 pull = player.position().add(0.0D, 1.0D, 0.0D).subtract(target.position()).normalize().scale(0.7D);
                    target.setDeltaMovement(pull.x, pull.y, pull.z);
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0), player);
                    player.level().addParticle(ParticleTypes.BUBBLE,
                            target.getX(), target.getY() + 0.5D, target.getZ(), 0.0D, 0.1D, 0.0D);
                }
            }
            case TRAPDOOR -> {
                if (player.isShiftKeyDown()) {
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), player);
                    target.knockback(0.5D, player.getX() - target.getX(), player.getZ() - target.getZ(),
                            player.level().damageSources().generic(), 0.0F);
                    player.level().addParticle(ParticleTypes.SMOKE,
                            target.getX(), target.getY() + 0.6D, target.getZ(), 0.0D, 0.1D, 0.0D);
                }
            }
            case SWORD -> {
                if (player.getRandom().nextFloat() < 0.30F) {
                    // 剑刃风暴：横扫周围多目标。
                    AABB box = target.getBoundingBox().inflate(3.0D);
                    for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box,
                            e -> e != target && e.isAlive() && e instanceof Enemy)) {
                        entity.hurtServer((ServerLevel) player.level(),
                                player.level().damageSources().sweetBerryBush(), 3.0F);
                    }
                    player.level().addParticle(ParticleTypes.SWEEP_ATTACK,
                            target.getX(), target.getY() + 0.5D, target.getZ(), 0.0D, 0.0D, 0.0D);
                }
            }
            case SHIELD -> {
                if (player.isShiftKeyDown()) {
                    target.knockback(1.4D, player.getX() - target.getX(), player.getZ() - target.getZ(),
                            player.level().damageSources().generic(), 0.0F);
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), player);
                    sound(player,SoundEvents.SHIELD_BLOCK.value(), 0.9F, 1.1F);
                }
            }
            case STICKY_PISTON -> {
                target.knockback(1.8D, player.getX() - target.getX(), player.getZ() - target.getZ(),
                        player.level().damageSources().generic(), 0.0F);
                sound(player,SoundEvents.PISTON_EXTEND, 0.9F, 1.0F);
            }
            default -> { }
        }
    }

    /** PROMOTION 击杀累积：每次击杀积一层军阶，攒满自动晋升。 */
    static void onKill(Player player, ItemStack worn) {
        if (worn.getItem() instanceof BriefsArmorItem item && item.kind() == BriefsMaterialKind.PROMOTION) {
            addTag(player, worn, "rank", 1, 0, RANK_MAX);
            promotionTick(player, worn);
        }
    }

    // ---------------------------------------------------------------- 主动技能（空手右键）
    static void activeRightClick(Player player) {
        BriefsMaterialKind kind = wornKind(player);
        if (kind == null) return;
        ItemStack worn = wornStack(player);
        switch (kind) {
            case SPICY -> {
                int heat = tag(worn, "heat");
                if (heat >= 8) {
                    setTag(player, worn, "heat", heat - 6);
                    AABB box = player.getBoundingBox().inflate(5.0D);
                    for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box,
                            e -> e != player && e.isAlive() && e instanceof Enemy)) {
                        entity.igniteForTicks(60);
                        entity.knockback(0.8D, entity.getX() - player.getX(), entity.getZ() - player.getZ(),
                                player.level().damageSources().generic(), 0.0F);
                    }
                    sound(player,SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F);
                    for (int i = 0; i < 8; i++) {
                        player.level().addParticle(ParticleTypes.FLAME,
                                player.getX() + (player.getRandom().nextDouble() - 0.5D), player.getY() + 0.8D,
                                player.getZ() + (player.getRandom().nextDouble() - 0.5D), 0.0D, 0.08D, 0.0D);
                    }
                }
            }
            case EDIBLE -> {
                int full = tag(worn, "full");
                if (full > 0) {
                    setTag(player, worn, "full", 0);
                    player.heal(full * 2.0F);
                    sound(player,SoundEvents.GENERIC_EAT.value(), 0.9F, 1.1F);
                }
            }
            default -> { }
        }
    }

    private static void spawnSilverfishPlayer(Player player, LivingEntity attacker) {
        int count = 1 + player.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) {
            Silverfish silverfish = EntityTypes.SILVERFISH.create(player.level(), EntitySpawnReason.MOB_SUMMONED);
            if (silverfish == null) continue;
            // 26.2: Entity 移除了 moveTo(x,y,z,yaw,pitch),改用 setPos + setYRot。
            silverfish.setPos(player.getX() + (player.getRandom().nextDouble() - 0.5D), player.getY(),
                    player.getZ() + (player.getRandom().nextDouble() - 0.5D));
            silverfish.setYRot(player.getRandom().nextFloat() * 360.0F);
            silverfish.setTarget(attacker);
            player.level().addFreshEntity(silverfish);
        }
    }
}