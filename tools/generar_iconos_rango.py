"""Íconos del molde de rango (2026-10-04, "ayuda con el icono del rango... base de papel con las cuatro areas de
distinta iluminacion... los destaques van en el color de la modeladora... un indicador acumulativo del 1 al 7").

Mismo papel que el ícono automático del molde de aplique (client/IconoMoldeAplique: kraft con cuatro zonas de luz,
pliegues en cruz y grano) con el recuadro en el cobre de la Modeladora y siete peldaños que se van llenando.

Orden por cobertura (de la más corta a la más larga): CERO 1, MINIMO 2, CORTO 3, MEDIO 4, MEDIOLARGO 5, LARGO 6,
MAXIMO 7.

Uso:
  python tools/generar_iconos_rango.py            # solo arma la hoja de vista previa (tools/vista_iconos_rango.png)
  python tools/generar_iconos_rango.py --aplicar  # además guarda los 7 en textures/item (respalda los actuales)
"""
import os
import shutil
import sys

from PIL import Image

RAIZ = os.path.join(os.path.dirname(__file__), "..")
ITEMS = os.path.join(RAIZ, "src", "main", "resources", "assets", "femclothes", "textures", "item")
RESPALDO = os.path.join(RAIZ, "assets_viejos", "molde_rango_2026-10-04")

T = 64
PAPEL = (233, 204, 165)
TINTA = (59, 36, 16)
# Cobre de la Modeladora (tools/generar_textura_modelado.py, tema "cobre")
COBRE_CLARO = (242, 170, 120)
COBRE_MEDIO = (184, 102, 56)
COBRE_OSCURO = (96, 44, 22)

ORDEN = ["cero", "minimo", "corto", "medio", "mediolargo", "largo", "maximo"]


def papel(img):
    px = img.load()
    for y in range(T):
        for x in range(T):
            luz = (1.07 if x < T // 2 else 1.01) if y < T // 2 else (0.86 if x < T // 2 else 0.94)
            import math
            luz *= 1 + 0.03 * math.sin((x + y) * 0.35)
            grano = ((x * 73856093) ^ (y * 19349663)) & 7
            luz += (grano - 3.5) / 255 * 3
            if x == T // 2 - 1 or y == T // 2 - 1:
                luz *= 0.9
            if x == T // 2 or y == T // 2:
                luz *= 1.04
            px[x, y] = tuple(max(0, min(255, int(c * luz))) for c in PAPEL) + (255,)


def marco(img):
    px = img.load()
    for y in range(T):
        for x in range(T):
            d = min(x, y, T - 1 - x, T - 1 - y)
            if d == 0:
                px[x, y] = COBRE_OSCURO + (255,)
            elif d <= 2:
                px[x, y] = COBRE_CLARO + (255,)
            elif d == 3:
                px[x, y] = TINTA + (255,)


def peldanos(img, llenos):
    """7 peldaños que crecen de izquierda a derecha, los primeros `llenos` en cobre y el resto 'grabados'."""
    px = img.load()
    ancho, hueco = 5, 2
    total = 7 * ancho + 6 * hueco
    x0 = (T - total) // 2
    base = 54
    for i in range(7):
        alto = 6 + i * 4
        x = x0 + i * (ancho + hueco)
        top = base - alto
        lleno = i < llenos
        for yy in range(top, base + 1):
            for xx in range(x, x + ancho):
                borde = yy in (top, base) or xx in (x, x + ancho - 1)
                if lleno:
                    if borde:
                        c = COBRE_OSCURO
                    elif yy == top + 1 or xx == x + 1:
                        c = COBRE_CLARO
                    else:
                        c = COBRE_MEDIO
                else:
                    # peldaño vacío: solo el contorno, en marrón apagado, con el interior apenas hundido
                    r, g, b, _ = px[xx, yy]
                    c = tuple(int(v * 0.8) for v in (r, g, b)) if not borde else tuple(int(v * 0.55) for v in (r, g, b))
                px[xx, yy] = c + (255,)


def icono(llenos):
    img = Image.new("RGBA", (T, T))
    papel(img)
    peldanos(img, llenos)
    marco(img)
    return img


def main():
    iconos = [icono(i + 1) for i in range(7)]
    hoja = Image.new("RGBA", (7 * (T * 3 + 12) + 12, T * 3 + 24), (60, 52, 46, 255))
    for i, im in enumerate(iconos):
        hoja.paste(im.resize((T * 3, T * 3), Image.NEAREST), (12 + i * (T * 3 + 12), 12))
    hoja.save(os.path.join(os.path.dirname(__file__), "vista_iconos_rango.png"))
    if "--aplicar" in sys.argv:
        os.makedirs(RESPALDO, exist_ok=True)
        for nombre, im in zip(ORDEN, iconos):
            ruta = os.path.join(ITEMS, f"molde_rango_{nombre}.png")
            if os.path.exists(ruta) and not os.path.exists(os.path.join(RESPALDO, os.path.basename(ruta))):
                shutil.copy(ruta, RESPALDO)
            im.save(ruta)


if __name__ == "__main__":
    main()
