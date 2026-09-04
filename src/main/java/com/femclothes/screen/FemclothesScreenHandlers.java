package com.femclothes.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class FemclothesScreenHandlers {

    public static final ScreenHandlerType<ClothingLoomScreenHandler> CLOTHING_LOOM = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("femclothes", "clothing_loom"),
            new ScreenHandlerType<>(ClothingLoomScreenHandler::new, FeatureSet.empty()));

    public static void init() {
        // fuerza class-loading
    }

    private FemclothesScreenHandlers() {}
}
