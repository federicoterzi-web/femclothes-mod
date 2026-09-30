package com.femclothes.body;

import com.femclothes.Femclothes;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

/**
 * La parte de abajo de la ropa interior del cuerpo (2026-09-30). No tiene
 * "ninguna": es el mínimo que siempre está. Viaja por ordinal: valores
 * nuevos, al final.
 */
public enum InteriorAbajo implements StringIdentifiable {
    SLIP("slip"),
    CULOTTE("culotte"),
    BOXER("boxer");

    public final String clave;

    InteriorAbajo(String clave) {
        this.clave = clave;
    }

    @Override
    public String asString() {
        return clave;
    }

    /** Textura en gris con el layout de la skin (se tiñe con el color elegido). */
    public Identifier textura() {
        return Identifier.of(Femclothes.MOD_ID, "textures/entity/cuerpo/interior_abajo_" + clave + ".png");
    }

    public String traduccion() {
        return "femclothes.interior.abajo." + clave;
    }

    public InteriorAbajo siguiente() {
        return values()[(ordinal() + 1) % values().length];
    }
}
