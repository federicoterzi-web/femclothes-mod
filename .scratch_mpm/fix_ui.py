p = 'src/main/java/com/femclothes/client/ModeladoScreen.java'
s = open(p, encoding='utf-8').read()


def rep(old, new, cnt=1):
    global s
    assert old in s, old[:80]
    s = s.replace(old, new, cnt)


# 1) z-order: los ítems se dibujan en z=150, así que lo que va encima tiene que subir de z
rep("""            context.drawItem(icono, x, y);
            context.fill(x, y, x + 16, y + 16, 0xD2D9B98A);""",
    """            context.drawItem(icono, x, y);
            // drawItem dibuja en z=150: el velo tiene que ir más arriba o queda debajo del ícono
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 200);
            context.fill(x, y, x + 16, y + 16, 0xD2D9B98A);
            context.getMatrices().pop();""")
rep("""            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            c.setShaderColor(1f, 1f, 1f, active ? 1f : 0.55f);
            c.drawTexture(TEXTURE_CHINCHETA[cuadro], x, y, 0, 0, 12, 12, 12, 12);
            c.setShaderColor(1f, 1f, 1f, 1f);""",
    """            // Por encima de ítems (z=150) y del velo fantasma (z=200): antes
            // "a veces aparecía debajo, a veces arriba" según hubiera un ícono.
            c.getMatrices().push();
            c.getMatrices().translate(0, 0, 400);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            c.setShaderColor(1f, 1f, 1f, active ? 1f : 0.55f);
            c.drawTexture(TEXTURE_CHINCHETA[cuadro], x + 2, y + 2, 0, 0, 12, 12, 12, 12);
            c.setShaderColor(1f, 1f, 1f, 1f);
            c.getMatrices().pop();""")
# hit box 16x16, centrado en el mismo punto
rep("super(x, y, 12, 12, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);", "super(x, y, 16, 16, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);")
rep("if (isHovered() && active) c.fill(x, y, x + 12, y + 12, 0x33FFFFFF);", "if (isHovered() && active) c.fill(x + 2, y + 2, x + 14, y + 14, 0x33FFFFFF);")
rep("btnPines[i].setPosition(this.x + M_MEDIO + c[0] - 6, this.y + c[1] - 6);", "btnPines[i].setPosition(this.x + M_MEDIO + c[0] - 8, this.y + c[1] - 8);")

# 2) rótulos: títulos de grupo para las columnas de personalización, alineados a la derecha del centro
a = s.index("    private void dibujarRotulosDePines(DrawContext context) {")
b = s.index("    /**\n     * Ícono fantasma") if "    /**\n     * Ícono fantasma" in s else None
end = s.index("\n    }\n", a) + len("\n    }\n")
s = s[:a] + '''    private void dibujarRotulosDePines(DrawContext context) {
        int cat = this.handler.be.categoria().ordinal();
        float escala = 0.62f;
        for (int i = 0; i < ModeladoBlockEntity.PINES_POR_CATEGORIA; i++) {
            ModeladoBlockEntity.Rol rol = ModeladoBlockEntity.ROLES[cat][i];
            String clave = switch (rol) {
                case NINGUNO, PERS_IZQ2, PERS_IZQ3, PERS_DER2, PERS_DER3 -> null;
                // las columnas de 3 llevan UN título arriba, no uno por slot (los slots están casi pegados)
                case PERS_IZQ1 -> "pers_izq";
                case PERS_DER1 -> "pers_der";
                default -> rol.name().toLowerCase(java.util.Locale.ROOT);
            };
            if (clave == null) continue;
            Text t = Text.translatable("femclothes.modelado.rol." + clave);
            int[] pos = ModeladoScreenHandler.PIN_POS[cat][i];
            float ancho = this.textRenderer.getWidth(t) * escala;
            // El borde derecho del texto queda en el centro del slot: la chincheta ocupa la esquina superior derecha.
            float x0 = Math.max(M_MEDIO + 9, Math.min(M_MEDIO + pos[0] + 10 - ancho, M_MEDIO + 231 - ancho));
            context.getMatrices().push();
            context.getMatrices().translate(x0, pos[1] - 7, 0);
            context.getMatrices().scale(escala, escala, 1f);
            context.drawText(this.textRenderer, t, 0, 0, 0x3B2410, false);
            context.getMatrices().pop();
        }
    }
''' + s[end:]

# 3) 12 fijadas visibles
rep("private final BotonFijada[] btnFijadas = new BotonFijada[8];", "private final BotonFijada[] btnFijadas = new BotonFijada[12];")
open(p, 'w', encoding='utf-8').write(s)

# lang: títulos de grupo
for lang, izq, der in (('es_ar', 'Personalización Izq.', 'Personalización Der.'), ('en_us', 'Left Customization', 'Right Customization')):
    q = 'src/main/resources/assets/femclothes/lang/%s.json' % lang
    t = open(q, encoding='utf-8').read()
    anchor = '  "femclothes.modelado.almacen"'
    t = t.replace(anchor, '  "femclothes.modelado.rol.pers_izq": "%s",\n  "femclothes.modelado.rol.pers_der": "%s",\n' % (izq, der) + anchor, 1)
    open(q, 'w', encoding='utf-8').write(t)

# 4) tope de fijadas por categoría: 12 (había 8 y con 11 pines los últimos no se aplicaban en silencio)
p = 'src/main/java/com/femclothes/modelado/ModeladoBlockEntity.java'
s = open(p, encoding='utf-8').read()
assert s.count("lista.size() < 8") >= 1 and s.count("lista.size() >= 8") >= 1
s = s.replace("lista.size() < 8", "lista.size() < 12").replace("lista.size() >= 8", "lista.size() >= 12").replace("llena (8)", "llena (12)")
open(p, 'w', encoding='utf-8').write(s)
print('ok')
