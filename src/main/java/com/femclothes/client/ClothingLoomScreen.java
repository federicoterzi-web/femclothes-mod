package com.femclothes.client;

import com.femclothes.screen.ClothingLoomScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
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

    /** Mismo orden que ClothingLoomScreenHandler.Target. */
    private static final String[] TARGET_KEYS = {
            "femclothes.loom.target.both",
            "femclothes.loom.target.left",
            "femclothes.loom.target.right",
    };

    private ButtonWidget targetButton;

    public ClothingLoomScreen(ClothingLoomScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // Mismo offset de título que LoomScreen vanilla.
        this.titleY = 2;
    }

    @Override
    protected void init() {
        super.init();
        // Selector de pierna, en el panel donde el Telar vanilla pone su
        // listado de patrones (acá vacío, porque cada ClothingPatternItem ya
        // lleva un patrón fijo).
        // Debajo del fondo, no encima: el panel hundido de la derecha del
        // Telar vanilla es su listado de patrones y se lee como un preview,
        // asi que un boton ahi adentro queda fuera de lugar.
        this.targetButton = ButtonWidget.builder(targetLabel(), b -> cycleTarget())
                .dimensions(this.x + (this.backgroundWidth - 100) / 2,
                        this.y + this.backgroundHeight + 4, 100, 20)
                .build();
        this.addDrawableChild(this.targetButton);
    }

    private void cycleTarget() {
        ClothingLoomScreenHandler.Target[] all = ClothingLoomScreenHandler.Target.values();
        int next = (this.handler.getTarget().ordinal() + 1) % all.length;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null) return;

        // Primero el handler LOCAL, despues el paquete — es el orden que usa
        // LoomScreen vanilla. onButtonClick corre en el servidor, asi que sin
        // esta llamada el handler del cliente se queda en su valor inicial y
        // el ciclo se traba en el segundo item para siempre.
        if (this.handler.onButtonClick(client.player, next)) {
            client.interactionManager.clickButton(this.handler.syncId, next);
            this.targetButton.setMessage(Text.translatable(TARGET_KEYS[next]));
        }
    }

    private Text targetLabel() {
        return Text.translatable(TARGET_KEYS[this.handler.getTarget().ordinal()]);
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
