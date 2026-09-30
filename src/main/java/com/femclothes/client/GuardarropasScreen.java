package com.femclothes.client;

import com.femclothes.guardarropas.GuardarropasBlockEntity;
import com.femclothes.guardarropas.GuardarropasScreenHandler;
import com.femclothes.render.GarmentFeatureRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Guardarropas — PLACEHOLDER (2026-09-20, "quiero que el guardarropas me
 * permita combinar un croptop con un remeron largo de medias red o un
 * pantalon con una pollera y una calza. 4 slots por prenda se combinan en
 * 1 look"): grilla de borrador de 4 categorías x 4 capas cada una (ver
 * {@link GuardarropasBlockEntity}) — varias prendas de la MISMA categoría
 * pueden estar puestas a la vez, no solo una — que se ven combinadas en
 * un preview 3D, más Fijar (guardar el borrador actual como outfit
 * nuevo) y una fila de outfits ya guardados para volver a cargarlos —
 * mismo mecanismo que las fijadas de Tinturas/Sublimadora.
 */
public class GuardarropasScreen extends HandledScreen<GuardarropasScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of("femclothes", "textures/gui/container/guardarropas.png");
    // A pedido (2026-09-20, "hagamos las gui del mismo tamaño"): mismo
    // ANCHO/ALTO que Tinturas/Modeladora.
    private static final int ANCHO = 482;
    private static final int ALTO = 264;
    private static final int M_MEDIO = GuardarropasScreenHandler.M_MEDIO;

    private static final int PREVIEW_X1_LOCAL = 8, PREVIEW_Y1_LOCAL = 18, PREVIEW_X2_LOCAL = 94, PREVIEW_Y2_LOCAL = 164;
    private float anguloVista = 0f;
    private boolean arrastrandoPreview = false;
    private ButtonWidget btnVista;
    private ButtonWidget btnFijar;
    private final ButtonWidget[] btnFijadas = new ButtonWidget[GuardarropasBlockEntity.FIJADAS_MAXIMO];

    public GuardarropasScreen(GuardarropasScreenHandler handler, PlayerInventory inventory, Text title) {
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

        // Fila de botones/fijadas corrida a y=104 — a partir del ajuste
        // "4 slots por categoria" (2026-09-20) la grilla de borrador ocupa
        // 4 filas (y=20..98), no una sola (y=20..38) como antes.
        btnFijar = ButtonWidget.builder(Text.translatable("femclothes.guardarropas.fijar"),
                        b -> clickBoton(GuardarropasBlockEntity.BTN_FIJAR))
                .dimensions(this.x + M_MEDIO, this.y + 104, 78, 16)
                .build();
        this.addDrawableChild(btnFijar);

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("femclothes.guardarropas.equipar"),
                        b -> clickBoton(GuardarropasBlockEntity.BTN_EQUIPAR))
                .dimensions(this.x + M_MEDIO + 82, this.y + 104, 80, 16)
                .build());

        for (int i = 0; i < btnFijadas.length; i++) {
            int id = GuardarropasBlockEntity.BTN_FIJADA_BASE + i;
            btnFijadas[i] = ButtonWidget.builder(Text.literal(Integer.toString(i + 1)), b -> clickBoton(id))
                    .dimensions(this.x + M_MEDIO + i * 20, this.y + 124, 18, 18)
                    .build();
            this.addDrawableChild(btnFijadas[i]);
        }
    }

    private void clickBoton(int id) {
        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

    private boolean dentroDePreview(double mouseX, double mouseY) {
        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && dentroDePreview(mouseX, mouseY)) arrastrandoPreview = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) arrastrandoPreview = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (arrastrandoPreview) {
            anguloVista = Math.floorMod(Math.round(anguloVista + (float) deltaX * 1.15f), 360);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refrescar();
        super.render(context, mouseX, mouseY, delta);
        dibujarPreview(context, mouseY);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void refrescar() {
        btnVista.setMessage(Text.translatable("femclothes.preview.vista",
                Text.translatable(PreviewJugador.nombreVista(anguloVista))));

        GuardarropasBlockEntity be = handler.be;
        int seleccionado = be.fijadaSeleccionada();
        int cantidad = be.fijadas().size();
        for (int i = 0; i < btnFijadas.length; i++) {
            btnFijadas[i].active = i <= cantidad;
            btnFijadas[i].setMessage(Text.literal(i == seleccionado ? "[" + (i + 1) + "]" : Integer.toString(i + 1)));
        }
    }

    /** Las 4 prendas del borrador, combinadas — reemplaza lo que el jugador tenga puesto de verdad, solo para este preview. */
    private void dibujarPreview(DrawContext context, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity jugador = client.player;
        if (jugador == null) return;

        List<ItemStack> prendas = new ArrayList<>();
        for (int slot = 0; slot < GuardarropasBlockEntity.TAMANO; slot++) {
            ItemStack stack = handler.be.getStack(slot);
            if (!stack.isEmpty()) prendas.add(stack);
        }

        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        GarmentFeatureRenderer.previewOverride = prendas;
        try {
            PreviewJugador.dibujar(context, jugador, x1, y1, x2, y2, 35, anguloVista, (float) mouseY);
        } finally {
            GarmentFeatureRenderer.previewOverride = null;
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
        dibujarMarcosGrilla(context, this.x, this.y);
    }

    /**
     * Marcos de la grilla de 5 categorías x 4 capas (2026-09-30, quinta
     * columna = chaquetas): el fondo {@code guardarropas.png} solo trae la
     * fila de 4 de la versión vieja, así que se dibujan por código — los
     * usan el Guardarropas y el Maniquí (mismas posiciones de slot).
     */
    static void dibujarMarcosGrilla(DrawContext context, int x, int y) {
        for (int categoria = 0; categoria < GuardarropasBlockEntity.CATEGORIAS; categoria++) {
            for (int capa = 0; capa < GuardarropasBlockEntity.POR_CATEGORIA; capa++) {
                int sx = x + M_MEDIO + categoria * 20 - 1, sy = y + 20 + capa * 20 - 1;
                context.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
                context.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
                context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
            }
        }
    }
}
