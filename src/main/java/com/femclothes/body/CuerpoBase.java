package com.femclothes.body;

import com.femclothes.Femclothes;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

/**
 * El cuerpo que va DEBAJO de la ropa.
 *
 * Reemplaza la reconstruccion de piel. Antes el mod leia la skin del jugador,
 * le sampleaba el tono y repintaba a mano las zonas que la prenda dejaba a la
 * vista, con toda la lista de trampas que eso trajo: leer pixeles de vuelta
 * desde la GPU, el empaquetado ABGR, adivinar cual pixel es piel y cual es
 * manga. Un cuerpo base elegido a mano borra el problema entero: el UV es
 * conocido y las zonas desnudas tambien.
 *
 * Set FIJO y no uno por prenda: es arte que se dibuja una vez.
 *
 * SKIN_REAL sigue siendo el default a proposito. El set curado son
 * alternativas, no un reemplazo: la tesis del mod es que la ropa ande con
 * cualquier skin, y arrancar reemplazandole el cuerpo al jugador la
 * contradice.
 */
public enum CuerpoBase implements StringIdentifiable {

    /**
     * El cuerpo del propio jugador: tono sampleado de su skin y sombreado por
     * cara, sin textura curada de por medio. Es lo que hacia el mod antes,
     * pero dibujado como sustrato en vez de repintado sobre la skin.
     */
    SKIN_REAL("skin_real"),

    PLANO("plano"),
    ATLETICO("atletico"),
    CURVY("curvy"),

    /** Pecho aplanado. Hoy es solo textura; si algun dia hay geometria de pecho, la suprime. */
    BINDER("binder");

    public final String clave;

    CuerpoBase(String clave) {
        this.clave = clave;
    }

    @Override
    public String asString() {
        return clave;
    }

    /** Si el tono sale de la skin del jugador cuando el perfil no fija uno. */
    public boolean derivaDeLaSkin() {
        return this == SKIN_REAL;
    }

    /**
     * El mapa de sombras del cuerpo, en layout de skin a 8x.
     *
     * Es OPCIONAL: si el png no esta, el cuerpo sale liso con el sombreado
     * por cara, que es exactamente lo que ya producia la reconstruccion
     * vieja. O sea que el arte se enchufa soltando un png, igual que un
     * patron, sin tocar codigo — y mientras no exista no se pierde nada
     * respecto de antes.
     */
    public Identifier textura() {
        return Identifier.of(Femclothes.MOD_ID, "textures/entity/cuerpo/" + clave + ".png");
    }

    public String traduccion() {
        return "femclothes.cuerpo." + clave;
    }

    public CuerpoBase siguiente() {
        return values()[(ordinal() + 1) % values().length];
    }
}
