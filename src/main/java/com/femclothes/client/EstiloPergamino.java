package com.femclothes.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Estilo pergamino / madera / latón de la GUI (2026-09-26, "hacer toda la
 * GUI"): colores y botones dibujados con {@code fill} (sin texturas) para que
 * hagan juego con el fondo procedural de {@code tools/generar_textura_modelado.py}
 * y con los esquemas de prenda. Hoy solo lo usa la Modeladora.
 */
public final class EstiloPergamino {

    /** Texto oscuro sobre pergamino (títulos, rótulos). */
    public static final int TEXTO = 0x3B2410;
    /** Texto claro sobre madera/cuero (botones). */
    public static final int TEXTO_CLARO = 0xFFF1D9A8;
    public static final int TEXTO_APAGADO = 0xFF9A8466;

    private static final int MADERA_ARRIBA = 0xFF7A5230;
    private static final int MADERA_ABAJO = 0xFF4A2E1A;
    private static final int MADERA_HOVER_ARRIBA = 0xFF95663B;
    private static final int MADERA_HOVER_ABAJO = 0xFF5C3A22;
    private static final int MADERA_APAGADA_ARRIBA = 0xFF5C4A3A;
    private static final int MADERA_APAGADA_ABAJO = 0xFF3A2E26;
    /**
     * Metal de cada máquina (2026-09-29, "estacion de tintes verdoso,
     * modeladora cobrizo y sublimadora dorado"): mismos valores que
     * {@code TEMAS} en tools/generar_textura_tinturas.py. Cada pantalla
     * fija el suyo en {@code init()}; solo hay una abierta a la vez.
     */
    public enum Tema {
        LATON(0xFFF4D070, 0xFF684818),
        VERDIN(0xFF96D4AA, 0xFF1C4A38),
        COBRE(0xFFF2AA78, 0xFF602C16),
        ORO(0xFFFFE476, 0xFF7A5608),
        /** Guardarropas: acero plateado de sus bisagras y manijas (2026-09-30). */
        PLATA(0xFFE6EAF0, 0xFF424854),
        /** Maniquí: azul esmaltado de sus esquinas y parante (2026-09-30). */
        AZUL(0xFF96B8E6, 0xFF1E345A);

        public final int claro, oscuro;

        Tema(int claro, int oscuro) {
            this.claro = claro;
            this.oscuro = oscuro;
        }
    }

    private static Tema tema = Tema.LATON;

    public static void usarTema(Tema t) {
        tema = t;
    }

    public static Tema tema() {
        return tema;
    }
    private static final int BORDE = 0xFF2A180C;

    /** Fondo de botón: madera con degradado vertical, filo de latón (claro arriba/izq., oscuro abajo/der.). */
    public static void fondoBoton(DrawContext c, int x, int y, int w, int h, boolean hover, boolean activo) {
        int arriba = !activo ? MADERA_APAGADA_ARRIBA : hover ? MADERA_HOVER_ARRIBA : MADERA_ARRIBA;
        int abajo = !activo ? MADERA_APAGADA_ABAJO : hover ? MADERA_HOVER_ABAJO : MADERA_ABAJO;
        c.fillGradient(x, y, x + w, y + h, arriba, abajo);
        int claro = activo ? tema.claro : 0xFF8A7448, oscuro = activo ? tema.oscuro : 0xFF4A3A22;
        c.fill(x, y, x + w, y + 1, claro);
        c.fill(x, y, x + 1, y + h, claro);
        c.fill(x, y + h - 1, x + w, y + h, oscuro);
        c.fill(x + w - 1, y, x + w, y + h, oscuro);
        // contorno exterior oscuro para separar del pergamino
        c.fill(x - 1, y - 1, x + w + 1, y, BORDE);
        c.fill(x - 1, y + h, x + w + 1, y + h + 1, BORDE);
        c.fill(x - 1, y, x, y + h, BORDE);
        c.fill(x + w, y, x + w + 1, y + h, BORDE);
    }

    /** Botón de madera con etiqueta crema centrada. */
    public static class BotonPergamino extends ButtonWidget {
        public BotonPergamino(int x, int y, int w, int h, Text mensaje, PressAction onPress) {
            super(x, y, w, h, mensaje, onPress, DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        protected void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            fondoBoton(c, getX(), getY(), getWidth(), getHeight(), isHovered(), active);
            var fuente = MinecraftClient.getInstance().textRenderer;
            Text m = getMessage();
            int tx = getX() + (getWidth() - fuente.getWidth(m)) / 2;
            int ty = getY() + (getHeight() - 8) / 2;
            int color = active ? TEXTO_CLARO : TEXTO_APAGADO;
            c.drawText(fuente, m, tx + 1, ty + 1, 0xFF2A180C, false);   // sombra
            c.drawText(fuente, m, tx, ty, color, false);
        }
    }

    private EstiloPergamino() {}
}
