"""Íconos de 64x64 de las prendas (2026-09-29, "ok pero los hagamos 64x64" +
"B" — el ícono se arma solo con la tela real de la prenda).

Por cada ícono salen dos PNG en textures/item/icono/:

- <nombre>_sombra.png: silueta (alfa) + relieve en gris (costuras, pliegues,
  talón...). Sin contorno: el contorno lo pone Java, así también sale en el
  borde de lo que se recorta (manga corta, shorts, zoquetes).
- <nombre>_mapa.png: para cada píxel, de qué cara de qué parte del cuerpo se
  saca el color (R = código * 20), y en qué punto de esa cara (G = u, B = v,
  0..255 sobre el ancho/alto de la cara). La v es la fila de la parte (0 =
  arriba, 255 = abajo): Java la compara con filaDesde/filaHasta de la pieza
  para recortar lo que la prenda no tapa.

Códigos (mismos que IconoPrenda.java):
  1 torso frente, 2 brazo derecho, 3 brazo izquierdo, 4 pierna derecha,
  5 pierna izquierda, 6 torso espalda (el interior del cuello), 7 pollera,
  8 capa (exterior de la capa, visto de atrás).
Visto de frente, el lado DERECHO del jugador queda a la izquierda del dibujo.

Uso: python3 tools/generar_iconos_prendas.py (desde la raíz del repo).
"""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

T = 64
SALIDA = "src/main/resources/assets/femclothes/textures/item/icono"

TORSO, BRAZO_DER, BRAZO_IZQ, PIERNA_DER, PIERNA_IZQ, TORSO_ATRAS, POLLERA, CAPA = 1, 2, 3, 4, 5, 6, 7, 8


# ─── relieve ────────────────────────────────────────────────────────────────
def mascara(poligonos=(), rects=()):
    m = Image.new("L", (T, T), 0)
    d = ImageDraw.Draw(m)
    for p in poligonos:
        d.polygon(p, fill=255)
    for r in rects:
        d.rectangle(r, fill=255)
    return m


def relieve(m, radio=3.0):
    """Tela acolchada: normal del contorno desenfocado, luz de arriba a la izquierda. Plano ≈ 219."""
    a = np.asarray(m, np.float32) / 255
    h = np.asarray(m.filter(ImageFilter.GaussianBlur(radio)), np.float32) / 255
    gy, gx = np.gradient(h)
    nx, ny, nz = -gx * 6, -gy * 6, np.ones_like(h)
    n = np.sqrt(nx ** 2 + ny ** 2 + nz ** 2)
    lx, ly, lz = -0.55, -0.65, 0.9
    lam = (nx * lx + ny * ly + nz * lz) / (n * np.sqrt(lx * lx + ly * ly + lz * lz))
    v = 150 + 95 * lam
    return np.where(a > 0.5, v, -1.0)


def linea(v, pts, oscuro=0.72, luz=True, ancho=1):
    m = Image.new("L", (T, T), 0)
    ImageDraw.Draw(m).line(pts, fill=255, width=ancho)
    a = np.asarray(m) > 127
    v = v.copy()
    sel = a & (v >= 0)
    v[sel] *= oscuro
    if luz:
        b = np.roll(np.roll(a, 1, 0), 1, 1) & ~a & (v >= 0)
        v[b] = np.minimum(250, v[b] * 1.12)
    return v


def zona(v, forma, factor, dib):
    m = Image.new("L", (T, T), 0)
    getattr(ImageDraw.Draw(m), forma)(dib, fill=255)
    a = (np.asarray(m) > 127) & (v >= 0)
    v = v.copy()
    v[a] = np.clip(v[a] * factor, 30, 250)
    return v


