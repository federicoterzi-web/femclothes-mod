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
 * La Estilista automática (2026-10-05, etapa 3: "no quiero perder el tamaño del visualizador"): la pantalla de la Mesa
 * de estilado —vista 3D grande, apliques, correas, colores— editando la prenda de MUESTRA, y a la izquierda el panel de
 * la máquina: entrada y salida, Aplicar / Fijar / Borrar el diseño de ese tipo de prenda y el almacén de insumos.
 */
public class EstilistaScreen extends EstiladoScreen {

    private final EstilistaScreenHandler yo;

    public EstilistaScreen(EstilistaScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.yo = handler;
    }

    @Override
    protected void init() {
        super.init();
        boton("aplicar", EstilistaBlockEntity.BTN_APLICAR, 0);
        boton("fijar", EstilistaBlockEntity.BTN_FIJAR, 1);
        boton("borrar", EstilistaBlockEntity.BTN_BORRAR, 2);
    }

    private void boton(String clave, int id, int columna) {
        ButtonWidget b = new EstiloPergamino.BotonPergamino(this.x + EstilistaScreenHandler.X_PANEL + 6 + columna * 38,
                this.y + 70, 36, 16, Text.translatable("femclothes.estilista." + clave),
                btn -> this.client.interactionManager.clickButton(this.handler.syncId, id));
        b.setTooltip(Tooltip.of(Text.translatable("femclothes.estilista.tooltip." + clave)));
        this.addDrawableChild(b);
    }

    @Override
    protected void drawBackground(DrawContext c, float delta, int mouseX, int mouseY) {
        super.drawBackground(c, delta, mouseX, mouseY);
        int px = this.x + EstilistaScreenHandler.X_PANEL, py = this.y + EstilistaScreenHandler.Y_PANEL;
        int pw = EstilistaScreenHandler.ANCHO_PANEL, ph = EstilistaScreenHandler.ALTO_PANEL;
        c.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFF2A180C);
        c.fill(px, py, px + pw, py + ph, 0xFFE3CC9E);
        for (var s : handler.slots) {
            if (s.x >= EstilistaScreenHandler.X_PANEL && s.x < 0) {
                c.fill(this.x + s.x - 1, this.y + s.y - 1, this.x + s.x + 17, this.y + s.y + 17, 0xFF6B5136);
                c.fill(this.x + s.x, this.y + s.y, this.x + s.x + 16, this.y + s.y + 16, 0xFFC9A877);
            }
        }
        EstilistaBlockEntity be = yo.host;
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.panel"), px + 6, py + 5, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.entrada"), this.x + EstilistaScreenHandler.X_ENTRADA - 1,
                this.y + EstilistaScreenHandler.Y_ENTRADA - 10, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.salida"), this.x + EstilistaScreenHandler.X_SALIDA - 1,
                this.y + EstilistaScreenHandler.Y_ENTRADA - 10, EstiloPergamino.TEXTO, false);
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
        int y = py + 92;
        for (var linea : textRenderer.wrapLines(info, pw - 12)) { c.drawText(textRenderer, linea, px + 6, y, EstiloPergamino.TEXTO, false); y += 10; }
        ItemStack falta = be.getStack(EstilistaBlockEntity.SLOT_PRENDA).isEmpty() ? ItemStack.EMPTY : be.faltaParaLaEntrada();
        if (!falta.isEmpty()) {
            for (var linea : textRenderer.wrapLines(Text.translatable("femclothes.estilista.info_falta", falta.getCount(), falta.getName()), pw - 12)) {
                c.drawText(textRenderer, linea, px + 6, y, 0xFF8B1A1A, false);
                y += 10;
            }
        }
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.insumos"), px + 6, this.y + EstilistaScreenHandler.Y_INSUMOS - 11,
                EstiloPergamino.TEXTO, false);
    }

    @Override
    protected boolean isClickOutsideBounds(double mx, double my, int left, int top, int button) {
        // El panel de la máquina está afuera de la ventana: tocarlo con un ítem en el cursor no lo tira.
        if (mx >= this.x + EstilistaScreenHandler.X_PANEL && mx < this.x && my >= this.y + EstilistaScreenHandler.Y_PANEL
                && my < this.y + EstilistaScreenHandler.Y_PANEL + EstilistaScreenHandler.ALTO_PANEL) return false;
        return super.isClickOutsideBounds(mx, my, left, top, button);
    }
}
