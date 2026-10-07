package com.modamod.client;

import com.modamod.telar.TelarBlockEntity;
import com.modamod.telar.TelarPrenda;
import com.modamod.telar.TelarScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * El Telar automático (2026-10-07, "produzca prendas basicas a base de lana e hilo" + "se pueda seleccionar varias
 * prendas y vaya alternando, y se le pueda configurar que cantidad de prenda se quiere que saque"): fondo, marcos y
 * barra de progreso dibujados por código (sin texturas), con el acento granate del Telar (2026-10-07, "el color del telar es granate"). A la izquierda
 * lana e hilo, en el medio las seis prendas (se tildan varias) y el lote, a la derecha la prenda terminada; abajo el
 * inventario.
 */
public class TelarScreen extends net.minecraft.client.gui.screen.ingame.HandledScreen<TelarScreenHandler> {

    private static final int X_BOTONES = 52, Y_BOTONES = 24, ANCHO_BOTON = 38, ALTO_BOTON = 18, PASO_Y = 20;
    private static final int Y_LOTE = 86, Y_BARRA = 106, Y_TEXTO = 116;
    private final List<ButtonWidget> botones = new ArrayList<>();

    public TelarScreen(TelarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = TelarScreenHandler.ANCHO;
        this.backgroundHeight = TelarScreenHandler.ALTO;
        this.playerInventoryTitleY = TelarScreenHandler.Y_INV - 12;
    }

    @Override
    protected void init() {
        super.init();
        EstiloPergamino.usarTema(EstiloPergamino.Tema.GRANATE);
        botones.clear();
        TelarPrenda[] prendas = TelarPrenda.values();
        for (int i = 0; i < prendas.length; i++) {
            int bx = this.x + X_BOTONES + (i % 2) * (ANCHO_BOTON + 4), by = this.y + Y_BOTONES + (i / 2) * PASO_Y;
            ButtonWidget b = boton("", bx, by, ANCHO_BOTON, ALTO_BOTON, TelarBlockEntity.BTN_PRENDA_BASE + i);
            b.setTooltip(Tooltip.of(Text.translatable("modamod.telar.tooltip.prenda", prendas[i].lana, prendas[i].hilo)));
            botones.add(b);
        }
        // El lote: −10 − + +10 y reiniciar.
        int ly = this.y + Y_LOTE;
        boton("-10", this.x + 52, ly, 22, 16, TelarBlockEntity.BTN_LOTE_MENOS_10);
        boton("-", this.x + 76, ly, 16, 16, TelarBlockEntity.BTN_LOTE_MENOS);
        boton("+", this.x + 94, ly, 16, 16, TelarBlockEntity.BTN_LOTE_MAS);
        boton("+10", this.x + 112, ly, 22, 16, TelarBlockEntity.BTN_LOTE_MAS_10);
        ButtonWidget reiniciar = boton("↻", this.x + 138, ly, 18, 16, TelarBlockEntity.BTN_LOTE_REINICIAR);
        reiniciar.setTooltip(Tooltip.of(Text.translatable("modamod.telar.tooltip.reiniciar")));
    }

    private ButtonWidget boton(String texto, int bx, int by, int w, int h, int id) {
        ButtonWidget b = new EstiloPergamino.BotonPergamino(bx, by, w, h, Text.literal(texto),
                btn -> this.client.interactionManager.clickButton(this.handler.syncId, id));
        this.addDrawableChild(b);
        return b;
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        TelarPrenda[] prendas = TelarPrenda.values();
        for (int i = 0; i < botones.size(); i++) {
            Text nombre = Text.translatable("modamod.telar.prenda." + prendas[i].clave());
            botones.get(i).setMessage(handler.host.elegida(i) ? Text.literal("✔ ").append(nombre) : nombre);
        }
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
        int yi = y0 + TelarScreenHandler.Y_INV;
        for (int f = 0; f < 3; f++) for (int col = 0; col < 9; col++) marco(c, x0 + 8 + col * 18, yi + f * 18);
        for (int col = 0; col < 9; col++) marco(c, x0 + 8 + col * 18, yi + 58);
        // Rótulos.
        TelarBlockEntity be = handler.host;
        c.drawText(textRenderer, Text.translatable("modamod.telar.lana"), x0 + 6, y0 + 24, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("modamod.telar.hilo"), x0 + 6, y0 + 52, EstiloPergamino.TEXTO, false);
        c.drawText(textRenderer, Text.translatable("modamod.telar.lote"), x0 + 6, y0 + Y_LOTE + 4, EstiloPergamino.TEXTO, false);
        // Barra de progreso de la prenda en curso.
        float nivel = switch (be.estado()) {
            case PROCESANDO -> be.progreso() / (float) be.duracion();
            case LISTO -> 1f;
            default -> 0f;
        };
        int bx = x0 + 52, by = y0 + Y_BARRA, bw = 104;
        c.fill(bx - 1, by - 1, bx + bw + 1, by + 7, 0xFF2A180C);
        c.fill(bx, by, bx + bw, by + 6, 0xFF111111);
        c.fill(bx, by, bx + (int) (bw * nivel), by + 6, 0xFFB3283F);
        // Lote y costo de la que sigue.
        Text lote = be.lote() == 0 ? Text.translatable("modamod.telar.lote_sin_limite")
                : Text.translatable("modamod.telar.lote_n", be.lote(), be.restantes());
        c.drawText(textRenderer, lote, x0 + 6, y0 + Y_TEXTO, EstiloPergamino.TEXTO, false);
        int v = be.prendaVista();
        Text sigue = v < 0 ? Text.translatable("modamod.telar.nada_tildado")
                : Text.translatable("modamod.telar.sigue", Text.translatable("modamod.telar.prenda." + TelarPrenda.values()[v].clave()),
                        TelarPrenda.values()[v].lana, TelarPrenda.values()[v].hilo);
        c.drawText(textRenderer, sigue, x0 + 6, y0 + Y_TEXTO + 10, EstiloPergamino.TEXTO, false);
    }

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
