"""SUPERSEDIDO (2026-09-24): el usuario curó un asset pergamino/madera
real ("me gustaba mas el estilo q te pase") que reemplazó el PNG que
generaba este script — esquema_remera.png ya no se genera con esto, es
un archivo pegado a mano. Este script queda solo de referencia histórica
(y por si algún día hace falta volver a un placeholder programático). Las
posiciones reales de los 8 PinSlot en ModeladoScreenHandler NO matchean
más las constantes PIN_* de acá — fueron remedidas sobre el asset nuevo.

Esquema de la remera con un pin por eje de corte (2026-09-24, "un
esquema de la prenda... arrastrar un patron a un slot en el esquema",
imagen de referencia con pines de pergamino) — versión placeholder en el
mismo estilo gris/bisel que el resto de la GUI (el arte pulido se
consigue aparte, ver el prompt de imagen que se le pasó al usuario).

Reemplaza `generar_esquema_manga.py` (v1, solo una regla para la manga) —
ahora es la remera completa: cuello, manga izq./der. (independientes,
2026-09-24 "vamos con mangas distintas"), torso (cobertura inferior),
calce y 3 pines de "Materiales/Calado" (2026-09-24, a pedido — "la idea
de materiales/textura es para agregar esas cosas ahi pongamos eso de
nuevo"; capas de patrón apiladas, ver ComboCorte.CapaIndexada). Mismo
tamaño de fila que ocupaba Activo/Prenda/Salida + Anclaje/Lado antes
(162 de ancho, y=18 a y=110 en la columna del medio), pintado OPACO para
tapar el bisel de Activo horneado en modelado.png (esa posición ya no se
usa para REMERA, ver ModeladoScreen#dibujarEsquemaRemera).

Los 8 pines entran en el mismo canvas de 92px de alto sin agrandarlo
(no hay lugar debajo: btnFijar ya arranca en y=110 relativo al panel, un
pixel después de donde termina este PNG) — los 3 de Materiales van en
las esquinas/centros que quedaban libres de las 3 filas existentes
(cuello, mangas, torso/calce), no en una fila nueva.
"""
from PIL import Image, ImageDraw

ANCHO, ALTO = 162, 92  # y=18..110 relativo al panel

PANEL = (198, 198, 198, 255)
BORDE_CLARO = (255, 255, 255, 255)
BORDE_OSCURO = (85, 85, 85, 255)
SLOT_FONDO = (139, 139, 139, 255)
SLOT_OSCURO = (55, 55, 55, 255)
SLOT_CLARO = (255, 255, 255, 255)
TELA = (222, 214, 196, 255)
TELA_BORDE = (120, 110, 95, 255)

# Posiciones de los 8 pines, relativas a este PNG (no al panel completo)
# — TIENEN que matchear ModeladoScreenHandler (mMedio+X, Y-18) y
# ModeladoScreen#dibujarEsquemaRemera. Torso/Calce bajaron de local
# y=74->74 (sin cambio) pero en pantalla real ahora son y=92, no 100
# (arreglado un desfasaje de 8px que tenía el Slot real vs este dibujo).
PIN_CUELLO = (72, 20)
PIN_MATERIAL_1 = (8, 20)
PIN_MATERIAL_2 = (136, 20)
PIN_MANGA_IZQ = (8, 42)
PIN_MATERIAL_3 = (72, 42)
PIN_MANGA_DER = (136, 42)
PIN_CALCE = (8, 74)
PIN_TORSO = (72, 74)


def slot(img, sx, sy):
    px = img.load()
    for y in range(18):
        for x in range(18):
            px[sx + x, sy + y] = SLOT_FONDO
    for x in range(18):
        px[sx + x, sy] = SLOT_OSCURO
        px[sx + x, sy + 17] = SLOT_CLARO
    for y in range(18):
        px[sx, sy + y] = SLOT_OSCURO
        px[sx + 17, sy + y] = SLOT_CLARO


def main():
    img = Image.new("RGBA", (ANCHO, ALTO), PANEL)
    px = img.load()
    for y in range(ALTO):
        for x in range(ANCHO):
            px[x, y] = PANEL

    draw = ImageDraw.Draw(img)
    # Silueta simple de remera: torso rectangular + 2 mangas trapezoidales,
    # líneas nomás (sin arte elaborada, es placeholder).
    cx = ANCHO // 2
    torso = [(cx - 26, 30), (cx + 26, 30), (cx + 26, 78), (cx - 26, 78)]
    draw.polygon(torso, fill=TELA, outline=TELA_BORDE)
    manga_izq = [(cx - 26, 32), (cx - 50, 40), (cx - 46, 58), (cx - 26, 52)]
    draw.polygon(manga_izq, fill=TELA, outline=TELA_BORDE)
    manga_der = [(cx + 26, 32), (cx + 50, 40), (cx + 46, 58), (cx + 26, 52)]
    draw.polygon(manga_der, fill=TELA, outline=TELA_BORDE)
    # Cuello: semicírculo chico arriba del torso.
    draw.arc([cx - 10, 22, cx + 10, 38], start=0, end=180, fill=TELA_BORDE, width=2)

    for (px_, py_) in (PIN_CUELLO, PIN_MATERIAL_1, PIN_MATERIAL_2, PIN_MANGA_IZQ,
                        PIN_MATERIAL_3, PIN_MANGA_DER, PIN_CALCE, PIN_TORSO):
        slot(img, px_, py_)

    out = "src/main/resources/assets/modamod/textures/gui/container/esquema_remera.png"
    img.save(out)
    print("guardado:", out)


if __name__ == "__main__":
    main()
