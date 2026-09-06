package com.femclothes.render;

import com.femclothes.garment.Parte;
import net.minecraft.util.Identifier;

/**
 * Un pedazo dibujable de una prenda: que parte del cuerpo, en que capa, con
 * que textura.
 *
 * Que una prenda sea una LISTA de estas y no una sola cosa es lo que deja que
 * una remera dibuje torso y brazos con dos texturas distintas, que un remeron
 * baje al muslo, y que una polera sea torso mas una banda en la cabeza. El
 * slot sigue existiendo para el inventario; lo que se dibuja son piezas.
 *
 * La textura va en LAYOUT DE SKIN a {@link CuerpoGeometria#ESCALA}x, y tiene
 * que ser TRANSPARENTE donde la prenda no tiene tela. Ese es el contrato del
 * sistema de capas: rellenar el hueco con piel es lo que hacia que dos
 * prendas en la misma pierna se taparan entre si.
 */
public record Pieza(Parte parte, int capa, Identifier textura) {
}
