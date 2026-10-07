package com.modamod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Casillero de diseño numerado (2026-09-27) — usado por {@code ModeladoScreen}
 * y {@code TinturasScreen}: click izquierdo carga el diseño (todos los
 * cortes/tintes fijados a la vez), click derecho lo borra.
 * {@link ButtonWidget#mouseClicked} solo reacciona al botón izquierdo de
 * fábrica (ver {@code ClickableWidget#isValidClickButton}), así que el
 * derecho se intercepta acá antes de llamar al {@code super}.
 */
public class BotonDiseno extends ButtonWidget {
    private final Runnable alClickDerecho;

    /**
     * @param indice        posición del casillero (0-based), se muestra como {@code indice+1}
     * @param onPress       click izquierdo: carga este diseño
     * @param alClickDerecho click derecho: borra este diseño
     */
    public BotonDiseno(int indice, int x, int y, int width, int height, PressAction onPress, Runnable alClickDerecho) {
        super(x, y, width, height, Text.literal(String.valueOf(indice + 1)), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.alClickDerecho = alClickDerecho;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
            alClickDerecho.run();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        EstiloPergamino.fondoBoton(context, getX(), getY(), getWidth(), getHeight(), isHovered(), active);
        var fuente = MinecraftClient.getInstance().textRenderer;
        Text m = getMessage();
        int tx = getX() + (getWidth() - fuente.getWidth(m)) / 2;
        int ty = getY() + (getHeight() - 8) / 2;
        int color = active ? EstiloPergamino.TEXTO_CLARO : EstiloPergamino.TEXTO_APAGADO;
        context.drawText(fuente, m, tx + 1, ty + 1, 0xFF2A180C, false);
        context.drawText(fuente, m, tx, ty, color, false);
    }
}
