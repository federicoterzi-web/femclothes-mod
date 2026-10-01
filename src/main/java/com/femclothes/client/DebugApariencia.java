package com.femclothes.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Herramienta de debug (a pedido, 2026-09-16): cambiar de skin y alternar
 * slim/ancho sin tocar la cuenta real, para ver cómo queda la ropa sobre
 * cuerpos distintos mientras se prueba Calce y compañía.
 *
 * Cliente-only y solo para EL PROPIO jugador local (ver
 * {@code AbstractClientPlayerEntityMixin}) — no sincroniza nada, no es
 * parte del juego real, solo cambia lo que VE quien lo usa.
 *
 * Reusa las skins default de Minecraft ({@code DefaultSkinHelper}, que no
 * es público) en vez de bundlear arte propia — ya vienen en todas las
 * combinaciones slim/ancho que hacen falta para probar.
 */
public final class DebugApariencia {

    private DebugApariencia() {}

    private record SkinDePrueba(String nombre, Identifier textura, SkinTextures.Model modelo) {}

    private static final SkinDePrueba[] SKINS = {
            new SkinDePrueba("steve", Identifier.ofVanilla("textures/entity/player/wide/steve.png"), SkinTextures.Model.WIDE),
            new SkinDePrueba("alex", Identifier.ofVanilla("textures/entity/player/slim/alex.png"), SkinTextures.Model.SLIM),
            new SkinDePrueba("zuri", Identifier.ofVanilla("textures/entity/player/wide/zuri.png"), SkinTextures.Model.WIDE),
            new SkinDePrueba("noor", Identifier.ofVanilla("textures/entity/player/slim/noor.png"), SkinTextures.Model.SLIM),
            new SkinDePrueba("kai", Identifier.ofVanilla("textures/entity/player/wide/kai.png"), SkinTextures.Model.WIDE),
    };

    /** -1 = skin real del jugador. */
    private static int indiceSkin = -1;

    /** Fuerza el modelo de brazo sin importar la skin activa; null = el que traiga la skin. */
    @Nullable
    private static SkinTextures.Model modeloForzado = null;

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(raiz()));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> raiz() {
        return ClientCommandManager.literal("femclothesdebug")
                .then(ClientCommandManager.literal("skin").executes(ctx -> {
                    // OJO: (indiceSkin + 1) % (SKINS.length + 1) - 1 NO
                    // cicla -- para indiceSkin=-1 da (0)%n-1=-1 de vuelta,
                    // se queda pegado ahí para siempre (bug real, probado
                    // jugando: el comando corría pero la skin nunca
                    // cambiaba). Esto sí avanza.
                    indiceSkin = indiceSkin + 1 >= SKINS.length ? -1 : indiceSkin + 1;
                    feedback(ctx.getSource());
                    return 1;
                }))
                .then(ClientCommandManager.literal("slim").executes(ctx -> {
                    modeloForzado = SkinTextures.Model.SLIM;
                    feedback(ctx.getSource());
                    return 1;
                }))
                .then(ClientCommandManager.literal("ancho").executes(ctx -> {
                    modeloForzado = SkinTextures.Model.WIDE;
                    feedback(ctx.getSource());
                    return 1;
                }))
                .then(ClientCommandManager.literal("automodelo").executes(ctx -> {
                    modeloForzado = null;
                    feedback(ctx.getSource());
                    return 1;
                }))
                .then(ClientCommandManager.literal("reset").executes(ctx -> {
                    indiceSkin = -1;
                    modeloForzado = null;
                    feedback(ctx.getSource());
                    return 1;
                }))
                .then(pollera())
                .then(relieve());
    }

    /**
     * Cómo se comporta la pollera con las piernas (2026-09-29, "probaria las
     * dos"): {@code abierta} = el ruedo se corre donde pasa cada pierna,
     * {@code rigida} = quieta pero más ancha. Solo cambia lo que VE quien lo
     * usa, sin persistencia — es para comparar en el juego.
     */
    private static LiteralArgumentBuilder<FabricClientCommandSource> pollera() {
        LiteralArgumentBuilder<FabricClientCommandSource> raiz = ClientCommandManager.literal("pollera");
        for (String modo : new String[]{"abierta", "rigida"}) {
            raiz.then(ClientCommandManager.literal(modo).executes(ctx -> {
                com.femclothes.render.PolleraMalla.piernasAbiertas = modo.equals("abierta");
                ctx.getSource().sendFeedback(Text.literal("[femclothes debug] pollera " + modo
                        + (modo.equals("abierta") ? ": sigue las piernas y se mueve" : ": quieta, sin piernas ni movimiento")));
                return 1;
            }));
        }
        return raiz;
    }

    /**
     * Estilo del relieve (2026-10-01, "podemos hacer una muestra de prueba de
     * los dos?"): {@code suave} (superficie continua), {@code escalonado}
     * (bloquecitos tipo 3D Skin Layers) o {@code apagado} (las cajas de
     * siempre). Solo para quien lo usa, sin persistencia.
     */
    private static LiteralArgumentBuilder<FabricClientCommandSource> relieve() {
        LiteralArgumentBuilder<FabricClientCommandSource> raiz = ClientCommandManager.literal("relieve");
        for (com.femclothes.render.relieve.RelieveRender.Estilo e : com.femclothes.render.relieve.RelieveRender.Estilo.values()) {
            String nombre = e.name().toLowerCase(java.util.Locale.ROOT);
            raiz.then(ClientCommandManager.literal(nombre).executes(ctx -> {
                com.femclothes.render.relieve.RelieveRender.estilo = e;
                ctx.getSource().sendFeedback(Text.literal("[femclothes debug] relieve " + nombre));
                return 1;
            }));
        }
        return raiz;
    }

    private static void feedback(FabricClientCommandSource fuente) {
        String skin = indiceSkin < 0 ? "real" : SKINS[indiceSkin].nombre();
        String modelo = modeloForzado != null ? modeloForzado.getName()
                : (indiceSkin < 0 ? "el de tu skin" : SKINS[indiceSkin].modelo().getName());
        fuente.sendFeedback(Text.literal("[femclothes debug] skin=" + skin + " modelo=" + modelo));
    }

    /** Aplica el override activo (si hay) sobre la skin real del jugador local. */
    public static SkinTextures aplicar(SkinTextures original) {
        if (indiceSkin < 0 && modeloForzado == null) return original;
        Identifier textura = original.texture();
        SkinTextures.Model modelo = original.model();
        if (indiceSkin >= 0) {
            textura = SKINS[indiceSkin].textura();
            modelo = SKINS[indiceSkin].modelo();
        }
        if (modeloForzado != null) modelo = modeloForzado;
        return new SkinTextures(textura, original.textureUrl(), original.capeTexture(),
                original.elytraTexture(), modelo, original.secure());
    }
}
