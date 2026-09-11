package cn.blockforge.generated.briefguard;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;

public final class BriefsCreative {
    private BriefsCreative() {}

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.add(BriefGuardMod.LEATHER_BRIEFS);
            entries.add(BriefGuardMod.COPPER_BRIEFS);
            entries.add(BriefGuardMod.CHAIN_BRIEFS);
            entries.add(BriefGuardMod.IRON_BRIEFS);
            entries.add(BriefGuardMod.GOLD_BRIEFS);
            entries.add(BriefGuardMod.DIAMOND_BRIEFS);
            entries.add(BriefGuardMod.NETHERITE_BRIEFS);
        });
    }
}
