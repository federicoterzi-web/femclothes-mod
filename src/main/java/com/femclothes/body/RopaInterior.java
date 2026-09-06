package com.femclothes.body;

import com.femclothes.Femclothes;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

/**
 * Lo que trae puesto el cuerpo base.
 *
 * Viene baked en la textura del sustrato y no como prenda, porque el sustrato
 * se dibuja siempre que haya cualquier prenda: si la ropa interior fuera una
 * prenda mas, un croptop sin nada abajo dejaria el torso desnudo. BASICA es
 * el fallback y por eso es el default — el jugador no tiene que elegir nada
 * para que el mod se comporte.
 *
 * Los slots {@code torso/interior} y {@code piernas/interior} son otra cosa y
 * son posteriores: ahi va ropa interior TENIBLE, que se saca y se cambia. Lo
 * de aca es el minimo que siempre esta.
 */
public enum RopaInterior implements StringIdentifiable {

    BASICA("basica"),
    SLIP("slip"),
    BOXER("boxer"),
    BRALETTE("bralette"),
    DEPORTIVA("deportiva");

    public final String clave;

    RopaInterior(String clave) {
        this.clave = clave;
    }

    @Override
    public String asString() {
        return clave;
    }

    /**
     * La textura, en layout de skin a 8x, que se dibuja ENCIMA del cuerpo.
     *
     * Tambien opcional: sin el png el cuerpo sale sin ropa interior pintada.
     * Se compone aparte del cuerpo y no baked en cada uno para no multiplicar
     * el arte por cinco cuerpos.
     */
    public Identifier textura() {
        return Identifier.of(Femclothes.MOD_ID, "textures/entity/cuerpo/interior_" + clave + ".png");
    }

    public String traduccion() {
        return "femclothes.interior." + clave;
    }

    public RopaInterior siguiente() {
        return values()[(ordinal() + 1) % values().length];
    }
}
