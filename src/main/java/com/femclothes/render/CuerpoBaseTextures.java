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

        boolean propio = perfil.cuerpo() == CuerpoBase.SKIN_REAL;
        for (Parte parte : Parte.values()) {
            // La cabeza NO lleva cuerpo base: es la cara del jugador y
            // taparla con un tono plano le borraria los ojos. Solo se dibuja
            // si una prenda la pide (la banda de una polera), y esa prenda
            // trae su propia tela.
            if (parte == Parte.CABEZA) continue;
            CajaSkin caja = LayoutSkin.base(parte, slim).escalada(S);
            if (propio && parte == Parte.TORSO) pintarTorsoHastaLaCintura(img, caja, tonoRgb, S);
            else pintarCuerpo(img, caja, tonoRgb);
        }

        multiplicar(img, perfil.cuerpo() == CuerpoBase.SKIN_REAL ? null : perfil.cuerpo().textura());
        superponer(img, perfil.interior().textura());
        return img;
    }

    /**
     * De las 12 filas del torso, las ultimas tres son la cintura.
     *
     * En una skin, la cintura del pantalon va pintada en las ultimas filas
     * del TORSO y no en las piernas. La regla vieja del mod —la prenda manda
     * de los hombros a la cintura, el pantalon de ahi abajo— sigue valiendo
     * mientras el cuerpo salga de la propia skin del jugador: pintar el torso
     * entero le borraria la cintura y le dejaria una costura visible contra
     * unas piernas que si conservan su pantalon.
     *
     * Con un cuerpo CURADO no aplica: ese cuerpo es dueño de su cintura y
     * trae su propia ropa interior, asi que se pinta completo.
     */
    private static final int CINTURA = 9;

    private static void pintarTorsoHastaLaCintura(NativeImage img, CajaSkin caja,
                                                  int tonoRgb, int escala) {
        int corte = caja.v() + caja.prof() + CINTURA * escala;
        pintar(img, caja.arriba(), tonoRgb, ARRIBA);
        // La tapa de abajo tampoco: esta a la altura de la cintura.
        pintar(img, recortar(caja.derecha(), corte), tonoRgb, LADO);
        pintar(img, recortar(caja.frente(), corte), tonoRgb, FRENTE);
        pintar(img, recortar(caja.izquierda(), corte), tonoRgb, LADO);
        pintar(img, recortar(caja.atras(), corte), tonoRgb, ATRAS);
    }

    private static CajaSkin.Rect recortar(CajaSkin.Rect r, int hasta) {
        return new CajaSkin.Rect(r.x0(), r.y0(), r.x1(), Math.min(r.y1(), hasta));
    }

    /** Cada cara del cuboide con su nivel de luz. */
    private static void pintarCuerpo(NativeImage img, CajaSkin caja, int tonoRgb) {
        pintar(img, caja.arriba(), tonoRgb, ARRIBA);
        pintar(img, caja.abajo(), tonoRgb, ABAJO);
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

    /** El png del cuerpo elegido, como mapa de sombras. Si no esta, no pasa nada. */
    private static void multiplicar(NativeImage img, @Nullable Identifier textura) {
        if (textura == null) return;
        NativeImage mapa = ClothingTextureCache.imagenBase(textura);
        if (mapa == null) return;
        for (int y = 0; y < Math.min(img.getHeight(), mapa.getHeight()); y++) {
            for (int x = 0; x < Math.min(img.getWidth(), mapa.getWidth()); x++) {
                int m = mapa.getColor(x, y);
                if (((m >> 24) & 0xFF) == 0) continue;
                int base = img.getColor(x, y);
                if (((base >> 24) & 0xFF) == 0) continue;
                int r = ((base & 0xFF) * (m & 0xFF)) / 255;
                int g = (((base >> 8) & 0xFF) * ((m >> 8) & 0xFF)) / 255;
                int b = (((base >> 16) & 0xFF) * ((m >> 16) & 0xFF)) / 255;
                img.setColor(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
            }
        }
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
