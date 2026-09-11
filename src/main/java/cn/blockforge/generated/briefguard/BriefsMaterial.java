package cn.blockforge.generated.briefguard;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 26.2 不再有 ArmorMaterial 接口;本类只是纯数据 holder。
 * 物料的耐久/防御等直接烘焙进物品属性组件里,见 {@link BriefsArmorItem}。
 */
public record BriefsMaterial(
        String name,
        int defense,
        float toughness,
        float knockbackResistance,
        int enchantmentValue,
        Holder<SoundEvent> equipSound,
        TagKey<Item> repairTag
) {
}