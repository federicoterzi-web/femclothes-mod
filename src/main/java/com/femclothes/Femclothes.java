package com.femclothes;

import com.femclothes.item.FemclothesComponents;
import com.femclothes.item.FemclothesItems;
import com.femclothes.screen.FemclothesScreenHandlers;
import net.fabricmc.api.ModInitializer;

public class Femclothes implements ModInitializer {

    public static final String MOD_ID = "femclothes";

    @Override
    public void onInitialize() {
        FemclothesItems.init();
        FemclothesComponents.init();
        ClothingLoomInteraction.init();
        FemclothesScreenHandlers.init();
    }
}
