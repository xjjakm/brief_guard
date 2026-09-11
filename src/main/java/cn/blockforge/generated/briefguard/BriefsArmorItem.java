package cn.blockforge.generated.briefguard;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * 26.2 中 ArmorItem 已移除,改为普通 Item + DataComponents 组件。
 * 装备槽/属性和耐久全部通过组件烘焙,特殊"内衣槽"逻辑仍然走 {@link BriefsData}。
 */
public final class BriefsArmorItem extends Item {
    private final BriefsMaterial material;
    private final BriefsMaterialKind kind;

    public BriefsArmorItem(BriefsMaterial material, BriefsMaterialKind kind, Item.Properties properties) {
        super(withAttributes(properties, material, kind));
        this.material = material;
        this.kind = kind;
    }

    private static Item.Properties withAttributes(Item.Properties properties, BriefsMaterial material, BriefsMaterialKind kind) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        addAttr(builder, Attributes.ATTACK_DAMAGE, kind.attackDamage(), BriefGuardMod.id("briefs.attack_damage"), EquipmentSlotGroup.MAINHAND);
        addAttr(builder, Attributes.ATTACK_KNOCKBACK, kind.attackKnockback(), BriefGuardMod.id("briefs.attack_knockback"), EquipmentSlotGroup.MAINHAND);
        addAttr(builder, Attributes.LUCK, kind.luck(), BriefGuardMod.id("briefs.luck"), EquipmentSlotGroup.MAINHAND);
        EquipmentSlotGroup slotGroup = kind == BriefsMaterialKind.LEATHER ? EquipmentSlotGroup.HEAD : EquipmentSlotGroup.CHEST;
        addAttr(builder, Attributes.ARMOR, material.defense(), BriefGuardMod.id("briefs.armor"), slotGroup);
        addAttr(builder, Attributes.ARMOR_TOUGHNESS, material.toughness(), BriefGuardMod.id("briefs.armor_toughness"), slotGroup);
        addAttr(builder, Attributes.KNOCKBACK_RESISTANCE, material.knockbackResistance(), BriefGuardMod.id("briefs.knockback_resistance"), slotGroup);
        return properties.component(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }

    private static void addAttr(ItemAttributeModifiers.Builder builder, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                double amount, Identifier id, EquipmentSlotGroup slotGroup) {
        if (amount != 0.0D) {
            builder.add(attribute, new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE), slotGroup);
        }
    }

    public BriefsMaterial material() {
        return material;
    }

    public BriefsMaterialKind kind() {
        return kind;
    }

    public boolean usesUnderwearSlot() {
        return kind.usesBriefsSlot();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.CONSUME;

        if (!usesUnderwearSlot()) {
            // 皮革:装备到头盔槽
            ItemStack previous = player.getItemBySlot(EquipmentSlot.HEAD);
            player.setItemSlot(EquipmentSlot.HEAD, held.split(1));
            if (!previous.isEmpty()) player.getInventory().placeItemBackInInventory(previous);
        } else {
            // 其它:装备到自定义内衣槽
            ItemStack previous = BriefsData.getStack(player);
            BriefsData.setStack(player, held.split(1));
            if (!previous.isEmpty()) player.getInventory().placeItemBackInInventory(previous);
        }

        player.playSound(material.equipSound().value(), 1.0F, 1.0F);
        BriefsEvents.refreshAttributes(player);
        if (player instanceof ServerPlayer serverPlayer) BriefsNetwork.sync(serverPlayer);
        return InteractionResult.CONSUME;
    }

    /** 26.2 中 Item.appendHoverText 被 @Deprecated 但无新替代签名(官方物品仍按此覆写),故压制告警。 */
    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        if (kind == BriefsMaterialKind.LEATHER) {
            builder.accept(Component.translatable("tooltip.brief_guard.leather_slot"));
        } else {
            builder.accept(Component.translatable("tooltip.brief_guard.underwear_slot"));
        }
        builder.accept(Component.translatable("tooltip.brief_guard.attack", String.format(Locale.ROOT, "%.1f", kind.attackDamage())));
    }
}