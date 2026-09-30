"""Genera el fondo de GUI de la Estación de Tintes en estilo pergamino /
madera / latón (2026-09-27, "armemos la gui de tinturas") — mismo
tratamiento que ya tiene la Modeladora (ver generar_textura_modelado.py,
de donde salen las funciones de dibujo, copiadas y no importadas: son dos
scripts de build, no código del mod, duplicar acá es más simple que
partir un módulo compartido para dos usos). Mismas coordenadas exactas
que usa TinturasScreenHandler.java para los Slot reales.

Convención de slots: el ítem de un Slot(x, y) se dibuja en (x, y) de
16x16, y el marco de 18x18 va en (x-1, y-1), como en vanilla. Las filas
de Fijadas/Diseños NO se hornean acá: son botones (FijadaButton/
BotonDiseno) que dibujan su propio fondo en Java.

Salida determinística (semilla fija).
"""
import numpy as np
from PIL import Image, ImageDraw

PX = 100
M_MEDIO = 19 + PX               # 119, misma columna que Modeladora
M_MEDIO_ANCHO = 240              # ensanchada (2026-09-27) para el esquema con pines
M_DERECHA = M_MEDIO + M_MEDIO_ANCHO + 20   # 379
ANCHO = M_DERECHA + 162 + 19     # 560
# 408 (2026-09-27 v3, "copia el layout de la modeladora"): visor crecido
# a 196 alto (igual que la Modeladora), Entrada/Salida ahora son un
# cinturón grande de 32x32 y el Almacén se mudó a la columna derecha.
ALTO = 408

PERGAMINO = np.array([222, 188, 132], np.float32)
MADERA_OSCURA = np.array([74, 46, 26], np.float32)
MADERA_CLARA = np.array([122, 80, 46], np.float32)
LATON_CLARO = np.array([244, 208, 112], np.float32)
LATON = np.array([196, 150, 62], np.float32)
LATON_OSCURO = np.array([104, 72, 26], np.float32)
CUERO = np.array([44, 34, 28], np.float32)
COSTURA = (128, 88, 52)
COSTURA_LUZ = (240, 214, 160)

# Tema de color por máquina (2026-09-29, "cada una tenia un tema de color,
# estacion de tintes verdoso, modeladora cobrizo y sublimadora dorado,
# podemos cambiar los colores de las guis para que reflejen estas
# tematicas?"): cambia el metal de los filos/esquinas/slots y tiñe un poco
# el pergamino; la madera queda igual en las tres para que sigan siendo
# hermanas. Mismos valores que EstiloPergamino.Tema en Java.
TEMAS = {
    "laton": dict(pergamino=[222, 188, 132], claro=[244, 208, 112], medio=[196, 150, 62], oscuro=[104, 72, 26]),
    "verdin": dict(pergamino=[208, 198, 144], claro=[150, 212, 170], medio=[72, 146, 112], oscuro=[28, 74, 56]),
    "cobre": dict(pergamino=[226, 184, 140], claro=[242, 170, 120], medio=[184, 102, 56], oscuro=[96, 44, 22]),
    "oro": dict(pergamino=[230, 198, 124], claro=[255, 228, 118], medio=[216, 170, 38], oscuro=[122, 86, 8]),
}
# Interior hundido de los slots, relativo al pergamino (antes fijo en 178,142,96 / 120,88,54 / 150,114,72).
_SLOT_FONDO_REL = np.array([178, 142, 96], np.float32) / np.array([222, 188, 132], np.float32)
_SLOT_SOMBRA_REL = np.array([120, 88, 54], np.float32) / np.array([222, 188, 132], np.float32)
_SLOT_MEDIO_REL = np.array([150, 114, 72], np.float32) / np.array([222, 188, 132], np.float32)
SLOT_FONDO, SLOT_SOMBRA, SLOT_MEDIO = (178, 142, 96), (120, 88, 54), (150, 114, 72)


