package com.femclothes.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La pollera — quinta vuelta (2026-09-17). Las 4 anteriores se armaron a
 * mano en Java; esta lee la geometría de archivos JSON diseñados con un
 * Artifact de claude.ai (formato clásico de modelo de bloque/ítem de
 * Minecraft: {@code elements}, cada uno con {@code from/to} y una
 * {@code rotation} de un solo eje) — 4 estilos (campana/gajos/dos_vuelos/
 * movimiento) × 3 largos (rodilla/media_pierna/tobillo), en
 * {@code assets/femclothes/pollera_moldes/}.
 *
 * <h2>Conversión de coordenadas</h2>
 * El archivo usa la convención de modelo de bloque: Y crece hacia
 * ARRIBA, cadera en Y=12, pies en Y=0 (documentado en su propio
 * {@code credit}), X/Z de 0 a 16 centrado en 8. Nuestro {@code ModelPart}
 * usa la convención de entidad: Y crece hacia ABAJO desde el pivote del
 * TORSO (cadera también en Y=12 — coincide porque el torso mide 12 de
 * alto), X/Z centrados en 0. La fórmula ({@link #ex}/{@link #ey}/{@link
 * #ez}) sale de igualar ambas cinturas: si el archivo mide "cuánto por
 * encima de los pies" y nosotros medimos "cuánto por debajo de la
 * cadera", la distancia por debajo de la cadera es {@code 12 - fileY}, y
 * sumada a la cadera en nuestra escala ({@code 12}) da {@code 24 - fileY}.
 *
 * <h2>Por qué no hace falta jerarquía</h2>
 * El campo {@code groups} del archivo es puramente organizativo (qué
 * cuboides pertenecen a qué "banda"/"gajo" en el editor) — cada
 * {@code element} ya trae su rotación y origen PROPIOS y ABSOLUTOS, no
 * relativos a un padre. Por eso alcanza con una raíz vacía que cuelga
 * todos los cuboides como hijos directos, sin replicar {@code groups}.
 */
public final class PolleraJsonGeometria {

    private PolleraJsonGeometria() {}

    /** Selección activa para pruebas (ver {@code DebugApariencia#pollera}) — antes de tener el molde real. */
    public static String estilo = "gajos";
    public static String largo = "rodilla";

    private static final Map<String, ModelPart> CACHE = new HashMap<>();

    public static ModelPart raiz(String estilo, String largo, float dilatacion) {
        String clave = estilo + "_" + largo + "_" + dilatacion;
        ModelPart cacheada = CACHE.get(clave);
        if (cacheada != null) return cacheada;

        Identifier id = Identifier.of("femclothes", "pollera_moldes/pollera_" + estilo + "_" + largo + ".json");
        ModelPart raiz = cargar(id, dilatacion);
        CACHE.put(clave, raiz);
        return raiz;
    }

    private static ModelPart cargar(Identifier id, float dilatacion) {
        try {
            Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (resource.isEmpty()) return new ModelPart(List.of(), Map.of());
            JsonObject root;
            try (InputStream stream = resource.get().getInputStream();
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            return construir(root, dilatacion);
        } catch (Exception e) {
            return new ModelPart(List.of(), Map.of());
        }
    }

    private static float ex(float fx) { return fx - 8f; }
    private static float ey(float fy) { return 24f - fy; }
    private static float ez(float fz) { return fz - 8f; }

    private static ModelPart construir(JsonObject root, float dilatacion) {
        JsonArray elementos = root.getAsJsonArray("elements");
        Map<String, ModelPart> hijos = new HashMap<>();
        EnumSet<Direction> caras = EnumSet.allOf(Direction.class);
        int i = 0;
        for (JsonElement el : elementos) {
            JsonObject e = el.getAsJsonObject();
            JsonArray from = e.getAsJsonArray("from");
            JsonArray to = e.getAsJsonArray("to");
            float ex0 = ex(from.get(0).getAsFloat()), ex1 = ex(to.get(0).getAsFloat());
            // el flip de Y invierte cual de los dos es el minimo
            float ey0 = ey(to.get(1).getAsFloat()), ey1 = ey(from.get(1).getAsFloat());
            float ez0 = ez(from.get(2).getAsFloat()), ez1 = ez(to.get(2).getAsFloat());

            float pivotX = 0, pivotY = 0, pivotZ = 0, yawGrados = 0;
            if (e.has("rotation")) {
                JsonObject rot = e.getAsJsonObject("rotation");
                JsonArray origen = rot.getAsJsonArray("origin");
                pivotX = ex(origen.get(0).getAsFloat());
                pivotY = ey(origen.get(1).getAsFloat());
                pivotZ = ez(origen.get(2).getAsFloat());
                // Eje siempre "y" en los 12 archivos (verificado). Signo
                // sin confirmar todavia contra la convencion de ModelPart
                // -- si sale espejado, este es el primer lugar a mirar.
                yawGrados = rot.get("angle").getAsFloat();
            }

            float lx0 = Math.min(ex0, ex1) - pivotX, lx1 = Math.max(ex0, ex1) - pivotX;
            float ly0 = Math.min(ey0, ey1) - pivotY, ly1 = Math.max(ey0, ey1) - pivotY;
            float lz0 = Math.min(ez0, ez1) - pivotZ, lz1 = Math.max(ez0, ez1) - pivotZ;

            ModelPart.Cuboid cubo = new ModelPart.Cuboid(0, 0,
                    lx0, ly0, lz0, lx1 - lx0, ly1 - ly0, lz1 - lz0,
                    dilatacion, dilatacion, dilatacion, false, 64, 64, caras);
            ModelPart parte = new ModelPart(List.of(cubo), Map.of());
            parte.pivotX = pivotX;
            parte.pivotY = pivotY;
            parte.pivotZ = pivotZ;
            parte.yaw = (float) Math.toRadians(yawGrados);

            hijos.put("e" + (i++), parte);
        }
        return new ModelPart(List.of(), hijos);
    }
}
