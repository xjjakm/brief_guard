package cn.blockforge.generated.briefguard;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class BriefsCreative {
    private static final ResourceKey<CreativeModeTab> BRIEFS_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, BriefGuardMod.id("briefs"));

    private BriefsCreative() {}

    public static void register() {
        // 新增物品栏分类:内裤
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, BRIEFS_TAB_KEY, FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.brief_guard"))
                .icon(() -> new ItemStack(BriefGuardMod.DIAMOND_BRIEFS))
                .displayItems((context, output) -> acceptAll(output::accept))
                .build());

        // 保留在战斗分类中的显示
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register((FabricCreativeModeTabOutput output) -> acceptAll(output::accept));
    }

    private static void acceptAll(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BriefGuardMod.LEATHER_BRIEFS));
        output.accept(new ItemStack(BriefGuardMod.COPPER_BRIEFS));
        output.accept(new ItemStack(BriefGuardMod.CHAIN_BRIEFS));
        output.accept(new ItemStack(BriefGuardMod.IRON_BRIEFS));
        output.accept(new ItemStack(BriefGuardMod.GOLD_BRIEFS));
        output.accept(new ItemStack(BriefGuardMod.DIAMOND_BRIEFS));
        output.accept(new ItemStack(BriefGuardMod.NETHERITE_BRIEFS));
    }
}