def aplicar_tema(nombre):
    """Reasigna la paleta del módulo (las funciones de dibujo la leen al llamarse)."""
    global PERGAMINO, LATON_CLARO, LATON, LATON_OSCURO, SLOT_FONDO, SLOT_SOMBRA, SLOT_MEDIO
    t = TEMAS[nombre]
    PERGAMINO = np.array(t["pergamino"], np.float32)
    LATON_CLARO = np.array(t["claro"], np.float32)
    LATON = np.array(t["medio"], np.float32)
    LATON_OSCURO = np.array(t["oscuro"], np.float32)
    SLOT_FONDO = tuple(int(v) for v in np.clip(PERGAMINO * _SLOT_FONDO_REL, 0, 255))
    SLOT_SOMBRA = tuple(int(v) for v in np.clip(PERGAMINO * _SLOT_SOMBRA_REL, 0, 255))
    SLOT_MEDIO = tuple(int(v) for v in np.clip(PERGAMINO * _SLOT_MEDIO_REL, 0, 255))

rng = np.random.default_rng(20260927)


def ruido(w, h, celda):
    gw, gh = max(2, w // celda + 2), max(2, h // celda + 2)
    g = rng.random((gh, gw)).astype(np.float32)
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
    return np.asarray(img, np.float32) / 255.0


def ruido_fractal(w, h, celdas, pesos):
    total = np.zeros((h, w), np.float32)
    for c, p in zip(celdas, pesos):
        total += (ruido(w, h, c) - 0.5) * p
    return total


def rgb(img):
    return np.clip(img, 0, 255).astype(np.uint8)


def pergamino(w, h):
    base = np.ones((h, w, 3), np.float32) * PERGAMINO
    manchas = ruido_fractal(w, h, [96, 48, 24, 12], [38, 26, 14, 8])
    fib = np.asarray(Image.fromarray((rng.random((h // 2 + 1, w // 24 + 2)) * 255).astype(np.uint8))
                     .resize((w, h), Image.BICUBIC), np.float32) / 255.0
    grano = (rng.random((h, w)).astype(np.float32) - 0.5) * 7
    val = manchas + (fib - 0.5) * 9 + grano
    base += val[:, :, None] * np.array([1.0, 0.95, 0.8], np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.maximum(np.abs(xx / w - 0.5), np.abs(yy / h - 0.5)) * 2
    base *= (1.0 - 0.30 * d ** 3)[:, :, None]
    return base


def madera(w, h, vertical=False):
    if vertical:
        v = np.asarray(Image.fromarray((rng.random((w // 2 + 1, h // 40 + 2)) * 255).astype(np.uint8))
                       .resize((h, w), Image.BICUBIC), np.float32).T / 255.0
        v = v[:h, :w] if v.shape == (h, w) else np.asarray(
            Image.fromarray((v * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), np.float32) / 255.0
    else:
        v = np.asarray(Image.fromarray((rng.random((h // 2 + 1, w // 40 + 2)) * 255).astype(np.uint8))
                       .resize((w, h), Image.BICUBIC), np.float32) / 255.0
    t = MADERA_OSCURA + (MADERA_CLARA - MADERA_OSCURA) * (0.25 + 0.75 * v)[:, :, None]
    t += ((rng.random((h, w)).astype(np.float32) - 0.5) * 6)[:, :, None]
    return t


def poner(dst, src, x, y):
    h, w = src.shape[:2]
    dst[y:y + h, x:x + w] = src


def marco_madera(img, borde):
    h, w = img.shape[:2]
    poner(img, madera(w, borde), 0, 0)
    poner(img, madera(w, borde), 0, h - borde)
    poner(img, madera(borde, h, True), 0, 0)
    poner(img, madera(borde, h, True), w - borde, 0)
    im = Image.fromarray(rgb(img))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, w - 1, h - 1], outline=(44, 26, 14))
    d.line([(1, 1), (w - 2, 1)], fill=(150, 104, 62))
    d.line([(1, 1), (1, h - 2)], fill=(150, 104, 62))
    d.rectangle([borde - 1, borde - 1, w - borde, h - borde], outline=(52, 32, 18))
    d.line([(borde, borde), (w - borde - 1, borde)], fill=(120, 84, 40))
    d.line([(borde, borde), (borde, h - borde - 1)], fill=(140, 102, 56))
    return np.asarray(im, np.float32)


def esquina(img, x, y, dx, dy, lado=26):
    for j in range(lado):
        for i in range(lado - j):
            px, py = x + dx * i, y + dy * j
            if not (0 <= px < img.shape[1] and 0 <= py < img.shape[0]):
                continue
            t = (i + j) / lado
            col = LATON_CLARO * (1 - t) + LATON * t * 0.95
            if i + j >= lado - 2:
                col = LATON_OSCURO
            elif i == 0 or j == 0:
                col = LATON_CLARO * 0.92
            img[py, px] = col
    for (ri, rj) in ((5, 5), (15, 4), (4, 15)):
        for oi in (-1, 0, 1):
            for oj in (-1, 0, 1):
                px, py = x + dx * (ri + oi), y + dy * (rj + oj)
                if 0 <= px < img.shape[1] and 0 <= py < img.shape[0]:
                    img[py, px] = LATON_OSCURO if (oi == 1 or oj == 1) else LATON_CLARO
    return img


def slot(img, sx, sy):
    x0, y0 = sx - 1, sy - 1
    im = Image.fromarray(rgb(img))
    d = ImageDraw.Draw(im)
    d.rectangle([x0, y0, x0 + 17, y0 + 17], fill=SLOT_FONDO)
    d.line([(x0 + 1, y0 + 1), (x0 + 16, y0 + 1)], fill=SLOT_SOMBRA)
    d.line([(x0 + 1, y0 + 1), (x0 + 1, y0 + 16)], fill=SLOT_SOMBRA)
    d.line([(x0 + 2, y0 + 2), (x0 + 15, y0 + 2)], fill=SLOT_MEDIO)
    d.line([(x0 + 2, y0 + 2), (x0 + 2, y0 + 15)], fill=SLOT_MEDIO)
    d.line([(x0, y0), (x0 + 17, y0)], fill=tuple(LATON_CLARO.astype(int)))
    d.line([(x0, y0), (x0, y0 + 17)], fill=tuple(LATON_CLARO.astype(int)))
    d.line([(x0, y0 + 17), (x0 + 17, y0 + 17)], fill=tuple(LATON_OSCURO.astype(int)))
    d.line([(x0 + 17, y0), (x0 + 17, y0 + 17)], fill=tuple(LATON_OSCURO.astype(int)))
    return np.asarray(im, np.float32)


def marco_visor(img, x0, y0, x1, y1):
    im = Image.fromarray(rgb(img))
    d = ImageDraw.Draw(im)
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=tuple(MADERA_OSCURA.astype(int)))
    d.rectangle([x0 + 2, y0 + 2, x1 - 3, y1 - 3], fill=tuple(CUERO.astype(int)))
    d.line([(x0, y0), (x1 - 1, y0)], fill=(40, 24, 12))
    d.line([(x0, y0), (x0, y1 - 1)], fill=(40, 24, 12))
    d.line([(x0, y1 - 1), (x1 - 1, y1 - 1)], fill=(150, 108, 62))
    d.line([(x1 - 1, y0), (x1 - 1, y1 - 1)], fill=(150, 108, 62))
    d.rectangle([x0 + 1, y0 + 1, x1 - 2, y1 - 2], outline=tuple(LATON_OSCURO.astype(int)))
    arr = np.asarray(im, np.float32)
    sub = arr[y0 + 3:y1 - 3, x0 + 3:x1 - 3]
    sub += ((rng.random(sub.shape[:2]).astype(np.float32) - 0.5) * 5)[:, :, None]
    return arr


def slot_grande(img, x0, y0, lado=32):
    """Marco de 32x32 (copiado de generar_textura_modelado.py): latón fino por fuera, interior de pergamino hundido."""
    im = Image.fromarray(rgb(img))
    d = ImageDraw.Draw(im)
    d.rectangle([x0, y0, x0 + lado - 1, y0 + lado - 1], fill=SLOT_FONDO)
    d.rectangle([x0 + 1, y0 + 1, x0 + lado - 2, y0 + lado - 2], outline=SLOT_SOMBRA)
    d.line([(x0 + 2, y0 + 2), (x0 + lado - 3, y0 + 2)], fill=SLOT_MEDIO)
    d.line([(x0 + 2, y0 + 2), (x0 + 2, y0 + lado - 3)], fill=SLOT_MEDIO)
    d.line([(x0, y0), (x0 + lado - 1, y0)], fill=tuple(LATON_CLARO.astype(int)))
    d.line([(x0, y0), (x0, y0 + lado - 1)], fill=tuple(LATON_CLARO.astype(int)))
    d.line([(x0, y0 + lado - 1), (x0 + lado - 1, y0 + lado - 1)], fill=tuple(LATON_OSCURO.astype(int)))
    d.line([(x0 + lado - 1, y0), (x0 + lado - 1, y0 + lado - 1)], fill=tuple(LATON_OSCURO.astype(int)))
    return np.asarray(im, np.float32)


def flecha(img, x0, y0, w, h):
    """Flecha de madera oscura con filo de latón, apuntando a la derecha (copiada de generar_textura_modelado.py)."""
    im = Image.fromarray(rgb(img))
    d = ImageDraw.Draw(im)
    cuerpo = h // 2
    yc = y0 + h // 2
    punta = w // 3
    pts = [(x0, yc - cuerpo // 2), (x0 + w - punta, yc - cuerpo // 2), (x0 + w - punta, y0),
           (x0 + w, yc), (x0 + w - punta, y0 + h), (x0 + w - punta, yc + cuerpo // 2), (x0, yc + cuerpo // 2)]
    d.polygon(pts, fill=tuple(MADERA_OSCURA.astype(int)), outline=tuple(LATON_OSCURO.astype(int)))
    d.line([(x0 + 1, yc - cuerpo // 2 + 1), (x0 + w - punta - 1, yc - cuerpo // 2 + 1)], fill=tuple(MADERA_CLARA.astype(int)))
    return np.asarray(im, np.float32)


def costura_vertical(img, x, y0, y1):
    im = Image.fromarray(rgb(img))
    d = ImageDraw.Draw(im)
    for yy in range(y0, y1, 5):
        d.line([(x + 1, yy + 1), (x + 1, yy + 3)], fill=COSTURA_LUZ)
        d.line([(x, yy), (x, yy + 2)], fill=COSTURA)
    return np.asarray(im, np.float32)


def main():
    aplicar_tema("verdin")
    img = pergamino(ANCHO, ALTO)
    img = marco_madera(img, 5)

    img = esquina(img, 0, 0, 1, 1)
    img = esquina(img, ANCHO - 1, 0, -1, 1)
    img = esquina(img, 0, ALTO - 1, 1, -1)
    img = esquina(img, ANCHO - 1, ALTO - 1, -1, -1)

    # Visor 3D, columna izquierda — crecido a 196 alto, igual que
    # ModeladoScreen (2026-09-27 v3, "copia el layout de la modeladora").
    img = marco_visor(img, 8, 18, 94, 214)

    # Columna del medio: esquema (36..172, sin hornear — lo pinta el PNG
    # de esquema encima, y los 3 Activo por categoría van ADENTRO de esos
    # cuadraditos, sin marco propio horneado), cinturón grande Entrada->
    # Salida (176, mismas coordenadas que ModeladoScreenHandler.ENTRADA_X/
    # SALIDA_X/SLOT_Y_IO), inventario y hotbar. Fijadas/Diseños son
    # botones, no Slot: sin hornear. Pollera no tiene esquema: sus 3
    # cuadraditos caen en una fila fija DENTRO del hueco 36..172 (mismas
    # coordenadas que TinturasScreenHandler#posCasilla, separados 28 para
    # que entre la chincheta) — se hornea acá porque en esa categoría no
    # hay PNG de esquema tapándola.
    for i in range(3):
        img = slot(img, M_MEDIO + 84 + i * 28, 94)
    img = slot_grande(img, M_MEDIO + 40, 176)
    img = slot_grande(img, M_MEDIO + 168, 176)
    img = flecha(img, M_MEDIO + 96, 184, 48, 16)
    for fila in range(3):
        for col in range(9):
            img = slot(img, M_MEDIO + col * 18, 324 + fila * 18)
    for col in range(9):
        img = slot(img, M_MEDIO + col * 18, 382)

    # Almacén de patrones: 30 lugares en 5x6 en la columna IZQUIERDA,
    # debajo de Guardar diseño (2026-09-28, "quiero mas espacios de
    # almacenamiento") — antes era una fila de 9 en la derecha. Mismas
    # coordenadas que TinturasScreenHandler.ALMACEN_X/Y/COLUMNAS.
    for i in range(30):
        img = slot(img, 8 + (i % 5) * 18, 280 + (i // 5) * 18)

    # Separadores cosidos entre las tres columnas.
    img = costura_vertical(img, M_MEDIO - 9, 16, ALTO - 16)
    img = costura_vertical(img, M_DERECHA - 9, 16, ALTO - 16)

    out = "src/main/resources/assets/femclothes/textures/gui/container/tinturas.png"
    Image.fromarray(rgb(img)).save(out)
    print("guardado:", out, ANCHO, "x", ALTO)


if __name__ == "__main__":
    main()
