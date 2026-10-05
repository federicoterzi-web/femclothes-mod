"""Esquema de la Modeladora para el sombrero de bruja (2026-10-05, "segunda tanda del sombrero"): como los de
pollera y capa, parte del esquema de Tintes (arte del usuario: pergamino, marco de madera y bronce, líneas y puntos
fucsia) pero con un sombrero dibujado por código (formas simples) y dos marcos: Ala y Punta. Escribe
`textures/gui/container/esquema_sombrero.png` (960x544, paleta de 256) e imprime los PIN_POS/PIN_BTN para
`ModeladoScreenHandler` (origen del ítem de 16x16 = centro/4 - 8; la Y de PIN_POS es absoluta: +36).

Uso: python tools/generar_esquema_sombrero.py
"""
import os
import random

from PIL import Image, ImageDraw, ImageFilter

RAIZ = os.path.join(os.path.dirname(__file__), "..")
GUI = os.path.join(RAIZ, "src", "main", "resources", "assets", "femclothes", "textures", "gui", "container")
ESQUEMA_Y = 36
SS = 4

TINTA = (45, 28, 16)
CREMA = (240, 226, 200)
CREMA_SOMBRA = (222, 204, 172)
ALA_CARA = (231, 214, 184)
PUNTADO = (178, 150, 118)

# Marcos y puntos de los esquemas de Tintes (medidos sobre los PNG de 960x544)
MARCO_IZQ_CAPA = (184, 207, 261, 282)
MARCO_DER_CAPA = (693, 65, 770, 141)
PUNTO_CAPA = (524, 95)
# (rol, lado, centro del marco, punto sobre el sombrero)
PINES = [
    ("ALA", "izq", (205, 330), (318, 392)),
    ("PUNTA", "der", (760, 120), (448, 108)),
]


