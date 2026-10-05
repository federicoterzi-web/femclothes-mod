package com.femclothes.client;

import com.femclothes.aplique.Aplique;
import com.femclothes.estilado.EstiladoBlockEntity;
import com.femclothes.estilado.EstiladoScreenHandler;
import com.femclothes.estilado.PonerApliquePayload;
import com.femclothes.garment.Parte;
import com.femclothes.render.ApliqueRenderer;
import com.femclothes.render.GarmentFeatureRenderer;
import com.femclothes.render.Pieza;
import com.femclothes.render.PiezasDePrenda;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mesa de estilado (2026-10-01, "agregarles modelos 3d anclados en la
 * geometria de la prenda" + "libre con click en la vista 3D"): a la izquierda
 * el jugador con la prenda del slot (y sus apliques), a la derecha los slots,
 * la lista de apliques puestos y los botones de giro/tamaño/quitar.
 *
 * <h2>El click</h2>
 * Mientras se dibuja la vista previa, {@code GarmentFeatureRenderer} guarda por
 * parte la matriz espacio-de-la-parte → pantalla ({@code capturaPoses}). Para
 * el click se invierte esa matriz, el mouse pasa a ser un rayo en el espacio de
 * la parte (la GUI es ortogonal: el rayo va en z) y se corta contra la caja de
 * tela de cada pieza de la prenda (inflada por su calce, solo las filas con
 * tela). Gana el punto más cercano a la cámara (el z de pantalla más grande).
 */
public class EstiladoScreen extends HandledScreen<EstiladoScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of("femclothes", "textures/gui/container/estilado.png");
    private static final int ANCHO = 384, ALTO = 256;
    private static final int PX1 = 8, PY1 = 18, PX2 = 204, PY2 = 250;
    private static final int X_DER = EstiladoScreenHandler.X_DERECHA;

    private float anguloVista = 0f;
    /** Inclinación de la vista (grados, 2026-10-04): + = se ve más desde arriba, − desde abajo. */
    private float inclinacionVista = 0f;
    private float zoomVista = 1f;
    private static final float INCLINACION_MAX = 85f;
    /** Mueve el muñeco para ver cómo se agita la tela de los apliques (2026-10-04). */
    private boolean sacudiendo = false;
    private boolean arrastrando = false;
    /** Dónde quedó cada aplique en la última vista previa (2026-10-04, para poner apliques sobre apliques). */
    private final Map<Integer, ApliqueRenderer.Captura> cajasApliques = new java.util.HashMap<>();
    /** Matrices de la última vista previa (por parte). */
    private final Map<Parte, Matrix4f> poses = new EnumMap<>(Parte.class);
    /** Las mallas de la pollera y la capa del último dibujo de la vista, en pantalla (2026-10-02). */
    private final Map<String, com.femclothes.render.MallaCapturada> mallas = new java.util.HashMap<>();
    @Nullable private Text aviso;

    private final ButtonWidget[] btnApliques = new ButtonWidget[Aplique.MAXIMO_POR_PRENDA];
    private ButtonWidget btnGiro, btnEscala, btnQuitar, btnSacudir;
    /** Controles del aplique de objeto (2026-10-04): arriba de la vista, solo con uno elegido. */
    private ButtonWidget btnObjModo, btnObjVariante;
    /**
     * Colorear el sombrero de bruja (2026-10-05, "que se le apliquen los colores en la mesa de estilado sobre todo si
     * tiene tres areas"): con un sombrero en la prenda hay un botón que cambia los controles de apliques por 3 filas
     * (ala, cono, cinta) con un botón por cada color del retazo, y "retazo entero".
     */
    private boolean modoColor = false;
    private ButtonWidget btnModoColor, btnColorEntero;
    private final ButtonWidget[][] btnColorZona = new ButtonWidget[3][3];
    /** Los controles de apliques que se esconden mientras se colorea. */
    private final List<ButtonWidget> grupoAplique = new java.util.ArrayList<>();


    public EstiladoScreen(EstiladoScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = ANCHO;
        this.backgroundHeight = ALTO;
        this.titleX = PX1;
        this.titleY = 7;
        this.playerInventoryTitleX = X_DER;
        this.playerInventoryTitleY = EstiladoScreenHandler.Y_INVENTARIO - 11;
    }

    private ButtonWidget boton(int x, int y, int w, Text texto, String tooltip, Runnable accion) {
        ButtonWidget b = new EstiloPergamino.BotonPergamino(this.x + x, this.y + y, w, 16, texto, btn -> accion.run());
        if (tooltip != null) b.setTooltip(Tooltip.of(Text.translatable(tooltip)));
        this.addDrawableChild(b);
        return b;
    }

    private void clickBoton(int id) {
        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

    @Override
    protected void init() {
        EstiloPergamino.usarTema(EstiloPergamino.Tema.LILA);
        super.init();
        for (int i = 0; i < btnApliques.length; i++) {
            int id = EstiladoBlockEntity.BTN_SELECCIONAR_BASE + i;
            btnApliques[i] = boton(X_DER + Math.round(i * 13.5f), 66, 13, Text.literal(Integer.toString(i + 1)),
                    "femclothes.estilado.tooltip.aplique", () -> clickBoton(id));
            grupoAplique.add(btnApliques[i]);
        }
        grupoAplique.add(boton(X_DER, 88, 14, Text.literal("<"), "femclothes.estilado.tooltip.giro", () -> clickBoton(EstiladoBlockEntity.BTN_GIRO_ATRAS)));
        btnGiro = boton(X_DER + 15, 88, 64, Text.empty(), "femclothes.estilado.tooltip.giro", () -> clickBoton(EstiladoBlockEntity.BTN_GIRO));
        grupoAplique.add(btnGiro);
        grupoAplique.add(boton(X_DER + 80, 88, 14, Text.literal(">"), "femclothes.estilado.tooltip.giro", () -> clickBoton(EstiladoBlockEntity.BTN_GIRO)));
        grupoAplique.add(boton(X_DER, 108, 14, Text.literal("<"), "femclothes.estilado.tooltip.escala", () -> clickBoton(EstiladoBlockEntity.BTN_ESCALA_ATRAS)));
        btnEscala = boton(X_DER + 15, 108, 64, Text.empty(), "femclothes.estilado.tooltip.escala", () -> clickBoton(EstiladoBlockEntity.BTN_ESCALA));
        grupoAplique.add(btnEscala);
        grupoAplique.add(boton(X_DER + 80, 108, 14, Text.literal(">"), "femclothes.estilado.tooltip.escala", () -> clickBoton(EstiladoBlockEntity.BTN_ESCALA)));
        btnQuitar = boton(X_DER + 98, 88, 64, Text.translatable("femclothes.estilado.quitar"),
                "femclothes.estilado.tooltip.quitar", () -> clickBoton(EstiladoBlockEntity.BTN_QUITAR));
        grupoAplique.add(btnQuitar);
        boton(X_DER + 98, 108, 31, Text.literal("⟲"), "femclothes.estilado.tooltip.vista",
                () -> anguloVista = Math.floorMod(Math.round(anguloVista) - 45, 360));
        boton(X_DER + 131, 108, 31, Text.literal("⟳"), "femclothes.estilado.tooltip.vista",
                () -> anguloVista = Math.floorMod(Math.round(anguloVista) + 45, 360));
        // Textura de tela (2026-10-01, relieve): con un Molde de textura en el slot del molde.
        btnTextura = boton(X_DER, 132, 96, Text.empty(), "femclothes.estilado.tooltip.textura",
                () -> clickBoton(EstiladoBlockEntity.BTN_TEXTURA));
        grupoAplique.add(btnTextura);
        // Colorear el sombrero (2026-10-05): modo, 3 filas (ala, cono, cinta) x 3 colores del retazo, y retazo entero.
        btnModoColor = boton(X_DER, 150, 96, Text.empty(), "femclothes.estilado.tooltip.modo_color",
                () -> modoColor = !modoColor);
        for (int z = 0; z < 3; z++) {
            for (int c = 0; c < 3; c++) {
                int id = EstiladoBlockEntity.BTN_COLOR_BASE + z * 4 + c;
                btnColorZona[z][c] = boton(X_DER + 46 + c * 17, 66 + z * 22, 15, Text.literal("■"),
                        "femclothes.estilado.tooltip.color_zona", () -> clickBoton(id));
            }
        }
        btnColorEntero = boton(X_DER, 132, 96, Text.translatable("femclothes.estilado.color.entero"),
                "femclothes.estilado.tooltip.color_entero", () -> clickBoton(EstiladoBlockEntity.BTN_COLOR_ENTERO));
        // Sacudir (2026-10-04, "boton de sacudir... alternar mover el muñeco"): prende y apaga el vaivén.
        btnSacudir = boton(X_DER + 98, 132, 64, Text.empty(), "femclothes.estilado.tooltip.sacudir",
                () -> sacudiendo = !sacudiendo);
        // Aplique de objeto (2026-10-04, "seleccionar si item o bloque"): en la franja de arriba de la vista; solo
        // se ven con un aplique de objeto elegido. La colocación y el movimiento están en el panel lateral.
        int by = PY1 + 3;
        btnObjModo = boton(PX1 + 4, by, 58, Text.empty(), "femclothes.estilado.tooltip.obj_modo",
                () -> clickBoton(EstiladoBlockEntity.BTN_OBJ_MODO));
        btnObjVariante = boton(PX1 + 64, by, 58, Text.empty(), "femclothes.estilado.tooltip.obj_variante",
                () -> clickBoton(EstiladoBlockEntity.BTN_OBJ_VARIANTE));
        // Mesa creativa (2026-10-01): elegir cualquier molde sin tenerlo.
        ButtonWidget moldeCreativo = boton(X_DER + 98, 44, 64, Text.translatable("femclothes.estilado.siguiente_molde"),
                "femclothes.estilado.tooltip.siguiente_molde", () -> clickBoton(EstiladoBlockEntity.BTN_SIGUIENTE_MOLDE));
        moldeCreativo.visible = com.femclothes.util.MaquinaCreativa.es(handler.be);
        crearPanel();
    }

    private ButtonWidget btnTextura;

    // ── panel lateral (2026-10-04, "posición, rotación, escala, cara de contacto, profundidad, pivote, oscilación") ──
    private static final int PW = 160;
    /** Una página del panel: 0 = Colocación, 1 = Movimiento, 2 = Moldes (la biblioteca de la Mesa creativa). */
    private int paginaPanel = 0;
    /** La biblioteca de moldes: 12 filas por vez (2026-10-04). */
    private static final int FILAS_BIBLIO = 12;
    private int paginaBiblio = 0;
    private final ButtonWidget[] btnBiblioDar = new ButtonWidget[FILAS_BIBLIO], btnBiblioBorrar = new ButtonWidget[FILAS_BIBLIO];
    private ButtonWidget btnPaginaMoldes, btnBiblioAnt, btnBiblioSig;
    private int panelX;
    private ButtonWidget btnPaginaColocacion, btnPaginaMovimiento, btnRestablecer, btnPivote, btnEje, btnCrearMolde;
    /** El slider de blandura de la Mesa normal (2026-10-04): como estaba antes del panel lateral. */
    private SliderAjuste sliderNormal;
    /** El nombre del molde que se va a fabricar (2026-10-04, "me falta un casillero para ponerle el nombre al aplique"). */
    private net.minecraft.client.gui.widget.TextFieldWidget txtNombreMolde;

    /** Si el molde es uno personalizado de un objeto (no pide retazo: la muestra de color es opcional). */
    private static boolean plantillaDeObjeto(ItemStack molde) {
        Aplique p = molde.getItem() instanceof com.femclothes.aplique.MoldeApliquePersonalizadoItem
                ? com.femclothes.aplique.MoldeApliquePersonalizadoItem.plantilla(molde) : null;
        return p != null && p.objeto() != null;
    }

    /** La Mesa creativa tiene el slot de objeto, el panel lateral y la fábrica de moldes; la normal, no. */
    private boolean creativa() {
        return com.femclothes.util.MaquinaCreativa.es(handler.be);
    }
    private final ButtonWidget[] btnCaras = new ButtonWidget[6];
    private final List<SliderAjuste> slidersColocacion = new java.util.ArrayList<>();
    private final List<SliderAjuste> slidersMovimiento = new java.util.ArrayList<>();
    /** La cara de contacto de cada botón: arriba, abajo, izquierda, derecha, frente, atrás (del objeto). */
    private static final Direction[] CARAS = { Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST,
            Direction.NORTH, Direction.SOUTH };
    private static final String[] NOMBRE_CARA = { "arriba", "abajo", "izquierda", "derecha", "frente", "atras" };

    /** El aplique elegido, o null. */
    @Nullable
    private Aplique elegido() {
        List<Aplique> l = handler.be.apliques();
        int sel = handler.be.seleccionado();
        return sel >= 0 && sel < l.size() ? l.get(sel) : null;
    }

    /** Aplica un cambio al aplique elegido: se ve YA en la vista (copia local) y se manda al servidor. */
    private void ajustar(java.util.function.UnaryOperator<Aplique> cambio) {
        Aplique a = elegido();
        if (a == null) return;
        Aplique nuevo = cambio.apply(a);
        int sel = handler.be.seleccionado();
        handler.be.ajustar(sel, nuevo.colocacion(), nuevo.oscilacion(), nuevo.blandura());
        ClientPlayNetworking.send(new com.femclothes.estilado.AjustarApliquePayload(handler.be.getPos(), sel,
                nuevo.colocacion(), nuevo.oscilacion(), nuevo.blandura()));
    }

    /** Un slider del panel: mapea su 0..1 a [min, max] en pasos, lee el valor del aplique elegido y manda el cambio. */
    private final class SliderAjuste extends net.minecraft.client.gui.widget.SliderWidget {
        private final String clave;
        private final float min, max, paso;
        private final java.util.function.Function<Aplique, Float> leer;
        private final java.util.function.BiFunction<Aplique, Float, Aplique> escribir;
        private final boolean porcentaje;
        private boolean editando = false;
        private float ultimo = Float.NaN;

        SliderAjuste(int x, int y, int ancho, String clave, float min, float max, float paso, boolean porcentaje,
                     java.util.function.Function<Aplique, Float> leer,
                     java.util.function.BiFunction<Aplique, Float, Aplique> escribir) {
            super(x, y, ancho, 12, Text.empty(), 0.5);
            this.clave = clave;
            this.min = min;
            this.max = max;
            this.paso = paso;
            this.porcentaje = porcentaje;
            this.leer = leer;
            this.escribir = escribir;
        }

        private float valor() {
            float v = min + (float) value * (max - min);
            return Math.round(v / paso) * paso;
        }

        /** Sigue al aplique elegido, salvo mientras se lo arrastra. */
        void refrescar(@Nullable Aplique a) {
            active = a != null;
            if (a != null && !editando) {
                float v = leer.apply(a);
                value = (v - min) / (max - min);
            }
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float v = valor();
            String t = porcentaje ? Integer.toString(Math.round(v * 100))
                    : (v == Math.rint(v) ? Integer.toString((int) v) : String.format(java.util.Locale.ROOT, "%.2f", v));
            setMessage(Text.translatable("femclothes.estilado.panel." + clave, t));
        }

        @Override
        protected void applyValue() {
            float v = valor();
            if (v == ultimo) return;
            ultimo = v;
            ajustar(a -> escribir.apply(a, v));
        }

        @Override
        public void onClick(double mx, double my) {
            editando = true;
            ultimo = Float.NaN;
            super.onClick(mx, my);
        }

        @Override
        public void onRelease(double mx, double my) {
            super.onRelease(mx, my);
            editando = false;
        }

        @Override
        public void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            int x0 = getX(), y0 = getY(), w = getWidth(), h = getHeight();
            c.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, 0xFF2A180C);
            c.fill(x0, y0, x0 + w, y0 + h, active ? 0xFF5A4028 : 0xFF3A2C1C);
            c.fill(x0, y0 + h - 1, x0 + w, y0 + h, EstiloPergamino.tema().claro);
            int mango = x0 + (int) (value * (w - 6));
            EstiloPergamino.fondoBoton(c, mango, y0, 6, h, isHovered(), active);
            var fuente = MinecraftClient.getInstance().textRenderer;
            Text m = getMessage();
            int tx = x0 + (w - fuente.getWidth(m)) / 2, ty = y0 + (h - 8) / 2 + 1;
            c.drawText(fuente, m, tx + 1, ty + 1, 0xFF2A180C, false);
            c.drawText(fuente, m, tx, ty, active ? EstiloPergamino.TEXTO_CLARO : EstiloPergamino.TEXTO_APAGADO, false);
        }
    }

    private SliderAjuste slider(List<SliderAjuste> pagina, int y, String clave, float min, float max, float paso,
                                boolean porcentaje, java.util.function.Function<Aplique, Float> leer,
                                java.util.function.BiFunction<Aplique, Float, Aplique> escribir, String tooltip) {
        SliderAjuste s = new SliderAjuste(panelX + 6, this.y + y, PW - 12, clave, min, max, paso, porcentaje, leer, escribir);
        if (tooltip != null) s.setTooltip(Tooltip.of(Text.translatable(tooltip)));
        this.addDrawableChild(s);
        pagina.add(s);
        return s;
    }

    private ButtonWidget botonPanel(int x, int y, int w, Text texto, String tooltip, Runnable accion) {
        ButtonWidget b = new EstiloPergamino.BotonPergamino(panelX + x, this.y + y, w, 14, texto, btn -> accion.run());
        if (tooltip != null) b.setTooltip(Tooltip.of(Text.translatable(tooltip)));
        this.addDrawableChild(b);
        return b;
    }

    private void crearPanel() {
        // A la derecha de la ventana; si no hay lugar, a la izquierda.
        panelX = this.x + ANCHO + 4;
        if (panelX + PW > this.width && this.x - PW - 4 >= 0) panelX = this.x - PW - 4;
        slidersColocacion.clear();
        slidersMovimiento.clear();
        btnPaginaColocacion = botonPanel(6, 6, 48, Text.translatable("femclothes.estilado.panel.pagina.colocacion"),
                null, () -> paginaPanel = 0);
        btnPaginaMovimiento = botonPanel(56, 6, 48, Text.translatable("femclothes.estilado.panel.pagina.movimiento"),
                null, () -> paginaPanel = 1);
        btnPaginaMoldes = botonPanel(106, 6, 48, Text.translatable("femclothes.estilado.panel.pagina.moldes"),
                "femclothes.estilado.tooltip.biblioteca", () -> paginaPanel = 2);
        // Página Moldes: la biblioteca de lo que fabricó esta Mesa (click = una copia; la X lo borra de la lista).
        for (int i = 0; i < FILAS_BIBLIO; i++) {
            int fila = i;
            btnBiblioDar[i] = botonPanel(6, 28 + i * 15, 126, Text.empty(), "femclothes.estilado.tooltip.biblioteca_dar",
                    () -> this.client.interactionManager.clickButton(this.handler.syncId,
                            EstiladoBlockEntity.BTN_BIBLIO_DAR_BASE + paginaBiblio * FILAS_BIBLIO + fila));
            btnBiblioBorrar[i] = botonPanel(134, 28 + i * 15, 20, Text.literal("x"), "femclothes.estilado.tooltip.biblioteca_borrar",
                    () -> this.client.interactionManager.clickButton(this.handler.syncId,
                            EstiladoBlockEntity.BTN_BIBLIO_BORRAR_BASE + paginaBiblio * FILAS_BIBLIO + fila));
        }
        btnBiblioAnt = botonPanel(6, 232, 34, Text.literal("<"), null, () -> paginaBiblio = 0);
        btnBiblioSig = botonPanel(44, 232, 34, Text.literal(">"), null, () -> paginaBiblio = 1);

        // Página Colocación: cara de contacto (solo objetos), profundidad, desplazamiento, rotación, escala.
        for (int i = 0; i < 6; i++) {
            int idx = i;
            btnCaras[i] = botonPanel(6 + (i % 3) * 50, 40 + (i / 3) * 16, 48,
                    Text.translatable("femclothes.estilado.cara." + NOMBRE_CARA[i]), "femclothes.estilado.tooltip.cara_base",
                    () -> ajustar(a -> a.conColocacion(a.colocacion().conCara(CARAS[idx]))));
        }
        slider(slidersColocacion, 74, "profundidad", -8f, 8f, 0.25f, false, a -> a.colocacion().campo(0),
                (a, v) -> a.conColocacion(a.colocacion().conCampo(0, v)), "femclothes.estilado.tooltip.profundidad");
        String[] ejes = { "x", "y", "z" };
        for (int i = 0; i < 3; i++) {
            int c = i;
            slider(slidersColocacion, 90 + i * 14, "d" + ejes[i], -16f, 16f, 0.25f, false, a -> a.colocacion().campo(1 + c),
                    (a, v) -> a.conColocacion(a.colocacion().conCampo(1 + c, v)), null);
            slider(slidersColocacion, 136 + i * 14, "r" + ejes[i], -180f, 180f, 5f, false, a -> a.colocacion().campo(4 + c),
                    (a, v) -> a.conColocacion(a.colocacion().conCampo(4 + c, v)), null);
            slider(slidersColocacion, 182 + i * 14, "s" + ejes[i], 0.1f, 4f, 0.05f, true, a -> a.colocacion().campo(7 + c),
                    (a, v) -> a.conColocacion(a.colocacion().conCampo(7 + c, v)), null);
        }

        // Página Movimiento: pivote, intensidad (= blandura), velocidad, amplitud y eje.
        btnPivote = botonPanel(6, 34, PW - 12, Text.empty(), "femclothes.estilado.tooltip.pivote", () -> ajustar(a -> {
            var v = com.femclothes.aplique.Oscilacion.Pivote.values();
            return a.conOscilacion(a.oscilacion().conPivote(v[(a.oscilacion().pivote().ordinal() + 1) % v.length]));
        }));
        for (int i = 0; i < 3; i++) {
            int c = i;
            slider(slidersMovimiento, 52 + i * 14, "po" + ejes[i], -16f, 16f, 0.25f, false, a -> a.oscilacion().campo(c),
                    (a, v) -> a.conOscilacion(a.oscilacion().conCampo(c, v)), "femclothes.estilado.tooltip.pivote_offset");
        }
        slider(slidersMovimiento, 98, "intensidad", 0f, 1f, 0.1f, true, Aplique::blandura,
                (a, v) -> a.conBlandura(v), "femclothes.estilado.tooltip.blandura");
        slider(slidersMovimiento, 112, "velocidad", 0f, 4f, 0.1f, false, a -> a.oscilacion().campo(3),
                (a, v) -> a.conOscilacion(a.oscilacion().conCampo(3, v)), "femclothes.estilado.tooltip.velocidad");
        slider(slidersMovimiento, 126, "amplitud", 0f, 60f, 1f, false, a -> a.oscilacion().campo(4),
                (a, v) -> a.conOscilacion(a.oscilacion().conCampo(4, v)), "femclothes.estilado.tooltip.amplitud");
        btnEje = botonPanel(6, 144, PW - 12, Text.empty(), "femclothes.estilado.tooltip.eje", () -> ajustar(a -> {
            var v = com.femclothes.aplique.Oscilacion.Eje.values();
            return a.conOscilacion(a.oscilacion().conEje(v[(a.oscilacion().eje().ordinal() + 1) % v.length]));
        }));
        sliderNormal = new SliderAjuste(this.x + X_DER, this.y + 150, 162, "blandura", 0f, 1f, 0.1f, true, Aplique::blandura,
                (a, v) -> a.conBlandura(v));
        sliderNormal.setTooltip(Tooltip.of(Text.translatable("femclothes.estilado.tooltip.blandura")));
        this.addDrawableChild(sliderNormal);
        btnCrearMolde = botonPanel(82, 232, 72, Text.translatable("femclothes.estilado.panel.crear_molde"),
                "femclothes.estilado.tooltip.crear_molde", () -> {
                    ClientPlayNetworking.send(new com.femclothes.estilado.CrearMoldePayload(handler.be.getPos(), txtNombreMolde.getText()));
                    txtNombreMolde.setText("");
                });
        txtNombreMolde = new net.minecraft.client.gui.widget.TextFieldWidget(this.textRenderer, panelX + 6, this.y + 214, PW - 12, 14,
                Text.translatable("femclothes.estilado.panel.nombre"));
        txtNombreMolde.setMaxLength(40);
        txtNombreMolde.setPlaceholder(Text.translatable("femclothes.estilado.panel.nombre"));
        this.addDrawableChild(txtNombreMolde);
        btnRestablecer = botonPanel(6, 232, 72, Text.translatable("femclothes.estilado.panel.restablecer"),
                "femclothes.estilado.tooltip.restablecer", () -> ajustar(a -> a
                        .conColocacion(com.femclothes.aplique.Colocacion.DEFECTO.conCara(a.colocacion().caraBase()))
                        .conOscilacion(com.femclothes.aplique.Oscilacion.DEFECTO)));
    }

    /** Muestra la página elegida y pone en cada control el valor del aplique elegido. */
    private void actualizarPanel(@Nullable Aplique a) {
        boolean hay = a != null;
        boolean objeto = hay && a.objeto() != null;
        boolean cre = creativa();
        sliderNormal.visible = !cre;
        sliderNormal.refrescar(a);
        if (!cre) {
            // La Mesa normal no tiene panel lateral.
            for (var w : new ButtonWidget[] { btnPaginaColocacion, btnPaginaMovimiento, btnPaginaMoldes, btnRestablecer, btnPivote, btnEje,
                    btnCrearMolde, btnBiblioAnt, btnBiblioSig }) w.visible = false;
            for (int i = 0; i < FILAS_BIBLIO; i++) btnBiblioDar[i].visible = btnBiblioBorrar[i].visible = false;
            txtNombreMolde.visible = false;
            for (SliderAjuste sl : slidersColocacion) sl.visible = false;
            for (SliderAjuste sl : slidersMovimiento) sl.visible = false;
            for (ButtonWidget b : btnCaras) b.visible = false;
            return;
        }
        btnCrearMolde.visible = true;
        btnCrearMolde.active = hay;
        txtNombreMolde.visible = true;
        txtNombreMolde.setEditable(hay);
        btnPaginaColocacion.active = paginaPanel != 0;
        btnPaginaMovimiento.active = paginaPanel != 1;
        btnPaginaMoldes.visible = true;
        btnPaginaMoldes.active = paginaPanel != 2;
        // La biblioteca (página 2): una fila por molde guardado en esta Mesa.
        boolean biblio = paginaPanel == 2;
        btnBiblioAnt.visible = btnBiblioSig.visible = biblio;
        btnBiblioAnt.active = paginaBiblio != 0;
        btnBiblioSig.active = paginaBiblio != 1;
        for (int i = 0; i < FILAS_BIBLIO; i++) {
            ItemStack molde = handler.be.getStack(EstiladoBlockEntity.SLOT_BIBLIOTECA + paginaBiblio * FILAS_BIBLIO + i);
            boolean hayMolde = biblio && !molde.isEmpty();
            btnBiblioDar[i].visible = btnBiblioBorrar[i].visible = hayMolde;
            if (hayMolde) btnBiblioDar[i].setMessage(molde.getName());
        }
        for (SliderAjuste s : slidersColocacion) {
            s.visible = paginaPanel == 0;
            s.refrescar(a);
        }
        for (SliderAjuste s : slidersMovimiento) {
            s.visible = paginaPanel == 1;
            s.refrescar(a);
        }
        for (int i = 0; i < 6; i++) {
            btnCaras[i].visible = paginaPanel == 0;
            btnCaras[i].active = objeto && a.colocacion().caraBase() != CARAS[i];
        }
        btnPivote.visible = btnEje.visible = paginaPanel == 1;
        btnRestablecer.visible = paginaPanel != 2;
        btnRestablecer.active = hay;
        if (hay) {
            btnPivote.setMessage(Text.translatable("femclothes.estilado.panel.pivote",
                    Text.translatable("femclothes.estilado.pivote." + a.oscilacion().pivote().asString())));
            btnEje.setMessage(Text.translatable("femclothes.estilado.panel.eje",
                    Text.translatable("femclothes.estilado.eje." + a.oscilacion().eje().asString())));
        }
    }

    private void dibujarPanel(DrawContext c) {
        if (!creativa()) return;
        int x0 = panelX, y0 = this.y;
        c.fill(x0 - 1, y0 - 1, x0 + PW + 1, y0 + ALTO + 1, 0xFF2A180C);
        c.fill(x0, y0, x0 + PW, y0 + ALTO, 0xFF4B3F5A);
        c.fill(x0 + 2, y0 + 2, x0 + PW - 2, y0 + ALTO - 2, 0xFF5C4F6E);
        var fuente = MinecraftClient.getInstance().textRenderer;
        if (paginaPanel == 2) {
            if (handler.be.getStack(EstiladoBlockEntity.SLOT_BIBLIOTECA).isEmpty()) {
                c.drawTextWrapped(fuente, Text.translatable("femclothes.estilado.panel.biblioteca_vacia"), x0 + 8, y0 + 32,
                        PW - 16, EstiloPergamino.TEXTO_CLARO);
            }
            return;
        }
        Aplique a = elegido();
        if (a == null) {
            c.drawTextWrapped(fuente, Text.translatable("femclothes.estilado.panel.ninguno"), x0 + 8, y0 + 40, PW - 16,
                    EstiloPergamino.TEXTO_CLARO);
            return;
        }
        if (paginaPanel == 0) {
            Text t = a.objeto() != null ? Text.translatable("femclothes.estilado.panel.cara_base")
                    : Text.translatable("femclothes.estilado.panel.cara_modelo");
            c.drawText(fuente, t, x0 + 8, y0 + 28, EstiloPergamino.TEXTO_CLARO, false);
        }
    }

    @Override
    protected boolean isClickOutsideBounds(double mx, double my, int left, int top, int button) {
        // El panel está afuera de la ventana: tocarlo con un ítem en el cursor no lo tira.
        if (creativa() && mx >= panelX && mx < panelX + PW && my >= this.y && my < this.y + ALTO) return false;
        return super.isClickOutsideBounds(mx, my, left, top, button);
    }

    // ── vista previa ───────────────────────────────────────────────────────
    private boolean dentroDeVista(double mx, double my) {
        return mx >= this.x + PX1 && mx < this.x + PX2 && my >= this.y + PY1 && my < this.y + PY2;
    }

    private void dibujarVista(DrawContext context) {
        PlayerEntity jugador = MinecraftClient.getInstance().player;
        if (jugador == null) return;
        ItemStack prenda = handler.be.getStack(EstiladoBlockEntity.SLOT_PRENDA);
        int x1 = this.x + PX1 + 2, y1 = this.y + PY1 + 2, x2 = this.x + PX2 - 2, y2 = this.y + PY2 - 2;
        Map<Parte, Matrix4f> captura = new EnumMap<>(Parte.class);
        Map<String, com.femclothes.render.MallaCapturada> capturaMallas = new java.util.HashMap<>();
        boolean delMod = com.femclothes.garment.Garments.esPrenda(prenda);
        GarmentFeatureRenderer.previewOverride = delMod ? List.of(prenda) : List.of();
        GarmentFeatureRenderer.capturaPoses = captura;
        GarmentFeatureRenderer.capturaMallas = capturaMallas;
        Map<Integer, ApliqueRenderer.Captura> capturaApliques = new java.util.HashMap<>();
        ApliqueRenderer.capturaApliques = capturaApliques;
        // Una armadura (2026-10-02, "extender apliques para toda armadura o
        // wearable"): puesta de mentira en el inventario del jugador SOLO del
        // cliente durante este dibujo, como la vista previa del Guardarropas.
        var armadura = jugador.getInventory().armor;
        ItemStack[] antes = new ItemStack[armadura.size()];
        for (int i = 0; i < antes.length; i++) antes[i] = armadura.get(i);
        net.minecraft.entity.EquipmentSlot slot = delMod ? null : slotDe(prenda);
        // El sombrero de bruja no está en los Trinkets del jugador: se dibuja de mentira (2026-10-05).
        GarmentFeatureRenderer.sombreroOverride = prenda.getItem() instanceof com.femclothes.item.SombreroBrujaItem ? prenda : null;
        try {
            if (slot != null && slot.getType() == net.minecraft.entity.EquipmentSlot.Type.HUMANOID_ARMOR) {
                armadura.set(slot.getEntitySlotId(), prenda);
            }
            // mouseY en el centro: la vista no se inclina con el mouse (el click necesita una pose quieta).
            actualizarModoDeTela();
            PreviewJugador.dibujar(context, jugador, x1, y1, x2, y2, Math.round(78 * zoomVista), anguloVista,
                    (y1 + y2) / 2f, inclinacionVista);
        } finally {
            com.femclothes.render.FisicaApliques.modoVistaPrevia = com.femclothes.render.FisicaApliques.Modo.QUIETO;
            GarmentFeatureRenderer.previewOverride = null;
            GarmentFeatureRenderer.sombreroOverride = null;
            GarmentFeatureRenderer.capturaPoses = null;
            ApliqueRenderer.capturaApliques = null;
            GarmentFeatureRenderer.capturaMallas = null;
            for (int i = 0; i < antes.length; i++) armadura.set(i, antes[i]);
        }
        cajasApliques.clear();
        cajasApliques.putAll(capturaApliques);
        poses.clear();
        poses.putAll(captura);
        mallas.clear();
        mallas.putAll(capturaMallas);
    }

    /** La tela de los apliques se agita en la vista previa solo mientras Sacudir está prendido. */
    private void actualizarModoDeTela() {
        com.femclothes.render.FisicaApliques.modoVistaPrevia = sacudiendo
                ? com.femclothes.render.FisicaApliques.Modo.SACUDIDA : com.femclothes.render.FisicaApliques.Modo.QUIETO;
    }

    /** Resultado del click: parte, punto sobre la caja sin inflar (px) y cara. */
    private record Toque(Parte parte, float x, float y, float z, Direction cara, float profundidad,
                         Aplique.Superficie superficie, int padre) {
        Toque(Parte parte, float x, float y, float z, Direction cara, float profundidad) {
            this(parte, x, y, z, cara, profundidad, Aplique.Superficie.CAJA, -1);
        }

        Toque(Parte parte, float x, float y, float z, Direction cara, float profundidad, Aplique.Superficie superficie) {
            this(parte, x, y, z, cara, profundidad, superficie, -1);
        }

        /** La profundidad en el mismo eje para todos: z de pantalla (más alto = más cerca de quien mira). */
        float zPantalla() {
            // La malla de pollera/capa ya guarda el z de pantalla; las cajas guardan el parámetro t del rayo (de -10000 a 10000).
            return superficie == Aplique.Superficie.POLLERA || superficie == Aplique.Superficie.CAPA
                    ? profundidad : -10000f + 20000f * profundidad;
        }
    }

    /** El slot de armadura (o de mano) de un ítem que se pone, o null. */
    @Nullable
    private static net.minecraft.entity.EquipmentSlot slotDe(ItemStack stack) {
        net.minecraft.item.Equipment e = net.minecraft.item.Equipment.fromStack(stack);
        return e == null ? null : e.getSlotType();
    }

    /**
     * Las cajas donde se puede poner un aplique en una armadura o wearable
     * que no es del mod (2026-10-02): las partes que cubre su slot, infladas
     * lo que sale del cuerpo ({@link ApliqueRenderer#dilatacionDeSlot}). Sin
     * slot de armadura (Trinkets, otros), todo el cuerpo.
     */
    private static List<Pieza> piezasDeVestible(ItemStack stack) {
        net.minecraft.entity.EquipmentSlot slot = slotDe(stack);
        float d = ApliqueRenderer.dilatacionDeSlot(slot);
        Identifier nada = Identifier.of("femclothes", "vacio");
        java.util.List<Pieza> out = new java.util.ArrayList<>();
        if (slot == net.minecraft.entity.EquipmentSlot.HEAD) {
            out.add(new Pieza(Parte.CABEZA, 0, nada, d));
        } else if (slot == net.minecraft.entity.EquipmentSlot.CHEST) {
            for (Parte p : new Parte[]{Parte.TORSO, Parte.BRAZO_DER, Parte.BRAZO_IZQ}) out.add(new Pieza(p, 0, nada, d));
        } else if (slot == net.minecraft.entity.EquipmentSlot.LEGS) {
            out.add(new Pieza(Parte.TORSO, 0, nada, d, 8, 12));
            out.add(new Pieza(Parte.PIERNA_DER, 0, nada, d));
            out.add(new Pieza(Parte.PIERNA_IZQ, 0, nada, d));
        } else if (slot == net.minecraft.entity.EquipmentSlot.FEET) {
            out.add(new Pieza(Parte.PIERNA_DER, 0, nada, d, 6, 12));
            out.add(new Pieza(Parte.PIERNA_IZQ, 0, nada, d, 6, 12));
        } else {
            for (Parte p : Parte.values()) out.add(new Pieza(p, 0, nada, d));
        }
        return out;
    }

    /** Lo más cercano a quien mira entre la prenda y los apliques que ya tiene (2026-10-04, apliques sobre apliques). */
    @Nullable
    private Toque tocar(double mx, double my) {
        Toque prenda = tocarPrenda(mx, my);
        Toque aplique = tocarApliques((float) mx, (float) my);
        if (aplique == null) return prenda;
        return prenda == null || aplique.zPantalla() > prenda.zPantalla() ? aplique : prenda;
    }

    /** El rayo del mouse contra la caja de cada aplique (el espacio de objeto de su padre): el punto y la cara tocados. */
    @Nullable
    private Toque tocarApliques(float mx, float my) {
        Toque mejor = null;
        for (var e : cajasApliques.entrySet()) {
            ApliqueRenderer.Captura c = e.getValue();
            Matrix4f m = c.matriz();
            if (Math.abs(m.determinant()) < 1e-12f) continue;
            Matrix4f inversa = new Matrix4f(m).invert();
            final float Z = 10000f;
            Vector3f p0 = inversa.transformPosition(new Vector3f(mx, my, -Z));
            Vector3f p1 = inversa.transformPosition(new Vector3f(mx, my, Z));
            float[] o = { p0.x, p0.y, p0.z };
            float[] d = { p1.x - p0.x, p1.y - p0.y, p1.z - p0.z };
            float tMin = -Float.MAX_VALUE, tMax = Float.MAX_VALUE;
            int eje = -1;
            float signo = 0;
            boolean fuera = false;
            for (int i = 0; i < 3 && !fuera; i++) {
                if (Math.abs(d[i]) < 1e-9f) {
                    if (o[i] < c.min()[i] || o[i] > c.max()[i]) fuera = true;
                    continue;
                }
                float t1 = (c.min()[i] - o[i]) / d[i], t2 = (c.max()[i] - o[i]) / d[i];
                float cerca = Math.min(t1, t2), lejos = Math.max(t1, t2);
                if (cerca > tMin) tMin = cerca;
                if (lejos < tMax) {
                    tMax = lejos;
                    eje = i;
                    signo = d[i] > 0 ? 1 : -1;
                }
            }
            if (fuera || eje < 0 || tMin > tMax || tMax < 0 || tMax > 1) continue;
            // La cara que se ve es por donde sale el rayo (la más cercana a quien mira).
            float[] p = { o[0] + d[0] * tMax, o[1] + d[1] * tMax, o[2] + d[2] * tMax };
            for (int i = 0; i < 3; i++) {
                p[i] = i == eje ? (signo > 0 ? c.max()[i] : c.min()[i]) : Math.max(c.min()[i], Math.min(c.max()[i], p[i]));
            }
            Direction cara = Direction.getFacing(eje == 0 ? signo : 0, eje == 1 ? signo : 0, eje == 2 ? signo : 0);
            Toque t = new Toque(Parte.TORSO, p[0] * 16f, p[1] * 16f, p[2] * 16f, cara, tMax, Aplique.Superficie.APLIQUE, e.getKey());
            if (mejor == null || t.profundidad() > mejor.profundidad()) mejor = t;
        }
        return mejor;
    }

    @Nullable
    private Toque tocarPrenda(double mx, double my) {
        ItemStack prenda = handler.be.getStack(EstiladoBlockEntity.SLOT_PRENDA);
        PlayerEntity jugador = MinecraftClient.getInstance().player;
        if (prenda.isEmpty() || jugador == null || poses.isEmpty()) return null;
        boolean slim = MinecraftClient.getInstance().player.getSkinTextures().model() == SkinTextures.Model.SLIM;
        // Pollera y capa (2026-10-02, "no registran click on garment"): contra su malla, por UV.
        Aplique.Superficie malla = prenda.getItem() instanceof com.femclothes.item.PolleraItem ? Aplique.Superficie.POLLERA
                : prenda.getItem() instanceof com.femclothes.item.CapaItem ? Aplique.Superficie.CAPA : null;
        if (malla != null) {
            com.femclothes.render.MallaCapturada m = mallas.get(malla == Aplique.Superficie.POLLERA ? "pollera" : "capa");
            float[] h = m == null ? null : m.tocar((float) mx, (float) my);
            if (h == null) return null;
            return new Toque(Parte.TORSO, h[0] * 64f, h[1] * 64f, 0f, Direction.SOUTH, h[2], malla);
        }
        Toque mejor = null;
        com.femclothes.render.relieve.BustoRender.Busto busto =
                com.femclothes.render.GarmentFeatureRenderer.bustoDe(jugador, 0f, false);
        List<Pieza> piezas = com.femclothes.garment.Garments.esPrenda(prenda)
                ? PiezasDePrenda.de(prenda, jugador) : piezasDeVestible(prenda);
        for (Pieza pieza : piezas) {
            Matrix4f m = poses.get(pieza.parte());
            if (m == null) continue;
            Matrix4f inversa = new Matrix4f(m).invert();
            Toque t = cortar(pieza, inversa, slim, (float) mx, (float) my);
            if (t != null && (mejor == null || t.profundidad() > mejor.profundidad())) mejor = t;
            // El busto (2026-10-02, "se puede las dos? cosa que si armo la prenda sin
            // pechos despues siga sirviendo?"): el click le pega a la cúpula, pero se
            // guarda el punto del frente plano que tiene debajo.
            if (busto != null && pieza.parte() == Parte.TORSO
                    && com.femclothes.render.relieve.BustoRender.cubre(pieza.filaDesde(), pieza.filaHasta())) {
                Toque tb = cortarBusto(busto, pieza, inversa, (float) mx, (float) my);
                if (tb != null && (mejor == null || tb.profundidad() > mejor.profundidad())) mejor = tb;
            }
            // La cola (2026-10-02): igual, en la espalda.
            if (busto != null && pieza.parte() == Parte.TORSO
                    && com.femclothes.render.relieve.BustoRender.cubreCola(pieza.filaDesde(), pieza.filaHasta())) {
                Toque tc = cortarCola(busto, pieza, inversa, (float) mx, (float) my);
                if (tc != null && (mejor == null || tc.profundidad() > mejor.profundidad())) mejor = tc;
            }
        }
        return mejor;
    }

    /** El rayo del mouse contra la cola de esta pieza: guarda el punto de la espalda plana de debajo. */
    @Nullable
    private static Toque cortarCola(com.femclothes.render.relieve.BustoRender.Busto busto, Pieza pieza,
                                    Matrix4f inversa, float mx, float my) {
        final float Z = 10000f;
        Vector3f lejos = inversa.transformPosition(new Vector3f(mx, my, -Z)).mul(16f);
        Vector3f cerca = inversa.transformPosition(new Vector3f(mx, my, Z)).mul(16f);
        Vector3f dir = new Vector3f(lejos).sub(cerca);
        float[] h = com.femclothes.render.relieve.BustoRender.rayoCola(busto, cerca, dir,
                Math.max(0f, pieza.dilatacion()) + 0.02f,
                com.femclothes.render.relieve.BustoRender.carpaDe(com.femclothes.item.Calce.de(pieza.dilatacion())));
        if (h == null) return null;
        return new Toque(Parte.TORSO, h[1], h[2], 2f, Direction.SOUTH, 1f - h[0]);
    }

    /** El rayo del mouse contra las cúpulas del busto de esta pieza (desde el lado del que mira). */
    @Nullable
    private static Toque cortarBusto(com.femclothes.render.relieve.BustoRender.Busto busto, Pieza pieza,
                                     Matrix4f inversa, float mx, float my) {
        final float Z = 10000f;
        Vector3f lejos = inversa.transformPosition(new Vector3f(mx, my, -Z)).mul(16f);
        Vector3f cerca = inversa.transformPosition(new Vector3f(mx, my, Z)).mul(16f);
        Vector3f dir = new Vector3f(lejos).sub(cerca);
        float[] h = com.femclothes.render.relieve.BustoRender.rayo(busto, cerca, dir,
                Math.max(0f, pieza.dilatacion()) + 0.02f,
                com.femclothes.render.relieve.BustoRender.carpaDe(com.femclothes.item.Calce.de(pieza.dilatacion())));
        if (h == null) return null;
        // Profundidad en la misma escala que cortar(): 1 = del lado del que mira.
        return new Toque(Parte.TORSO, h[1], h[2], -2f, Direction.NORTH, 1f - h[0]);
    }

    @Nullable
    private static Toque cortar(Pieza pieza, Matrix4f inversa, boolean slim, float mx, float my) {
        float[] c = ApliqueRenderer.caja(pieza.parte(), slim);
        float dil = Math.max(0f, pieza.dilatacion());
        int desde = pieza.parte() == Parte.CABEZA ? 0 : Math.max(0, pieza.filaDesde());
        int hasta = pieza.parte() == Parte.CABEZA ? 8 : Math.min(12, pieza.filaHasta());
        if (hasta <= desde) return null;
        float[] min = { (c[0] - dil) / 16f, (c[1] + desde) / 16f, (c[2] - dil) / 16f };
        float[] max = { (c[0] + c[3] + dil) / 16f, (c[1] + hasta) / 16f, (c[2] + c[5] + dil) / 16f };

        final float Z = 10000f;
        Vector3f p0 = inversa.transformPosition(new Vector3f(mx, my, -Z));
        Vector3f p1 = inversa.transformPosition(new Vector3f(mx, my, Z));
        float[] o = { p0.x, p0.y, p0.z };
        float[] d = { p1.x - p0.x, p1.y - p0.y, p1.z - p0.z };
        float tMin = -Float.MAX_VALUE, tMax = Float.MAX_VALUE;
        int eje = -1;
        float signo = 0;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9f) {
                if (o[i] < min[i] || o[i] > max[i]) return null;
                continue;
            }
            float t1 = (min[i] - o[i]) / d[i], t2 = (max[i] - o[i]) / d[i];
            float cerca = Math.min(t1, t2), lejos = Math.max(t1, t2);
            if (cerca > tMin) tMin = cerca;
            // La salida del rayo (z de pantalla más alto) es la cara que se ve.
            if (lejos < tMax) {
                tMax = lejos;
                eje = i;
                signo = d[i] > 0 ? 1 : -1;
            }
        }
        if (eje < 0 || tMin > tMax || tMax < 0 || tMax > 1) return null;
        float[] p = { (o[0] + d[0] * tMax) * 16, (o[1] + d[1] * tMax) * 16, (o[2] + d[2] * tMax) * 16 };
        // Sobre la caja SIN inflar: la cara tocada al ras y el resto acotado.
        float[] cMin = { c[0], c[1] + desde, c[2] }, cMax = { c[0] + c[3], c[1] + hasta, c[2] + c[5] };
        for (int i = 0; i < 3; i++) {
            p[i] = i == eje ? (signo > 0 ? cMax[i] : cMin[i]) : Math.max(cMin[i], Math.min(cMax[i], p[i]));
        }
        Direction cara = Direction.getFacing(eje == 0 ? signo : 0, eje == 1 ? signo : 0, eje == 2 ? signo : 0);
        return new Toque(pieza.parte(), p[0], p[1], p[2], cara, tMax);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Los botones de objeto están dentro de la vista: tienen prioridad sobre poner un aplique.
        for (ButtonWidget b : objetoBotones()) {
            if (b.visible && b.isMouseOver(mx, my)) return super.mouseClicked(mx, my, button);
        }
        if (dentroDeVista(mx, my)) {
            if (button == 1) {
                arrastrando = true;
                return true;
            }
            if (button == 0 && modoColor) {
                // Colorear el sombrero: el click en la vista no pone apliques (2026-10-05).
                return true;
            }
            if (button == 0) {
                Toque t = tocar(mx, my);
                EstiladoBlockEntity be = handler.be;
                boolean objeto = !be.getStack(EstiladoBlockEntity.SLOT_OBJETO).isEmpty();
                if (be.getStack(EstiladoBlockEntity.SLOT_PRENDA).isEmpty()) aviso = Text.translatable("femclothes.estilado.aviso.prenda");
                else if (!objeto && be.getStack(EstiladoBlockEntity.SLOT_MOLDE).getItem() instanceof com.femclothes.item.MoldeTexturaItem)
                    aviso = Text.translatable("femclothes.estilado.aviso.textura");
                else if (!objeto && be.getStack(EstiladoBlockEntity.SLOT_MOLDE).isEmpty()) aviso = Text.translatable("femclothes.estilado.aviso.molde");
                else if (!objeto && !(be.getStack(EstiladoBlockEntity.SLOT_RETAZO).getItem() instanceof com.femclothes.aplique.RetazoApliqueItem)
                        && !(plantillaDeObjeto(be.getStack(EstiladoBlockEntity.SLOT_MOLDE)))
                        && !com.femclothes.util.MaquinaCreativa.es(be)) aviso = Text.translatable("femclothes.estilado.aviso.retazo");
                else if (be.apliques().size() >= Aplique.MAXIMO_POR_PRENDA) aviso = Text.translatable("femclothes.estilado.aviso.lleno");
                else if (t == null) aviso = Text.translatable("femclothes.estilado.aviso.fuera");
                else {
                    aviso = null;
                    ClientPlayNetworking.send(new PonerApliquePayload(be.getPos(), t.parte().ordinal(),
                            t.x(), t.y(), t.z(), t.cara().ordinal(), t.superficie().ordinal(), t.padre()));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private List<ButtonWidget> objetoBotones() {
        return List.of(btnObjModo, btnObjVariante);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 1) arrastrando = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (arrastrando) {
            anguloVista = Math.floorMod(Math.round(anguloVista + (float) dx * 1.15f), 360);
            inclinacionVista = net.minecraft.util.math.MathHelper.clamp(
                    inclinacionVista + (float) dy * 0.8f, -INCLINACION_MAX, INCLINACION_MAX);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (dentroDeVista(mx, my)) {
            zoomVista = net.minecraft.util.math.MathHelper.clamp(zoomVista + (float) vertical * 0.1f, 0.5f, 2.5f);
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    /** Sin esto, escribir una "e" en el nombre del molde cierra la pantalla (mismo motivo que la Sublimadora). */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (txtNombreMolde != null && txtNombreMolde.visible
                && (txtNombreMolde.keyPressed(keyCode, scanCode, modifiers) || txtNombreMolde.isActive())) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── dibujo ─────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        EstiladoBlockEntity be = handler.be;
        List<Aplique> apliques = be.apliques();
        int sel = be.seleccionado();
        for (int i = 0; i < btnApliques.length; i++) {
            btnApliques[i].active = i < apliques.size();
            btnApliques[i].setMessage(Text.literal(i == sel ? "[" + (i + 1) + "]" : Integer.toString(i + 1)));
        }
        boolean hay = sel >= 0 && sel < apliques.size();
        // Sombrero de bruja en la prenda (2026-10-05): colorear o poner apliques.
        ItemStack enPrenda = be.getStack(EstiladoBlockEntity.SLOT_PRENDA);
        boolean sombrero = enPrenda.getItem() instanceof com.femclothes.item.SombreroBrujaItem;
        if (!sombrero) modoColor = false;
        btnModoColor.visible = sombrero;
        btnModoColor.setMessage(Text.translatable(modoColor ? "femclothes.estilado.modo_color.apliques"
                : "femclothes.estilado.modo_color.colorear"));
        for (ButtonWidget b : grupoAplique) b.visible = !modoColor;
        java.util.List<Integer> fuente = be.coloresDeLaFuente();
        for (int z = 0; z < 3; z++) {
            for (int c = 0; c < 3; c++) {
                ButtonWidget b = btnColorZona[z][c];
                b.visible = modoColor;
                b.active = fuente != null;
                int rgb = fuente == null ? 0x808080 : fuente.get(c);
                b.setMessage(Text.literal("■").styled(st -> st.withColor(rgb)));
            }
        }
        btnColorEntero.visible = modoColor;
        btnColorEntero.active = fuente != null;
        btnGiro.active = btnEscala.active = btnQuitar.active = hay;
        actualizarPanel(hay ? apliques.get(sel) : null);
        com.femclothes.aplique.ObjetoAplique obj = hay ? apliques.get(sel).objeto() : null;
        for (ButtonWidget b : objetoBotones()) b.visible = obj != null && creativa();
        if (obj != null) {
            boolean esBloque = com.femclothes.aplique.ObjetoAplique.bloqueDe(obj.item()) != null;
            btnObjModo.active = esBloque;
            btnObjModo.setMessage(Text.translatable(obj.bloque() ? "femclothes.estilado.obj.bloque" : "femclothes.estilado.obj.item"));
            btnObjVariante.active = obj.bloque() && obj.cantidadDeVariantes() > 1;
            btnObjVariante.setMessage(Text.translatable("femclothes.estilado.obj.variante",
                    Math.floorMod(obj.variante(), obj.cantidadDeVariantes()) + 1, obj.cantidadDeVariantes()));
        }
        btnSacudir.setMessage(Text.translatable(sacudiendo ? "femclothes.estilado.sacudir.parar" : "femclothes.estilado.sacudir"));
        btnGiro.setMessage(Text.translatable("femclothes.estilado.giro", hay ? Math.round(apliques.get(sel).giro()) : 0));
        btnEscala.setMessage(Text.translatable("femclothes.estilado.escala",
                hay ? Math.round(apliques.get(sel).escala() * 100) : 100));

        ItemStack prendaPuesta = be.getStack(EstiladoBlockEntity.SLOT_PRENDA);
        com.femclothes.item.TexturaTela actual = prendaPuesta.getOrDefault(
                com.femclothes.item.FemclothesComponents.TEXTURA_TELA, com.femclothes.item.TexturaTela.LISA);
        if (be.getStack(EstiladoBlockEntity.SLOT_MOLDE).getItem() instanceof com.femclothes.item.MoldeTexturaItem mt
                && !prendaPuesta.isEmpty()) {
            btnTextura.active = true;
            btnTextura.setMessage(Text.translatable(actual == mt.textura ? "femclothes.estilado.textura.quitar"
                    : "femclothes.estilado.textura.poner", Text.translatable(mt.textura.traduccion())));
        } else {
            btnTextura.active = false;
            btnTextura.setMessage(Text.translatable("femclothes.estilado.textura.actual",
                    Text.translatable(actual.traduccion())));
        }

        super.render(context, mouseX, mouseY, delta);
        dibujarVista(context);
        // Mira: dónde caería el aplique.
        if (dentroDeVista(mouseX, mouseY) && tocar(mouseX, mouseY) != null) {
            context.fill(mouseX - 3, mouseY, mouseX + 4, mouseY + 1, 0xFFFFFFFF);
            context.fill(mouseX, mouseY - 3, mouseX + 1, mouseY + 4, 0xFFFFFFFF);
        }
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX,
                this.playerInventoryTitleY, EstiloPergamino.TEXTO, false);
        int y = EstiladoScreenHandler.Y_SLOTS - 10;
        context.drawText(this.textRenderer, Text.translatable("femclothes.estilado.slot.prenda"), X_DER - 1, y, EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, Text.translatable("femclothes.estilado.slot.molde"), X_DER + 25, y + 30, EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, Text.translatable("femclothes.estilado.slot.retazo"), X_DER + 51, y, EstiloPergamino.TEXTO, false);
        if (creativa()) {
            context.drawText(this.textRenderer, Text.translatable("femclothes.estilado.slot.objeto"), X_DER + 77, y + 30, EstiloPergamino.TEXTO, false);
        }
        context.drawText(this.textRenderer, Text.translatable("femclothes.estilado.apliques", handler.be.apliques().size(),
                Aplique.MAXIMO_POR_PRENDA), X_DER + (creativa() ? 102 : 80), EstiladoScreenHandler.Y_SLOTS + 4, EstiloPergamino.TEXTO, false);
        if (modoColor) {
            for (int z = 0; z < 3; z++) {
                context.drawText(this.textRenderer, Text.translatable("femclothes.sombrero.zona." + (z + 1)),
                        X_DER, 66 + z * 22 + 4, EstiloPergamino.TEXTO, false);
            }
            if (handler.be.coloresDeLaFuente() == null) {
                context.drawText(this.textRenderer, Text.translatable("femclothes.estilado.color.sin_retazo"),
                        X_DER, 154 + 16, 0xFFFF9090, false);
            }
        }
        Text ayuda = aviso != null ? aviso : Text.translatable("femclothes.estilado.ayuda");
        int color = aviso != null ? 0xFFFF9090 : 0xFFE8DCC8;
        for (var linea : this.textRenderer.wrapLines(ayuda, PX2 - PX1 - 12)) {
            context.drawText(this.textRenderer, linea, PX1 + 6, PY2 - 22, color, true);
            break;
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight,
                this.backgroundWidth, this.backgroundHeight);
        // Marco del slot de objeto (2026-10-04), dibujado por código: el fondo no lo trae.
        int ox = this.x + X_DER + 3 * 26, oy = this.y + EstiladoScreenHandler.Y_SLOTS;
        if (creativa()) {
            context.fill(ox - 1, oy - 1, ox + 17, oy + 17, 0xFF2A180C);
            context.fill(ox, oy, ox + 16, oy + 16, 0xFF6B5A78);
        }
        dibujarPanel(context);
    }
}
