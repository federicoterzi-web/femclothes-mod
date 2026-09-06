package com.femclothes;

import com.femclothes.body.ComandoCuerpo;
import com.femclothes.body.PerfilesDeCuerpo;
import com.femclothes.garment.PrendasDelMod;
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
        // Que items son prendas del sistema de capas. Va DESPUES de los
        // items: registra por instancia, no por id.
        PrendasDelMod.init();
        PerfilesDeCuerpo.init();
        ComandoCuerpo.init();
        ClothingLoomInteraction.init();
        FemclothesScreenHandlers.init();
    }
}
