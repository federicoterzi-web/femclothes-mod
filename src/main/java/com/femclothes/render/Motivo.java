package com.femclothes.render;

/**
 * Motivos de estampado repetido para la Estación de Tintes (2026-09-28,
 * propuesta de patrones, Fase 1: "corazones, estrellas..."). A diferencia
 * de las rayas ({@link PatronGenerador.Forma}), un motivo es un dibujo
 * chico en pixel art que {@link PatronGenerador} repite según la
 * {@link Repeticion} del cuadradito. Los sprites se escriben a mano acá:
 * 'X' = pinta, cualquier otra cosa = hueco.
 *
 * <p>{@link #VICHY} no tiene sprite: es procedural (franjas cruzadas con
 * cobertura parcial, ver {@link PatronGenerador#coberturaVichy}), y la
 * repetición no le cambia nada.
 */
public enum Motivo {
    CORAZONES(new String[]{
            ".XX...XX.",
            "XXXX.XXXX",
            "XXXXXXXXX",
            "XXXXXXXXX",
            ".XXXXXXX.",
            "..XXXXX..",
            "...XXX...",
            "....X....",
    }),
    ESTRELLAS(new String[]{
            "....X....",
            "....X....",
            "...XXX...",
            "XXXXXXXXX",
            ".XXXXXXX.",
            "..XXXXX..",
            "..XXXXX..",
            ".XX...XX.",
            ".X.....X.",
    }),
    LUNARES(new String[]{
            "..XXX..",
            ".XXXXX.",
            "XXXXXXX",
            "XXXXXXX",
            "XXXXXXX",
            ".XXXXX.",
            "..XXX..",
    }),
    VICHY(null);

    /** Filas del sprite, o null si el motivo es procedural. */
    private final String[] sprite;
    public final int ancho, alto;

    Motivo(String[] sprite) {
        this.sprite = sprite;
        this.ancho = sprite == null ? 0 : sprite[0].length();
        this.alto = sprite == null ? 0 : sprite.length;
    }

    public boolean esProcedural() { return sprite == null; }

    /** ¿Pinta el pixel (sx, sy) del sprite? Fuera del sprite, no. */
    public boolean pinta(int sx, int sy) {
        if (sprite == null || sx < 0 || sy < 0 || sy >= alto || sx >= ancho) return false;
        return sprite[sy].charAt(sx) == 'X';
    }
}
