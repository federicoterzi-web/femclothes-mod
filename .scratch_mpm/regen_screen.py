import re
p = 'src/main/java/com/femclothes/modelado/ModeladoScreenHandler.java'
s = open(p, encoding='utf-8').read()
old = "addSlot(new SlotValidado(activoAdaptador(be), 0, mMedio, 86));"
assert old in s
s = s.replace(old, '''addSlot(new SlotValidado(activoAdaptador(be), 0, mMedio, 86) {
            // Sin uso desde que las 4 categorías tienen pines (2026-09-26).
            @Override
            public boolean isEnabled() { return false; }
        });''')
open(p, 'w', encoding='utf-8').write(s)

p = 'src/main/java/com/femclothes/client/ModeladoScreen.java'
s = open(p, encoding='utf-8').read()


def rep(old, new, cnt=1):
    global s
    assert old in s, old[:90]
    s = s.replace(old, new, cnt)


# texturas del esquema por categoría
rep('''    private static final Identifier TEXTURE_ESQUEMA = Identifier.of("femclothes", "textures/gui/container/esquema_remera.png");''',
    '''    private static final Identifier[] TEXTURE_ESQUEMA = {
            Identifier.of("femclothes", "textures/gui/container/esquema_remera.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_pantalon.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_medias.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_calientabrazos.png"),
    };
    /** Cuadros de la chincheta: 0 sin fijar (aguja a la vista), 1 a mitad de clavarse, 2 fijada (sin aguja). */
    private static final Identifier[] TEXTURE_CHINCHETA = {
            Identifier.of("femclothes", "textures/gui/container/chincheta_0.png"),
            Identifier.of("femclothes", "textures/gui/container/chincheta_1.png"),
            Identifier.of("femclothes", "textures/gui/container/chincheta_2.png"),
    };''')

# chinchetas: posición dinámica por categoría
rep('''            int idx = i;
            int[] pos = ModeladoScreenHandler.PIN_POS[i];
            btnPines[i] = new BotonChincheta(this.x + M_MEDIO + pos[0] + 19, this.y + pos[1] + 3,
                    b -> clickBoton(ModeladoBlockEntity.BTN_PIN_BASE + idx));''',
    '''            btnPines[i] = new BotonChincheta(0, 0, b -> { });''')
rep("        btnPines[i] = new BotonChincheta(0, 0, b -> { });", "        btnPines[i] = new BotonChincheta(0, 0, b -> { });") if False else None

# refrescar: botones viejos siempre ocultos, simetría siempre visible, chinchetas por categoría
a = s.index("        boolean esRemera = categoria == ModeladoBlockEntity.Categoria.REMERA;")
b = s.index("        for (int i = 0; i < btnFijadas.length; i++) {\n            boolean hay")
s = s[:a] + '''        // Con pines en las 4 categorías (2026-09-26) Activo/Fijar/Anclaje/Lado
        // quedaron sin uso: el esquema los reemplaza. Simetría aplica a todo
        // pin de lado (mangas, botas, cortes izq/der de medias y cubrebrazos).
        btnFijar.visible = false;
        btnAnclaje.visible = false;
        btnLado.visible = false;
        btnSimetria.visible = true;
        btnSimetria.setMessage(Text.translatable("femclothes.modelado.simetria",
                Text.translatable(be.remeraSimetria() ? "femclothes.si" : "femclothes.no")));

        int cat = categoria.ordinal();
        for (int i = 0; i < btnPines.length; i++) {
            int p = cat * ModeladoBlockEntity.PINES_POR_CATEGORIA + i;
            boolean usable = ModeladoBlockEntity.ROLES[cat][i] != ModeladoBlockEntity.Rol.NINGUNO;
            boolean fijado = be.pinFijado(p);
            boolean conMolde = !be.getStack(ModeladoBlockEntity.PINES_INICIO + p).isEmpty();
            int[] c = ModeladoScreenHandler.PIN_BTN[cat][i];
            btnPines[i].setPosition(this.x + M_MEDIO + c[0] - 6, this.y + c[1] - 6);
            btnPines[i].pin = p;
            btnPines[i].visible = usable;
            btnPines[i].active = !prendida && (conMolde || fijado);
            btnPines[i].actualizar(fijado);
            ComboCorte combo = be.pinCombo(p);
            btnPines[i].setTooltip(Tooltip.of(fijado && combo != null
                    ? Text.translatable("femclothes.modelado.tooltip.chincheta_quitar", descripcion(combo))
                    : Text.translatable("femclothes.modelado.tooltip.chincheta")));
        }

''' + s[b:]

