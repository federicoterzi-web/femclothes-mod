package com.femclothes.client;

import com.femclothes.telar.TelarBlockEntity;
import com.femclothes.telar.TelarPrenda;
import com.femclothes.telar.TelarScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * El Telar automático (2026-10-07, "produzca prendas basicas a base de lana e hilo"): fondo, marcos y barra de progreso
 * dibujados por código (sin texturas), con el acento lila de las máquinas de estilo. A la izquierda lana e hilo, en el
 * medio la prenda a tejer (seis botones), a la derecha la prenda terminada; abajo el inventario.
 */
public class TelarScreen extends net.minecraft.client.gui.screen.ingame.HandledScreen<TelarScreenHandler> {

    private static final int X_BOTONES = 52, Y_BOTONES = 28, ANCHO_BOTON = 38, ALTO_BOTON = 18;
    private final List<ButtonWidget> botones = new ArrayList<>();

    public TelarScreen(TelarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = TelarScreenHandler.ANCHO;
        this.backgroundHeight = TelarScreenHandler.ALTO;
        this.playerInventoryTitleY = 94;
    }

    @Override
    protected void init() {
        super.init();
        EstiloPergamino.usarTema(EstiloPergamino.Tema.LILA);
        botones.clear();
        TelarPrenda[] prendas = TelarPrenda.values();
        for (int i = 0; i < prendas.length; i++) {
            int id = TelarBlockEntity.BTN_PRENDA_BASE + i;
            int bx = this.x + X_BOTONES + (i % 2) * (ANCHO_BOTON + 4), by = this.y + Y_BOTONES + (i / 2) * (ALTO_BOTON + 3);
            ButtonWidget b = new EstiloPergamino.BotonPergamino(bx, by, ANCHO_BOTON, ALTO_BOTON,
                    Text.translatable("femclothes.telar.prenda." + prendas[i].clave()),
                    btn -> this.client.interactionManager.clickButton(this.handler.syncId, id));
            botones.add(b);
            this.addDrawableChild(b);
        }
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        int elegida = handler.host.prenda().ordinal();
        for (int i = 0; i < botones.size(); i++) botones.get(i).active = i != elegida;
    }

    @Override
    protected void drawBackground(DrawContext c, float delta, int mx, int my) {
        int x0 = this.x, y0 = this.y, w = backgroundWidth, h = backgroundHeight;
        c.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, 0xFF2A180C);
        c.fill(x0, y0, x0 + w, y0 + h, 0xFFE9D8B4);
        c.fill(x0, y0, x0 + w, y0 + 3, EstiloPergamino.tema().claro);
        // Marcos de los slots.
        marco(c, x0 + TelarScreenHandler.X_LANA, y0 + TelarScreenHandler.Y_LANA);
        marco(c, x0 + TelarScreenHandler.X_HILO, y0 + TelarScreenHandler.Y_HILO);
        marco(c, x0 + TelarScreenHandler.X_SALIDA, y0 + TelarScreenHandler.Y_SALIDA);
        for (int f = 0; f < 3; f++) for (int col = 0; col < 9; col++) marco(c, x0 + 8 + col * 18, y0 + 106 + f * 18);
        for (int col = 0; col < 9; col++) marco(c, x0 + 8 + col * 18, y0 + 106 + 58);
        // Rótulos y costo de la prenda elegida.
        TelarPrenda p = handler.host.prenda();
        c.drawText(textRenderer, Text.translatable("femclothes.telar.lana"), x0 + 6, y0 + 24, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("femclothes.telar.hilo"), x0 + 6, y0 + 52, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("femclothes.telar.costo", p.lana, p.hilo), x0 + X_COSTO, y0 + Y_COSTO, EstiloPergamino.TEXTO, false);
        // Barra de progreso de la prenda en curso.
        float nivel = switch (handler.host.estado()) {
            case PROCESANDO -> handler.host.progreso() / (float) handler.host.duracion();
            case LISTO -> 1f;
            default -> 0f;
        };
        int bx = x0 + 52, by = y0 + 86, bw = 112;
        c.fill(bx - 1, by - 1, bx + bw + 1, by + 7, 0xFF2A180C);
        c.fill(bx, by, bx + bw, by + 6, 0xFF111111);
        c.fill(bx, by, bx + (int) (bw * nivel), by + 6, 0xFFD6AAE8);
    }

    private static final int X_COSTO = 52, Y_COSTO = 70;

    private void marco(DrawContext c, int sx, int sy) {
        c.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF2A180C);
        c.fill(sx, sy, sx + 16, sy + 16, 0xFF8B7355);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        renderBackground(c, mx, my, delta);
        super.render(c, mx, my, delta);
        drawMouseoverTooltip(c, mx, my);
    }
}
