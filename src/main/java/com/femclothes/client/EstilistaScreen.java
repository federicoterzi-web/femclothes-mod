package com.femclothes.client;

import com.femclothes.estilista.EstilistaBlockEntity;
import com.femclothes.estilista.EstilistaScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Pantalla provisoria de la Estilista (etapa 2; la definitiva, con la vista grande, es la etapa 3): la prenda, la salida,
 * los botones Aplicar / Fijar muestra / Borrar, lo que dice el diseño de ese tipo y el almacén de insumos
 * (2026-10-05, "la primera que entre o la del input... que gaste y recupere").
 */
public class EstilistaScreen extends HandledScreen<EstilistaScreenHandler> {

    public EstilistaScreen(EstilistaScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 224;
        this.playerInventoryTitleY = EstilistaScreenHandler.Y_INV - 11;
    }

    @Override
    protected void init() {
        super.init();
        boton("femclothes.estilista.aplicar", EstilistaBlockEntity.BTN_APLICAR, 8);
        boton("femclothes.estilista.fijar", EstilistaBlockEntity.BTN_FIJAR, 62);
        boton("femclothes.estilista.borrar", EstilistaBlockEntity.BTN_BORRAR, 116);
    }

    private void boton(String clave, int id, int dx) {
        addDrawableChild(ButtonWidget.builder(Text.translatable(clave), b -> client.interactionManager.clickButton(handler.syncId, id))
                .dimensions(x + dx, y + 38, 52, 18).build());
    }

    @Override
    protected void drawBackground(DrawContext c, float delta, int mouseX, int mouseY) {
        c.fill(x - 2, y - 2, x + backgroundWidth + 2, y + backgroundHeight + 2, 0xFF2A1A0E);
        c.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFFE3CC9E);
        for (var s : handler.slots) {
            c.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF6B5136);
            c.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFFC9A877);
        }
        EstilistaBlockEntity be = handler.be;
        ItemStack prenda = be.getStack(EstilistaBlockEntity.SLOT_PRENDA);
        Text info;
        if (prenda.isEmpty()) {
            info = Text.translatable("femclothes.estilista.info_vacia", be.cuantosDisenos());
        } else {
            ItemStack d = be.diseno(prenda);
            info = d == null ? Text.translatable("femclothes.estilista.info_sin_diseno")
                    : Text.translatable("femclothes.estilista.info_diseno", EstilistaBlockEntity.apliquesDe(d), EstilistaBlockEntity.correasDe(d));
        }
        c.drawText(textRenderer, info, x + 8, y + 60, EstiloPergamino.TEXTO, false);
        ItemStack falta = prenda.isEmpty() ? ItemStack.EMPTY : be.faltaParaLaEntrada();
        if (!falta.isEmpty()) {
            c.drawText(textRenderer, Text.translatable("femclothes.estilista.info_falta", falta.getCount(), falta.getName()),
                    x + 8, y + 71, 0xFF8B1A1A, false);
        }
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.insumos"), x + 8, y + 82, EstiloPergamino.TEXTO, false);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        drawMouseoverTooltip(c, mouseX, mouseY);
    }
}
