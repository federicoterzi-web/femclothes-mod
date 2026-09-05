package com.ejemplo.sublimadora;

import dev.emi.trinkets.api.client.TrinketRendererRegistry;

/**
 * Registro del renderer de la remera puesta, aislado del resto del cliente.
 *
 * Mismo motivo que CameraptureClientCompat: si esto viviera en
 * SublimadoraModClient, la JVM tendria que resolver clases de Trinkets al
 * cargarlo y el mod no arrancaria sin Trinkets instalado. Sin Trinkets la
 * remera sigue siendo un item que se craftea y se estampa, solo que no se
 * puede vestir.
 */
final class TrinketsClientCompat {

    private TrinketsClientCompat() {}

    static void registrarRenderer() {
        TrinketRendererRegistry.registerRenderer(ModItems.REMERA, new RemeraTrinketRenderer());
    }
}
