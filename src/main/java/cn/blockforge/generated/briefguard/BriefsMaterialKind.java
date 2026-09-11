package cn.blockforge.generated.briefguard;

public enum BriefsMaterialKind {
    LEATHER(1.0D, 0.0D, 0.0D),
    COPPER(2.0D, 0.5D, 0.0D),
    CHAIN(3.0D, 0.0D, 0.0D),
    IRON(4.0D, 0.0D, 0.0D),
    GOLD(3.0D, 0.0D, 1.0D),
    DIAMOND(5.0D, 0.5D, 0.0D),
    NETHERITE(7.0D, 1.0D, 0.0D);

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
