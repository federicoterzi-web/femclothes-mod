"""Deriva una textura de prenda recortando una banda de filas de un asset
YA VALIDADO, en vez de generar arte de cero.

Es la 4ta vez que se hace este tipo de derivacion en este mod (medias,
pantalon inicial, manga de remera "siete_octavos", y ahora calientabrazos) -
las primeras tres veces el script se escribio ad-hoc y se tiro, ver
docs/CANAL.md v9-v11. Esta vez se guarda, parametrizado por direccion, para
cubrir las dos que ya existen en el mod:

- "muneca" (remera): la manga completa cubre las 12 filas desde el hombro
  hacia abajo; un corte mas corto trunca desde la MUNECA (fila 11, la que
  queda mas lejos del hombro) y repinta el nuevo borde visible con el
  dobladillo.
- "hombro" (calientabrazos): la cobertura llena desde la MUNECA hacia
  arriba; un corte mas corto trunca desde el HOMBRO (fila 0) y repinta el
  nuevo borde visible con el dobladillo.

Las filas se cuentan siempre 0 = arriba de la textura (hombro), 11 = abajo
(muneca) - es la convencion real del PNG, verificada por lectura de pixeles
contra cuerpo_normal_larga_redondo.png antes de escribir este script, NO la
convencion (invertida) del borrador viejo en docs/PRENDAS.md.

IMPORTANTE: el resultado parte de un LIENZO TRANSPARENTE, no de una copia de
la fuente completa -la fuente (cuerpo_*.png) tiene tela en el torso ademas
de la manga, y el contrato de Pieza pide transparente donde la prenda no
tiene tela (ver PiezasDelMod.java). Solo se copian los rects pedidos.

IMPORTANTE (encontrado jugando, "los brazos no estan espejados, es copy
paste sin flip horizontal"): BRAZO_IZQ y BRAZO_DER son el MISMO brazo
espejado en el modelo real (uno pivota en +X, el otro en -X), pero el
unwrap UV de un cuboide usa SIEMPRE el mismo orden de columnas
(der/frente/izq/atras) sin importar de que lado del cuerpo este -es la
misma trampa que ya documenta ClothingTextureCache.faceFactor para el
sombreado de piernas ("un cambio simetrico sale espejado en un solo
brazo"). Copiar el mismo recorte crudo a los dos no alcanza: hay que
espejarlo horizontalmente para uno de los dos (BRAZO_IZQ, dejando
BRAZO_DER como referencia) antes de pegarlo.

Requiere Pillow (`pip install pillow`), ya confirmado disponible.
"""

from PIL import Image, ImageOps

FILAS_TOTALES = 12
ALTO_FILA = 8  # px, a ESCALA_TELA (ver CuerpoGeometria.java)

# Rects en pixeles reales (skin-unit * 8), calculados desde
# LayoutSkin.base(Parte.BRAZO_*, false).escalada(8) - no adivinados:
# BRAZO_DER = CajaSkin(40,16,4,12,4), BRAZO_IZQ = CajaSkin(32,48,4,12,4).
# El segundo valor de cada tupla es si hay que espejar horizontalmente ese
# recorte antes de pegarlo -BRAZO_IZQ es el espejo de BRAZO_DER en el
# modelo real, el UV crudo no lo es.
CARAS_BRAZO_DER = (320, 160, 448, 256)   # .caras(): las 4 caras laterales
CARAS_BRAZO_IZQ = (256, 416, 384, 512)
TAPAS_BRAZO_DER = (352, 128, 416, 160)   # .tapas(): arriba+abajo
TAPAS_BRAZO_IZQ = (288, 384, 352, 416)

CARAS_RECTS = [(CARAS_BRAZO_DER, False), (CARAS_BRAZO_IZQ, True)]
# TAPAS NO se espeja -encontrado jugando (captura desde arriba, "una tapa
# sale naranja"): tapas() son "arriba" (mitad izquierda del rect, opaca) +
# "abajo" (mitad derecha, transparente) lado a lado, NO un par exterior/
# interior. Espejar horizontalmente les da vuelta el orden: la mitad
# opaca ("arriba", la cara real de arriba del cubo) termina en la posicion
# de "abajo", dejando la cara de arriba transparente -se ve la piel.
TAPAS_RECTS = [(TAPAS_BRAZO_DER, False), (TAPAS_BRAZO_IZQ, False)]

HEM_REMERA = (188, 188, 198, 255)  # dobladillo real de la manga de remera


