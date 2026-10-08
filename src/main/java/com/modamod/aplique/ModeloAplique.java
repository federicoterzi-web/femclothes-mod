package com.modamod.aplique;

import com.modamod.Modamod;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

/**
 * Los modelos de aplique (2026-10-01, Mesa de estilado: "moños mariposas flores
 * etc"). Cada uno es un {@code .geo.json} de GeckoLib generado por
 * {@code tools/generar_apliques.py}, con 3 zonas de color (columnas del atlas
 * {@code textures/entity/aplique_atlas.png}). Se guarda por nombre: agregar
 * modelos nuevos en cualquier lugar no rompe lo guardado.
 */
public enum ModeloAplique implements StringIdentifiable {
    MONO("mono"),
    MARIPOSA("mariposa"),
    FLOR("flor"),
    /** Un ítem o bloque cualquiera dibujado por Minecraft (2026-10-04); ver {@link ObjetoAplique}. No tiene .geo.json. */
    OBJETO("objeto"),
    /** Bolsillo de canguro: parche plano de tela (2026-10-08). */
    CANGURO("canguro"),
    /** Corbata: nudo y hoja en tramos que cuelgan (2026-10-08). */
    CORBATA("corbata");

    public final String clave;

    ModeloAplique(String clave) {
        this.clave = clave;
    }

    /** Clave con la que GeckoLib guarda el modelo horneado. */
    public Identifier geo() {
        return Identifier.of(Modamod.MOD_ID, "geo/aplique_" + clave + ".geo.json");
    }

    public String traduccion() {
        return "modamod.aplique." + clave;
    }

    @Override
    public String asString() {
        return clave;
    }
}
