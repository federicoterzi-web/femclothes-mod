package com.femclothes.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class FemclothesScreenHandlers {

    /**
     * Extended, no un {@code ScreenHandlerType} común: la Mesa de Modelado
     * necesita que el handler del CLIENTE apunte al block entity REAL de esa
     * posición (no uno de mentira) para que la lista de fijadas —que no cabe
     * en un {@code PropertyDelegate}, viaja por NBT del block entity— llegue
     * actualizada. El payload es el {@code BlockPos} del bloque abierto.
     */
    public static final net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<
            com.femclothes.modelado.ModeladoScreenHandler, net.minecraft.util.math.BlockPos> MODELADO = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("femclothes", "modelado"),
            new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<>(
                    (syncId, inv, pos) -> com.femclothes.modelado.ModeladoScreenHandler.deCliente(syncId, inv, pos),
                    net.minecraft.util.math.BlockPos.PACKET_CODEC));

    /** Mismo motivo que MODELADO: los patrones aprendidos viajan por NBT del block entity, no por PropertyDelegate. */
    public static final net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<
            com.femclothes.tinturas.TinturasScreenHandler, net.minecraft.util.math.BlockPos> TINTURAS = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("femclothes", "tinturas"),
            new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<>(
                    (syncId, inv, pos) -> com.femclothes.tinturas.TinturasScreenHandler.deCliente(syncId, inv, pos),
                    net.minecraft.util.math.BlockPos.PACKET_CODEC));

    /** Mismo motivo que TINTURAS: nada que no quepa en un PropertyDelegate, pero igual mantiene el patrón. */
    public static final net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<
            com.femclothes.sublimadora.SublimadoraScreenHandler, net.minecraft.util.math.BlockPos> SUBLIMADORA = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("femclothes", "sublimadora"),
            new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<>(
                    (syncId, inv, pos) -> com.femclothes.sublimadora.SublimadoraScreenHandler.deCliente(syncId, inv, pos),
                    net.minecraft.util.math.BlockPos.PACKET_CODEC));

    /** Mismo motivo que TINTURAS/SUBLIMADORA: la lista de outfits guardados viaja por NBT del block entity, no por PropertyDelegate. */
    public static final net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<
            com.femclothes.guardarropas.GuardarropasScreenHandler, net.minecraft.util.math.BlockPos> GUARDARROPAS = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("femclothes", "guardarropas"),
            new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<>(
                    (syncId, inv, pos) -> com.femclothes.guardarropas.GuardarropasScreenHandler.deCliente(syncId, inv, pos),
                    net.minecraft.util.math.BlockPos.PACKET_CODEC));

    public static void init() {
        // fuerza class-loading
    }

    private FemclothesScreenHandlers() {}
}
