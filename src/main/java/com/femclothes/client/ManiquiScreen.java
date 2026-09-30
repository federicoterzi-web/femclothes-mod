package com.femclothes.client;

import com.femclothes.maniqui.ManiquiBlockEntity;
import com.femclothes.maniqui.ManiquiScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Maniquí (2026-09-30): la grilla de 16 slots en el mismo lugar que el
 * Guardarropas (reusa su fondo), más Girar/Detener e "Intercambiar conmigo"
 * (la ropa del maniquí pasa a Trinkets y la tuya al maniquí). El preview de
 * la izquierda te muestra con la ropa del maniquí puesta — la figura real
 * ya se ve en el mundo.
 */
public class ManiquiScreen extends HandledScreen<ManiquiScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of("femclothes", "textures/gui/container/guardarropas.png");
    private static final int ANCHO = 482;
    private static final int ALTO = 264;
    private static final int M_MEDIO = ManiquiScreenHandler.M_MEDIO;

    private static final int PREVIEW_X1_LOCAL = 8, PREVIEW_Y1_LOCAL = 18, PREVIEW_X2_LOCAL = 94, PREVIEW_Y2_LOCAL = 164;
    private float anguloVista = 0f;
    private ButtonWidget btnVista;
    private ButtonWidget btnGirar;

    public ManiquiScreen(ManiquiScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = ANCHO;
        this.backgroundHeight = ALTO;
        this.titleX = M_MEDIO;
        this.titleY = 6;
        this.playerInventoryTitleX = M_MEDIO;
        this.playerInventoryTitleY = 170;
    }

    @Override
    protected void init() {
        super.init();

        btnVista = ButtonWidget.builder(Text.literal(""),
                        b -> anguloVista = Math.floorMod(Math.round(anguloVista) + 90, 360))
                .dimensions(this.x + PREVIEW_X1_LOCAL, this.y + 166, 86, 16)
                .build();
        this.addDrawableChild(btnVista);

        btnGirar = ButtonWidget.builder(Text.literal(""), b -> clickBoton(ManiquiBlockEntity.BTN_GIRAR))
                .dimensions(this.x + M_MEDIO, this.y + 104, 78, 16)
                .build();
        this.addDrawableChild(btnGirar);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("femclothes.maniqui.intercambiar"),
                        b -> clickBoton(ManiquiBlockEntity.BTN_INTERCAMBIAR))
                .dimensions(this.x + M_MEDIO + 82, this.y + 104, 80, 16)
                .build());

        // Poses y figura (2026-09-30).
        btnPose = ButtonWidget.builder(Text.literal(""), b -> clickBoton(ManiquiBlockEntity.BTN_POSE))
                .dimensions(this.x + M_MEDIO, this.y + 124, 78, 16)
                .build();
        this.addDrawableChild(btnPose);
        btnFigura = ButtonWidget.builder(Text.literal(""), b -> clickBoton(ManiquiBlockEntity.BTN_FIGURA))
                .dimensions(this.x + M_MEDIO + 82, this.y + 124, 80, 16)
                .build();
        this.addDrawableChild(btnFigura);

        // A la derecha de los botones (M_MEDIO + 162) y de la columna de armadura.
        int sx = this.x + M_MEDIO + 170;
        int ancho = this.x + ANCHO - 8 - sx;
        for (int i = 0; i < sliders.length; i++) {
            sliders[i] = new SliderPose(sx, this.y + 18 + i * 14, ancho, 12, i);
            this.addDrawableChild(sliders[i]);
        }
    }

    private ButtonWidget btnPose;
    private ButtonWidget btnFigura;
    private final SliderPose[] sliders = new SliderPose[com.femclothes.maniqui.PoseManiqui.ANGULOS];
    /** Mientras se arrastra un slider no se pisa con lo que llega del servidor. */
    private boolean arrastrando = false;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) arrastrando = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) arrastrando = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Un ángulo de la pose libre (2026-09-30, "pose libre por partes"): manda
     * el valor en grados por {@code PoseManiquiPayload}; el maniquí pasa a
     * pose Libre. Rango por eje en {@code PoseManiqui.RANGO}.
     */
    private final class SliderPose extends net.minecraft.client.gui.widget.SliderWidget {
        private final int indice;

        SliderPose(int x, int y, int ancho, int alto, int indice) {
            super(x, y, ancho, alto, Text.empty(), 0.5);
            this.indice = indice;
            this.value = aValor(handler.be.angulo(indice));
            updateMessage();
        }

        private float[] rango() { return com.femclothes.maniqui.PoseManiqui.RANGO[indice]; }

        private double aValor(float grados) {
            float[] r = rango();
            return (grados - r[0]) / (r[1] - r[0]);
        }

        private float grados() {
            float[] r = rango();
            return Math.round(r[0] + (float) value * (r[1] - r[0]));
        }

        void refrescar() {
            double v = aValor(handler.be.angulo(indice));
            if (Math.abs(v - value) > 0.001) {
                value = v;
                updateMessage();
            }
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("femclothes.maniqui.eje." + indice, (int) grados()));
        }

        @Override
        protected void applyValue() {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new com.femclothes.maniqui.PoseManiquiPayload(handler.be.getPos(), indice, grados()));
        }
    }

    private void clickBoton(int id) {
        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        btnVista.setMessage(Text.translatable("femclothes.preview.vista",
                Text.translatable(PreviewJugador.nombreVista(anguloVista))));
        btnGirar.setMessage(Text.translatable(handler.be.girando()
                ? "femclothes.maniqui.detener" : "femclothes.maniqui.girar"));
        btnPose.setMessage(Text.translatable("femclothes.maniqui.pose",
                Text.translatable(handler.be.pose().traduccion())));
        btnFigura.setMessage(Text.translatable(handler.be.figuraSkin()
                ? "femclothes.maniqui.figura.skin" : "femclothes.maniqui.figura.maniqui"));
        if (!arrastrando) for (SliderPose s : sliders) s.refrescar();
        super.render(context, mouseX, mouseY, delta);
        dibujarPreview(context, mouseY);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void dibujarPreview(DrawContext context, int mouseY) {
        PlayerEntity jugador = MinecraftClient.getInstance().player;
        if (jugador == null) return;
        GuardarropasScreen.dibujarConOutfit(context, jugador, handler.be, handler.be.prendasPuestas(),
                this.x + PREVIEW_X1_LOCAL, this.y + PREVIEW_Y1_LOCAL,
                this.x + PREVIEW_X2_LOCAL, this.y + PREVIEW_Y2_LOCAL, anguloVista, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
        GuardarropasScreen.dibujarMarcosGrilla(context, this.x, this.y);
    }
}
