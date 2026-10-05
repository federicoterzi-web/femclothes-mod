"""Esquema de la Modeladora para la banda (2026-10-05, "correas y cintos"): igual que el del sombrero (parte del
esquema de Tintes y reusa sus funciones) pero con un cinto dibujado por código y tres marcos: Zona, Ancho y Herraje.
Escribe `textures/gui/container/esquema_banda.png` e imprime los PIN_POS/PIN_BTN para `ModeladoScreenHandler`.

Uso: python tools/generar_esquema_banda.py
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
import generar_esquema_sombrero as base

SS = base.SS


def cinto():
    capa = Image.new("RGBA", (960 * SS, 544 * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(capa)
    g = 7
    x0, y0, x1, y1 = 300, 286, 672, 372
    d.rounded_rectangle((( x0 - g) * SS, (y0 - g) * SS, (x1 + g) * SS, (y1 + g) * SS), radius=18 * SS, fill=base.TINTA + (255,))
    d.rounded_rectangle((x0 * SS, y0 * SS, x1 * SS, y1 * SS), radius=14 * SS, fill=base.ALA_CARA + (255,))
    d.rectangle(((x0 + 8) * SS, (y0 + 8) * SS, (x1 - 8) * SS, (y0 + 22) * SS), fill=base.CREMA + (255,))
    # puntadas arriba y abajo
    for y in (y0 + 14, y1 - 14):
        x = x0 + 22
        while x < x1 - 26:
            d.line([(x * SS, y * SS), ((x + 14) * SS, y * SS)], fill=base.PUNTADO + (255,), width=3 * SS)
            x += 26
    # agujeritos a la derecha
    for x in range(560, 650, 30):
        d.ellipse(((x - 7) * SS, (329 - 7) * SS, (x + 7) * SS, (329 + 7) * SS), fill=base.TINTA + (255,))
    # herraje: placa con aro al centro
    hx0, hy0, hx1, hy1 = 420, 262, 510, 396
    d.rectangle(((hx0 - g) * SS, (hy0 - g) * SS, (hx1 + g) * SS, (hy1 + g) * SS), fill=base.TINTA + (255,))
    d.rectangle((hx0 * SS, hy0 * SS, hx1 * SS, hy1 * SS), fill=(214, 176, 92, 255))
    d.rectangle(((hx0 + 20) * SS, (hy0 + 22) * SS, (hx1 - 20) * SS, (hy1 - 22) * SS), fill=base.TINTA + (255,))
    d.rectangle(((hx0 + 28) * SS, (hy0 + 30) * SS, (hx1 - 28) * SS, (hy1 - 30) * SS), fill=base.ALA_CARA + (255,))
    d.rectangle(((hx0 + 38) * SS, 322 * SS, (hx1 + 60) * SS, 336 * SS), fill=(214, 176, 92, 255))  # púa
    return capa.resize((960, 544), Image.LANCZOS)


if __name__ == "__main__":
    base.PINES = [
        ("ZONA", "izq", (205, 330), (306, 330)),
        ("ANCHO", "der", (760, 120), (600, 288)),
        ("HERRAJE", "der", (749, 251), (505, 330)),
    ]
    base.sombrero = cinto
    base.main("esquema_banda.png")
