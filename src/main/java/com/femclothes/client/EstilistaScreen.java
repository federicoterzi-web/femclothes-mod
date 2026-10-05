package com.femclothes.client;

import com.femclothes.estilista.EstilistaBlockEntity;
import com.femclothes.estilista.EstilistaScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * La Estilista automática (2026-10-05, etapa 3: "no quiero perder el tamaño del visualizador" + "más como las otras
 * guis con un gran slot para input y output y un apply en el medio"): a la izquierda la vista 3D de la Mesa de estilado
 * (apliques, correas, colores) editando la prenda de MUESTRA; a la derecha el panel de la máquina, como el de las
 * hermanas: entrada grande, Aplicar en el medio, salida grande, Fijar / Borrar el diseño, contadores de hilo y cuero
 * (se cargan soltando el ítem encima, con shift-click o con la mano en el bloque) y el almacén de plantillas, retazos y
 * objetos.
 */
public class EstilistaScreen extends EstiladoScreen {

    private static final int ANCHO_TOTAL = ANCHO + 6 + EstilistaScreenHandler.ANCHO_PANEL + 4;
    private final EstilistaScreenHandler yo;

    public EstilistaScreen(EstilistaScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.yo = handler;
    }

    @Override
    int panelExtra() { return 6 + EstilistaScreenHandler.ANCHO_PANEL; }

    @Override
    protected void init() {
        // El conjunto (vista + panel) se centra junto: HandledScreen centra con backgroundWidth.
        this.backgroundWidth = ANCHO_TOTAL + (handler.be.creativa() ? 170 : 0);
        super.init();
        this.backgroundWidth = ANCHO;
        int px = this.x + EstilistaScreenHandler.X_PANEL;
        boton("aplicar", EstilistaBlockEntity.BTN_APLICAR, px + 8 + EstilistaScreenHandler.GRANDE + 4, this.y + EstilistaScreenHandler.Y_MARCO + 4,
                EstilistaScreenHandler.ANCHO_PANEL - 16 - 2 * (EstilistaScreenHandler.GRANDE + 4), 18);
        boton("fijar", EstilistaBlockEntity.BTN_FIJAR, px + 8, this.y + EstilistaScreenHandler.Y_PANEL + 52, 54, 16);
        boton("borrar", EstilistaBlockEntity.BTN_BORRAR, px + EstilistaScreenHandler.ANCHO_PANEL - 8 - 54, this.y + EstilistaScreenHandler.Y_PANEL + 52, 54, 16);
    }

    private void boton(String clave, int id, int bx, int by, int w, int h) {
        ButtonWidget b = new EstiloPergamino.BotonPergamino(bx, by, w, h, Text.translatable("femclothes.estilista." + clave),
                btn -> this.client.interactionManager.clickButton(this.handler.syncId, id));
        b.setTooltip(Tooltip.of(Text.translatable("femclothes.estilista.tooltip." + clave)));
        this.addDrawableChild(b);
    }

    private void marcoGrande(DrawContext c, int mx, int my) {
        int g = EstilistaScreenHandler.GRANDE;
        c.fill(mx - 1, my - 1, mx + g + 1, my + g + 1, 0xFF2A180C);
        c.fill(mx, my, mx + g, my + g, 0xFF6B5A78);
        c.fill(mx + 2, my + 2, mx + g - 2, my + g - 2, 0xFFC9A877);
    }

    private void contador(DrawContext c, int y0, String clave, int valor, int color, String unidad) {
        int cx = this.x + EstilistaScreenHandler.X_CONTADOR, cy = this.y + y0, w = EstilistaScreenHandler.ANCHO_CONTADOR;
        c.drawText(textRenderer, Text.translatable("femclothes.estilista." + clave, valor, EstilistaBlockEntity.TOPE_CONTADOR), cx, cy, EstiloPergamino.TEXTO, false);
        c.fill(cx - 1, cy + 10, cx + w + 1, cy + 17, 0xFF2A180C);
        c.fill(cx, cy + 11, cx + w, cy + 16, 0xFF000000);
        int lleno = Math.round(w * (valor / (float) EstilistaBlockEntity.TOPE_CONTADOR));
        if (lleno > 0) c.fill(cx, cy + 11, cx + lleno, cy + 16, color);
    }

    @Override
    protected void drawBackground(DrawContext c, float delta, int mouseX, int mouseY) {
        super.drawBackground(c, delta, mouseX, mouseY);
        int px = this.x + EstilistaScreenHandler.X_PANEL, py = this.y + EstilistaScreenHandler.Y_PANEL;
        int pw = EstilistaScreenHandler.ANCHO_PANEL, ph = EstilistaScreenHandler.ALTO_PANEL;
        c.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFF2A180C);
        c.fill(px, py, px + pw, py + ph, 0xFFE3CC9E);
        marcoGrande(c, this.x + EstilistaScreenHandler.X_MARCO_ENTRADA, this.y + EstilistaScreenHandler.Y_MARCO);
        marcoGrande(c, this.x + EstilistaScreenHandler.X_MARCO_SALIDA, this.y + EstilistaScreenHandler.Y_MARCO);
        for (var s : handler.slots) {
            if (s.x >= EstilistaScreenHandler.X_PANEL) {
                c.fill(this.x + s.x - 1, this.y + s.y - 1, this.x + s.x + 17, this.y + s.y + 17, 0xFF6B5136);
                c.fill(this.x + s.x, this.y + s.y, this.x + s.x + 16, this.y + s.y + 16, 0xFFC9A877);
            }
        }
        EstilistaBlockEntity be = yo.host;
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.panel"), px + 6, py + 5, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.entrada"), this.x + EstilistaScreenHandler.X_MARCO_ENTRADA,
                py + 10, EstiloPergamino.TEXTO, false);
        Text sal = Text.translatable("femclothes.estilista.salida");
        c.drawText(textRenderer, sal, this.x + EstilistaScreenHandler.X_MARCO_SALIDA + EstilistaScreenHandler.GRANDE - textRenderer.getWidth(sal),
                py + 10, EstiloPergamino.TEXTO, false);
        // Qué diseño hay para la prenda de la entrada (o, si no hay, para la muestra del editor).
        ItemStack ref = be.getStack(EstilistaBlockEntity.SLOT_PRENDA);
        if (ref.isEmpty()) ref = be.editor().getStack(com.femclothes.estilado.EstiladoBlockEntity.SLOT_PRENDA);
        Text info;
        if (ref.isEmpty()) {
            info = Text.translatable("femclothes.estilista.info_vacia", be.cuantosDisenos());
        } else {
            ItemStack d = be.diseno(ref);
            info = d == null ? Text.translatable("femclothes.estilista.info_sin_diseno")
                    : Text.translatable("femclothes.estilista.info_diseno", EstilistaBlockEntity.apliquesDe(d), EstilistaBlockEntity.correasDe(d));
        }
        int y = py + 74;
        for (var linea : textRenderer.wrapLines(info, pw - 12)) { c.drawText(textRenderer, linea, px + 6, y, EstiloPergamino.TEXTO, false); y += 10; }
        ItemStack falta = be.getStack(EstilistaBlockEntity.SLOT_PRENDA).isEmpty() ? ItemStack.EMPTY : be.faltaParaLaEntrada();
        if (!falta.isEmpty()) {
            for (var linea : textRenderer.wrapLines(Text.translatable("femclothes.estilista.info_falta", falta.getCount(), falta.getName()), pw - 12)) {
                c.drawText(textRenderer, linea, px + 6, y, 0xFF8B1A1A, false);
                y += 10;
            }
        }
        contador(c, EstilistaScreenHandler.Y_HILO, "hilo", be.hilo(), 0xFFE8DCC8, "");
        contador(c, EstilistaScreenHandler.Y_CUERO, "cuero", be.cuero(), 0xFF8B5A2B, "");
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.almacen"), px + 6, py + 148, EstiloPergamino.TEXTO, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Soltar hilo o cuero sobre su contador.
        int cx = this.x + EstilistaScreenHandler.X_CONTADOR, w = EstilistaScreenHandler.ANCHO_CONTADOR, h = EstilistaScreenHandler.ALTO_CONTADOR;
        if (mx >= cx && mx < cx + w) {
            int hy = this.y + EstilistaScreenHandler.Y_HILO, cy = this.y + EstilistaScreenHandler.Y_CUERO;
            if (my >= hy && my < hy + h) { this.client.interactionManager.clickButton(this.handler.syncId, EstilistaBlockEntity.BTN_CARGAR_HILO); return true; }
            if (my >= cy && my < cy + h) { this.client.interactionManager.clickButton(this.handler.syncId, EstilistaBlockEntity.BTN_CARGAR_CUERO); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected boolean isClickOutsideBounds(double mx, double my, int left, int top, int button) {
        // El panel de la máquina está afuera de la ventana: tocarlo con un ítem en el cursor no lo tira.
        if (mx >= this.x + EstilistaScreenHandler.X_PANEL - 2 && mx < this.x + EstilistaScreenHandler.X_PANEL + EstilistaScreenHandler.ANCHO_PANEL + 2
                && my >= this.y + EstilistaScreenHandler.Y_PANEL && my < this.y + EstilistaScreenHandler.Y_PANEL + EstilistaScreenHandler.ALTO_PANEL) return false;
        return super.isClickOutsideBounds(mx, my, left, top, button);
    }
}
