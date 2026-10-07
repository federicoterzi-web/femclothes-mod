"""Genera los mapas de extrusión por prenda del relieve.

A pedido (2026-10-01, relieve: "3 archivo por prenda"): gris en el layout de
la skin clásica (64x64, acá a 4x = 256x256), 0 = plano, 255 = el máximo de
render.relieve.RelieveTela.EXTRUSION_MAXIMA (0.6 px hacia afuera); alfa 0 =
sin extrusión. El archivo se llama como el id de la prenda y lo lee
RelieveTela.extrusionDe (textures/models/relieve/<id>.png). Solo los costados
de cada parte (frente, espalda y laterales); las tapas no llevan relieve.

Muestras:
  - chaqueta (hoodie): bolsillo canguro con borde cosido, rib en el ruedo y
    en los puños, costura de la sisa.
  - pantalon: costuras laterales, bolsillos de atrás, bragueta y pretina.
  - socks_solid (medias): tejido acanalado fino y puño elastizado arriba.

Uso: python tools/generar_relieve_prendas.py
"""
from pathlib import Path

from PIL import Image

ESCALA = 4
RAIZ = Path(__file__).resolve().parent.parent / "src/main/resources/assets/femclothes/textures/models/relieve"

# (x0, y0) de cada parte en la skin clásica; ancho de frente.
PARTES = {
    "torso": (16, 16, 8),
    "brazo_der": (40, 16, 4),
    "brazo_izq": (32, 48, 4),
    "pierna_der": (0, 16, 4),
    "pierna_izq": (16, 48, 4),
}
CARAS = ("lado_a", "frente", "lado_b", "espalda")


def rect(parte, cara):
    """(x, y, ancho, alto) en px de skin de una cara de costado."""
    x0, y0, w = PARTES[parte]
    i = CARAS.index(cara)
    x = (x0, x0 + 4, x0 + 4 + w, x0 + 8 + w)[i]
    return x, y0 + 4, (4, w, 4, w)[i], 12


class Lienzo:
    def __init__(self):
        self.img = Image.new("RGBA", (64 * ESCALA, 64 * ESCALA), (0, 0, 0, 0))
        self.px = self.img.load()

    def pintar(self, parte, cara, f):
        """f(x, y) con x, y en px de la cara (0..ancho, 0..12) → 0..1 o None."""
        rx, ry, w, h = rect(parte, cara)
        for j in range(h * ESCALA):
            for i in range(w * ESCALA):
                x, y = (i + 0.5) / ESCALA, (j + 0.5) / ESCALA
                v = f(x, y, w)
                if v is None:
                    continue
                X, Y = rx * ESCALA + i, ry * ESCALA + j
                actual = self.px[X, Y][0] if self.px[X, Y][3] else 0
                g = max(actual, int(round(max(0.0, min(1.0, v)) * 255)))
                self.px[X, Y] = (g, g, g, 255)

    def todas(self, partes, f):
        for p in partes:
            for c in CARAS:
                self.pintar(p, c, f)


def rib(x, alto, desde, hasta):
    """Acanalado vertical (un canal por medio px) entre las filas desde..hasta."""
    def f(xx, y, w):
        if not (desde <= y < hasta):
            return None
        return alto * (0.55 if int(xx * 2) % 2 == 0 else 1.0)
    return f


def chaqueta():
    l = Lienzo()
    # Rib del ruedo (última fila del torso) y de los puños (última fila de cada brazo).
    l.todas(["torso"], rib(0, 0.7, 11, 12))
    l.todas(["brazo_der", "brazo_izq"], rib(0, 0.7, 11, 12))

    # Bolsillo canguro: panel apenas levantado con el borde cosido más alto.
    # Misma forma que el pintado de render/DetallesHoodie (hoodie largo): filas
    # 8..11, x 1..7 de la cara, las bocas en diagonal (1 px más angosto arriba).
    def bolsillo(x, y, w):
        y0, y1 = 8.0, 11.0
        if not (y0 <= y < y1):
            return None
        entrada = (y1 - y) / (y1 - y0)
        x0, x1 = 1.0 + entrada, 7.0 - entrada
        if not (x0 <= x < x1):
            return None
        borde = min(x - x0, x1 - x, y - y0, y1 - y)
        return 0.85 if borde < 0.3 else 0.4
    l.pintar("torso", "frente", bolsillo)

    # Costura de la sisa: una línea fina arriba de cada manga.
    def sisa(x, y, w):
        return 0.5 if 0.6 <= y < 0.9 else None
    l.todas(["brazo_der", "brazo_izq"], sisa)
    return l.img


def pantalon():
    l = Lienzo()
    # Costuras laterales: el centro de las caras de costado de cada pierna.
    def costura(x, y, w):
        return 0.55 if abs(x - w / 2) < 0.18 else None
    for p in ("pierna_der", "pierna_izq"):
        l.pintar(p, "lado_a", costura)
        l.pintar(p, "lado_b", costura)

    # Bolsillos de atrás: contorno cosido arriba de cada pierna.
    def bolsillo(x, y, w):
        x0, x1, y0, y1 = 0.7, w - 0.7, 0.6, 3.4
        if not (x0 <= x <= x1 and y0 <= y <= y1):
            return None
        borde = min(x - x0, x1 - x, y - y0, y1 - y)
        return 0.8 if borde < 0.25 else 0.25
    l.pintar("pierna_der", "espalda", bolsillo)
    l.pintar("pierna_izq", "espalda", bolsillo)

    # Pretina (filas 9..10 del torso) y bragueta (centro del frente, 10..12).
    def pretina(x, y, w):
        return 0.6 if 9.0 <= y < 10.0 else None
    l.todas(["torso"], pretina)

    def bragueta(x, y, w):
        return 0.5 if 10.0 <= y < 12 and abs(x - (w / 2 - 0.6)) < 0.15 else None
    l.pintar("torso", "frente", bragueta)
    return l.img


def medias():
    l = Lienzo()
    # Tejido acanalado fino en toda la media.
    def tejido(x, y, w):
        return 0.25 if int(x * 2) % 2 == 0 else 0.5
    l.todas(["pierna_der", "pierna_izq"], tejido)
    return l.img


def main():
    RAIZ.mkdir(parents=True, exist_ok=True)
    for nombre, img in (("chaqueta", chaqueta()), ("pantalon", pantalon()), ("socks_solid", medias())):
        img.save(RAIZ / f"{nombre}.png")
    print("ok", RAIZ)


if __name__ == "__main__":
    main()
