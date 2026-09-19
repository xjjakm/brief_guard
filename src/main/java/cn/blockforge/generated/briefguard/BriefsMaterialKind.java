package cn.blockforge.generated.briefguard;

public enum BriefsMaterialKind {
    LEATHER(1.0D, 0.0D, 0.0D),
    COPPER(2.0D, 0.5D, 0.0D),
    CHAIN(3.0D, 0.0D, 0.0D),
    IRON(4.0D, 0.0D, 0.0D),
    GOLD(3.0D, 0.0D, 1.0D),
    DIAMOND(5.0D, 0.5D, 0.0D),
    NETHERITE(7.0D, 1.0D, 0.0D),

    // 扩展：十四种以机制为主的内裤(数值与上游 E:\mod 一致)
    DRAGON_HEAD(6.0D, 0.5D, 0.0D),      // 龙首内裤：火焰免疫 + 岩浆漂浮 + 蓄能爆破
    CHASTITY(4.0D, 0.0D, 0.0D),         // 贞操带内裤：蓄层完全格挡
    SLIME(2.0D, 0.0D, 0.0D),            // 史莱姆内裤：免摔伤 + 子弹跳
    SPICY(2.0D, 0.0D, 0.0D),            // 辣条内裤：辣度蓄能喷火
    POOP(1.0D, 0.0D, 0.0D),             // 大粪内裤：粪臭光环 + 坠落溅射
    SILVERFISH(2.0D, 0.0D, 0.0D),       // 蠹虫内裤：受击召唤蠹虫
    TENTACLE(3.0D, 0.0D, 0.0D),         // 触手内裤：水下呼吸 + 水下抓取
    EDIBLE(1.0D, 0.0D, 0.0D),           // 可食用内裤：自动进食回血
    TRAPDOOR(2.0D, 0.0D, 0.0D),         // 活版门内裤：潜行隐身 + 背刺
    PROMOTION(3.0D, 0.0D, 2.0D),        // 晋升内裤：击杀累积晋升
    STICKY_PISTON(5.0D, 2.0D, 0.0D),    // 粘性活塞内裤：强力击退 + 吸附
    GASEOUS(2.0D, 0.0D, 0.0D),          // 气态内裤：空中缓降 + 受击喷气
    SWORD(8.0D, 0.5D, 0.0D),            // 剑型内裤：极高伤害 + 旋风斩
    SHIELD(4.0D, 1.0D, 0.0D);           // 盾牌内裤：举盾减伤 + 盾击

    private final double attackDamage;
    private final double attackKnockback;
    private final double luck;

    BriefsMaterialKind(double attackDamage, double attackKnockback, double luck) {
        this.attackDamage = attackDamage;
        this.attackKnockback = attackKnockback;
        this.luck = luck;
    }

    public double attackDamage() { return attackDamage; }
    public double attackKnockback() { return attackKnockback; }
    public double luck() { return luck; }
    public boolean usesBriefsSlot() { return this != LEATHER; }
}
