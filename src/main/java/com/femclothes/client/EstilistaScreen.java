package com.femclothes.client;

import com.femclothes.estilista.EstilistaBlockEntity;
import com.femclothes.estilista.EstilistaScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/** Pantalla provisoria de la Estilista (etapa 1; la definitiva viene en la etapa 3): prenda, salida y un botón Probar. */
public class EstilistaScreen extends HandledScreen<EstilistaScreenHandler> {

    public EstilistaScreen(EstilistaScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 202;
        this.playerInventoryTitleY = EstilistaScreenHandler.Y_INV - 11;
    }

    @Override
    protected void init() {
        super.init();
        addDrawableChild(ButtonWidget.builder(Text.translatable("femclothes.estilista.probar"),
                        b -> client.interactionManager.clickButton(handler.syncId, EstilistaBlockEntity.BTN_PROBAR))
                .dimensions(x + 62, y + 38, 52, 20).build());
    }

    @Override
    protected void drawBackground(DrawContext c, float delta, int mouseX, int mouseY) {
        c.fill(x - 2, y - 2, x + backgroundWidth + 2, y + backgroundHeight + 2, 0xFF2A1A0E);
        c.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFFE3CC9E);
        for (var s : handler.slots) {
            c.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF6B5136);
            c.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFFC9A877);
        }
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.aviso"), x + 8, y + 76, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("femclothes.estilista.aviso2"), x + 8, y + 88, EstiloPergamino.TEXTO, false);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        drawMouseoverTooltip(c, mouseX, mouseY);
    }
}