def elastico(v, x0, y0, x1, y1, paso=2):
    v = v.copy()
    for y in range(max(0, y0), min(T, y1)):
        for x in range(max(0, x0), min(T, x1)):
            if v[y, x] >= 0:
                v[y, x] *= 1.08 if (x // paso) % 2 == 0 else 0.84
    return v


# ─── mapa ───────────────────────────────────────────────────────────────────
class Mapa:
    def __init__(self):
        self.codigo = np.zeros((T, T), np.uint8)
        self.u = np.zeros((T, T), np.float32)
        self.v = np.zeros((T, T), np.float32)

    def poner(self, x, y, codigo, u, v):
        self.codigo[y, x] = codigo
        self.u[y, x] = min(max(u, 0.0), 0.999)
        self.v[y, x] = min(max(v, 0.0), 0.999)


def extension_fila(a, y, x0, x1):
    """Primer y último x con tela en la fila y, dentro de [x0, x1]."""
    xs = [x for x in range(x0, x1 + 1) if a[y, x]]
    return (xs[0], xs[-1]) if xs else (x0, x1)


def guardar(nombre, sombra, mapa):
    os.makedirs(SALIDA, exist_ok=True)
    s = np.zeros((T, T, 4), np.uint8)
    dentro = sombra >= 0
    g = np.clip(sombra, 0, 255).astype(np.uint8)
    s[..., 0] = s[..., 1] = s[..., 2] = np.where(dentro, g, 0)
    s[..., 3] = np.where(dentro, 255, 0)
    Image.fromarray(s, "RGBA").save(os.path.join(SALIDA, nombre + "_sombra.png"))
    mp = np.zeros((T, T, 4), np.uint8)
    mp[..., 3] = 255
    mp[..., 0] = np.where(dentro, mapa.codigo * 20, 0)
    mp[..., 1] = (mapa.u * 256).astype(np.uint8)
    mp[..., 2] = (mapa.v * 256).astype(np.uint8)
    Image.fromarray(mp, "RGBA").save(os.path.join(SALIDA, nombre + "_mapa.png"))
    faltan = int((dentro & (mapa.codigo == 0)).sum())
    print("guardado:", nombre, "" if faltan == 0 else f"(¡{faltan} píxeles sin mapa!)")


# ─── remera ─────────────────────────────────────────────────────────────────
# Silueta MÁXIMA (remerón de manga larga): el largo y la manga reales salen
# recortando filas en Java. Una por cuello, que va pintado en la tapa de la
# textura y no se puede leer del frente.
CUERPO_REMERA = [(22, 5), (41, 5), (50, 10), (50, 62), (13, 62), (13, 10)]
MANGA_IZQ = [(50, 10), (60, 20), (62, 58), (54, 59), (50, 26)]      # a la derecha del dibujo
MANGA_DER = [(63 - x, y) for x, y in MANGA_IZQ]
HOMBRO_Y, RUEDO_Y = 5, 62


def remera(cuello):
    rects = [(23, 0, 40, 10)] if cuello == "polera" else []
    m = mascara([CUERPO_REMERA, MANGA_IZQ, MANGA_DER], rects)
    a = np.asarray(m) > 127
    v = relieve(m)
    mapa = Mapa()
    for y in range(T):
        for x in range(T):
            if not a[y, x]:
                continue
            if 13 <= x <= 50:
                mapa.poner(x, y, TORSO, (x - 13) / 38, (y - HOMBRO_Y) / (RUEDO_Y - HOMBRO_Y + 1))
            else:
                # Manga: v a lo largo del eje hombro → puño, u de costado a costado.
                derecha_dibujo = x > 50
                ax, ay, bx, by = (53, 12, 58, 58) if derecha_dibujo else (10, 12, 5, 58)
                dx, dy = bx - ax, by - ay
                t = ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)
                t = min(max(t, 0.0), 1.0)
                eje_x = ax + t * dx
                mapa.poner(x, y, BRAZO_IZQ if derecha_dibujo else BRAZO_DER, 0.5 + (x - eje_x) / 9, t)
    # Cuello: el interior es la espalda vista por dentro, más oscura.
    if cuello == "polera":
        v = elastico(v, 23, 0, 41, 10)
        v = linea(v, [(23, 10), (40, 10)])
        for y in range(0, 11):
            for x in range(23, 41):
                mapa.poner(x, y, TORSO, (x - 13) / 38, 0.0)
    else:
        interior = Image.new("L", (T, T), 0)
        di = ImageDraw.Draw(interior)
        if cuello == "v":
            di.polygon([(24, 5), (39, 5), (31.5, 22)], fill=255)
        else:
            di.ellipse((24, 1, 39, 12), fill=255)
        ai = (np.asarray(interior) > 127) & a
        v = np.where(ai, v * 0.5, v)
        for y, x in zip(*np.nonzero(ai)):
            mapa.poner(x, y, TORSO_ATRAS, (x - 13) / 38, 0.02)
        if cuello == "v":
            v = linea(v, [(23, 5), (31.5, 24), (40, 5)], 0.8, ancho=2)
        else:
            v = linea(v, [(22, 6), (26, 11), (31, 13), (36, 11), (41, 6)], 0.8, ancho=2)
    # Costuras de hombro y un pliegue suave.
    v = linea(v, [(50, 11), (50, 25)])
    v = linea(v, [(13, 11), (13, 25)])
    v = linea(v, [(20, 30), (22, 56)], 0.9, luz=False)
    return v, mapa


