"""Ropa interior del cuerpo base (2026-09-30, "dale pero de esto si haceme vos
los assets"): 6 PNG en gris con el layout de la skin, a 6x (384x384) como las
máscaras de cuerpo. El gris multiplica al color elegido en la GUI
(CuerpoBaseTextures#superponer), así que el blanco es la tela y los grises son
costuras, elásticos y encaje.

Coordenadas en píxeles de skin (64x64). El torso se dibuja sobre su "tira":
s = 0..24 alrededor del cuerpo (0..4 costado derecho, 4..12 frente, 12..16
costado izquierdo, 16..24 espalda) y r = 0..12 de los hombros a la
entrepierna. Las piernas igual, con r desde la cadera.

Uso: python3 tools/generar_ropa_interior.py (desde la raíz del repo).
"""
import os

import numpy as np
from PIL import Image, ImageDraw

S = 6
T = 64 * S
SALIDA = "src/main/resources/assets/femclothes/textures/entity/cuerpo"

TELA, COSTURA, ELASTICO, RAYA, ENCAJE = 244, 196, 214, 176, 212

# Solo las caras que puede tapar la ropa interior: torso (tira y tapas) y piernas.
PERMITIDO = np.zeros((T, T), bool)
for x0, y0, x1, y1 in ((16, 20, 40, 32), (20, 16, 36, 20), (0, 20, 16, 32), (16, 52, 32, 64)):
    PERMITIDO[y0 * S:y1 * S, x0 * S:x1 * S] = True


class Lienzo:
    def __init__(self):
        self.g = Image.new("L", (T, T), 0)
        self.a = Image.new("L", (T, T), 0)
        self.dg = ImageDraw.Draw(self.g)
        self.da = ImageDraw.Draw(self.a)

    def poly(self, pts, gris, tapar=True):
        p = [(x * S, y * S) for x, y in pts]
        self.dg.polygon(p, fill=gris)
        if tapar:
            self.da.polygon(p, fill=255)

    def rect(self, x0, y0, x1, y1, gris, tapar=True):
        # PIL pinta también el borde de abajo/derecha: un pixel menos para no invadir la cara vecina.
        e = 1 / S
        self.poly([(x0, y0), (x1 - e, y0), (x1 - e, y1 - e), (x0, y1 - e)], gris, tapar)

    def borrar(self, pts):
        p = [(x * S, y * S) for x, y in pts]
        self.da.polygon(p, fill=0)

    def elipse_borrar(self, x0, y0, x1, y1):
        self.da.ellipse((x0 * S, y0 * S, x1 * S, y1 * S), fill=0)

    def linea(self, pts, gris, ancho=0.34):
        self.dg.line([(x * S, y * S) for x, y in pts], fill=gris, width=max(1, round(ancho * S)))

    def guardar(self, nombre):
        g = np.asarray(self.g, np.float32)
        a = (np.asarray(self.a) > 127) & PERMITIDO
        # Borde un poco más oscuro: se lee el contorno de la prenda sobre la piel.
        borde = np.zeros_like(a)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            borde |= a & ~np.roll(np.roll(a, dy, 0), dx, 1)
        g = np.where(borde, g * 0.84, g)
        out = np.zeros((T, T, 4), np.uint8)
        out[..., 0] = out[..., 1] = out[..., 2] = np.clip(g, 0, 255).astype(np.uint8)
        out[..., 3] = np.where(a, 255, 0)
        Image.fromarray(out, "RGBA").save(os.path.join(SALIDA, nombre + ".png"))
        print("guardado:", nombre)


# ─── tiras ──────────────────────────────────────────────────────────────────
def torso(s, r):
    """Tira del torso → skin: s 0..24 empieza en u16, r 0..12 empieza en v20."""
    return (16 + s, 20 + r)


def pierna_der(s, r):
    return (0 + s, 20 + r)


def pierna_izq(s, r):
    return (16 + s, 52 + r)


def en(f, pts):
    return [f(s, r) for s, r in pts]


def banda(L, f, r0, r1, gris, s0=0, s1=24):
    L.poly(en(f, [(s0, r0), (s1, r0), (s1, r1), (s0, r1)]), gris)


