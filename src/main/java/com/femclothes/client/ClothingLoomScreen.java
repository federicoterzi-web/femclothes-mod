package com.femclothes.client;

import com.femclothes.screen.ClothingLoomScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Pantalla del clothing_loom. Reusa el fondo del Telar vanilla: los slots
 * del handler ya están en las mismas coordenadas que los del Telar
 * (13/33 arriba, 23 abajo, salida en 143), así que el fondo calza pixel a
 * pixel sin arte propio.
 *
 * A diferencia de LoomScreen no hay listado de patrones ni scrollbar: cada
 * ClothingPatternItem lleva UN patrón fijo, así que el patrón se elige
 * poniendo el ítem en su slot y no hay nada que seleccionar.
 *
 * TODO (arte): el panel de la derecha del fondo vanilla es el listado de
 * patrones del Telar, que acá queda vacío. Cuando haya arte propio,
 * reemplazar TEXTURE por un png nuestro de 176x166 sin ese panel.
 */
public class ClothingLoomScreen extends HandledScreen<ClothingLoomScreenHandler> {

    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/loom.png");

    public ClothingLoomScreen(ClothingLoomScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // Mismo offset de título que LoomScreen vanilla.
        this.titleY = 2;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        // HandledScreen.render NO dibuja el tooltip del slot bajo el mouse
        // (solo llama a drawForeground) — hay que pedirlo a mano, igual que
        // hacen las pantallas vanilla.
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
