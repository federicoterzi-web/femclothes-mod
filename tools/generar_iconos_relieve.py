"""Íconos 64x64 del relieve (2026-10-01, "hacele texturas y agregaselas"):
Estrógenos y los dos Moldes de textura (fruncido, acolchado), en el estilo de
los sprites del mod ("Taller de sastrería": papel kraft con alfiler de bronce
y dibujo lila para los moldes; frasco de vidrio como la Muestra de color).
Se dibujan a 32x32 y se escalan x2 sin suavizar, como los demás.

Uso: python tools/generar_iconos_relieve.py
"""
from pathlib import Path

from PIL import Image

SALIDA = Path(__file__).resolve().parent.parent / "src/main/resources/assets/modamod/textures/item"

T = (0, 0, 0, 0)
# Papel kraft (mismos tonos que los moldes).
K_BORDE = (104, 72, 38, 255)
K_SOMBRA = (160, 118, 68, 255)
K = (198, 160, 106, 255)
K_LUZ = (222, 190, 136, 255)
# Bronce del alfiler.
B_OSC = (128, 92, 20, 255)
B = (222, 178, 52, 255)
B_LUZ = (252, 230, 140, 255)
# Lila de los dibujos de molde.
L_OSC = (92, 54, 150, 255)
L = (140, 92, 214, 255)
# Tela cruda.
TELA_OSC = (178, 160, 140, 255)
TELA = (236, 224, 204, 255)
TELA_LUZ = (252, 246, 232, 255)
# Vidrio y rosa.
V_BORDE = (112, 146, 172, 255)
V = (214, 230, 238, 255)
V_LUZ = (250, 252, 255, 255)
R_OSC = (178, 72, 118, 255)
R = (236, 132, 178, 255)
R_LUZ = (252, 190, 216, 255)
CORCHO = (176, 108, 58, 255)
CORCHO_OSC = (120, 70, 36, 255)
ETIQ = (244, 232, 206, 255)


