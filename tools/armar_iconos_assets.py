"""Arma los íconos de molde de 64x64 con los sprites unificados del usuario (2026-10-06, "escucha ahi tengo assets para armar
los items... esto me parece lo mejor para unificar" -> "empecemos solo por la tabla de faldas pantalones torsos y capas").

Cada ícono = el panel de pergamino plegado de fondo (`papel.png`) + el sprite centrado encima. Los sprites vienen en tiras
(`faldas.png`, `pantalones.png`, `torsos.png`, `capas.png`) de assets_nuevos_prueba/assets_2026-10-06/; se cortan por columnas
vacías (donde dos se tocan, por el valle de menos alfa). Los íconos que reemplaza se copian antes a
assets_viejos/iconos_assets_2026-10-06/.
"""
import os, shutil
import numpy as np
from PIL import Image

RAIZ = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
FUENTE = os.path.join(RAIZ, "assets_nuevos_prueba", "assets_2026-10-06")
ITEMS = os.path.join(RAIZ, "src", "main", "resources", "assets", "modamod", "textures", "item")
VIEJOS = os.path.join(RAIZ, "assets_viejos", "iconos_assets_2026-10-06")

# tira -> nombres de ítem en el orden de la imagen
TIRAS = {
    "faldas.png": ["molde_pollera_campana", "molde_pollera_circular", "molde_pollera_tubo", "molde_pollera_tableada", "molde_pollera_globo"],
    "pantalones.png": ["molde_calce_pegado", "molde_calce_ajustado", "molde_calce_normal", "molde_calce_suelto", "molde_calce_oversize"],
    "torsos.png": ["molde_cuello_redondo", "molde_cuello_v", "molde_cuello_polera", "molde_cuello_cuadrado", "molde_cuello_corazon"],
    "volantes.png": [None, "molde_volado_recto", "molde_volado_circular"],   # el 1.º (falda lisa con puntada) todavía no tiene dónde ir
    "capas.png": ["molde_capa_ruedo_redondeado", "molde_capa_ruedo_recto", "molde_capa_cuello_alto", "molde_capa_ruedo_cola"],
}
TAM = 64
CAJA = 54          # lado máximo del sprite MÁS GRANDE de cada tira dentro del papel (margen para el marco); los demás de la tira escalan igual


def cortar(tira, n):
    """Las n columnas-rango de la tira; si hay menos rangos que sprites, parte el más ancho por su valle de alfa."""
    alfa = np.array(tira)[..., 3] > 20
    col = alfa.any(axis=0)
    rangos, s = [], None
    for x, v in enumerate(col):
        if v and s is None: s = x
        if not v and s is not None: rangos.append([s, x]); s = None
    if s is not None: rangos.append([s, len(col)])
    cuenta = alfa.sum(axis=0)
    while len(rangos) < n:
        i = max(range(len(rangos)), key=lambda k: rangos[k][1] - rangos[k][0])
        a, b = rangos[i]
        ancho = b - a
        # el valle en el tramo del medio, con tantos sprites como falten partir en ese rango
        desde, hasta = a + ancho // 5, b - ancho // 5
        corte = desde + int(np.argmin(cuenta[desde:hasta]))
        rangos[i:i + 1] = [[a, corte], [corte, b]]
    return [tuple(r) for r in rangos]


def sprite(tira, x0, x1):
    recorte = tira.crop((x0, 0, x1, tira.height))
    caja = recorte.getbbox()
    return recorte.crop(caja)


def componer(papel, spr, k):
    fondo = papel.resize((TAM, TAM), Image.LANCZOS).convert("RGBA")
    w, h = spr.size
    spr = spr.resize((max(1, round(w * k)), max(1, round(h * k))), Image.LANCZOS)
    fondo.alpha_composite(spr, ((TAM - spr.width) // 2, (TAM - spr.height) // 2 + 1))
    return fondo


def guardar(nombre, img):
    destino = os.path.join(ITEMS, nombre + ".png")
    if os.path.exists(destino) and not os.path.exists(os.path.join(VIEJOS, nombre + ".png")):
        shutil.copy(destino, os.path.join(VIEJOS, nombre + ".png"))
    img.save(destino)
    print("ok", nombre)


# motivos.png: grilla de 10 x 3 íconos; (fila, columna) de cada uno que se usa
MOTIVOS = {
    "molde_aplique_mono": (0, 9),       # moño rojo (el verde está en (2, 1))
    "molde_aplique_mariposa": (2, 0),   # mariposa violeta
    "molde_aplique_flor": (0, 3),       # flor rosa (la blanca está en (2, 2))
}
# los patrones llevan el motivo adentro del aro de bordado
PATRONES = {
    "pattern_corazones": (0, 1),
    "pattern_estrellas": (1, 9),
}
CAJA_MOTIVO = 40     # lado máximo del motivo sobre el papel
CAJA_ARO = 30        # lado máximo del motivo adentro del aro


def motivo(grilla, fila, col):
    celda_w, celda_h = grilla.width / 10, grilla.height / 3
    recorte = grilla.crop((round(col * celda_w), round(fila * celda_h), round((col + 1) * celda_w), round((fila + 1) * celda_h)))
    return recorte.crop(recorte.getbbox())


def en_caja(spr, caja):
    k = min(caja / spr.width, caja / spr.height)
    return spr.resize((max(1, round(spr.width * k)), max(1, round(spr.height * k))), Image.LANCZOS)


def main():
    papel = Image.open(os.path.join(FUENTE, "papel.png")).convert("RGBA")
    os.makedirs(VIEJOS, exist_ok=True)
    for archivo, nombres in TIRAS.items():
        tira = Image.open(os.path.join(FUENTE, archivo)).convert("RGBA")
        rangos = cortar(tira, len(nombres))
        assert len(rangos) >= len(nombres), (archivo, rangos)   # torsos.png trae un 6.º (capucha) que todavía no se usa
        sprites = [sprite(tira, x0, x1) for (x0, x1) in rangos[:len(nombres)]]
        k = min(CAJA / max(sp.width for sp in sprites), CAJA / max(sp.height for sp in sprites))   # misma escala en toda la tira
        for nombre, spr in zip(nombres, sprites):
            if nombre:
                guardar(nombre, componer(papel, spr, k))

    # Apliques: el motivo sobre el pergamino
    grilla = Image.open(os.path.join(FUENTE, "motivos.png")).convert("RGBA")
    for nombre, (fila, col) in MOTIVOS.items():
        spr = en_caja(motivo(grilla, fila, col), CAJA_MOTIVO)
        fondo = papel.resize((TAM, TAM), Image.LANCZOS).convert("RGBA")
        fondo.alpha_composite(spr, ((TAM - spr.width) // 2, (TAM - spr.height) // 2))
        guardar(nombre, fondo)

    # Patrones: el aro de bordado (el "bastidor" de tu sistema de diseño) con el motivo adentro
    aro = Image.open(os.path.join(FUENTE, "aro.png")).convert("RGBA")
    base = aro.crop(aro.getbbox()).resize((TAM - 6, TAM - 6), Image.LANCZOS)
    for nombre, (fila, col) in PATRONES.items():
        fondo = Image.new("RGBA", (TAM, TAM), (0, 0, 0, 0))
        fondo.alpha_composite(base, (3, 3))
        spr = en_caja(motivo(grilla, fila, col), CAJA_ARO)
        fondo.alpha_composite(spr, ((TAM - spr.width) // 2, (TAM - spr.height) // 2 + 3))
        guardar(nombre, fondo)


if __name__ == "__main__":
    main()
