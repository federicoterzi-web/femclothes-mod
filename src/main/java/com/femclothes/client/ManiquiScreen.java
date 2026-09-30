package com.femclothes.client;

import com.femclothes.maniqui.ManiquiBlockEntity;
import com.femclothes.maniqui.ManiquiScreenHandler;
import com.femclothes.render.GarmentFeatureRenderer;
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
        super.render(context, mouseX, mouseY, delta);
        dibujarPreview(context, mouseY);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void dibujarPreview(DrawContext context, int mouseY) {
        PlayerEntity jugador = MinecraftClient.getInstance().player;
        if (jugador == null) return;
        GarmentFeatureRenderer.previewOverride = handler.be.prendasPuestas();
        try {
            PreviewJugador.dibujar(context, jugador,
                    this.x + PREVIEW_X1_LOCAL, this.y + PREVIEW_Y1_LOCAL,
                    this.x + PREVIEW_X2_LOCAL, this.y + PREVIEW_Y2_LOCAL,
                    35, anguloVista, (float) mouseY);
        } finally {
            GarmentFeatureRenderer.previewOverride = null;
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
        GuardarropasScreen.dibujarMarcosGrilla(context, this.x, this.y);
    }
}
