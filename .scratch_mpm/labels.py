import json

ES = {
    'cuello': 'Cuello', 'mat1': 'Personalización 1', 'mat2': 'Personalización 2', 'mat3': 'Personalización 3',
    'manga_izq': 'Manga Izq.', 'manga_der': 'Manga Der.', 'calce': 'Calce', 'torso': 'Corte Inferior',
    'tiro': 'Tiro', 'bota_izq': 'Corte Bota Izq.', 'bota_der': 'Corte Bota Der.',
    'sup_izq': 'Corte Superior Izq.', 'sup_der': 'Corte Superior Der.',
    'inf_izq': 'Corte Inferior Izq.', 'inf_der': 'Corte Inferior Der.',
    'pers_izq1': 'Personalización Izq. 1', 'pers_izq2': 'Personalización Izq. 2', 'pers_izq3': 'Personalización Izq. 3',
    'pers_der1': 'Personalización Der. 1', 'pers_der2': 'Personalización Der. 2', 'pers_der3': 'Personalización Der. 3',
}
EN = {
    'cuello': 'Neckline', 'mat1': 'Customization 1', 'mat2': 'Customization 2', 'mat3': 'Customization 3',
    'manga_izq': 'Left Sleeve', 'manga_der': 'Right Sleeve', 'calce': 'Fit', 'torso': 'Bottom Cut',
    'tiro': 'Rise', 'bota_izq': 'Left Hem Cut', 'bota_der': 'Right Hem Cut',
    'sup_izq': 'Top Cut Left', 'sup_der': 'Top Cut Right',
    'inf_izq': 'Bottom Cut Left', 'inf_der': 'Bottom Cut Right',
    'pers_izq1': 'Left Customization 1', 'pers_izq2': 'Left Customization 2', 'pers_izq3': 'Left Customization 3',
    'pers_der1': 'Right Customization 1', 'pers_der2': 'Right Customization 2', 'pers_der3': 'Right Customization 3',
}
for lang, d in (('es_ar', ES), ('en_us', EN)):
    p = 'src/main/resources/assets/femclothes/lang/%s.json' % lang
    s = open(p, encoding='utf-8').read()
    anchor = '  "femclothes.modelado.almacen"'
    add = ''.join('  "femclothes.modelado.rol.%s": "%s",\n' % (k, v) for k, v in d.items())
    assert anchor in s
    s = s.replace(anchor, add + anchor, 1)
    open(p, 'w', encoding='utf-8').write(s)
    json.loads(s)

p = 'src/main/java/com/femclothes/client/ModeladoScreen.java'
s = open(p, encoding='utf-8').read()
old = "        dibujarFantasmasDePines(context);\n    }"
assert old in s
s = s.replace(old, '''        dibujarFantasmasDePines(context);
        dibujarRotulosDePines(context);
    }

    /**
     * Rótulo de cada pin, dibujado por el juego (2026-09-26, "los textos
     * serían mejor generados in game por el tema de las traducciones"): los
     * PNG del esquema ya no traen letras. Centrado arriba de cada slot, a
     * escala chica para que entre entre pines vecinos.
     */
    private void dibujarRotulosDePines(DrawContext context) {
        int cat = this.handler.be.categoria().ordinal();
        float escala = 0.62f;
        for (int i = 0; i < ModeladoBlockEntity.PINES_POR_CATEGORIA; i++) {
            ModeladoBlockEntity.Rol rol = ModeladoBlockEntity.ROLES[cat][i];
            if (rol == ModeladoBlockEntity.Rol.NINGUNO) continue;
            Text t = Text.translatable("femclothes.modelado.rol." + rol.name().toLowerCase(java.util.Locale.ROOT));
            int[] pos = ModeladoScreenHandler.PIN_POS[cat][i];
            float cx = M_MEDIO + pos[0] + 8, y = pos[1] - 7;
            int ancho = this.textRenderer.getWidth(t);
            context.getMatrices().push();
            context.getMatrices().translate(cx - ancho * escala / 2f, y, 0);
            context.getMatrices().scale(escala, escala, 1f);
            context.drawText(this.textRenderer, t, 0, 0, 0x3B2410, false);
            context.getMatrices().pop();
        }
    }''', 1)
open(p, 'w', encoding='utf-8').write(s)
print('ok')