# ─── pantalón ───────────────────────────────────────────────────────────────
PANTALON = [(13, 4), (50, 4), (54, 61), (36, 61), (32, 26), (27, 61), (9, 61)]
CADERA_Y = 18    # arriba: la pieza del torso (tiro); abajo: las piernas


def pantalon():
    m = mascara([PANTALON])
    a = np.asarray(m) > 127
    v = relieve(m)
    v = zona(v, "rectangle", 0.82, (13, 4, 50, 10))
    v = linea(v, [(13, 11), (50, 11)])
    for x in (18, 26, 37, 45):
        v = linea(v, [(x, 4), (x, 10)], 0.7, luz=False)
    v = linea(v, [(32, 11), (32, 24)], 0.75)
    v = linea(v, [(34, 11), (35, 20), (32, 24)], 0.85, luz=False)
    v = linea(v, [(16, 12), (22, 18)], 0.8)
    v = linea(v, [(47, 12), (41, 18)], 0.8)
    v = linea(v, [(20, 30), (18, 52)], 0.9, luz=False)
    v = linea(v, [(43, 30), (45, 52)], 0.9, luz=False)
    mapa = Mapa()
    for y in range(T):
        for x in range(T):
            if not a[y, x]:
                continue
            if y < CADERA_Y:
                # Filas 8.5..12 del torso: lo que tapa el tiro del pantalón.
                mapa.poner(x, y, TORSO, (x - 13) / 38, (8.5 + 3.5 * (y - 4) / (CADERA_Y - 4)) / 12)
            else:
                izquierda_dibujo = x <= 31
                x0, x1 = extension_fila(a, y, 0, 31) if izquierda_dibujo else extension_fila(a, y, 32, 63)
                mapa.poner(x, y, PIERNA_DER if izquierda_dibujo else PIERNA_IZQ,
                           (x - x0) / (x1 - x0 + 1), (y - CADERA_Y) / (61 - CADERA_Y + 1))
    return v, mapa


# ─── pollera ────────────────────────────────────────────────────────────────
def pollera():
    m = mascara([[(20, 13), (43, 13), (60, 56), (3, 56)]], [(20, 6, 43, 13)])
    a = np.asarray(m) > 127
    v = relieve(m, 2)
    v = zona(v, "rectangle", 0.82, (20, 6, 43, 13))
    v = linea(v, [(20, 14), (43, 14)])
    n = 8
    for i in range(n):
        xa0, xa1 = 20 + 23 * i / n, 20 + 23 * (i + 1) / n
        xb0, xb1 = 3 + 57 * i / n, 3 + 57 * (i + 1) / n
        v = zona(v, "polygon", 1.1 if i % 2 == 0 else 0.82, [(xa0, 15), (xa1, 15), (xb1, 56), (xb0, 56)])
        v = linea(v, [(xa1, 15), (xb1, 56)], 0.75, luz=False)
    v = linea(v, [(4, 52), (59, 52)], 0.85)
    mapa = Mapa()
    for y in range(T):
        for x in range(T):
            if not a[y, x]:
                continue
            if y < 15:
                mapa.poner(x, y, POLLERA, (x - 20) / 24, 0.02)
            else:
                t = (y - 15) / 41
                xa = 20 - 17 * t
                xb = 43 + 17 * t
                f = (x - xa) / (xb - xa) * n
                mapa.poner(x, y, POLLERA, f - int(f), t)
    return v, mapa


