package cn.blockforge.generated.briefguard;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 26.2 不再有 ArmorMaterial 接口;本类只是纯数据 holder。
 * 物料的耐久/防御等直接烘焙进物品属性组件里,见 {@link BriefsArmorItem}。
 * repairTag 与 repairItem 二选一:优先 tag,为空时用单物品修复。
 */
public record BriefsMaterial(
        String name,
        int defense,
        float toughness,
        float knockbackResistance,
        int enchantmentValue,
        Holder<SoundEvent> equipSound,
        TagKey<Item> repairTag,
        Item repairItem
) {
    public BriefsMaterial(String name, int defense, float toughness, float knockbackResistance,
                          int enchantmentValue, Holder<SoundEvent> equipSound, TagKey<Item> repairTag) {
        this(name, defense, toughness, knockbackResistance, enchantmentValue, equipSound, repairTag, null);
    }

    public BriefsMaterial(String name, int defense, float toughness, float knockbackResistance,
                          int enchantmentValue, Holder<SoundEvent> equipSound, Item repairItem) {
        this(name, defense, toughness, knockbackResistance, enchantmentValue, equipSound, null, repairItem);
    }
}