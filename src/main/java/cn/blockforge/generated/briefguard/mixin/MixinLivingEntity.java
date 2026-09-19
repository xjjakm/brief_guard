package cn.blockforge.generated.briefguard.mixin;

import cn.blockforge.generated.briefguard.BriefsEvents;
import cn.blockforge.generated.briefguard.BriefsMaterialKind;
import cn.blockforge.generated.briefguard.BriefsMechanic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.2 Fabric 缺 Forge 的 LivingFallEvent/LivingHurtEvent（改伤害数值）、无 EntityTick 事件，此处补桥：
 * <ul>
 *   <li>{@code causeFallDamage}：SLIME 免摔 + POOP 溅射（cancel 原版坠落伤害）</li>
 *   <li>{@code hurtServer}：SHIELD 举盾减伤（@ModifyVariable 改 damage 参数）</li>
 *   <li>{@code travel} HEAD：SLIME 蓄力触发跳跃（时机正确,在 gravity 前,客户端也能跑）</li>
 *   <li>{@code getEffectiveGravity}：气态内裤缓降（不产生药水粒子）</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void briefguard$fall(double fallDistance, float damageModifier, DamageSource damageSource,
                                 CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && !player.level().isClientSide()) {
            if (BriefsMechanic.onFall(player, fallDistance)) {
                cir.setReturnValue(false);
            }
        }
    }

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float briefguard$modifyDamage(float damage, ServerLevel level, DamageSource source) {
        if ((Object) this instanceof Player player && !level.isClientSide()) {
            return BriefsMechanic.modifyHurtDamage(player, source, damage);
        }
        return damage;
    }

    /** SLIME 蓄力触发跳跃:travel HEAD 时机(gravity 还没作用),手动调 jumpFromGround 再加 bonus。
     *  不守卫 isClientSide——jumpFromGround 客户端 LocalPlayer 也需要跑。 */
    @Inject(method = "travel", at = @At("HEAD"))
    private void briefguard$slimeJump(Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            int spring = BriefsMechanic.consumeSlimeSpringFlag(player);
            if (spring > 0 && player.onGround()) {
                player.jumpFromGround();
                player.addDeltaMovement(new Vec3(0, 0.1F * spring, 0));
            }
        }
    }

    /** 气态内裤:改重力(复刻原版 SLOW_FALLING 药水效果但不产生药水粒子)。 */
    @Inject(method = "getEffectiveGravity", at = @At("HEAD"), cancellable = true)
    private void briefguard$gaseousGravity(CallbackInfoReturnable<Double> cir) {
        if ((Object) this instanceof Player player) {
            BriefsMaterialKind kind = BriefsEvents.wornKind(player);
            if (kind == BriefsMaterialKind.GASEOUS && !player.onGround()
                    && player.getDeltaMovement().y <= 0.0D && !player.isShiftKeyDown()) {
                cir.setReturnValue(0.01D);
            }
        }
    }
}