package com.ejemplo.sublimadora;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;

public class SublimadoraMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ModBlocks.register();
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register(entries -> entries.add(ModBlocks.SUBLIMADORA_ITEM));
    }
}
