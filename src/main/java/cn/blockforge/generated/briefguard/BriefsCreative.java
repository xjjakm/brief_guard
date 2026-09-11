package cn.blockforge.generated.briefguard;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

public final class BriefsCreative {
    private BriefsCreative() {}

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register((FabricCreativeModeTabOutput output) -> {
            output.accept(new ItemStack(BriefGuardMod.LEATHER_BRIEFS));
            output.accept(new ItemStack(BriefGuardMod.COPPER_BRIEFS));
            output.accept(new ItemStack(BriefGuardMod.CHAIN_BRIEFS));
            output.accept(new ItemStack(BriefGuardMod.IRON_BRIEFS));
            output.accept(new ItemStack(BriefGuardMod.GOLD_BRIEFS));
            output.accept(new ItemStack(BriefGuardMod.DIAMOND_BRIEFS));
            output.accept(new ItemStack(BriefGuardMod.NETHERITE_BRIEFS));
        });
    }
}