class Lienzo:
    def __init__(self):
        self.img = Image.new("RGBA", (32, 32), T)
        self.p = self.img.load()

    def px(self, x, y, c):
        if 0 <= x < 32 and 0 <= y < 32:
            self.p[x, y] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def get(self, x, y):
        return self.p[x, y] if 0 <= x < 32 and 0 <= y < 32 else T

    def contorno(self, c, sobre=None):
        """Borde de 1 px alrededor de todo lo pintado."""
        src = self.img.copy().load()
        for y in range(32):
            for x in range(32):
                if src[x, y][3]:
                    continue
                if any(0 <= x + dx < 32 and 0 <= y + dy < 32 and src[x + dx, y + dy][3]
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    self.px(x, y, c)

    def guardar(self, nombre):
        self.img.resize((64, 64), Image.NEAREST).save(SALIDA / f"{nombre}.png")


def papel(l):
    """El pedazo de papel kraft con el borde irregular y la sombra abajo."""
    l.rect(5, 5, 26, 27, K)
    # Bordes mordidos, como recortado a tijera.
    for x, y in ((5, 5), (26, 5), (5, 27), (26, 27), (12, 5), (19, 27), (5, 14), (26, 19)):
        l.px(x, y, T)
    l.rect(6, 6, 25, 6, K_LUZ)
    l.rect(6, 26, 25, 26, K_SOMBRA)
    l.rect(25, 7, 25, 25, K_SOMBRA)
    l.contorno(K_BORDE)


def alfiler(l, x, y):
    l.px(x, y, B_OSC); l.px(x + 1, y, B); l.px(x, y + 1, B); l.px(x + 1, y + 1, B_LUZ)
    l.px(x - 1, y, B_OSC); l.px(x, y - 1, B_OSC); l.px(x + 1, y - 1, B_OSC); l.px(x + 2, y, B_OSC)
    l.px(x - 1, y + 1, B_OSC); l.px(x + 2, y + 1, B_OSC); l.px(x, y + 2, B_OSC); l.px(x + 1, y + 2, B_OSC)
    # La aguja, en diagonal.
    for k in range(1, 5):
        l.px(x + 2 + k, y + 2 + k, K_BORDE)


def fruncido():
    l = Lienzo()
    papel(l)
    # Retazo de tela con pliegues verticales apretados.
    x0, x1, y0, y1 = 9, 23, 11, 24
    for x in range(x0, x1 + 1):
        tono = (TELA_LUZ, TELA, TELA_OSC)[(x - x0) % 3]
        for y in range(y0, y1 + 1):
            l.px(x, y, tono)
    # El ruedo de abajo ondulado.
    for x in range(x0, x1 + 1):
        if (x - x0) % 3 == 2:
            l.px(x, y1, T if l.get(x, y1 + 1)[3] == 0 else K)
            l.px(x, y1, K)
    # El hilo que frunce, arriba, en lila con puntadas.
    for x in range(x0 - 1, x1 + 2):
        l.px(x, y0, L if x % 2 else L_OSC)
    l.px(x1 + 2, y0 + 1, L); l.px(x1 + 3, y0 + 2, L_OSC)
    # Contorno de la tela.
    for x in range(x0, x1 + 1):
        l.px(x, y0 - 1, K_SOMBRA)
    alfiler(l, 7, 7)
    l.guardar("molde_textura_fruncido")


def acolchado():
    l = Lienzo()
    papel(l)
    x0, x1, y0, y1 = 9, 23, 11, 24
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            # Costuras en rombo cada 5 px; almohadón con luz arriba a la izquierda.
            a, b = (x + y) % 5, (x - y) % 5
            if a == 0 or b == 0:
                c = L_OSC if (x + y) % 2 else L
            else:
                c = TELA_LUZ if a <= 1 and b >= 3 else TELA_OSC if a >= 4 or b <= 1 else TELA
            l.px(x, y, c)
    for x in range(x0, x1 + 1):
        l.px(x, y1 + 1, K_SOMBRA)
    for y in range(y0, y1 + 1):
        l.px(x1 + 1, y, K_SOMBRA)
    alfiler(l, 7, 7)
    l.guardar("molde_textura_acolchado")


def estrogenos():
    l = Lienzo()
    # Frasquito de farmacia: tapa a rosca, cuello corto, cuerpo redondeado.
    l.rect(13, 4, 18, 7, CORCHO)
    l.rect(13, 7, 18, 7, CORCHO_OSC)
    for x in range(13, 19, 2):
        l.rect(x, 4, x, 6, CORCHO_OSC)
    l.rect(12, 8, 19, 9, V)
    l.rect(9, 10, 22, 27, V)
    for x, y in ((9, 10), (22, 10), (9, 27), (22, 27)):
        l.px(x, y, T)
    # Líquido rosa hasta un poco más de la mitad, con brillo.
    l.rect(10, 15, 21, 26, R)
    l.rect(10, 15, 21, 15, R_LUZ)
    l.rect(10, 25, 21, 26, R_OSC)
    for x, y in ((10, 26), (21, 26)):
        l.px(x, y, R_OSC)
    # Etiqueta crema con el símbolo ♀ en rosa oscuro.
    l.rect(11, 17, 20, 24, ETIQ)
    l.rect(11, 24, 20, 24, TELA_OSC)
    sim = ["..XXX..", ".X...X.", ".X...X.", "..XXX..", "...X...", "..XXX..", "...X..."]
    for j, fila in enumerate(sim):
        for i, ch in enumerate(fila):
            if ch == "X":
                l.px(12 + i, 17 + j, R_OSC)
    # Reflejo del vidrio.
    l.rect(10, 11, 10, 14, V_LUZ)
    l.px(11, 11, V_LUZ)
    l.contorno(V_BORDE)
    l.guardar("estrogenos")


def main():
    SALIDA.mkdir(parents=True, exist_ok=True)
    fruncido()
    acolchado()
    estrogenos()
    print("ok", SALIDA)


if __name__ == "__main__":
    main()
