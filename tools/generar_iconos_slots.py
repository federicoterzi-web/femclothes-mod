"""Genera los íconos de 16x16 de los slots de Trinkets (textures/gui/slot/*.png).

A pedido (2026-09-30, "los iconos de los slots... nada que ver con lo que
traen"): cada ícono es la silueta de la prenda que va en ese slot, sacada del
alfa de su sombra de ícono de 64x64 (textures/item/icono/<prenda>_sombra.png),
en gris oscuro semitransparente como los slots vacíos de armadura de vanilla.
La remera, el hoodie (con capucha y bolsillo) y las medias van escritos a
mano: a 16 px las sombras pierden las mangas y la separación de las piernas.

Uso: python tools/generar_iconos_slots.py
"""
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parent.parent / "src/main/resources/assets/femclothes/textures"
ICONOS = RAIZ / "item/icono"
SALIDA = RAIZ / "gui/slot"

RELLENO = (58, 58, 62, 120)
BORDE = (40, 40, 44, 190)


def mascara(prenda, lado=14, alto=14):
    """Alfa de la sombra, recortado a lo que tiene dibujo y encajado en lado x alto (sin deformar)."""
    im = Image.open(ICONOS / f"{prenda}_sombra.png").convert("RGBA")
    a = im.getchannel("A")
    a = a.crop(a.getbbox())
    escala = min(lado / a.width, alto / a.height)
    w, h = max(1, round(a.width * escala)), max(1, round(a.height * escala))
    chica = a.resize((w, h), Image.LANCZOS).point(lambda v: 255 if v > 110 else 0)
    out = Image.new("L", (16, 16), 0)
    out.paste(chica, ((16 - w) // 2, 16 - 1 - h))
    return out


def icono(m):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px, mp = img.load(), m.load()
    for y in range(16):
        for x in range(16):
            if not mp[x, y]:
                continue
            borde = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16 and mp[x + dx, y + dy])
                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            px[x, y] = BORDE if borde else RELLENO
    return img


def dibujada(filas):
    """Máscara escrita a mano (# = tela): a 16 px la sombra de la remera y de
    las medias pierde las mangas / la separación entre piernas."""
    m = Image.new("L", (16, 16), 0)
    px = m.load()
    for y, fila in enumerate(filas):
        for x, c in enumerate(fila):
            if c == "#":
                px[x, y] = 255
    return m


REMERA = [
    "................",
    "................",
    ".....##..##.....",
    "..#####....####.",
    ".##############.",
    "###############.",
    "###.#########.##",
    "##..#########..#",
    "....#########...",
    "....#########...",
    "....#########...",
    "....#########...",
    "....#########...",
    "....#########...",
    "................",
    "................",
]
HOODIE = [
    ".....######.....",
    "....##....##....",
    "...##......##...",
    "..####....####..",
    ".##############.",
    "################",
    "###.#########.##",
    "###.#########.##",
    "###.#########.##",
    "###.#########.##",
    "###.##.....##.##",
    "....##.....##...",
    "....#########...",
    "....#########...",
    "................",
    "................",
]
MEDIAS = [
    "................",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...####..####...",
    "...#####.#####..",
    "...######.#####.",
    "...######.#####.",
    "................",
    "................",
    "................",
]


def main():
    SALIDA.mkdir(parents=True, exist_ok=True)
    icono(dibujada(REMERA)).save(SALIDA / "remera.png")
    icono(dibujada(HOODIE)).save(SALIDA / "chaqueta.png")
    icono(mascara("pantalon")).save(SALIDA / "pantalon.png")
    icono(dibujada(MEDIAS)).save(SALIDA / "socks.png")
    icono(mascara("calientabrazos")).save(SALIDA / "armwarmer.png")
    icono(mascara("capa")).save(SALIDA / "capa.png")
    print("ok", SALIDA)


if __name__ == "__main__":
    main()
