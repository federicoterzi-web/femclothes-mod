package com.modamod.client;

import com.modamod.estilista.EstilistaBlockEntity;
import com.modamod.estilista.EstilistaScreenHandler;
import com.modamod.estilista.GuardarDisenoPayload;
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

    private static final int ANCHO_TOTAL = ANCHO + 6 + EstilistaScreenHandler.ANCHO_PANEL + 6 + EstilistaScreenHandler.ANCHO_DISENOS + 4;
    private static final int FILA = 24, Y_LISTA = 52;
    private net.minecraft.client.gui.widget.TextFieldWidget txtNombre;
    private final EstilistaScreenHandler yo;

    public EstilistaScreen(EstilistaScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.yo = handler;
    }

    @Override
    boolean sinSlotPrenda() { return true; }

    @Override
    int panelExtra() { return 6 + EstilistaScreenHandler.ANCHO_PANEL + 6 + EstilistaScreenHandler.ANCHO_DISENOS; }

    @Override
    protected void init() {
        // El conjunto (vista + panel) se centra junto: HandledScreen centra con backgroundWidth.
        this.backgroundWidth = ANCHO_TOTAL + (handler.be.creativa() ? 170 : 0);
        super.init();
        this.backgroundWidth = ANCHO;
        int px = this.x + EstilistaScreenHandler.X_PANEL;
        boton("aplicar", EstilistaBlockEntity.BTN_APLICAR, px + 8 + EstilistaScreenHandler.GRANDE + 4, this.y + EstilistaScreenHandler.Y_MARCO + 4,
                EstilistaScreenHandler.ANCHO_PANEL - 16 - 2 * (EstilistaScreenHandler.GRANDE + 4), 18);
        this.addDrawableChild(new EstiloPergamino.BotonLinea(px + 8, this.y + EstilistaScreenHandler.Y_PANEL + 84,
                EstilistaScreenHandler.ANCHO_PANEL - 16, 16, () -> ((EstilistaScreenHandler) handler).host.linea(),
                b -> this.client.interactionManager.clickButton(this.handler.syncId, EstilistaBlockEntity.BTN_LINEA)));
        // Panel de diseños: nombre + Guardar abajo.
        int dx = this.x + EstilistaScreenHandler.X_DISENOS, dy = this.y + EstilistaScreenHandler.Y_PANEL;
        txtNombre = new net.minecraft.client.gui.widget.TextFieldWidget(this.textRenderer, dx + 6, dy + EstilistaScreenHandler.ALTO_PANEL - 40,
                EstilistaScreenHandler.ANCHO_DISENOS - 12, 14, Text.translatable("modamod.estilista.disenos.nombre"));
        txtNombre.setMaxLength(40);
        txtNombre.setPlaceholder(Text.translatable("modamod.estilista.disenos.nombre"));
        this.addDrawableChild(txtNombre);
        ButtonWidget guardar = new EstiloPergamino.BotonPergamino(dx + 6, dy + EstilistaScreenHandler.ALTO_PANEL - 22,
                EstilistaScreenHandler.ANCHO_DISENOS - 12, 16, Text.translatable("modamod.estilista.disenos.guardar"), btn -> {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new GuardarDisenoPayload(yo.host.getPos(), txtNombre.getText()));
            txtNombre.setText("");
        });
        guardar.setTooltip(Tooltip.of(Text.translatable("modamod.estilista.disenos.tooltip.guardar")));
        this.addDrawableChild(guardar);
        boton("borrar", EstilistaBlockEntity.BTN_BORRAR, px + 8, this.y + EstilistaScreenHandler.Y_PANEL + 52, EstilistaScreenHandler.ANCHO_PANEL - 16, 16);
    }

    private void boton(String clave, int id, int bx, int by, int w, int h) {
        ButtonWidget b = new EstiloPergamino.BotonPergamino(bx, by, w, h, Text.translatable("modamod.estilista." + clave),
                btn -> this.client.interactionManager.clickButton(this.handler.syncId, id));
        b.setTooltip(Tooltip.of(Text.translatable("modamod.estilista.tooltip." + clave)));
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
        c.drawText(textRenderer, Text.translatable("modamod.estilista." + clave, valor, EstilistaBlockEntity.TOPE_CONTADOR), cx, cy, EstiloPergamino.TEXTO, false);
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
        c.drawText(textRenderer, Text.translatable("modamod.estilista.panel"), px + 6, py + 5, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("modamod.estilista.entrada"), this.x + EstilistaScreenHandler.X_MARCO_ENTRADA,
                py + 10, EstiloPergamino.TEXTO, false);
        Text sal = Text.translatable("modamod.estilista.salida");
        c.drawText(textRenderer, sal, this.x + EstilistaScreenHandler.X_MARCO_SALIDA + EstilistaScreenHandler.GRANDE - textRenderer.getWidth(sal),
                py + 10, EstiloPergamino.TEXTO, false);
        // Qué diseño hay para la prenda de la entrada (o, si no hay, para la muestra del editor).
        ItemStack ref = be.getStack(EstilistaBlockEntity.SLOT_PRENDA);
        if (ref.isEmpty()) ref = be.editor().getStack(com.modamod.estilado.EstiladoBlockEntity.SLOT_PRENDA);
        Text info;
        if (ref.isEmpty()) {
            info = Text.translatable("modamod.estilista.info_vacia", be.cuantosDisenos());
        } else {
            ItemStack d = be.diseno(ref);
            info = d == null ? Text.translatable("modamod.estilista.info_sin_diseno")
                    : Text.translatable("modamod.estilista.info_diseno", EstilistaBlockEntity.resumen(d));
        }
        int y = py + 74;
        for (var linea : textRenderer.wrapLines(info, pw - 12)) { c.drawText(textRenderer, linea, px + 6, y, EstiloPergamino.TEXTO, false); y += 10; }
        ItemStack falta = be.getStack(EstilistaBlockEntity.SLOT_PRENDA).isEmpty() ? ItemStack.EMPTY : be.faltaParaLaEntrada();
        if (!falta.isEmpty()) {
            for (var linea : textRenderer.wrapLines(Text.translatable("modamod.estilista.info_falta", falta.getCount(), falta.getName()), pw - 12)) {
                c.drawText(textRenderer, linea, px + 6, y, 0xFF8B1A1A, false);
                y += 10;
            }
        }
        dibujarDisenos(c, be, mouseX, mouseY);
        contador(c, EstilistaScreenHandler.Y_HILO, "hilo", be.hilo(), 0xFFE8DCC8, "");
        contador(c, EstilistaScreenHandler.Y_CUERO, "cuero", be.cuero(), 0xFF8B5A2B, "");
        c.drawText(textRenderer, Text.translatable("modamod.estilista.almacen"), px + 6, py + 148, EstiloPergamino.TEXTO, false);
    }

    // ── panel de diseños (2026-10-08, "guarde setting por prenda de manera visible y clara") ──

    private void dibujarDisenos(DrawContext c, EstilistaBlockEntity be, int mx, int my) {
        int px = this.x + EstilistaScreenHandler.X_DISENOS, py = this.y + EstilistaScreenHandler.Y_PANEL;
        int pw = EstilistaScreenHandler.ANCHO_DISENOS, ph = EstilistaScreenHandler.ALTO_PANEL;
        c.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFF2A180C);
        c.fill(px, py, px + pw, py + ph, 0xFFE3CC9E);
        net.minecraft.item.Item tipo = be.tipoDeReferencia();
        c.drawText(textRenderer, Text.translatable("modamod.estilista.disenos"), px + 6, py + 5, EstiloPergamino.TEXTO, false);
        if (tipo == null) {
            int y = py + 22;
            for (var linea : textRenderer.wrapLines(Text.translatable("modamod.estilista.disenos.sin_tipo"), pw - 12)) {
                c.drawText(textRenderer, linea, px + 6, y, EstiloPergamino.TEXTO, false);
                y += 10;
            }
            return;
        }
        ItemStack icono = new ItemStack(tipo);
        c.drawItem(icono, px + 6, py + 16);
        c.drawText(textRenderer, textRenderer.trimToWidth(icono.getName().getString(), pw - 34), px + 26, py + 21, EstiloPergamino.TEXTO, false);

        // El último: automático, se usa al aplicar.
        ItemStack u = be.diseno(icono);
        int y0 = py + 40;
        c.fill(px + 4, y0, px + pw - 4, y0 + FILA, 0xFFD2B47F);
        c.fill(px + 4, y0, px + 6, y0 + FILA, 0xFF7B4FA8);
        c.drawText(textRenderer, Text.translatable("modamod.estilista.disenos.ultimo"), px + 10, y0 + 3, EstiloPergamino.TEXTO, false);
        Text res = u == null ? Text.translatable("modamod.estilista.disenos.ninguno") : EstilistaBlockEntity.resumen(u);
        c.drawText(textRenderer, textRenderer.trimToWidth(res.getString(), pw - 18), px + 10, y0 + 13, u == null ? 0xFF7A6A52 : 0xFF4B2E83, false);

        // Los guardados con nombre.
        var lista = be.nombrados(tipo);
        if (lista.isEmpty()) {
            int y = py + Y_LISTA + FILA + 4;
            for (var linea : textRenderer.wrapLines(Text.translatable("modamod.estilista.disenos.vacio_lista"), pw - 12)) {
                c.drawText(textRenderer, linea, px + 6, y, 0xFF7A6A52, false);
                y += 10;
            }
        }
        for (int k = 0; k < lista.size() && k < EstilistaBlockEntity.MAX_NOMBRADOS; k++) {
            int ry = py + Y_LISTA + FILA + 4 + k * (FILA - 2) - 2;
            boolean sobre = mx >= px + 4 && mx < px + pw - 4 && my >= ry && my < ry + FILA - 3;
            boolean sobreX = sobre && mx >= px + pw - 18;
            c.fill(px + 4, ry, px + pw - 4, ry + FILA - 3, sobre ? 0xFFC9A877 : 0xFFD9BF8E);
            ItemStack d = lista.get(k);
            c.drawText(textRenderer, textRenderer.trimToWidth(d.getName().getString(), pw - 34), px + 8, ry + 2, EstiloPergamino.TEXTO, false);
            c.drawText(textRenderer, textRenderer.trimToWidth(EstilistaBlockEntity.resumen(d).getString(), pw - 34), px + 8, ry + 11, 0xFF7A6A52, false);
            c.drawText(textRenderer, "✕", px + pw - 14, ry + 7, sobreX ? 0xFFB02020 : 0xFF6B5136, false);
        }
        c.drawText(textRenderer, Text.translatable("modamod.estilista.disenos.ayuda"), px + 6,
                py + ph - 54, 0xFF7A6A52, false);
    }

    /** Click en la lista de diseños con nombre: la fila carga, la ✕ borra. */
    private boolean clickDisenos(double mx, double my) {
        net.minecraft.item.Item tipo = yo.host.tipoDeReferencia();
        if (tipo == null) return false;
        int px = this.x + EstilistaScreenHandler.X_DISENOS, py = this.y + EstilistaScreenHandler.Y_PANEL;
        int pw = EstilistaScreenHandler.ANCHO_DISENOS;
        var lista = yo.host.nombrados(tipo);
        for (int k = 0; k < lista.size() && k < EstilistaBlockEntity.MAX_NOMBRADOS; k++) {
            int ry = py + Y_LISTA + FILA + 4 + k * (FILA - 2) - 2;
            if (mx >= px + 4 && mx < px + pw - 4 && my >= ry && my < ry + FILA - 3) {
                int id = mx >= px + pw - 18 ? EstilistaBlockEntity.BTN_DISENO_BORRAR_BASE + k : EstilistaBlockEntity.BTN_DISENO_CARGAR_BASE + k;
                this.client.interactionManager.clickButton(this.handler.syncId, id);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Con el cursor en el nombre, las teclas son del texto (la E no cierra la pantalla).
        if (txtNombre != null && (txtNombre.keyPressed(keyCode, scanCode, modifiers) || txtNombre.isActive())) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        if (clickDisenos(mx, my)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected boolean isClickOutsideBounds(double mx, double my, int left, int top, int button) {
        // El panel de la máquina está afuera de la ventana: tocarlo con un ítem en el cursor no lo tira.
        if (mx >= this.x + EstilistaScreenHandler.X_PANEL - 2 && mx < this.x + EstilistaScreenHandler.X_PANEL + EstilistaScreenHandler.ANCHO_PANEL + 2
                && my >= this.y + EstilistaScreenHandler.Y_PANEL && my < this.y + EstilistaScreenHandler.Y_PANEL + EstilistaScreenHandler.ALTO_PANEL) return false;
        if (mx >= this.x + EstilistaScreenHandler.X_DISENOS - 2 && mx < this.x + EstilistaScreenHandler.X_DISENOS + EstilistaScreenHandler.ANCHO_DISENOS + 2
                && my >= this.y + EstilistaScreenHandler.Y_PANEL && my < this.y + EstilistaScreenHandler.Y_PANEL + EstilistaScreenHandler.ALTO_PANEL) return false;
        return super.isClickOutsideBounds(mx, my, left, top, button);
    }
}
