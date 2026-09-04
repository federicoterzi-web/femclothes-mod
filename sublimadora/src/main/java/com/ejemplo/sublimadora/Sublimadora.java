package com.ejemplo.sublimadora;

import net.fabricmc.api.ModInitializer;

public class Sublimadora implements ModInitializer {

    @Override
    public void onInitialize() {
        // GeckoLib 4.8.4 es un ModInitializer propio y se arranca solo por
        // su entrypoint — no hay nada que llamar desde aca.
        ModBlocks.init();
    }
}
