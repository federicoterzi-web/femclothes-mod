package com.femclothes.render;

import com.femclothes.body.CuerpoBase;
import com.femclothes.body.PerfilCuerpo;
import com.femclothes.garment.Parte;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Compone la textura del CUERPO BASE: el sustrato sobre el que se apoya toda
 * la ropa.
 *
 * Reemplaza la reconstruccion de piel. Antes, cada prenda rellenaba con tono
 * de piel todo lo que dejaba transparente, y por eso dos prendas en la misma
 * pierna se borraban entre si. Ahora la piel se dibuja UNA vez, abajo de
 * todo, y cada prenda deja transparente lo que no cubre.
 *
 * La textura sale en LAYOUT DE SKIN a {@link CuerpoGeometria#ESCALA_CUERPO}x
 * —o sea 1x, una skin comun de 64x64—, DISTINTA de la escala de la tela
 * ({@link CuerpoGeometria#ESCALA_TELA}). No hace falta que coincidan: el UV
 * de un cuboide es la MISMA fraccion sea cual sea la escala (ver el javadoc
 * de {@link CuerpoGeometria}), asi que dos superficies con resoluciones
 * distintas siguen cayendo en el mismo lugar del cuerpo. Y el cuerpo no
 * necesita mas que esto: no hay fotos ni patrones sobre piel, nada mas fino
 * que un pixel de skin. Ir a 8x ahi son 64 veces los texels para dibujar
 * exactamente lo mismo.
 *
 * <h2>Como se arma</h2>
 * <ol>
 *   <li>tono plano por cara, sacado de la rampa;</li>
 *   <li>el png del cuerpo elegido multiplicado encima, si existe;</li>
 *   <li>la ropa interior compuesta arriba, si existe.</li>
 * </ol>
 * Los dos png son OPCIONALES. Sin ninguno sale un cuerpo liso sombreado por
 * cara — que es exactamente lo que producia la reconstruccion vieja, o sea
 * que el sistema anda completo desde el dia uno y el arte se enchufa despues
 * soltando archivos, igual que un patron.
 *
 * <h2>Pensado para la ruta 3DSL</h2>
 * Todo el pintado esta en metodos que reciben la imagen y una {@link CajaSkin},
 * sin suponer la escala. Cuando llegue el camino de 3D Skin Layers —componer
 * la tela DENTRO de la capa externa de la skin para que 3DSL la extruya— las
 * mismas rutinas sirven apuntando a una skin de 64x64.
 */
public final class CuerpoBaseTextures {

    /**
     * Cuanta luz recibe cada cara.
     *
     * Siguen la convencion con la que estan pintadas las skins de Minecraft:
     * frente y espalda al tono base, los costados mas oscuros, la tapa de
     * arriba mas clara y la de abajo la mas oscura. Plano por cara y no un
     * degrade, que es como se ve una caja de verdad y como esta pintado el
     * resto del pixel art.
     */
    private static final float FRENTE = 0.55f;
    private static final float ATRAS = 0.50f;
    private static final float LADO = 0.32f;
    private static final float ARRIBA = 0.85f;
    private static final float ABAJO = 0.15f;

    private static final Map<String, Identifier> CACHE = new HashMap<>();

    private CuerpoBaseTextures() {}

    /**
     * La textura del cuerpo de este jugador, o null si no se pudo componer.
     *
     * Null pasa cuando el tono tiene que salir de la skin y la skin todavia
     * no bajo. Es transitorio y no se cachea: el frame siguiente reintenta.
     */
    @Nullable
    public static Identifier de(LivingEntity entidad, PerfilCuerpo perfil, boolean slim) {
        Integer tono = tonoDe(entidad, perfil);
        if (tono == null) return null;

        String clave = perfil.clave() + "|" + Integer.toHexString(tono) + "|" + slim;
        Identifier cacheada = CACHE.get(clave);
        if (cacheada != null) return cacheada;

        NativeImage img;
        try {
            img = componer(perfil, tono, slim);
        } catch (Exception e) {
            return null;
        }

        Identifier id = Identifier.of("femclothes",
                "dynamic/cuerpo_" + Integer.toHexString(clave.hashCode()));
        MinecraftClient.getInstance().getTextureManager()
                .registerTexture(id, new NativeImageBackedTexture(img));
        CACHE.put(clave, id);
        if (com.femclothes.render.ClothingTextureCache.DEBUG_DUMP) {
            try {
                java.nio.file.Path dir = java.nio.file.Paths.get("femclothes_debug");
                java.nio.file.Files.createDirectories(dir);
                img.writeTo(dir.resolve("cuerpobase_" + Integer.toHexString(clave.hashCode()) + ".png"));
            } catch (java.io.IOException ignored) {}
        }
        return id;
    }

    /**
     * El tono a usar, en RGB.
     *
     * Si el perfil no fija uno, se saca de la skin del jugador UNA vez. Ese
     * es el default que no rompe la filosofia del mod: quien no elige nada
     * sigue viendose con su propio tono de piel.
     */
    @Nullable
    private static Integer tonoDe(LivingEntity entidad, PerfilCuerpo perfil) {
        if (!perfil.tonoDerivado()) return perfil.tono();
        return tonoDeLaSkin(entidad);
    }

    /** El tono sacado de la skin (RGB), o null si la skin todavía no bajó — también lo muestra la GUI de elegir cuerpo. */
    @Nullable
    public static Integer tonoDeLaSkin(LivingEntity entidad) {
        if (!(entidad instanceof AbstractClientPlayerEntity jugador)) {
            return abgrARgb(SkinToneSampler.fallbackTone());
        }
        SkinTextures skin = jugador.getSkinTextures();
        NativeImage img = SkinTextureAccess.tryGetImage(skin);
        if (img == null) return null;
        return abgrARgb(SkinToneSampler.sampleSkinTone(img, skin.model()));
    }

    private static NativeImage componer(PerfilCuerpo perfil, int tonoRgb, boolean slim) {
        final int S = CuerpoGeometria.ESCALA_CUERPO;
        final int lado = LayoutSkin.LADO * S;

        NativeImage img = new NativeImage(lado, lado, true);
        // Arranca vacia: solo se pinta el cuerpo, el resto queda transparente
        // y por ahi se sigue viendo la skin del jugador (la cara, sobre todo).
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) img.setColor(x, y, 0);
        }

        for (Parte parte : Parte.values()) {
            // La cabeza NO lleva cuerpo base: es la cara del jugador y
            // taparla con un tono plano le borraria los ojos. Solo se dibuja
            // si una prenda la pide (la banda de una polera), y esa prenda
            // trae su propia tela.
            if (parte == Parte.CABEZA) continue;
            CajaSkin caja = LayoutSkin.base(parte, slim).escalada(S);
            if (perfil.cuerpo().tieneTextura()) {
                // La mascara ya trae su propio sombreado: tono plano abajo,
                // si no se sombrea dos veces.
                for (CajaSkin.Rect cara : new CajaSkin.Rect[]{caja.arriba(), caja.abajo(), caja.derecha(),
                        caja.frente(), caja.izquierda(), caja.atras()}) {
                    pintarPlano(img, cara, tonoRgb);
                }
            } else {
                pintarCuerpo(img, caja, tonoRgb, parte);
            }
        }

        if (perfil.cuerpo().tieneTextura()) {
            NativeImage mascara = mascaraPara(perfil.cuerpo(), slim);
            if (mascara != null) multiplicarNormalizado(img, mascara);
        }
        superponer(img, perfil.interior().textura());
        return img;
    }

    /**
     * El gris de la mascara que deja el tono tal cual (2026-09-29, cuerpos
     * del zip): las mascaras humanas andan por 160-200, asi que el pecho sale
     * apenas mas oscuro que el tono elegido, las luces un poco mas claras y
     * las manchas negras de los animales, negras.
     */
    private static final float GRIS_NEUTRO = 180f;

    private static void pintarPlano(NativeImage img, CajaSkin.Rect r, int tonoRgb) {
        int abgr = 0xFF000000 | ((tonoRgb & 0xFF) << 16) | (tonoRgb & 0xFF00) | ((tonoRgb >> 16) & 0xFF);
        for (int y = Math.max(0, r.y0()); y < Math.min(r.y1(), img.getHeight()); y++) {
            for (int x = Math.max(0, r.x0()); x < Math.min(r.x1(), img.getWidth()); x++) img.setColor(x, y, abgr);
        }
    }

    /** Tono x gris/{@link #GRIS_NEUTRO}, solo donde hay cuerpo y mascara. */
    private static void multiplicarNormalizado(NativeImage img, NativeImage mascara) {
        for (int y = 0; y < Math.min(img.getHeight(), mascara.getHeight()); y++) {
            for (int x = 0; x < Math.min(img.getWidth(), mascara.getWidth()); x++) {
                int m = mascara.getColor(x, y);
                if (((m >> 24) & 0xFF) == 0) continue;
                int base = img.getColor(x, y);
                if (((base >> 24) & 0xFF) == 0) continue;
                int r = Math.min(255, Math.round((base & 0xFF) * (m & 0xFF) / GRIS_NEUTRO));
                int g = Math.min(255, Math.round(((base >> 8) & 0xFF) * ((m >> 8) & 0xFF) / GRIS_NEUTRO));
                int b = Math.min(255, Math.round(((base >> 16) & 0xFF) * ((m >> 16) & 0xFF) / GRIS_NEUTRO));
                img.setColor(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
    }

    private static final Map<String, NativeImage> MASCARAS = new HashMap<>();

    /**
     * La mascara del cuerpo con los brazos del ancho de la skin del jugador
     * (2026-09-29, "adaptar la textura sola"): cada mascara viene pintada
     * para brazos classic o slim; si no coincide, cada cara del brazo se
     * reescala a lo ancho (vecino mas cercano — de 4 a 3 se pierde una
     * columna, de 3 a 4 se repite una).
     */
    @Nullable
    public static NativeImage mascaraPara(CuerpoBase cuerpo, boolean slim) {
        String clave = cuerpo.clave + "|" + slim;
        NativeImage hecha = MASCARAS.get(clave);
        if (hecha != null) return hecha;
        NativeImage original = ClothingTextureCache.imagenBase(cuerpo.textura());
        if (original == null) return null;
        if (cuerpo.slim == slim) {
            MASCARAS.put(clave, original);
            return original;
        }
        NativeImage adaptada = new NativeImage(original.getWidth(), original.getHeight(), true);
        adaptada.copyFrom(original);
        for (Parte brazo : new Parte[]{Parte.BRAZO_DER, Parte.BRAZO_IZQ}) {
            CajaSkin desde = LayoutSkin.base(brazo, cuerpo.slim), hacia = LayoutSkin.base(brazo, slim);
            // Primero se limpia el lugar de las dos versiones (la classic es mas ancha).
            CajaSkin.Rect a = desde.todo(), b = hacia.todo();
            for (int y = Math.min(a.y0(), b.y0()); y < Math.max(a.y1(), b.y1()); y++) {
                for (int x = Math.min(a.x0(), b.x0()); x < Math.max(a.x1(), b.x1()); x++) adaptada.setColor(x, y, 0);
            }
            CajaSkin.Rect[] origen = {desde.arriba(), desde.abajo(), desde.derecha(), desde.frente(), desde.izquierda(), desde.atras()};
            CajaSkin.Rect[] destino = {hacia.arriba(), hacia.abajo(), hacia.derecha(), hacia.frente(), hacia.izquierda(), hacia.atras()};
            for (int k = 0; k < origen.length; k++) {
                CajaSkin.Rect o = origen[k], d = destino[k];
                for (int y = d.y0(); y < d.y1(); y++) {
                    int oy = o.y0() + (y - d.y0()) * o.altoRect() / Math.max(1, d.altoRect());
                    for (int x = d.x0(); x < d.x1(); x++) {
                        int ox = o.x0() + (x - d.x0()) * o.anchoRect() / Math.max(1, d.anchoRect());
                        adaptada.setColor(x, y, original.getColor(ox, oy));
                    }
                }
            }
        }
        MASCARAS.put(clave, adaptada);
        return adaptada;
    }

    /**
     * Cada cara del cuboide con su nivel de luz.
     *
     * 2026-09-16, probado en juego: la tapa de abajo del BRAZO es la
     * muñeca, no una zona lógicamente en sombra como la entrepierna del
     * torso o la planta del pie — con el mismo ABAJO=0.15 que esas dos
     * quedaba un cuadrado bien oscuro pegado directo contra el lateral
     * (LADO=0.32, mucho más claro), y esa diferencia tan brusca en un
     * área tan chica se leía como una línea recta de color, no un
     * sombreado suave. En el brazo se pinta la tapa de abajo al mismo
     * nivel que el lateral: mismo tono, sin costura.
     */
    private static void pintarCuerpo(NativeImage img, CajaSkin caja, int tonoRgb, Parte parte) {
        boolean brazo = parte == Parte.BRAZO_DER || parte == Parte.BRAZO_IZQ;
        pintar(img, caja.arriba(), tonoRgb, ARRIBA);
        pintar(img, caja.abajo(), tonoRgb, brazo ? LADO : ABAJO);
        pintar(img, caja.derecha(), tonoRgb, LADO);
        pintar(img, caja.frente(), tonoRgb, FRENTE);
        pintar(img, caja.izquierda(), tonoRgb, LADO);
        pintar(img, caja.atras(), tonoRgb, ATRAS);
    }

    /**
     * Un rectangulo con el tono a ese nivel, mas un ruido de un escalon.
     *
     * El ruido es lo que lo saca de "plancha de color": las skins pintadas a
     * mano nunca son un color uniforme, y una zona que si lo es canta al lado
     * de una que no. Es DETERMINISTA por pixel — si dependiera del azar
     * cambiaria en cada recomposicion y la piel titilaria.
     */
    private static void pintar(NativeImage img, CajaSkin.Rect r, int tonoRgb, float nivel) {
        for (int y = r.y0(); y < Math.min(r.y1(), img.getHeight()); y++) {
            for (int x = r.x0(); x < Math.min(r.x1(), img.getWidth()); x++) {
                int ruido = ((x * 73856093) ^ (y * 19349663)) & 0x7FFFFFFF;
                float desvio = (ruido % 3 - 1) * 0.02f;
                img.setColor(x, y, rampa(tonoRgb, nivel + desvio));
            }
        }
    }

    /**
     * El tono a esa altura de la escala, de 0 (sombra) a 1 (luz), en ABGR.
     *
     * Es una rampa y no tres tonos sueltos porque el sombreado de una caja
     * tiene cinco niveles distintos y elegir entre tres dejaba tres caras
     * pintadas iguales.
     */
    private static int rampa(int rgb, float nivel) {
        float f = 0.62f + 0.90f * Math.min(Math.max(nivel, 0f), 1f);
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round((rgb & 0xFF) * f));
        // NativeImage empaqueta ABGR, no ARGB: el rojo va en los bits bajos.
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    /** La ropa interior, encima del cuerpo. Alfa parcial mezcla en vez de pisar. */
    private static void superponer(NativeImage img, Identifier textura) {
        NativeImage encima = ClothingTextureCache.imagenBase(textura);
        if (encima == null) return;
        for (int y = 0; y < Math.min(img.getHeight(), encima.getHeight()); y++) {
            for (int x = 0; x < Math.min(img.getWidth(), encima.getWidth()); x++) {
                int px = encima.getColor(x, y);
                int a = (px >> 24) & 0xFF;
                if (a == 0) continue;
                if (a == 255) {
                    img.setColor(x, y, px);
                    continue;
                }
                int base = img.getColor(x, y);
                int r = mezclar(base & 0xFF, px & 0xFF, a);
                int g = mezclar((base >> 8) & 0xFF, (px >> 8) & 0xFF, a);
                int b = mezclar((base >> 16) & 0xFF, (px >> 16) & 0xFF, a);
                img.setColor(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
    }

    private static int mezclar(int abajo, int arriba, int alfa) {
        return (arriba * alfa + abajo * (255 - alfa)) / 255;
    }

    /** SkinToneSampler trabaja en ABGR; el resto del mod piensa en RGB. */
    private static int abgrARgb(int abgr) {
        return ((abgr & 0xFF) << 16) | (abgr & 0xFF00) | ((abgr >> 16) & 0xFF);
    }
}
