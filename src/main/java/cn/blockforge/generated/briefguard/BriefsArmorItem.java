package cn.blockforge.generated.briefguard;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class BriefsArmorItem extends ArmorItem {
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("b0e3db54-9a74-4e2d-9a64-cd6f2b2e6f11");
    private static final UUID ATTACK_KNOCKBACK_UUID = UUID.fromString("7aa1c4de-3c51-4a2e-a7e0-3a76e38c9022");
    private static final UUID LUCK_UUID = UUID.fromString("3c2fd4e5-2221-4c0b-b57c-6fdf4c8ddf22");
    private final BriefsMaterialKind kind;
    private final Multimap<Attribute, AttributeModifier> handModifiers;

    public BriefsArmorItem(ArmorMaterial material, Type type, BriefsMaterialKind kind, Item.Properties properties) {
        super(material, type, properties);
        this.kind = kind;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        if (kind.attackDamage() != 0.0D) {
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_UUID, "Briefs attack damage", kind.attackDamage(), AttributeModifier.Operation.ADDITION));
        }
        if (kind.attackKnockback() != 0.0D) {
            builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(ATTACK_KNOCKBACK_UUID, "Briefs attack knockback", kind.attackKnockback(), AttributeModifier.Operation.ADDITION));
        }
        if (kind.luck() != 0.0D) {
            builder.put(Attributes.LUCK, new AttributeModifier(LUCK_UUID, "Gold briefs luck", kind.luck(), AttributeModifier.Operation.ADDITION));
        }
        this.handModifiers = builder.build();
    }

    public BriefsMaterialKind kind() {
        return kind;
    }

    public boolean usesUnderwearSlot() {
        return kind.usesBriefsSlot();
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return kind == BriefsMaterialKind.LEATHER ? EquipmentSlot.HEAD : EquipmentSlot.CHEST;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) return handModifiers;
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!usesUnderwearSlot()) return super.use(level, player, hand);
        if (!level.isClientSide()) {
            ItemStack previous = BriefsData.getStack(player);
            BriefsData.setStack(player, held);
            held.shrink(1);
            if (!previous.isEmpty()) player.getInventory().placeItemBackInInventory(previous);
            player.playSound(getEquipSound(), 1.0F, 1.0F);
            BriefsEvents.refreshAttributes(player);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) BriefsNetwork.sync(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (kind == BriefsMaterialKind.LEATHER) tooltip.add(Component.translatable("tooltip.brief_guard.leather_slot"));
        else tooltip.add(Component.translatable("tooltip.brief_guard.underwear_slot"));
        tooltip.add(Component.translatable("tooltip.brief_guard.attack", String.format("%.1f", kind.attackDamage())));
    }

    public static boolean isWearing(LivingEntity entity, BriefsMaterialKind wanted) {
        if (!(entity instanceof Player player)) return false;
        if (wanted == BriefsMaterialKind.LEATHER) return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == BriefGuardMod.LEATHER_BRIEFS;
        ItemStack worn = BriefsData.getStack(player);
        return worn.getItem() instanceof BriefsArmorItem item && item.kind() == wanted;
    }
}