def borrar(im, cajas, rnd):
    """Rellena las cajas con el pergamino de alrededor (relleno por escalas: a cada píxel borrado le toca el promedio
    de lo conocido en el radio más chico que alcance) y le suma un poco de grano."""
    import numpy as np
    arr = np.array(im.convert("RGB")).astype(float)
    h, w = arr.shape[:2]
    borrado = np.zeros((h, w), bool)
    for x0, y0, x1, y1 in cajas:
        borrado[y0:y1, x0:x1] = True
    # Un margen de más alrededor (el contorno viejo de la prenda y las sombras no cuentan como pergamino) y fuera
    # todo lo oscuro o fucsia que quede suelto.
    ancho = np.array(Image.fromarray((borrado * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(21))) > 0
    oscuro = (arr.sum(axis=2) / 3 < 150) | ((arr[..., 0] - arr[..., 1]) > 70)
    borrado = borrado | (ancho & oscuro)
    borrado = np.array(Image.fromarray((borrado * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(9))) > 0
    conocido = (~borrado & ~oscuro).astype(float)
    pegado = np.zeros_like(arr)
    pendiente = borrado.copy()
    for radio in (3, 6, 12, 24, 48, 96, 192):
        peso = np.array(Image.fromarray((conocido * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radio))).astype(float) / 255
        canales = []
        for c in range(3):
            capa = Image.fromarray(np.clip(arr[..., c] * conocido, 0, 255).astype(np.uint8))
            canales.append(np.array(capa.filter(ImageFilter.GaussianBlur(radio))).astype(float) / 255)
        ok = pendiente & (peso > 0.12)
        for c in range(3):
            est = np.where(peso > 1e-6, canales[c] * 255 / np.maximum(peso, 1e-6), 0)
            pegado[..., c][ok] = est[ok]
        pendiente &= ~ok
    ruido = np.random.default_rng(11).normal(0, 3.0, (h, w, 1))
    arr[borrado] = np.clip(pegado[borrado] + ruido[borrado], 0, 255)
    im.paste(Image.fromarray(arr.astype(np.uint8)).convert("RGBA"))


def poligono(d, pts, relleno, contorno=None, grosor=0):
    d.polygon([(x * SS, y * SS) for x, y in pts], fill=relleno)
    if contorno:
        d.line([(x * SS, y * SS) for x, y in pts + [pts[0]]], fill=contorno, width=grosor * SS, joint="curve")


def sombrero():
    capa = Image.new("RGBA", (960 * SS, 544 * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(capa)
    g = 7
    # ala: elipse con borde
    cx, cy, rx, ry = 480, 392, 218, 50
    d.ellipse(((cx - rx - g) * SS, (cy - ry - g) * SS, (cx + rx + g) * SS, (cy + ry + g) * SS), fill=TINTA + (255,))
    d.ellipse(((cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS), fill=ALA_CARA + (255,))
    d.ellipse(((cx - rx + 14) * SS, (cy - ry + 8) * SS, (cx + rx - 14) * SS, (cy + ry - 14) * SS), fill=CREMA_SOMBRA + (255,))
    # cono con la punta doblada hacia la izquierda
    izq = [(352, 392), (376, 316), (402, 232), (430, 160), (456, 112), (440, 74), (398, 56)]
    der = [(428, 52), (470, 88), (500, 142), (540, 214), (580, 296), (606, 392)]
    cono = izq + der[::-1][::-1]
    contorno = izq + der
    poligono(d, [(x - 0, y) for x, y in contorno], CREMA + (255,), TINTA + (255,), g)
    # sombra del lado derecho
    sombra = [(498, 150), (540, 214), (580, 296), (606, 392), (500, 392), (478, 330), (480, 220)]
    poligono(d, sombra, CREMA_SOMBRA + (150,))
    # el contorno otra vez por encima de la sombra
    d.line([(x * SS, y * SS) for x, y in contorno], fill=TINTA + (255,), width=g * SS, joint="curve")
    # cinta: franja en la base del cono
    cinta = [(366, 345), (594, 345), (604, 392), (352, 392)]
    poligono(d, cinta, ALA_CARA + (255,), TINTA + (255,), 5)
    # puntadas de la cinta
    for y in (356, 380):
        x = 372
        while x < 590:
            d.line([(x * SS, y * SS), ((x + 14) * SS, y * SS)], fill=PUNTADO + (255,), width=3 * SS)
            x += 26
    # puntadas del borde del ala
    pasos = 44
    import math
    for i in range(pasos):
        if i % 2:
            continue
        a0, a1 = 2 * math.pi * i / pasos, 2 * math.pi * (i + 1) / pasos
        pts = [((cx + (rx - 10) * math.cos(a)) * SS, (cy + (ry - 6) * math.sin(a)) * SS) for a in (a0, (a0 + a1) / 2, a1)]
        d.line(pts, fill=PUNTADO + (255,), width=2 * SS)
    return capa.resize((960, 544), Image.LANCZOS)


def linea(base, desde, hasta, color):
    capa = Image.new("RGBA", (base.width * SS, base.height * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(capa)
    d.line([(desde[0] * SS, desde[1] * SS), (hasta[0] * SS, hasta[1] * SS)], fill=color + (255,), width=3 * SS)
    base.alpha_composite(capa.resize(base.size, Image.LANCZOS))


def main(nombre="esquema_sombrero.png"):
    rnd = random.Random(11)
    im = Image.open(os.path.join(GUI, "esquema_tintes_pollera.png")).convert("RGBA")
    capa_src = Image.open(os.path.join(GUI, "esquema_tintes_capa.png")).convert("RGBA")
    # marcos y punto del esquema de la capa (aún intactos)
    marcos = {"izq": capa_src.crop(MARCO_IZQ_CAPA), "der": capa_src.crop(MARCO_DER_CAPA)}
    px = capa_src.load()
    color = (190, 34, 98)   # el fucsia de las líneas de los otros esquemas
    punto = capa_src.crop((PUNTO_CAPA[0] - 12, PUNTO_CAPA[1] - 12, PUNTO_CAPA[0] + 12, PUNTO_CAPA[1] + 12))
    mascara = Image.new("L", punto.size, 0)
    pp, mp = punto.load(), mascara.load()
    for y in range(punto.height):
        for x in range(punto.width):
            r, g, b, _ = pp[x, y]
            if (r - g > 55) or (r > 215 and g > 200 and b > 190 and (x - 12) ** 2 + (y - 12) ** 2 < 49):
                mp[x, y] = 255
    mascara = mascara.filter(ImageFilter.MaxFilter(3))
    # limpiar la pollera, los 3 marcos y sus líneas
    borrar(im, [(258, 80, 706, 478), (192, 58, 292, 156), (690, 196, 808, 306), (138, 372, 334, 474)], rnd)
    im.alpha_composite(sombrero())
    for rol, lado, centro, objetivo in PINES:
        m = marcos[lado]
        x0, y0 = centro[0] - m.width // 2, centro[1] - m.height // 2
        im.paste(m, (x0, y0))
        borde = (x0 + m.width, centro[1]) if lado == "izq" else (x0, centro[1])
        linea(im, borde, objetivo, color)
        im.paste(punto, (objetivo[0] - 12, objetivo[1] - 12), mascara)
    im.convert("RGB").quantize(colors=256, method=Image.MEDIANCUT).save(os.path.join(GUI, nombre), optimize=True)
    out = [(rol, round(c[0] / 4 - 8), round(c[1] / 4 - 8) + ESQUEMA_Y) for rol, _, c, _ in PINES]
    print("PIN_POS", ", ".join(f"{{{x}, {y}}}" for _, x, y in out), "#", " ".join(r for r, _, _ in out))
    print("PIN_BTN", ", ".join(f"{{{x + 17}, {y - 1}}}" for _, x, y in out))


if __name__ == "__main__":
    main()