def elastico(L, f, r0, r1, s1=24, rayas=1):
    banda(L, f, r0, r1, ELASTICO, 0, s1)
    for k in range(rayas):
        rr = r0 + (r1 - r0) * (k + 1) / (rayas + 1)
        L.linea(en(f, [(0, rr), (s1, rr)]), RAYA, 0.2)


def costuras_costado(L, r0, r1):
    for s in (4, 12, 16):
        L.linea(en(torso, [(s, r0), (s, r1)]), COSTURA, 0.2)


# Tapas del torso: arriba u20..28 v16..20 (frente en v20); abajo u28..36 v16..20 (frente en v16).
def tapa_arriba(L, u0, u1, gris):
    L.rect(20 + u0, 16, 20 + u1, 20, gris)


def tapa_abajo(L, u0, u1, gris):
    L.rect(28 + u0, 16, 28 + u1, 20, gris)


# ─── arriba ─────────────────────────────────────────────────────────────────
def bralette():
    L = Lienzo()
    # Taza: triángulo con el lado de abajo sobre la banda.
    for sa, sb, pico in ((4.5, 7.9, 6.5), (8.1, 11.5, 9.5)):
        L.poly(en(torso, [(sa, 4.1), (sb, 4.1), (pico + 0.4, 1.2), (pico - 0.4, 1.2)]), TELA)
        # Encaje: puntitos en filas.
        for r in np.arange(2.0, 4.0, 0.55):
            for s in np.arange(sa + 0.5, sb - 0.3, 0.6):
                ancho_fila = (r - 1.2) / 2.9 * (sb - sa) / 2
                if abs(s - (sa + sb) / 2) < ancho_fila - 0.2:
                    x, y = torso(s + (0.3 if int(r * 2) % 2 else 0), r)
                    L.dg.ellipse(((x - 0.13) * S, (y - 0.13) * S, (x + 0.13) * S, (y + 0.13) * S), fill=ENCAJE)
        L.linea(en(torso, [(sa, 4.1), (pico - 0.4, 1.2)]), COSTURA, 0.2)
        L.linea(en(torso, [(sb, 4.1), (pico + 0.4, 1.2)]), COSTURA, 0.2)
    # Banda de abajo, alrededor.
    elastico(L, torso, 4.0, 4.9)
    # Breteles: frente, tapa de arriba y espalda (u22/u26 → espalda u38/u34).
    for s_frente, u_tapa, s_espalda in ((6.1, 2.1, 22.0), (9.9, 5.9, 18.0)):
        L.rect(*torso(s_frente - 0.3, 0), *torso(s_frente + 0.3, 1.3), TELA)
        L.rect(20 + u_tapa - 0.3, 16, 20 + u_tapa + 0.3, 20, TELA)
        L.rect(*torso(s_espalda - 0.3, 0), *torso(s_espalda + 0.3, 4.0), TELA)
    L.guardar("interior_arriba_bralette")


def deportivo():
    L = Lienzo()
    banda(L, torso, 0.6, 5.4, TELA)
    # Escote redondo adelante y espalda nadadora.
    L.elipse_borrar(*torso(5.6, -0.6), *torso(10.4, 2.0))
    L.borrar(en(torso, [(16.6, 0.6), (19.0, 0.6), (19.2, 2.4), (18.0, 3.0), (16.6, 2.6)]))
    L.borrar(en(torso, [(21.0, 0.6), (23.4, 0.6), (23.4, 2.6), (22.0, 3.0), (20.8, 2.4)]))
    # Costados: más bajos (sisa).
    L.borrar(en(torso, [(0, 0.6), (4, 0.6), (4, 1.4), (0, 1.4)]))
    L.borrar(en(torso, [(12, 0.6), (16, 0.6), (16, 1.4), (12, 1.4)]))
    tapa_arriba(L, 0.9, 3.0, TELA)
    tapa_arriba(L, 5.0, 7.1, TELA)
    # Costura bajo el busto y elástico ancho abajo.
    L.linea(en(torso, [(4.6, 3.9), (11.4, 3.9)]), COSTURA, 0.2)
    elastico(L, torso, 4.4, 5.4, rayas=2)
    costuras_costado(L, 1.4, 4.4)
    L.guardar("interior_arriba_deportivo")