# clase del botón
a = s.index("    private static class BotonChincheta extends ButtonWidget {")
b = s.index("    /**\n     * Botón de la fila de fijadas")
s = s[:a] + '''    private static class BotonChincheta extends ButtonWidget {
        boolean fijado;
        int pin;
        private long cambioMs = -1000;

        BotonChincheta(int x, int y, PressAction onPress) {
            super(x, y, 12, 12, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
        }

        /** Registra el cambio de estado para animar el clavado (cuadro intermedio unos ms). */
        void actualizar(boolean nuevo) {
            if (nuevo != fijado) cambioMs = net.minecraft.util.Util.getMeasuringTimeMs();
            fijado = nuevo;
        }

        @Override
        public void onPress() {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.interactionManager == null || client.currentScreen == null) return;
            if (client.currentScreen instanceof ModeladoScreen pantalla) {
                pantalla.clickBoton(ModeladoBlockEntity.BTN_PIN_BASE + pin);
            }
        }

        @Override
        protected void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            boolean animando = net.minecraft.util.Util.getMeasuringTimeMs() - cambioMs < 160;
            int cuadro = animando ? 1 : fijado ? 2 : 0;
            int x = getX(), y = getY();
            if (isHovered() && active) c.fill(x, y, x + 12, y + 12, 0x33FFFFFF);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            c.setShaderColor(1f, 1f, 1f, active ? 1f : 0.55f);
            c.drawTexture(TEXTURE_CHINCHETA[cuadro], x, y, 0, 0, 12, 12, 12, 12);
            c.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

''' + s[b:]

rep("    private void clickBoton(int id) {", "    void clickBoton(int id) {")

# esquema para todas las categorías
rep('''        if (this.handler.be.categoria() == ModeladoBlockEntity.Categoria.REMERA) {
            dibujarEsquemaRemera(context);
        }''', '''        dibujarEsquema(context);''')
rep("    private void dibujarEsquemaRemera(DrawContext context) {", "    private void dibujarEsquema(DrawContext context) {")
rep("        context.drawTexture(TEXTURE_ESQUEMA, ex, ey, 0, 0, ESQUEMA_ANCHO, ESQUEMA_ALTO, ESQUEMA_ANCHO, ESQUEMA_ALTO);",
    "        context.drawTexture(TEXTURE_ESQUEMA[this.handler.be.categoria().ordinal()], ex, ey, 0, 0,\n                ESQUEMA_ANCHO, ESQUEMA_ALTO, ESQUEMA_ANCHO, ESQUEMA_ALTO);")

# hint ya no se dibuja
rep('''        if (this.handler.be.categoria() != ModeladoBlockEntity.Categoria.REMERA) {
            context.drawText(this.textRenderer, hint(), M_MEDIO, 76, 0x404040, false);
        }
''', '')

# fantasmas por categoría
rep('''        if (be.categoria() != ModeladoBlockEntity.Categoria.REMERA) return;
        for (int i = 0; i < ModeladoBlockEntity.PINES_TAMANO; i++) {
            if (!be.pinFijado(i) || !be.getStack(ModeladoBlockEntity.PINES_INICIO + i).isEmpty()) continue;
            ComboCorte combo = be.pinCombo(i);
            if (combo == null) continue;
            ItemStack icono = iconoDe(combo);
            if (icono.isEmpty()) continue;
            int x = M_MEDIO + ModeladoScreenHandler.PIN_POS[i][0], y = ModeladoScreenHandler.PIN_POS[i][1];''',
    '''        int cat = be.categoria().ordinal();
        for (int i = 0; i < ModeladoBlockEntity.PINES_POR_CATEGORIA; i++) {
            int p = cat * ModeladoBlockEntity.PINES_POR_CATEGORIA + i;
            if (!be.pinFijado(p) || !be.getStack(ModeladoBlockEntity.PINES_INICIO + p).isEmpty()) continue;
            ComboCorte combo = be.pinCombo(p);
            if (combo == null) continue;
            ItemStack icono = iconoDe(combo);
            if (icono.isEmpty()) continue;
            int x = M_MEDIO + ModeladoScreenHandler.PIN_POS[cat][i][0], y = ModeladoScreenHandler.PIN_POS[cat][i][1];''')
open(p, 'w', encoding='utf-8').write(s)
print('ok')
