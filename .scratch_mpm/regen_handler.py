p = 'src/main/java/com/femclothes/modelado/ModeladoScreenHandler.java'
s = open(p, encoding='utf-8').read()

a = s.index("    /**\n     * Origen (x relativo a la columna del medio")
b = s.index("    public final ModeladoBlockEntity be;")
s = s[:a] + '''    /**
     * Origen (x relativo a la columna del medio, y absoluto) del ítem de
     * cada pin, por categoría (orden {@code Categoria.ordinal()}) y por pin
     * (orden de {@code ModeladoBlockEntity#ROLES}). y = 36 (donde arranca
     * el esquema, ver ModeladoScreen#ESQUEMA_Y) + el y local medido sobre
     * el PNG de esa categoría; x/y ya son el origen del ítem de 16x16,
     * centrado en cada slot dibujado. Público: ModeladoScreen lo usa.
     */
    public static final int[][][] PIN_POS = {
            // REMERA
            {{112, 48}, {58, 50}, {166, 50}, {42, 82}, {112, 88}, {182, 82}, {57, 125}, {112, 144}},
            // PANTALON
            {{112, 46}, {112, 79}, {112, 100}, {112, 122}, {47, 95}, {49, 143}, {175, 144}, {0, 0}},
            // MEDIAS
            {{44, 52}, {180, 52}, {112, 69}, {112, 94}, {112, 119}, {40, 104}, {43, 142}, {184, 142}},
            // CALIENTABRAZOS
            {{38, 55}, {112, 55}, {185, 55}, {111, 84}, {112, 112}, {37, 134}, {187, 135}, {112, 144}},
    };

    /**
     * Centro de la chincheta de cada pin (mismo esquema que {@link #PIN_POS}):
     * en el arte de las 3 prendas nuevas ahí estaba horneada la chincheta
     * (se borró del PNG, ver tools/procesar_assets_esquemas.py); en remera
     * (arte sin chinchetas) es la esquina superior derecha del slot.
     */
    public static final int[][][] PIN_BTN = {
            {{128, 49}, {74, 51}, {182, 51}, {58, 83}, {128, 89}, {198, 83}, {73, 126}, {128, 145}},
            {{126, 48}, {126, 82}, {126, 102}, {126, 124}, {61, 97}, {63, 146}, {189, 146}, {0, 0}},
            {{58, 54}, {195, 54}, {127, 71}, {127, 96}, {127, 121}, {54, 106}, {57, 144}, {198, 144}},
            {{53, 55}, {127, 56}, {200, 55}, {127, 84}, {127, 113}, {52, 135}, {202, 135}, {127, 144}},
    };

''' + s[b:]

# pines: 32 slots
a = s.index("        for (int i = 0; i < ModeladoBlockEntity.PINES_TAMANO; i++) {\n            addSlot(new PinSlot(")
b = s.index("        addSlot(new Slot(be, ModeladoBlockEntity.PRENDA")
s = s[:a] + '''        for (int p = 0; p < ModeladoBlockEntity.PINES_TAMANO; p++) {
            int cat = p / ModeladoBlockEntity.PINES_POR_CATEGORIA, i = p % ModeladoBlockEntity.PINES_POR_CATEGORIA;
            addSlot(new PinSlot(be, ModeladoBlockEntity.PINES_INICIO + p, mMedio + PIN_POS[cat][i][0], PIN_POS[cat][i][1]));
        }

''' + s[b:]
s = s.replace("private static final int CANTIDAD_PINES = 8;", "private static final int CANTIDAD_PINES = ModeladoBlockEntity.PINES_TAMANO;")

s = s.replace('''        @Override
        public boolean isEnabled() {
            return be.categoria() == ModeladoBlockEntity.Categoria.REMERA;
        }''', '''        @Override
        public boolean isEnabled() {
            int p = this.getIndex() - ModeladoBlockEntity.PINES_INICIO;
            int cat = p / ModeladoBlockEntity.PINES_POR_CATEGORIA;
            return be.categoria().ordinal() == cat
                    && ModeladoBlockEntity.ROLES[cat][p % ModeladoBlockEntity.PINES_POR_CATEGORIA]
                    != ModeladoBlockEntity.Rol.NINGUNO;
        }''')
open(p, 'w', encoding='utf-8').write(s)
print('ok')