# ─── capa ───────────────────────────────────────────────────────────────────
def capa():
    """Vista de atrás, colgando de los hombros (2026-09-29): la capa más
    larga; Java recorta desde abajo según el largo real."""
    m = mascara([[(16, 4), (47, 4), (52, 8), (58, 61), (5, 61), (11, 8)]])
    a = np.asarray(m) > 127
    v = relieve(m, 2.2)
    v = zona(v, "rectangle", 0.84, (11, 4, 52, 8))            # hombros
    v = linea(v, [(12, 9), (51, 9)], 0.8)
    for i, (xa, xb) in enumerate([(20, 16), (27, 25), (36, 38), (43, 47)]):
        v = linea(v, [(xa, 12), (xb, 60)], 0.78 if i % 2 == 0 else 0.86, luz=True)
    mapa = Mapa()
    for y in range(T):
        for x in range(T):
            if not a[y, x]:
                continue
            x0, x1 = extension_fila(a, y, 0, 63)
            mapa.poner(x, y, CAPA, (x - x0) / (x1 - x0 + 1), max(0.0, (y - 4) / 58))
    return v, mapa


# ─── medias ─────────────────────────────────────────────────────────────────
MEDIA = [(0, 0), (14, 0), (14, 36), (26, 38), (29, 42), (27, 46), (4, 46), (0, 42)]


def media(dx, dy, codigo):
    p = [(x + dx, y + dy) for x, y in MEDIA]
    m = mascara([p])
    a = np.asarray(m) > 127
    v = relieve(m, 2.5)
    v = zona(v, "ellipse", 0.8, (dx - 2, dy + 34, dx + 8, dy + 47))     # talón
    v = zona(v, "ellipse", 0.8, (dx + 20, dy + 36, dx + 32, dy + 48))   # punta
    mapa = Mapa()
    for y in range(T):
        for x in range(T):
            if a[y, x]:
                lx, ly = x - dx, y - dy
                # La caña va de la fila 0 (arriba) a la 12; el pie es la última fila.
                u = lx / 15 if lx <= 14 else 0.9
                mapa.poner(x, y, codigo, u, ly / 47)
    return v, mapa


def superponer(abajo, arriba):
    va, ma = abajo
    vb, mb = arriba
    sel = vb >= 0
    v = np.where(sel, vb, va)
    m = Mapa()
    m.codigo = np.where(sel, mb.codigo, ma.codigo)
    m.u = np.where(sel, mb.u, ma.u)
    m.v = np.where(sel, mb.v, ma.v)
    return v, m


def medias():
    # La de atrás (a la derecha del dibujo) es la pierna izquierda.
    return superponer(media(24, 2, PIERNA_IZQ), media(6, 14, PIERNA_DER))


# ─── calientabrazos ─────────────────────────────────────────────────────────
def calientabrazo(dx, dy, codigo):
    m = mascara([[(dx, dy), (dx + 17, dy), (dx + 15, dy + 50), (dx + 3, dy + 50)]])
    a = np.asarray(m) > 127
    v = relieve(m, 2.5)
    v = zona(v, "ellipse", 0.4, (dx + 12, dy + 33, dx + 16, dy + 39))   # agujero del pulgar
    mapa = Mapa()
    for y in range(T):
        for x in range(T):
            if a[y, x]:
                x0, x1 = extension_fila(a, y, 0, 63)
                mapa.poner(x, y, codigo, (x - x0) / (x1 - x0 + 1), (y - dy) / 51)
    return v, mapa


def calientabrazos():
    return superponer(calientabrazo(38, 2, BRAZO_IZQ), calientabrazo(10, 10, BRAZO_DER))


if __name__ == "__main__":
    for cuello in ("redondo", "v", "polera"):
        guardar("remera_" + cuello, *remera(cuello))
    guardar("pantalon", *pantalon())
    guardar("pollera", *pollera())
    guardar("capa", *capa())
    guardar("medias", *medias())
    guardar("calientabrazos", *calientabrazos())