def binder():
    L = Lienzo()
    banda(L, torso, 0.3, 7.6, TELA)
    L.elipse_borrar(*torso(6.0, -0.8), *torso(10.0, 1.3))
    L.elipse_borrar(*torso(18.4, -0.6), *torso(21.6, 0.9))
    L.borrar(en(torso, [(0, 0.3), (4, 0.3), (4, 1.0), (0, 1.0)]))
    L.borrar(en(torso, [(12, 0.3), (16, 0.3), (16, 1.0), (12, 1.0)]))
    # Hombros anchos, tipo musculosa.
    tapa_arriba(L, 0.5, 3.4, TELA)
    tapa_arriba(L, 4.6, 7.5, TELA)
    # Panel de compresión adelante: pespunte doble.
    for s in (5.2, 10.8):
        L.linea(en(torso, [(s, 1.4), (s, 7.0)]), COSTURA, 0.18)
        L.linea(en(torso, [(s + (0.35 if s < 8 else -0.35), 1.4), (s + (0.35 if s < 8 else -0.35), 7.0)]), COSTURA, 0.14)
    costuras_costado(L, 1.0, 7.6)
    # Dobladillo.
    banda(L, torso, 7.1, 7.6, ELASTICO)
    L.linea(en(torso, [(0, 7.35), (24, 7.35)]), RAYA, 0.15)
    L.guardar("interior_arriba_binder")


# ─── abajo ──────────────────────────────────────────────────────────────────
def slip():
    L = Lienzo()
    # Frente: triángulo que baja al centro; costados bien cortos (tiro alto de pierna).
    L.poly(en(torso, [(4.0, 9.2), (12.0, 9.2), (12.0, 10.0), (9.3, 12.0), (6.7, 12.0), (4.0, 10.0)]), TELA)
    L.poly(en(torso, [(16.0, 9.2), (24.0, 9.2), (24.0, 10.3), (21.6, 12.0), (18.4, 12.0), (16.0, 10.3)]), TELA)
    banda(L, torso, 9.2, 10.0, TELA, 0, 4)
    banda(L, torso, 9.2, 10.0, TELA, 12, 16)
    elastico(L, torso, 9.2, 9.7)
    # Entrepierna: la tapa de abajo del torso, al medio.
    tapa_abajo(L, 2.0, 6.0, TELA)
    L.linea(en(torso, [(4.0, 10.0), (6.7, 12.0)]), COSTURA, 0.2)
    L.linea(en(torso, [(12.0, 10.0), (9.3, 12.0)]), COSTURA, 0.2)
    L.guardar("interior_abajo_slip")


def culotte():
    L = Lienzo()
    banda(L, torso, 8.9, 12.0, TELA)
    elastico(L, torso, 8.9, 9.5)
    tapa_abajo(L, 0, 8, TELA)
    for f in (pierna_der, pierna_izq):
        banda(L, f, 0.0, 1.3, TELA, 0, 16)
        banda(L, f, 1.0, 1.3, ELASTICO, 0, 16)
    L.linea(en(torso, [(8, 10.2), (8, 12)]), COSTURA, 0.18)
    L.linea(en(torso, [(20, 10.2), (20, 12)]), COSTURA, 0.18)
    L.guardar("interior_abajo_culotte")


def boxer():
    L = Lienzo()
    banda(L, torso, 8.4, 12.0, TELA)
    elastico(L, torso, 8.4, 9.7, rayas=2)
    tapa_abajo(L, 0, 8, TELA)
    # Bragueta: la costura curva al frente.
    L.linea([torso(8.0, 9.7), torso(8.0, 11.2), torso(9.2, 12.0)], COSTURA, 0.2)
    L.linea([torso(7.7, 9.7), torso(7.7, 11.3)], COSTURA, 0.14)
    L.linea(en(torso, [(20, 9.7), (20, 12)]), COSTURA, 0.18)
    for f, afuera in ((pierna_der, 2.0), (pierna_izq, 10.0)):
        banda(L, f, 0.0, 4.2, TELA, 0, 16)
        banda(L, f, 3.7, 4.2, ELASTICO, 0, 16)
        L.linea(en(f, [(afuera, 0.0), (afuera, 3.7)]), COSTURA, 0.2)
    L.guardar("interior_abajo_boxer")


if __name__ == "__main__":
    bralette()
    deportivo()
    binder()
    slip()
    culotte()
    boxer()
