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
    private ButtonWidget btnEquipar;
    private ButtonWidget btnCandado;
    private final ButtonWidget[] btnFijadas = new ButtonWidget[GuardarropasBlockEntity.FIJADAS_MAXIMO];

    public GuardarropasScreen(GuardarropasScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = ANCHO;
        this.backgroundHeight = ALTO;
        this.titleX = M_MEDIO;
        this.titleY = 8;
        this.playerInventoryTitleX = M_MEDIO;
        this.playerInventoryTitleY = 170;
    }

    @Override
    protected void init() {
        // Estilo del mod con el metal del mueble (2026-09-30, "armame la gui...
        // con el estilo del mod y los colores de sus acentos metalicos").
        EstiloPergamino.usarTema(EstiloPergamino.Tema.PLATA);
        super.init();

        btnVista = new EstiloPergamino.BotonPergamino(this.x + PREVIEW_X1_LOCAL, this.y + 166, 86, 16, Text.literal(""), b -> anguloVista = Math.floorMod(Math.round(anguloVista) + 90, 360));
        this.addDrawableChild(btnVista);

        // Fila de botones/fijadas corrida a y=104 — a partir del ajuste
        // "4 slots por categoria" (2026-09-20) la grilla de borrador ocupa
        // 4 filas (y=20..98), no una sola (y=20..38) como antes.
        btnFijar = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO, this.y + 104, 78, 16, Text.translatable("femclothes.guardarropas.fijar"), b -> clickBoton(GuardarropasBlockEntity.BTN_FIJAR));
        this.addDrawableChild(btnFijar);

        btnEquipar = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO + 82, this.y + 104, 80, 16, Text.translatable("femclothes.guardarropas.equipar"), b -> clickBoton(GuardarropasBlockEntity.BTN_EQUIPAR));
        this.addDrawableChild(btnEquipar);
        // Candado (2026-10-04, "le pongamos un lock... tambien al guardarropas").
        btnCandado = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO, this.y + 146, 162, 16, Text.literal(""), b -> clickBoton(GuardarropasBlockEntity.BTN_CANDADO));
        this.addDrawableChild(btnCandado);

        for (int i = 0; i < btnFijadas.length; i++) {
            int id = GuardarropasBlockEntity.BTN_FIJADA_BASE + i;
            btnFijadas[i] = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO + i * 20, this.y + 124, 18, 18, Text.literal(Integer.toString(i + 1)), b -> clickBoton(id));
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
        boolean toca = handler.puedeTocar();
        btnFijar.active = toca;
        btnEquipar.active = toca;
        btnCandado.active = com.femclothes.util.Candado.puedeAlternar(handler.estado());
        btnCandado.setMessage(Text.translatable(com.femclothes.util.Candado.cerrado(handler.estado())
                ? "femclothes.candado.cerrado" : "femclothes.candado.abierto"));
        for (int i = 0; i < btnFijadas.length; i++) {
            btnFijadas[i].active = toca && i <= cantidad;
            btnFijadas[i].setMessage(Text.literal(i == seleccionado ? "[" + (i + 1) + "]" : Integer.toString(i + 1)));
        }
    }

    /** Las 4 prendas del borrador, combinadas — reemplaza lo que el jugador tenga puesto de verdad, solo para este preview. */
    private void dibujarPreview(DrawContext context, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity jugador = client.player;
        if (jugador == null) return;

        List<ItemStack> prendas = new ArrayList<>();
        for (int slot = 0; slot < GuardarropasBlockEntity.PRENDAS; slot++) {
            ItemStack stack = handler.be.getStack(slot);
            if (!stack.isEmpty()) prendas.add(stack);
        }

        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        dibujarConOutfit(context, jugador, handler.be, prendas, x1, y1, x2, y2, anguloVista, mouseY);
    }

    /**
     * El preview del jugador con las prendas Y la armadura de {@code inv}
     * (2026-09-30, columna de armadura): la armadura se pone de mentira en
     * el inventario del jugador SOLO del cliente, directo en la lista (sin
     * equipStack: nada de sonidos ni eventos), y se devuelve en el mismo
     * frame. Una pieza vacía en el outfit deja ver la que el jugador tiene
     * puesta, igual que "Equipar" no desequipa nada.
     */
    static void dibujarConOutfit(DrawContext context, PlayerEntity jugador, net.minecraft.inventory.Inventory inv,
                                 List<ItemStack> prendas, int x1, int y1, int x2, int y2,
                                 float angulo, int mouseY) {
        var armadura = jugador.getInventory().armor;
        ItemStack[] antes = new ItemStack[armadura.size()];
        for (int i = 0; i < antes.length; i++) antes[i] = armadura.get(i);
        GarmentFeatureRenderer.previewOverride = prendas;
        ItemStack sombrero = inv.getStack(GuardarropasBlockEntity.SLOT_SOMBRERO);
        GarmentFeatureRenderer.sombreroOverride = sombrero.isEmpty() ? null : sombrero;
        try {
            for (int i = 0; i < GuardarropasBlockEntity.SLOTS_ARMADURA.length; i++) {
                ItemStack pieza = inv.getStack(GuardarropasBlockEntity.ARMADURA_INICIO + i);
                if (!pieza.isEmpty()) armadura.set(GuardarropasBlockEntity.SLOTS_ARMADURA[i].getEntitySlotId(), pieza);
            }
            PreviewJugador.dibujar(context, jugador, x1, y1, x2, y2, 35, angulo, (float) mouseY);
        } finally {
            GarmentFeatureRenderer.previewOverride = null;
            GarmentFeatureRenderer.sombreroOverride = null;
            for (int i = 0; i < antes.length; i++) armadura.set(i, antes[i]);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX,
                this.playerInventoryTitleY, EstiloPergamino.TEXTO, false);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
        marcoSombrero(context, this.x, this.y);
    }

    /** Marco del slot del sombrero (2026-10-05), dibujado por código como los de la grilla de la chaqueta. */
    static void marcoSombrero(DrawContext c, int ox, int oy) {
        int x0 = ox + GuardarropasScreenHandler.X_ARMADURA, y0 = oy + GuardarropasScreenHandler.Y_SOMBRERO;
        c.fill(x0 - 1, y0 - 1, x0 + 17, y0 + 17, 0xFF2A180C);
        c.fill(x0, y0, x0 + 16, y0 + 16, 0xFF5A4028);
        c.drawTexture(Identifier.of("femclothes", "textures/gui/slot/sombrero.png"), x0, y0, 0, 0, 16, 16, 16, 16);
    }

}