def derivar_banda(src, filas_visibles, truncar_desde, hem_rgba,
                   caras_rects, tapas_rects,
                   filas_totales=FILAS_TOTALES, alto_fila=ALTO_FILA):
    """Lienzo transparente del mismo tamano que `src`, con SOLO los rects
    pedidos copiados -recortados a `filas_visibles` de las `filas_totales`,
    espejados horizontalmente donde corresponda (ver nota del modulo).

    Cada rect es (x0,y0,x1,y1,espejar). truncar_desde='hombro' mantiene las
    filas de ABAJO (muneca); 'muneca' es el espejo -mantiene las de ARRIBA.
    """
    dst = Image.new("RGBA", src.size, (0, 0, 0, 0))

    for (rect, espejar) in tapas_rects:
        x0, y0, x1, y1 = rect
        recorte = src.crop(rect)
        if espejar:
            recorte = ImageOps.mirror(recorte)
        dst.paste(recorte, (x0, y0))
        # La mitad "abajo" (la cara de abajo del cubo, hacia la muneca)
        # queda TRANSPARENTE a proposito -es el agujero del puno por donde
        # sale la mano, tiene que verse la piel del cuerpo base, no tela.
        # (Se probo rellenarla de gris y quedaba mal: un puno de tela solida
        # ahi no es lo que es un puno real.) La fuente ya la trae
        # transparente, no hace falta tocar nada mas aca.

    filas_a_blanquear = filas_totales - filas_visibles
    for (rect, espejar) in caras_rects:
        x0, y0, x1, y1 = rect
        ancho = x1 - x0
        dobladillo = Image.new("RGBA", (ancho, alto_fila), hem_rgba)
        if truncar_desde == "hombro":
            y_keep0 = y0 + filas_a_blanquear * alto_fila
            if y_keep0 < y1:
                recorte = src.crop((x0, y_keep0, x1, y1))
                if espejar:
                    recorte = ImageOps.mirror(recorte)
                dst.paste(recorte, (x0, y_keep0))
            if filas_a_blanquear > 0:
                dst.paste(dobladillo, (x0, y_keep0, x1, y_keep0 + alto_fila))
        elif truncar_desde == "muneca":
            y_keep1 = y0 + filas_visibles * alto_fila
            if y_keep1 > y0:
                recorte = src.crop((x0, y0, x1, y_keep1))
                if espejar:
                    recorte = ImageOps.mirror(recorte)
                dst.paste(recorte, (x0, y0))
            if filas_a_blanquear > 0:
                dst.paste(dobladillo, (x0, y_keep1 - alto_fila, x1, y_keep1))
        else:
            raise ValueError(truncar_desde)
    return _extender_bordes(dst)


def _extender_bordes(img, profundidad=8):
    """Copia el RGB de cada pixel opaco a sus vecinos transparentes, a
    varios pixeles de profundidad (una pasada de dilatacion por nivel).

    Encontrado jugando -"linea de un pixel en el puño"-: los pixeles
    transparentes quedaban con RGB negro puro (0,0,0,0). El filtrado de
    textura de Minecraft mezcla un poco de ese negro con el pixel opaco
    vecino en el borde, y se ve como una linea oscura fina justo en el
    corte. El alfa no cambia -sigue sin dibujar tela ahi-, solo el color
    de fondo para que la mezcla en el borde no tire a negro.

    `profundidad=8` -no 1- porque un solo pixel de sangrado alcanza para
    el filtrado bilineal comun, pero si Minecraft genera mipmaps para esta
    textura, un nivel de mip promedia un area mas grande (2x2, 4x4...) y
    1 pixel no alcanza a taparla. 8 cubre hasta el mip nivel 3 (8x8) sin
    costar nada -esto corre una vez al generar el asset, no en el juego.
    """
    ancho, alto = img.size
    vecinos = ((1, 0), (-1, 0), (0, 1), (0, -1))
    # Mascara aparte de "ya tiene color de sangrado" -el alfa se queda en 0
    # a proposito, asi que no sirve para saber si un pixel ya se coloreo en
    # una pasada anterior. Sin esto, un pixel recien sangrado nunca cuenta
    # como fuente para la pasada siguiente y la profundidad queda pegada
    # en 1 pixel sin importar cuantas iteraciones se pidan.
    px = img.load()
    tiene_color = [[px[x, y][3] != 0 for x in range(ancho)] for y in range(alto)]
    for _ in range(profundidad):
        nuevos = []
        for y in range(alto):
            for x in range(ancho):
                if tiene_color[y][x]:
                    continue
                for dx, dy in vecinos:
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < ancho and 0 <= ny < alto and tiene_color[ny][nx]:
                        nr, ng, nb, _ = px[nx, ny]
                        nuevos.append((x, y, nr, ng, nb))
                        break
        if not nuevos:
            break
        for x, y, r, g, b in nuevos:
            px[x, y] = (r, g, b, 0)
            tiene_color[y][x] = True
    return img


if __name__ == "__main__":
    import pathlib

    base_dir = pathlib.Path(__file__).resolve().parent.parent
    src_path = base_dir / "src/main/resources/assets/modamod/textures/entity/cuerpo_normal_larga_redondo.png"
    out_dir = base_dir / "src/main/resources/assets/modamod/textures/models/armor"

    src = Image.open(src_path).convert("RGBA")
    assert src.size == (512, 512), src.size

    # clave -> filas de cobertura contadas desde la muneca hacia arriba
    # (mismos valores que Variante.Manga, ver CalientabrazosItem).
    variantes = {
        "larga": 12,
        "siete_octavos": 10,
        "tres_cuartos": 8,
        "corta": 4,
    }

    for clave, filas in variantes.items():
        img = derivar_banda(src, filas, "hombro", HEM_REMERA,
                             CARAS_RECTS, TAPAS_RECTS)
        out_path = out_dir / f"calientabrazos_{clave}_layer_1.png"
        img.save(out_path)
        print(f"escrito {out_path} (filas={filas})")
