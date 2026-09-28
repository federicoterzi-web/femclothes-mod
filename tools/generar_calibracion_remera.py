"""Patron de calibracion para la manga de remera -mismo criterio y mismas
coordenadas que generar_calibracion_calientabrazos.py (remera y
calientabrazos comparten el layout de brazo: calientabrazos se derivo
originalmente de cuerpo_normal_larga_redondo.png, ver tools/derivar_banda.py).

Pensado para el bug reportado jugando: "la primera fila de pixeles del
puño de afuera en el brazo derecho y de adentro en el brazo izquierdo
salen pintados como manga" -si las columnas de este patron (rojo/verde/
azul/amarillo) no caen donde deberian en cada brazo, o la "L" blanca sale
espejada en un brazo y no en el otro, ahi esta el problema de mapeo.

Como leerlo en el juego (equipando una remera largo=normal, manga=larga,
cuello=redondo, o mirandola en el visor 3D de la Mesa de Modelado):
- Color = columna (der=rojo, frente=verde, izq=azul, atras=amarillo).
- Brillo = fila (fila 0/hombro brillante -> fila 11/muneca oscura).
- Diagonal negra: si se ve quebrada, las columnas no son contiguas.
- Franja magenta en cada tapa (hombro): referencia de esa pieza.
- L blanca asimetrica en la columna FRENTE: espejada = brazo invertido.

Para probar el recorte de manga en runtime: una vez puesta la remera de
calibracion, fijar en la Mesa de Modelado un valor de manga MAS CORTO
(ej. "Corta", 4 filas) -el corte tiene que dejar visible SOLO las filas
0-3 (las mas brillantes, cerca del hombro) y transparentar el resto, en
LOS DOS brazos por igual.

Genera calibracion/remera_calibracion.png (referencia) Y sobreescribe
TEMPORALMENTE cuerpo_normal_larga_redondo.png -con backup.
"""

from PIL import Image, ImageDraw
import pathlib
import shutil

FILAS_TOTALES = 12
ALTO_FILA = 8

CARAS_BRAZO_DER = (320, 160, 448, 256)
CARAS_BRAZO_IZQ = (256, 416, 384, 512)
TAPAS_BRAZO_DER = (352, 128, 416, 160)
TAPAS_BRAZO_IZQ = (288, 384, 352, 416)

COL_COLORS = [
    (220, 40, 40),    # 0 der - rojo
    (40, 180, 40),    # 1 frente - verde
    (40, 40, 220),    # 2 izq - azul
    (200, 180, 30),   # 3 atras - amarillo
]
MAGENTA = (230, 30, 230, 255)


def pintar_caras(img, rect):
    x0, y0, x1, y1 = rect
    ancho_total = x1 - x0
    alto_total = y1 - y0
    col_w = ancho_total // 4
    draw = ImageDraw.Draw(img)
    for fila in range(FILAS_TOTALES):
        brillo = 1.0 - (fila / (FILAS_TOTALES - 1)) * 0.6
        fy0 = y0 + fila * ALTO_FILA
        fy1 = fy0 + ALTO_FILA
        for col in range(4):
            r, g, b = COL_COLORS[col]
            color = (int(r * brillo), int(g * brillo), int(b * brillo), 255)
            fx0 = x0 + col * col_w
            fx1 = fx0 + col_w
            draw.rectangle([fx0, fy0, fx1 - 1, fy1 - 1], fill=color)
    draw.line([(x0, y0), (x1 - 1, y1 - 1)], fill=(0, 0, 0, 255), width=2)
    fx0 = x0 + col_w
    fx1 = fx0 + col_w
    ly0 = y0 + alto_total // 4
    ly1 = y0 + alto_total - alto_total // 6
    grosor = max(2, col_w // 6)
    draw.rectangle([fx0 + col_w // 4, ly0, fx0 + col_w // 4 + grosor, ly1], fill=(255, 255, 255, 255))
    draw.rectangle([fx0 + col_w // 4, ly1 - grosor, fx1 - col_w // 4, ly1], fill=(255, 255, 255, 255))


def pintar_tapas(img, rect):
    draw = ImageDraw.Draw(img)
    draw.rectangle(rect, fill=MAGENTA)


def main():
    base_dir = pathlib.Path(__file__).resolve().parent.parent
    out_dir = base_dir / "src/main/resources/assets/femclothes/textures/entity"
    calib_dir = base_dir / "calibracion"
    calib_dir.mkdir(exist_ok=True)

    img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    pintar_caras(img, CARAS_BRAZO_DER)
    pintar_caras(img, CARAS_BRAZO_IZQ)
    pintar_tapas(img, TAPAS_BRAZO_DER)
    pintar_tapas(img, TAPAS_BRAZO_IZQ)

    img.save(calib_dir / "remera_calibracion.png")
    print(f"escrito {calib_dir / 'remera_calibracion.png'}")

    target = out_dir / "cuerpo_normal_larga_redondo.png"
    backup = out_dir / "cuerpo_normal_larga_redondo.png.bak"
    if not backup.exists():
        shutil.copy(target, backup)
        print(f"backup guardado en {backup}")
    img.save(target)
    print(f"TEMPORAL: {target} sobreescrito con el patron de calibracion")
    print("Restaurar despues con: cp cuerpo_normal_larga_redondo.png.bak cuerpo_normal_larga_redondo.png")


if __name__ == "__main__":
    main()
