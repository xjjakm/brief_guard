package cn.blockforge.generated.briefguard;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public final class BriefsMaterial implements ArmorMaterial {
    private final String name;
    private final int durability;
    private final int defense;
    private final float toughness;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float knockbackResistance;
    private final Ingredient repairIngredient;

    public BriefsMaterial(String name, int durability, int defense, float toughness, int enchantmentValue,
                          SoundEvent equipSound, float knockbackResistance, Ingredient repairIngredient) {
        this.name = name;
        this.durability = durability;
        this.defense = defense;
        this.toughness = toughness;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int durability(ArmorItem.Type type) {
        return durability * (type == ArmorItem.Type.HELMET ? 11 : 16);
    }

    @Override
    public int defense(ArmorItem.Type type) {
        return defense;
    }

    @Override
    public int enchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent equipSound() {
        return equipSound;
    }

    @Override
    public Ingredient repairIngredient() {
        return repairIngredient;
    }

    @Override
    public String getName() {
        return BriefGuardMod.MOD_ID + ":" + name;
    }

    @Override
    public float toughness() {
        return toughness;
    }

    @Override
    public float knockbackResistance() {
        return knockbackResistance;
    }
}
