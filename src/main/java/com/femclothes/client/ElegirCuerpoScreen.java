package com.femclothes.client;

import com.femclothes.body.CuerpoBase;
import com.femclothes.body.PerfilCuerpo;
import com.femclothes.body.PerfilesDeCuerpo;
import com.femclothes.body.RedCuerpo;
import com.femclothes.render.CuerpoBaseTextures;
import com.femclothes.render.GarmentFeatureRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Elegir el cuerpo base (2026-09-29, "la primera vez que uno se pone una
 * prenda del mod te lance una gui con el color de skin calculado y te de la
 * alternativa de elegir cualquiera de esas texturas base y agregar un
 * comando para overridearla").
 *
 * <ul>
 *   <li>Izquierda: vista previa 3D del jugador con el cuerpo y el tono que
 *   se están eligiendo (arrastrar para girar) — {@link GarmentFeatureRenderer#perfilOverride}.</li>
 *   <li>Centro: "Tu skin" + los 19 cuerpos del zip, cada uno con una
 *   miniatura de frente teñida con el tono.</li>
 *   <li>Derecha: el tono — de entrada el calculado de la skin; los sliders
 *   lo pasan a elegido a mano, y "Tono de mi skin" lo vuelve al calculado.</li>
 * </ul>
 * Confirmar lo guarda en el perfil (y ya no salta sola). "Ahora no" la
 * cierra sin guardar: vuelve a saltar la próxima sesión. {@code /femclothes
 * elegir} la abre cuando quieras.
 */
public class ElegirCuerpoScreen extends Screen {

    private static final int ANCHO = 480, ALTO = 250;
    private static final int PREVIEW_X1 = 8, PREVIEW_Y1 = 22, PREVIEW_X2 = 108, PREVIEW_Y2 = 222;
    private static final int GRILLA_X = 116, GRILLA_Y = 22, CELDA_W = 44, CELDA_H = 46, COLUMNAS = 5;
    private static final int DERECHA_X = 344, DERECHA_ANCHO = 128;
    /** Tono de reserva mientras la skin no bajó (el mismo beige que usa el resto del mod). */
    private static final int TONO_RESERVA = 0xC89F7E;

    private int x0, y0;
    private CuerpoBase elegido;
    /** null = el tono sale de la skin. */
    @Nullable private Integer tonoManual;
    private final SliderCanal[] sliders = new SliderCanal[3];
    private float anguloVista = 0f;
    private boolean arrastrando = false;

    public ElegirCuerpoScreen() {
        super(Text.translatable("femclothes.elegir_cuerpo.titulo"));
        ClientPlayerEntity jugador = MinecraftClient.getInstance().player;
        PerfilCuerpo perfil = jugador == null ? PerfilCuerpo.DEFECTO : PerfilesDeCuerpo.de(jugador);
        this.elegido = perfil.cuerpo();
        this.tonoManual = perfil.tonoDerivado() ? null : perfil.tono();
    }

    private int tonoDeLaSkin() {
        ClientPlayerEntity jugador = MinecraftClient.getInstance().player;
        Integer t = jugador == null ? null : CuerpoBaseTextures.tonoDeLaSkin(jugador);
        return t == null ? TONO_RESERVA : t;
    }

    private int tonoActual() {
        return tonoManual != null ? tonoManual : tonoDeLaSkin();
    }

    @Override
    protected void init() {
        x0 = (this.width - ANCHO) / 2;
        y0 = (this.height - ALTO) / 2;

        // Una celda por cuerpo: el click lo elige.
        CuerpoBase[] todos = CuerpoBase.values();
        for (int i = 0; i < todos.length; i++) {
            CuerpoBase c = todos[i];
            int cx = x0 + GRILLA_X + (i % COLUMNAS) * CELDA_W, cy = y0 + GRILLA_Y + (i / COLUMNAS) * CELDA_H;
            BotonCuerpo b = new BotonCuerpo(c, cx, cy);
            b.setTooltip(Tooltip.of(Text.translatable(c.traduccion())));
            addDrawableChild(b);
        }

        int t = tonoActual();
        for (int canal = 0; canal < 3; canal++) {
            sliders[canal] = new SliderCanal(x0 + DERECHA_X, y0 + 78 + canal * 18, canal, (t >> (16 - 8 * canal)) & 0xFF);
            addDrawableChild(sliders[canal]);
        }

        ButtonWidget deLaSkin = new EstiloPergamino.BotonPergamino(x0 + DERECHA_X, y0 + 136, DERECHA_ANCHO, 16,
                Text.translatable("femclothes.elegir_cuerpo.boton.skin"), b -> {
            tonoManual = null;
            sincronizarSliders();
        });
        deLaSkin.setTooltip(Tooltip.of(Text.translatable("femclothes.elegir_cuerpo.tooltip.skin")));
        addDrawableChild(deLaSkin);

        addDrawableChild(new EstiloPergamino.BotonPergamino(x0 + DERECHA_X, y0 + 200, DERECHA_ANCHO, 18,
                Text.translatable("femclothes.elegir_cuerpo.boton.confirmar"), b -> confirmar()));
        addDrawableChild(new EstiloPergamino.BotonPergamino(x0 + DERECHA_X, y0 + 222, DERECHA_ANCHO, 18,
                Text.translatable("femclothes.elegir_cuerpo.boton.despues"), b -> close()));
    }

    private void sincronizarSliders() {
        int t = tonoActual();
        for (int canal = 0; canal < 3; canal++) sliders[canal].fijar((t >> (16 - 8 * canal)) & 0xFF);
    }

    private void confirmar() {
        ClientPlayNetworking.send(new RedCuerpo.Elegir(elegido.clave,
                tonoManual == null ? PerfilCuerpo.TONO_DE_LA_SKIN : tonoManual));
        close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        c.drawText(textRenderer, this.title, x0 + 8, y0 + 8, EstiloPergamino.TEXTO, false);

        // Nombre del elegido debajo de la grilla y la ayuda del comando.
        c.drawText(textRenderer, Text.translatable("femclothes.elegir_cuerpo.elegido",
                Text.translatable(elegido.traduccion())), x0 + GRILLA_X, y0 + 212, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("femclothes.elegir_cuerpo.ayuda"),
                x0 + GRILLA_X, y0 + 230, EstiloPergamino.TEXTO_APAGADO, false);

        // Tono: muestra, y de dónde sale.
        c.drawText(textRenderer, Text.translatable("femclothes.elegir_cuerpo.tono"), x0 + DERECHA_X, y0 + 22, EstiloPergamino.TEXTO, false);
        int sx = x0 + DERECHA_X, sy = y0 + 34;
        c.fill(sx - 1, sy - 1, sx + DERECHA_ANCHO + 1, sy + 25, 0xFF2A180C);
        c.fill(sx, sy, sx + DERECHA_ANCHO, sy + 24, 0xFF000000 | tonoActual());
        c.drawText(textRenderer, Text.translatable(tonoManual == null
                        ? "femclothes.elegir_cuerpo.tono_skin" : "femclothes.elegir_cuerpo.tono_mano"),
                x0 + DERECHA_X, y0 + 64, EstiloPergamino.TEXTO, false);

        dibujarPreview(c, mouseY);
    }

    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        super.renderBackground(c, mouseX, mouseY, delta);
        // Panel pergamino con marco de madera, mismos colores que las máquinas.
        c.fill(x0 - 4, y0 - 4, x0 + ANCHO + 4, y0 + ALTO + 4, 0xFF4A2E1A);
        c.fill(x0 - 2, y0 - 2, x0 + ANCHO + 2, y0 + ALTO + 2, 0xFF7A5230);
        c.fill(x0, y0, x0 + ANCHO, y0 + ALTO, 0xFFDEBC84);
        c.fill(x0 + PREVIEW_X1, y0 + PREVIEW_Y1, x0 + PREVIEW_X2, y0 + PREVIEW_Y2, 0xFF2C221C);
        c.drawBorder(x0 + PREVIEW_X1 - 1, y0 + PREVIEW_Y1 - 1, PREVIEW_X2 - PREVIEW_X1 + 2, PREVIEW_Y2 - PREVIEW_Y1 + 2, 0xFF684818);
    }

    private void dibujarPreview(DrawContext c, int mouseY) {
        ClientPlayerEntity jugador = MinecraftClient.getInstance().player;
        if (jugador == null) return;
        PerfilCuerpo actual = PerfilesDeCuerpo.de(jugador);
        GarmentFeatureRenderer.perfilOverride = new PerfilCuerpo(elegido,
                tonoManual == null ? PerfilCuerpo.TONO_DE_LA_SKIN : tonoManual, actual.interior(), true);
        try {
            PreviewJugador.dibujar(c, jugador, x0 + PREVIEW_X1, y0 + PREVIEW_Y1, x0 + PREVIEW_X2, y0 + PREVIEW_Y2,
                    70, anguloVista, (float) mouseY);
        } finally {
            GarmentFeatureRenderer.perfilOverride = null;
        }
    }

    private boolean enPreview(double mx, double my) {
        return mx >= x0 + PREVIEW_X1 && mx < x0 + PREVIEW_X2 && my >= y0 + PREVIEW_Y1 && my < y0 + PREVIEW_Y2;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && enPreview(mx, my)) arrastrando = true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) arrastrando = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (arrastrando) {
            anguloVista = Math.floorMod(Math.round(anguloVista + (float) dx * 1.15f), 360);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    /**
     * Miniatura de frente: cabeza de la skin del jugador + torso, brazos y
     * piernas de la máscara (o de la skin, en "Tu skin"), teñidos con el tono.
     */
    private void dibujarMiniatura(DrawContext c, CuerpoBase cuerpo, int x, int y) {
        ClientPlayerEntity jugador = MinecraftClient.getInstance().player;
        if (jugador == null) return;
        SkinTextures skin = jugador.getSkinTextures();
        boolean skinReal = !cuerpo.tieneTextura();
        Identifier tex = skinReal ? skin.texture() : cuerpo.textura();
        int aw = (skinReal ? skin.model() == SkinTextures.Model.SLIM : cuerpo.slim) ? 3 : 4;
        float escala = 1.25f;
        int anchoFig = aw * 2 + 8, altoFig = 32;
        float fx = x + (CELDA_W - anchoFig * escala) / 2f, fy = y + (CELDA_H - altoFig * escala) / 2f;

        c.getMatrices().push();
        c.getMatrices().translate(fx, fy, 0);
        c.getMatrices().scale(escala, escala, 1f);
        RenderSystem.enableBlend();
        // Cabeza: siempre la cara del jugador (el cuerpo base no la tapa).
        c.drawTexture(skin.texture(), aw, 0, 8, 8, 8, 8, 64, 64);
        if (!skinReal) {
            int t = tonoActual();
            float k = 1.35f;
            RenderSystem.setShaderColor(Math.min(1f, ((t >> 16) & 0xFF) / 255f * k),
                    Math.min(1f, ((t >> 8) & 0xFF) / 255f * k), Math.min(1f, (t & 0xFF) / 255f * k), 1f);
        }
        c.drawTexture(tex, aw, 8, 20, 20, 8, 12, 64, 64);          // torso
        c.drawTexture(tex, 0, 8, 44, 20, aw, 12, 64, 64);          // brazo derecho (a la izquierda de frente)
        c.drawTexture(tex, aw + 8, 8, 36, 52, aw, 12, 64, 64);     // brazo izquierdo
        c.drawTexture(tex, aw, 20, 4, 20, 4, 12, 64, 64);          // pierna derecha
        c.drawTexture(tex, aw + 4, 20, 20, 52, 4, 12, 64, 64);     // pierna izquierda
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        c.getMatrices().pop();
    }

    /** Una celda de la grilla: miniatura, marco dorado si es el elegido. */
    private class BotonCuerpo extends ButtonWidget {
        private final CuerpoBase cuerpo;

        BotonCuerpo(CuerpoBase cuerpo, int x, int y) {
            super(x, y, CELDA_W - 2, CELDA_H - 2, Text.translatable(cuerpo.traduccion()), b -> elegido = cuerpo, DEFAULT_NARRATION_SUPPLIER);
            this.cuerpo = cuerpo;
        }

        @Override
        protected void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            c.fill(x, y, x + w, y + h, isHovered() ? 0xFFC9A66E : 0xFFB9956A);
            dibujarMiniatura(c, cuerpo, x, y);
            boolean sel = cuerpo == elegido;
            c.drawBorder(x, y, w, h, sel ? 0xFFFFD24C : 0xFF6B4E2A);
            if (sel) c.drawBorder(x + 1, y + 1, w - 2, h - 2, 0xFFFFD24C);
        }
    }

    /** Un canal R/G/B del tono (0..255): moverlo pasa el tono a elegido a mano. */
    private class SliderCanal extends SliderWidget {
        private static final String[] CLAVE = {"rojo", "verde", "azul"};
        private final int canal;

        SliderCanal(int x, int y, int canal, int valor) {
            super(x, y, DERECHA_ANCHO, 16, Text.empty(), valor / 255.0);
            this.canal = canal;
            updateMessage();
        }

        int nivel() {
            return (int) Math.round(this.value * 255);
        }

        void fijar(int valor) {
            this.value = valor / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("femclothes.elegir_cuerpo.canal." + CLAVE[canal], nivel()));
        }

        @Override
        protected void applyValue() {
            int t = 0;
            for (int i = 0; i < 3; i++) t |= sliders[i] == null ? 0 : sliders[i].nivel() << (16 - 8 * i);
            // 0 es "sacarlo de la skin" (PerfilCuerpo.TONO_DE_LA_SKIN): el negro puro elegido a mano va como 1.
            tonoManual = t == 0 ? 1 : t;
        }
    }
}
