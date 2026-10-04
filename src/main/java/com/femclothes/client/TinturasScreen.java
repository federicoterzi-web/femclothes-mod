package com.femclothes.client;

import com.femclothes.item.ClothingPatternItem;
import com.femclothes.render.GarmentFeatureRenderer;
import com.femclothes.tinturas.TinturasBlockEntity;
import com.femclothes.tinturas.TinturasScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de la Estación de Tintes — v5 (2026-09-27, capas por
 * cuadradito: "poner un color y patron al cuello otro a la manga otro al
 * pecho", "concretizar los colores seleccionados", "aplicar un patron a
 * toda la prenda pudiendo controlar como se mezclan las capas"). Mismo
 * esqueleto que {@code ModeladoScreen}:
 * <ul>
 *   <li>Izquierda: visor 3D + Vista + Nombre + Guardar diseño.</li>
 *   <li>Centro: Categoría, esquema con un slot de molde + chincheta por
 *   cuadradito (cada uno una capa: región propia, o prenda entera en los
 *   de Materiales/Personalización), cinturón Entrada->Salida, casilleros de
 *   Diseño, y los controles del cuadradito SELECCIONADO: modo de mezcla,
 *   opacidad, orden, tamaño/ángulo/posición/forma/invertir.</li>
 *   <li>Derecha: los 4 sliders CMYK del cuadradito seleccionado + su
 *   muestra de color + almacén de moldes, y abajo el panel de capas
 *   (Fase B, 2026-09-28): una fila por capa, de la de arriba a la de
 *   abajo, con ojo, muestra, nombre y ▲▼.</li>
 * </ul>
 * Mouse encima de un cuadradito (o de su fila en el panel): el visor 3D
 * apaga todo lo que no es su zona.
 * Click en un cuadradito lo selecciona; su chincheta lo fija (entra al
 * diseño que se aplica) — sin molde queda como color liso.
 */
public class TinturasScreen extends HandledScreen<TinturasScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of("femclothes", "textures/gui/container/tinturas.png");
    private static final int ANCHO = 560;
    private static final int ALTO = 408;
    private static final int M_MEDIO = TinturasScreenHandler.M_MEDIO;
    private static final int M_DERECHA = TinturasScreenHandler.M_DERECHA;
    /** Etiquetas de los 5 sliders — el quinto es la Transparencia de la tinta (2026-09-28). */
    private static final String[] NOMBRE_CANAL = { "Cyan", "Magenta", "Yellow", "Key", "Transp." };
    /** Color del líquido de cada tanque C/M/Y/K — los mismos que la Sublimadora. */
    private static final int[] COLOR_TANQUE = { 0xFF1FB3D6, 0xFFD6287F, 0xFFE8C21E, 0xFF2A2A2A };

    /** Mismos esquemas que la Modeladora para las 4 primeras; Pollera y Capa con esquema propio de Tintes. */
    private static final Identifier[] TEXTURE_ESQUEMA = {
            Identifier.of("femclothes", "textures/gui/container/esquema_remera.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_pantalon.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_medias.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_calientabrazos.png"),
            // Pollera y capa tienen esquema propio de Tintes (2026-10-04, "capa y pollera no tienen asset"):
            // posiciones en TinturasScreenHandler#posCasilla.
            Identifier.of("femclothes", "textures/gui/container/esquema_tintes_pollera.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_tintes_capa.png"),
            // Retazo de aplique (2026-10-04, fase 4): provisoriamente el esquema de la capa (3 cuadraditos).
            Identifier.of("femclothes", "textures/gui/container/esquema_tintes_capa.png"),
            // Sombrero de bruja (2026-10-04): también provisorio, el de la capa (3 cuadraditos).
            Identifier.of("femclothes", "textures/gui/container/esquema_tintes_capa.png"),
    };
    private static final int ESQUEMA_Y = TinturasScreenHandler.ESQUEMA_Y;
    private static final int ESQUEMA_ANCHO = 240, ESQUEMA_ALTO = 136;
    /** Misma chincheta que la Modeladora — 0 sin fijar, 1 animando, 2 fijada. */
    private static final Identifier[] TEXTURE_CHINCHETA = {
            Identifier.of("femclothes", "textures/gui/container/chincheta_0.png"),
            Identifier.of("femclothes", "textures/gui/container/chincheta_1.png"),
            Identifier.of("femclothes", "textures/gui/container/chincheta_2.png"),
    };

    private ButtonWidget btnCategoria;
    private ButtonWidget btnModo;
    private ButtonWidget btnOpacidad;
    private ButtonWidget btnOrden;
    private ButtonWidget btnTamano;
    private ButtonWidget btnAngulo;
    private ButtonWidget btnGiro, btnGiroAtras, btnGiroAdelante;
    private DistanciaSlider sliderDistH, sliderDistV;
    private ButtonWidget btnEspejo, btnAlternancia, btnSimetria;
    private ButtonWidget btnPosicion;
    private ButtonWidget btnForma;
    private ButtonWidget btnSemilla;
    private ButtonWidget btnColores;
    private ButtonWidget btnContorno;
    private ButtonWidget btnVariacion;
    private final BotonMuestra[] btnMuestras = new BotonMuestra[3];
    private ButtonWidget btnInvertir;
    private ButtonWidget btnVista;
    private ButtonWidget btnTenir;
    private final BotonChincheta[] btnChinchetas = new BotonChincheta[TinturasBlockEntity.CASILLAS];
    private ButtonWidget btnGuardarDiseno;
    private net.minecraft.client.gui.widget.TextFieldWidget txtNombreDiseno;
    private final BotonDiseno[] btnDisenos = new BotonDiseno[TinturasBlockEntity.DISENOS_MAXIMO];
    private final CanalSlider[] sliders = new CanalSlider[TinturasBlockEntity.CANALES];
    /** Qué cuadradito (y de qué categoría) mostraban los sliders el frame pasado — si cambió, se reposicionan. */
    private int casillaMostradaEnSliders = -1;
    /** Hasta cuándo releer los sliders del servidor (después de "Usar muestra"). */
    private long releerSlidersHasta = 0;
    private TinturasBlockEntity.Categoria categoriaMostradaEnSliders = null;
    /** Qué color (1..3) editaban los sliders el frame pasado. */
    private int editandoMostrado = -1;

    public TinturasScreen(TinturasScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = ANCHO;
        this.backgroundHeight = ALTO;
        this.titleX = M_MEDIO;
        this.titleY = 9;
        this.playerInventoryTitleX = M_MEDIO;
        this.playerInventoryTitleY = 314;
    }

    /** Botón pergamino chico con tooltip, ya agregado a la pantalla. */
    private ButtonWidget boton(int x, int y, int w, Text texto, String tooltip, int id) {
        ButtonWidget b = new EstiloPergamino.BotonPergamino(this.x + x, this.y + y, w, 16, texto, btn -> clickBoton(id));
        b.setTooltip(Tooltip.of(Text.translatable(tooltip)));
        this.addDrawableChild(b);
        return b;
    }

    @Override
    protected void init() {
        EstiloPergamino.usarTema(EstiloPergamino.Tema.VERDIN);
        super.init();

        btnCategoria = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO, this.y + 20, TinturasScreenHandler.M_MEDIO_ANCHO, 14, Text.literal(""), b -> clickBoton(TinturasBlockEntity.BTN_CATEGORIA));
        btnCategoria.setTooltip(Tooltip.of(Text.translatable("femclothes.tinturas.tooltip.categoria")));
        this.addDrawableChild(btnCategoria);

        btnVista = new EstiloPergamino.BotonPergamino(this.x + PREVIEW_X1_LOCAL, this.y + 218, 86, 16, Text.literal(""),
                b -> anguloVista = Math.floorMod(Math.round(anguloVista) + 90, 360));
        btnVista.setTooltip(Tooltip.of(Text.translatable("femclothes.preview.tooltip.vista")));
        this.addDrawableChild(btnVista);

        txtNombreDiseno = new net.minecraft.client.gui.widget.TextFieldWidget(
                this.textRenderer, this.x + 8, this.y + 238, 86, 14, Text.translatable("femclothes.tinturas.nombre_diseno"));
        txtNombreDiseno.setMaxLength(24);
        txtNombreDiseno.setPlaceholder(Text.translatable("femclothes.tinturas.nombre_diseno"));
        this.addDrawableChild(txtNombreDiseno);

        btnGuardarDiseno = new EstiloPergamino.BotonPergamino(this.x + 8, this.y + 256, 86, 16, Text.translatable("femclothes.tinturas.boton.guardar_diseno"), b -> {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new com.femclothes.tinturas.GuardarDisenoTinturasPayload(this.handler.be.getPos(), txtNombreDiseno.getText()));
            txtNombreDiseno.setText("");
        });
        btnGuardarDiseno.setTooltip(Tooltip.of(Text.translatable("femclothes.tinturas.tooltip.guardar_diseno")));
        this.addDrawableChild(btnGuardarDiseno);

        // CMYK del cuadradito seleccionado, columna DERECHA.
        for (int canal = 0; canal < TinturasBlockEntity.CANALES; canal++) {
            sliders[canal] = new CanalSlider(this.x + M_DERECHA + 6, this.y + 18 + canal * 18, 150, 14,
                    canal, handler.be.nivelBorrador(canal));
            this.addDrawableChild(sliders[canal]);
        }
        casillaMostradaEnSliders = handler.be.seleccionada();
        categoriaMostradaEnSliders = handler.be.categoria();
        editandoMostrado = handler.be.casilla(handler.be.seleccionada()).editando;

        // Fase 2 (2026-09-28): los 3 colores de la capa, arriba de la
        // muestra grande — click elige cuál editan los sliders (y prende
        // uno apagado).
        for (int i = 0; i < btnMuestras.length; i++) {
            int idx = i;
            btnMuestras[i] = new BotonMuestra(idx, this.x + M_DERECHA + 6 + i * 51, this.y + 108,
                    b -> clickBoton(TinturasBlockEntity.BTN_EDITAR_COLOR_BASE + idx));
            btnMuestras[i].setTooltip(Tooltip.of(Text.translatable("femclothes.tinturas.tooltip.muestra", idx + 1)));
            this.addDrawableChild(btnMuestras[i]);
        }
        // Debajo del almacén: cuántos colores, contorno y variación.
        btnColores = boton(M_DERECHA + 6, 226, 74, Text.empty(), "femclothes.tinturas.tooltip.colores", TinturasBlockEntity.BTN_COLORES);
        btnContorno = boton(M_DERECHA + 82, 226, 74, Text.empty(), "femclothes.tinturas.tooltip.contorno", TinturasBlockEntity.BTN_CONTORNO);
        btnVariacion = boton(M_DERECHA + 6, 246, 150, Text.empty(), "femclothes.tinturas.tooltip.variacion", TinturasBlockEntity.BTN_VARIACION);
        // Muestras de color (2026-09-30, "por si la gente se quiere pasar colores"), debajo de la muestra grande.
        boton(M_DERECHA + 6, 204, 74, Text.translatable("femclothes.tinturas.boton.envasar"),
                "femclothes.tinturas.tooltip.envasar", TinturasBlockEntity.BTN_ENVASAR);
        ButtonWidget usar = new EstiloPergamino.BotonPergamino(this.x + M_DERECHA + 82, this.y + 204, 74, 16,
                Text.translatable("femclothes.tinturas.boton.usar_muestra"), btn -> {
            clickBoton(TinturasBlockEntity.BTN_USAR_MUESTRA);
            // La mezcla nueva llega por la sincronización: los sliders se releen un rato.
            releerSlidersHasta = System.currentTimeMillis() + 800;
        });
        usar.setTooltip(Tooltip.of(Text.translatable("femclothes.tinturas.tooltip.usar_muestra")));
        this.addDrawableChild(usar);

        // Diseños guardados: mismo Y=214 que la Modeladora.
        for (int i = 0; i < btnDisenos.length; i++) {
            int idx = i;
            btnDisenos[i] = new BotonDiseno(idx, this.x + M_MEDIO + i * 20, this.y + 214, 18, 14,
                    b -> clickBoton(TinturasBlockEntity.BTN_CARGAR_DISENO_BASE + idx),
                    () -> clickBoton(TinturasBlockEntity.BTN_BORRAR_DISENO_BASE + idx));
            this.addDrawableChild(btnDisenos[i]);
        }

        // Fila 1: cómo se funde la capa — modo, opacidad, orden.
        btnModo = boton(0 + M_MEDIO, 232, 76, Text.empty(), "femclothes.tinturas.tooltip.modo", TinturasBlockEntity.BTN_MODO);
        boton(M_MEDIO + 78, 232, 14, Text.literal("<"), "femclothes.tinturas.tooltip.opacidad", TinturasBlockEntity.BTN_OPACIDAD_ATRAS);
        btnOpacidad = boton(M_MEDIO + 93, 232, 44, Text.empty(), "femclothes.tinturas.tooltip.opacidad", TinturasBlockEntity.BTN_OPACIDAD);
        boton(M_MEDIO + 138, 232, 14, Text.literal(">"), "femclothes.tinturas.tooltip.opacidad", TinturasBlockEntity.BTN_OPACIDAD);
        boton(M_MEDIO + 156, 232, 14, Text.literal("▼"), "femclothes.tinturas.tooltip.bajar", TinturasBlockEntity.BTN_BAJAR);
        btnOrden = boton(M_MEDIO + 171, 232, 50, Text.empty(), "femclothes.tinturas.tooltip.orden", TinturasBlockEntity.BTN_SUBIR);
        boton(M_MEDIO + 222, 232, 14, Text.literal("▲"), "femclothes.tinturas.tooltip.subir", TinturasBlockEntity.BTN_SUBIR);

        // Fila 2: tamaño, ángulo, posición del patrón.
        btnTamano = boton(M_MEDIO, 252, 60, Text.empty(), "femclothes.tinturas.tooltip.tamano", TinturasBlockEntity.BTN_TAMANO);
        boton(M_MEDIO + 62, 252, 14, Text.literal("<"), "femclothes.tinturas.tooltip.angulo", TinturasBlockEntity.BTN_ANGULO_ATRAS);
        btnAngulo = boton(M_MEDIO + 77, 252, 50, Text.empty(), "femclothes.tinturas.tooltip.angulo", TinturasBlockEntity.BTN_ANGULO);
        boton(M_MEDIO + 128, 252, 14, Text.literal(">"), "femclothes.tinturas.tooltip.angulo", TinturasBlockEntity.BTN_ANGULO);
        boton(M_MEDIO + 144, 252, 14, Text.literal("<"), "femclothes.tinturas.tooltip.posicion", TinturasBlockEntity.BTN_POSICION_ATRAS);
        btnPosicion = boton(M_MEDIO + 159, 252, 62, Text.empty(), "femclothes.tinturas.tooltip.posicion", TinturasBlockEntity.BTN_POSICION);
        boton(M_MEDIO + 222, 252, 14, Text.literal(">"), "femclothes.tinturas.tooltip.posicion", TinturasBlockEntity.BTN_POSICION);

        // Fila 3: forma e invertir.
        btnForma = boton(M_MEDIO, 272, 84, Text.empty(), "femclothes.tinturas.tooltip.forma", TinturasBlockEntity.BTN_FORMA);
        // Otra semilla para Repetición: Disperso (2026-09-28, motivos).
        btnSemilla = boton(M_MEDIO + 86, 272, 32, Text.translatable("femclothes.tinturas.boton.semilla"),
                "femclothes.tinturas.tooltip.semilla", TinturasBlockEntity.BTN_SEMILLA);
        btnInvertir = boton(M_MEDIO + 120, 272, 116, Text.empty(), "femclothes.tinturas.tooltip.invertir", TinturasBlockEntity.BTN_INVERTIR);

        // Fila 4 (2026-09-30, "que los patrones si se puedan girar y que haya
        // un slider vertical y horizontal para ponerlos mas juntos"): giro de
        // cada motivo en su lugar (el Ángulo de arriba gira la grilla) y la
        // distancia horizontal/vertical.
        btnGiroAtras = boton(M_MEDIO, 292, 14, Text.literal("<"), "femclothes.tinturas.tooltip.giro_motivo", TinturasBlockEntity.BTN_GIRO_MOTIVO_ATRAS);
        btnGiro = boton(M_MEDIO + 15, 292, 48, Text.empty(), "femclothes.tinturas.tooltip.giro_motivo", TinturasBlockEntity.BTN_GIRO_MOTIVO);
        btnGiroAdelante = boton(M_MEDIO + 64, 292, 14, Text.literal(">"), "femclothes.tinturas.tooltip.giro_motivo", TinturasBlockEntity.BTN_GIRO_MOTIVO);
        TinturasBlockEntity.Casilla inicial = handler.be.casilla(handler.be.seleccionada());
        sliderDistH = new DistanciaSlider(this.x + M_MEDIO + 80, this.y + 292, 77, 16, true, inicial.distanciaH);
        sliderDistV = new DistanciaSlider(this.x + M_MEDIO + 159, this.y + 292, 77, 16, false, inicial.distanciaV);
        this.addDrawableChild(sliderDistH);
        this.addDrawableChild(sliderDistV);

        // Espejos (2026-09-30, "vamos con todo"): a la derecha del inventario,
        // en la columna del medio (el hueco entre la fila de 9 y la línea de puntos).
        btnEspejo = boton(M_MEDIO + 166, 324, 72, Text.empty(), "femclothes.tinturas.tooltip.espejo", TinturasBlockEntity.BTN_ESPEJO);
        btnAlternancia = boton(M_MEDIO + 166, 344, 72, Text.empty(), "femclothes.tinturas.tooltip.alternancia", TinturasBlockEntity.BTN_ALTERNANCIA);
        btnSimetria = boton(M_MEDIO + 166, 364, 72, Text.empty(), "femclothes.tinturas.tooltip.simetria", TinturasBlockEntity.BTN_SIMETRIA);

        // Teñir, ENCIMA de la flecha Entrada->Salida (2026-09-28, "la unica
        // forma de activacion de la maquina es saliendo de la gui o
        // poniendo la prenda arriba"): arranca la prenda que ya está en la
        // Entrada, p. ej. si se puso antes de fijar el diseño.
        btnTenir = boton(M_MEDIO + 96, 184, 48, Text.translatable("femclothes.tinturas.boton.tenir"),
                "femclothes.tinturas.tooltip.tenir", TinturasBlockEntity.BTN_TENIR);

        // Una chincheta por cuadradito — se dibujan a mano en render(),
        // encima del ícono fantasma (mismo criterio que ModeladoScreen).
        for (int i = 0; i < btnChinchetas.length; i++) {
            int idx = i;
            btnChinchetas[i] = new BotonChincheta(b -> clickBoton(TinturasBlockEntity.BTN_CHINCHETA_BASE + idx));
            this.addSelectableChild(btnChinchetas[i]);
        }
    }

    private void clickBoton(int id) {
        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

    // ── visor 3D: arrastrar para girar, ruedita para zoom ──
    private static final int PREVIEW_X1_LOCAL = 8, PREVIEW_Y1_LOCAL = 18, PREVIEW_X2_LOCAL = 94, PREVIEW_Y2_LOCAL = 214;
    private float anguloVista = 0f;
    private float zoomVista = 1f;
    private static final float ZOOM_MIN = 0.5f, ZOOM_MAX = 2.5f;
    private boolean arrastrandoPreview = false;

    private boolean dentroDePreview(double mouseX, double mouseY) {
        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickEnPanel(mouseX, mouseY)) return true;
        if (button == 0 && dentroDePreview(mouseX, mouseY)) {
            arrastrandoPreview = true;
        }
        // Click (cualquier botón) sobre un cuadradito lo SELECCIONA, además
        // de lo que haga el slot con el molde (poner/sacar) — así elegir qué
        // capa editan los sliders es tocar el cuadradito, sin botón aparte.
        net.minecraft.screen.slot.Slot slot = this.focusedSlot;
        if (slot instanceof TinturasScreenHandler.CasillaSlot casilla && casilla.isEnabled()
                && casilla.casilla != handler.be.seleccionada()) {
            clickBoton(TinturasBlockEntity.BTN_SELECCIONAR_BASE + casilla.casilla);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) arrastrandoPreview = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (arrastrandoPreview) {
            anguloVista = Math.floorMod(Math.round(anguloVista + (float) deltaX * 1.15f), 360);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (dentroDePreview(mouseX, mouseY)) {
            zoomVista = net.minecraft.util.math.MathHelper.clamp(
                    zoomVista + (float) verticalAmount * 0.1f, ZOOM_MIN, ZOOM_MAX);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /** Mismo motivo que {@code ModeladoScreen#keyPressed}: sin esto, escribir una "e" en {@link #txtNombreDiseno} cierra la pantalla. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return (!txtNombreDiseno.keyPressed(keyCode, scanCode, modifiers) && !txtNombreDiseno.isActive())
                ? super.keyPressed(keyCode, scanCode, modifiers) : true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refrescar();
        super.render(context, mouseX, mouseY, delta);
        dibujarPreview(context, mouseX, mouseY, casillaResaltada(mouseX, mouseY));
        // Chinchetas a mano, DESPUÉS de los slots/ítems/fantasmas (z=400
        // adentro de su renderWidget) — mismo criterio que ModeladoScreen.
        for (BotonChincheta b : btnChinchetas) {
            if (b.visible) b.render(context, mouseX, mouseY, delta);
        }
        drawMouseoverTooltip(context, mouseX, mouseY);
        tooltipPanel(context, mouseX, mouseY);
    }

    /**
     * Qué cuadradito resaltar en 3D (Fase B, 2026-09-28): el de la fila
     * del panel con el mouse encima, o el del esquema (slot o chincheta).
     * -1 = ninguno.
     */
    private int casillaResaltada(int mouseX, int mouseY) {
        int[] fila = filaBajoMouse(mouseX, mouseY);
        if (fila != null) return fila[0];
        if (this.focusedSlot instanceof TinturasScreenHandler.CasillaSlot casilla && casilla.isEnabled()) {
            return casilla.casilla;
        }
        for (int i = 0; i < btnChinchetas.length; i++) {
            if (btnChinchetas[i].visible && btnChinchetas[i].isMouseOver(mouseX, mouseY)) return i;
        }
        return -1;
    }

    private void dibujarPreview(DrawContext context, int mouseX, int mouseY, int resaltada) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity jugador = client.player;
        if (jugador == null) return;

        if (handler.be.categoria() == TinturasBlockEntity.Categoria.APLIQUE
                || handler.be.categoria() == TinturasBlockEntity.Categoria.SOMBRERO) {
            dibujarMuestrasRetazo(context, handler.be.prendaDeVistaPrevia(resaltada));
            return;
        }
        ItemStack prenda = handler.be.prendaDeVistaPrevia(resaltada);
        List<ItemStack> prendas = new ArrayList<>(GarmentFeatureRenderer.equipadas(jugador));
        if (!prenda.isEmpty()) {
            prendas.removeIf(s -> s.getItem().getClass() == prenda.getItem().getClass());
            prendas.add(prenda);
        }

        GarmentFeatureRenderer.previewOverride = prendas;
        try {
            int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
            int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
            PreviewJugador.dibujar(context, jugador, x1, y1, x2, y2, Math.round(35 * zoomVista), anguloVista, (float) mouseY);
        } finally {
            GarmentFeatureRenderer.previewOverride = null;
        }
    }

    /** Vista previa del retazo de aplique (2026-10-04, fase 4): el ítem grande y las 3 zonas con su color. */
    private void dibujarMuestrasRetazo(DrawContext context, ItemStack retazo) {
        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        int w = x2 - x1, h = y2 - y1;
        var m = context.getMatrices();
        m.push();
        m.translate(x1 + w / 2f - 24, y1 + 6, 0);
        m.scale(3f, 3f, 1f);
        context.drawItem(retazo, 0, 0);
        m.pop();
        boolean sombrero = retazo.getItem() instanceof com.femclothes.item.SombreroBrujaItem;
        List<Integer> colores = sombrero ? com.femclothes.item.SombreroBrujaItem.colores(retazo)
                : com.femclothes.aplique.RetazoApliqueItem.colores(retazo);
        int alto = Math.min(26, (h - 70) / 3);
        for (int i = 0; i < 3; i++) {
            int ty = y1 + 62 + i * (alto + 4);
            context.fill(x1 + 6, ty - 1, x2 - 6, ty + alto + 1, 0xFF2A180C);
            context.fill(x1 + 7, ty, x2 - 7, ty + alto, 0xFF000000 | colores.get(i));
            context.drawText(this.textRenderer, sombrero ? Text.translatable("femclothes.sombrero.zona." + (i + 1))
                            : Text.translatable("femclothes.aplique.zona", i + 1),
                    x1 + 10, ty + alto / 2 - 4, 0xFFFFFFFF, true);
        }
    }

    private void refrescar() {
        TinturasBlockEntity be = handler.be;
        TinturasBlockEntity.Categoria cat = be.categoria();
        int sel = be.seleccionada();
        TinturasBlockEntity.Casilla casilla = be.casilla(sel);

        btnCategoria.setMessage(Text.translatable("femclothes.modelado.categoria",
                Text.translatable("femclothes.modelado.categoria." + cat.name().toLowerCase(java.util.Locale.ROOT))));
        btnModo.setMessage(Text.translatable(casilla.modo.traduccion()));
        btnOpacidad.setMessage(Text.literal(casilla.opacidad + "%"));
        int pos = be.posicionEnOrden(sel);
        btnOrden.setMessage(Text.translatable("femclothes.tinturas.orden", pos, be.capasEnBorrador()));
        btnTamano.setMessage(Text.translatable(casilla.tamano.traduccion()));
        btnAngulo.setMessage(Text.translatable("femclothes.tinturas.angulo", Math.round(casilla.angulo)));
        btnPosicion.setMessage(Text.translatable("femclothes.tinturas.posicion", Math.round(casilla.posicion * 100)));
        btnGiro.setMessage(Text.translatable("femclothes.tinturas.giro_motivo", Math.round(casilla.giroMotivo)));
        // Molde de motivo: el botón muestra/cicla la Repetición; de rayas, la Forma.
        com.femclothes.render.Motivo motivo = be.motivoSeleccionado();
        if (motivo != null) {
            btnForma.setMessage(Text.translatable(casilla.repeticion.traduccion()));
            btnForma.setTooltip(Tooltip.of(Text.translatable("femclothes.tinturas.tooltip.repeticion")));
        } else {
            btnForma.setMessage(Text.translatable(be.formaBorrador().traduccion()));
            btnForma.setTooltip(Tooltip.of(Text.translatable("femclothes.tinturas.tooltip.forma")));
        }
        // Azar también sirve para Variación: Aleatorio (re-sortea los colores).
        // El giro de cada motivo no tiene sentido en rayas ni en el vichy (simétrico).
        boolean giraMotivo = motivo != null && !motivo.esProcedural();
        btnGiro.active = btnGiroAtras.active = btnGiroAdelante.active = giraMotivo;
        btnEspejo.active = btnAlternancia.active = giraMotivo;
        btnEspejo.setMessage(Text.translatable(casilla.espejo.traduccion()));
        btnAlternancia.setMessage(Text.translatable(casilla.alternancia.traduccion()));
        btnSimetria.setMessage(Text.translatable("femclothes.tinturas.simetria",
                Text.translatable(casilla.simetria ? "femclothes.si" : "femclothes.no")));
        btnSemilla.active = (motivo != null && casilla.repeticion == com.femclothes.render.Repeticion.DISPERSO)
                || (casilla.variacion == com.femclothes.render.Variacion.ALEATORIO && casilla.colores > 1);

        for (int i = 0; i < btnMuestras.length; i++) {
            btnMuestras[i].color = casilla.colorDe(i);
            btnMuestras[i].prendida = i < casilla.colores;
            btnMuestras[i].editando = i == casilla.editando;
            btnMuestras[i].contorno = casilla.contorno && casilla.colores > 1 && i == casilla.colores - 1;
        }
        btnColores.setMessage(Text.translatable("femclothes.tinturas.colores", casilla.colores));
        btnContorno.setMessage(Text.translatable("femclothes.tinturas.contorno",
                Text.translatable(casilla.contorno ? "femclothes.si" : "femclothes.no")));
        btnContorno.active = casilla.colores > 1;
        btnVariacion.setMessage(Text.translatable(casilla.variacion.traduccion()));
        btnInvertir.setMessage(Text.translatable("femclothes.tinturas.invertir",
                Text.translatable(casilla.invertido ? "femclothes.si" : "femclothes.no")));
        btnVista.setMessage(Text.translatable("femclothes.preview.vista",
                Text.translatable(PreviewJugador.nombreVista(anguloVista))));

        // Los sliders muestran la mezcla del cuadradito SELECCIONADO — si
        // cambió (otro cuadradito u otra categoría), se reposicionan a mano.
        if (sel != casillaMostradaEnSliders || cat != categoriaMostradaEnSliders || casilla.editando != editandoMostrado
                || System.currentTimeMillis() < releerSlidersHasta) {
            casillaMostradaEnSliders = sel;
            categoriaMostradaEnSliders = cat;
            editandoMostrado = casilla.editando;
            for (int canal = 0; canal < TinturasBlockEntity.CANALES; canal++) {
                sliders[canal].sincronizarDesdeServidor(be.nivelBorrador(canal));
            }
            sliderDistH.sincronizar(casilla.distanciaH);
            sliderDistV.sincronizar(casilla.distanciaV);
        }

        for (int i = 0; i < btnChinchetas.length; i++) {
            BotonChincheta b = btnChinchetas[i];
            com.femclothes.region.RegionPintura region = TinturasBlockEntity.regionDe(cat, i);
            b.visible = region != null;
            b.active = region != null;
            if (region == null) continue;
            int[] c = TinturasScreenHandler.posChincheta(cat, i);
            b.setPosition(this.x + c[0] - 8, this.y + c[1] - 8);
            TinturasBlockEntity.Casilla cas = be.casilla(i);
            b.actualizar(cas.fijada);
            boolean conMolde = !be.getStack(TinturasBlockEntity.casillaSlot(cat, i)).isEmpty();
            String clave = conMolde ? "femclothes.tinturas.tooltip.chincheta_molde"
                    : cas.fijada ? "femclothes.tinturas.tooltip.chincheta_quitar"
                    : "femclothes.tinturas.tooltip.chincheta_liso";
            b.setTooltip(Tooltip.of(Text.translatable(clave, Text.translatable(region.traduccion()))));
        }

        btnTenir.active = be.puedeReintentar();

        int guardados = be.disenosGuardados();
        btnGuardarDiseno.active = be.hayFijadas() && guardados < TinturasBlockEntity.DISENOS_MAXIMO;
        for (int i = 0; i < btnDisenos.length; i++) {
            String nombre = be.nombreDiseno(i);
            btnDisenos[i].active = nombre != null;
            btnDisenos[i].setTooltip(nombre == null ? null : Tooltip.of(Text.translatable(
                    "femclothes.tinturas.tooltip.casillero_diseno", nombre)));
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX,
                this.playerInventoryTitleY, EstiloPergamino.TEXTO, false);

        dibujarCasillas(context);

        TinturasBlockEntity be = handler.be;
        // Qué se está editando: región del cuadradito seleccionado y su estado.
        com.femclothes.region.RegionPintura region = TinturasBlockEntity.regionDe(be.categoria(), be.seleccionada());
        if (region != null) {
            TinturasBlockEntity.Casilla sel = be.casilla(be.seleccionada());
            Text estado = Text.translatable(sel.oculta ? "femclothes.tinturas.estado.oculta"
                    : sel.fijada ? "femclothes.tinturas.estado.fijada" : "femclothes.tinturas.estado.borrador");
            context.drawText(this.textRenderer, Text.translatable("femclothes.tinturas.editando",
                    Text.translatable(region.traduccion()), estado), M_MEDIO, 296, EstiloPergamino.TEXTO, false);
        }

        // Muestra del color CRUDO del cuadradito seleccionado.
        int x0 = M_DERECHA + 6, y0 = 132, x1 = M_DERECHA + 156, y1 = 180;
        dibujarMuestra(context, x0, y0, x1, y1, be.colorBorrador());
        context.drawBorder(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, 0xFF2A180C);

        context.drawText(this.textRenderer, hint(), M_DERECHA + 6, 190, EstiloPergamino.TEXTO, false);

        dibujarPanelCapas(context, mouseX, mouseY);
    }

    // ── panel de capas (Fase B, 2026-09-28): "lista con ojo para ocultar,
    // subir/bajar, click selecciona", columna derecha abajo ──
    private static final int PANEL_X = M_DERECHA + 6, PANEL_Y = 268, PANEL_ANCHO = 150;
    private static final int FILA_Y0 = PANEL_Y + 11, FILA_ALTO = 10;
    /** Zonas de una fila, en x local relativa a PANEL_X. */
    private static final int OJO_X1 = 11, SUBIR_X0 = PANEL_ANCHO - 20, BAJAR_X0 = PANEL_ANCHO - 10;
    private static final int ZONA_OJO = 0, ZONA_NOMBRE = 1, ZONA_SUBIR = 2, ZONA_BAJAR = 3;

    /** {cuadradito, zona} de la fila con el mouse encima, o null. */
    @org.jetbrains.annotations.Nullable
    private int[] filaBajoMouse(double mouseX, double mouseY) {
        double lx = mouseX - this.x - PANEL_X, ly = mouseY - this.y - FILA_Y0;
        if (lx < 0 || lx >= PANEL_ANCHO || ly < 0) return null;
        List<Integer> filas = handler.be.capasDelPanel();
        int f = (int) (ly / FILA_ALTO);
        if (f >= filas.size()) return null;
        int zona = lx < OJO_X1 ? ZONA_OJO : lx >= BAJAR_X0 ? ZONA_BAJAR : lx >= SUBIR_X0 ? ZONA_SUBIR : ZONA_NOMBRE;
        return new int[]{filas.get(f), zona};
    }

    private boolean clickEnPanel(double mouseX, double mouseY) {
        int[] fila = filaBajoMouse(mouseX, mouseY);
        if (fila == null) return false;
        int i = fila[0];
        int id = switch (fila[1]) {
            case ZONA_OJO -> TinturasBlockEntity.BTN_OJO_BASE + i;
            case ZONA_SUBIR -> TinturasBlockEntity.BTN_SUBIR_BASE + i;
            case ZONA_BAJAR -> TinturasBlockEntity.BTN_BAJAR_BASE + i;
            default -> i == handler.be.seleccionada() ? -1 : TinturasBlockEntity.BTN_SELECCIONAR_BASE + i;
        };
        if (id >= 0) {
            clickBoton(id);
            this.client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                    net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
        return true;
    }

    /** Coordenadas LOCALES (drawForeground ya está corrido a this.x/this.y). */
    private void dibujarPanelCapas(DrawContext c, int mouseX, int mouseY) {
        TinturasBlockEntity be = handler.be;
        TinturasBlockEntity.Categoria cat = be.categoria();
        c.drawText(this.textRenderer, Text.translatable("femclothes.tinturas.capas"), PANEL_X, PANEL_Y, EstiloPergamino.TEXTO, false);
        c.fill(PANEL_X, PANEL_Y + 9, PANEL_X + PANEL_ANCHO, PANEL_Y + 10, 0xFF6B4E2A);

        List<Integer> filas = be.capasDelPanel();
        if (filas.isEmpty()) {
            c.drawText(this.textRenderer, Text.translatable("femclothes.tinturas.capas.vacio"),
                    PANEL_X, FILA_Y0 + 1, EstiloPergamino.TEXTO_APAGADO, false);
            return;
        }
        int[] bajo = filaBajoMouse(mouseX, mouseY);
        for (int f = 0; f < filas.size(); f++) {
            int i = filas.get(f);
            TinturasBlockEntity.Casilla cas = be.casilla(i);
            int y = FILA_Y0 + f * FILA_ALTO;
            int x0 = PANEL_X, x1 = PANEL_X + PANEL_ANCHO;
            boolean hover = bajo != null && bajo[0] == i;
            if (i == be.seleccionada()) {
                c.fill(x0, y, x1, y + FILA_ALTO, 0x40FFD24C);
                c.drawBorder(x0, y, PANEL_ANCHO, FILA_ALTO, 0xFFC79A4B);
            } else if (hover) {
                c.fill(x0, y, x1, y + FILA_ALTO, 0x30FFFFFF);
            }

            dibujarOjo(c, x0 + 1, y + 1, !cas.oculta, hover && bajo[1] == ZONA_OJO);

            // Muestra: una franja por color de la capa.
            int mx0 = x0 + 12, mx1 = x0 + 21;
            int n = Math.max(1, cas.colores);
            for (int k = 0; k < n; k++) {
                int a = mx0 + (mx1 - mx0) * k / n, b = mx0 + (mx1 - mx0) * (k + 1) / n;
                dibujarMuestra(c, a, y + 1, b, y + 9, cas.colorDe(k));
            }
            c.drawBorder(mx0 - 1, y, mx1 - mx0 + 2, 10, 0xFF2A180C);
            if (cas.oculta) c.fill(mx0, y + 1, mx1, y + 9, 0xA0D9B98A);

            // Nombre: zona · patrón (o "liso"); sin fijar va en cursiva.
            com.femclothes.region.RegionPintura region = TinturasBlockEntity.regionDe(cat, i);
            Identifier patron = be.patronEnBorrador(cat, i);
            ClothingPatternItem item = patron == null ? null : ClothingPatternItem.porId(patron);
            Text detalle = item != null ? item.getName() : Text.translatable("femclothes.tinturas.capas.liso");
            net.minecraft.text.MutableText nombre = Text.empty()
                    .append(region == null ? Text.empty() : Text.translatable(region.traduccion()))
                    .append(" · ").append(detalle);
            if (!cas.fijada) nombre = nombre.formatted(net.minecraft.util.Formatting.ITALIC);
            int anchoNombre = SUBIR_X0 - 24;
            net.minecraft.text.StringVisitable recortado = this.textRenderer.trimToWidth(nombre, anchoNombre);
            c.drawText(this.textRenderer, net.minecraft.util.Language.getInstance().reorder(recortado),
                    x0 + 23, y + 1, cas.oculta ? EstiloPergamino.TEXTO_APAGADO : EstiloPergamino.TEXTO, false);

            boolean puedeSubir = f > 0, puedeBajar = f < filas.size() - 1;
            dibujarFlecha(c, "▲", x0 + SUBIR_X0, y, puedeSubir, hover && bajo[1] == ZONA_SUBIR);
            dibujarFlecha(c, "▼", x0 + BAJAR_X0, y, puedeBajar, hover && bajo[1] == ZONA_BAJAR);
        }
    }

    private void dibujarFlecha(DrawContext c, String flecha, int x, int y, boolean activa, boolean hover) {
        if (hover && activa) c.fill(x, y, x + 10, y + FILA_ALTO, 0x40FFFFFF);
        c.drawText(this.textRenderer, flecha, x + 2, y + 1,
                activa ? (hover ? 0xFF6B4E2A : EstiloPergamino.TEXTO) : EstiloPergamino.TEXTO_APAGADO, false);
    }

    /** Ojo de 9x7 a mano (la fuente no trae uno): abierto con pupila, o cerrado con pestañas. */
    private static void dibujarOjo(DrawContext c, int x, int y, boolean abierto, boolean hover) {
        int col = hover ? 0xFFC79A4B : 0xFF3B2410;
        if (abierto) {
            c.fill(x, y + 3, x + 1, y + 4, col);
            c.fill(x + 1, y + 2, x + 2, y + 3, col);
            c.fill(x + 1, y + 4, x + 2, y + 5, col);
            c.fill(x + 2, y + 1, x + 7, y + 2, col);
            c.fill(x + 2, y + 5, x + 7, y + 6, col);
            c.fill(x + 7, y + 2, x + 8, y + 3, col);
            c.fill(x + 7, y + 4, x + 8, y + 5, col);
            c.fill(x + 8, y + 3, x + 9, y + 4, col);
            c.fill(x + 3, y + 2, x + 6, y + 5, col);
        } else {
            c.fill(x, y + 3, x + 9, y + 4, col);
            c.fill(x + 1, y + 4, x + 2, y + 5, col);
            c.fill(x + 4, y + 4, x + 5, y + 6, col);
            c.fill(x + 7, y + 4, x + 8, y + 5, col);
        }
    }

    private void tooltipPanel(DrawContext context, int mouseX, int mouseY) {
        if (this.focusedSlot != null && this.focusedSlot.hasStack()) return;
        int[] fila = filaBajoMouse(mouseX, mouseY);
        if (fila == null) return;
        String clave = switch (fila[1]) {
            case ZONA_OJO -> handler.be.casilla(fila[0]).oculta
                    ? "femclothes.tinturas.tooltip.ojo_mostrar" : "femclothes.tinturas.tooltip.ojo_ocultar";
            case ZONA_SUBIR -> "femclothes.tinturas.tooltip.subir";
            case ZONA_BAJAR -> "femclothes.tinturas.tooltip.bajar";
            default -> null;
        };
        if (clave != null) context.drawTooltip(this.textRenderer, Text.translatable(clave), mouseX, mouseY);
    }

    /**
     * Estado de cada cuadradito sobre el esquema (coordenadas locales):
     * marco del color de su capa si entra en la vista previa, marco dorado
     * grueso en el SELECCIONADO, e ícono fantasma del patrón fijado cuando
     * el molde ya volvió al almacén (mismo velo que la Modeladora).
     */
    private void dibujarCasillas(DrawContext context) {
        TinturasBlockEntity be = handler.be;
        TinturasBlockEntity.Categoria cat = be.categoria();
        for (int i = 0; i < TinturasBlockEntity.CASILLAS; i++) {
            if (TinturasBlockEntity.regionDe(cat, i) == null) continue;
            int[] p = TinturasScreenHandler.posCasilla(cat, i);
            int x = p[0], y = p[1];
            TinturasBlockEntity.Casilla cas = be.casilla(i);
            if (be.enBorrador(cat, i)) {
                // Tira del color de la capa debajo del slot: se lee de un
                // vistazo qué color va en cada parte del dibujo.
                int c = 0xFF000000 | cas.color();
                context.fill(x - 1, y + 17, x + 17, y + 20, c);
                context.drawBorder(x - 2, y + 16, 20, 5, 0xFF2A180C);
            }
            if (i == be.seleccionada()) {
                context.drawBorder(x - 2, y - 2, 20, 20, 0xFFFFD24C);
                context.drawBorder(x - 3, y - 3, 22, 22, 0xFF6B4E2A);
            }
            if (cas.fijada && cas.patronFijado != null && be.getStack(TinturasBlockEntity.casillaSlot(cat, i)).isEmpty()) {
                ClothingPatternItem item = ClothingPatternItem.porId(cas.patronFijado);
                if (item != null) {
                    context.drawItem(new ItemStack(item), x, y);
                    context.getMatrices().push();
                    context.getMatrices().translate(0, 0, 200);
                    context.fill(x, y, x + 16, y + 16, 0x5CD9B98A);
                    context.getMatrices().pop();
                }
            }
        }
    }

    /**
     * Rellena una muestra con un color de la mezcla: si tiene
     * Transparencia (byte alto, 2026-09-28), primero un fondo a cuadros y
     * encima el color con ese alfa — así se ve cuánto calará la tela.
     */
    static void dibujarMuestra(DrawContext c, int x0, int y0, int x1, int y1, int color) {
        int t = (color >>> 24) & 0xFF;
        if (t > 0) {
            for (int y = y0; y < y1; y += 4) {
                for (int x = x0; x < x1; x += 4) {
                    boolean claro = ((x - x0) / 4 + (y - y0) / 4) % 2 == 0;
                    c.fill(x, y, Math.min(x + 4, x1), Math.min(y + 4, y1), claro ? 0xFFE0E0E0 : 0xFF9A9A9A);
                }
            }
        }
        c.fill(x0, y0, x1, y1, ((255 - t) << 24) | (color & 0xFFFFFF));
    }

    private Text hint() {
        TinturasBlockEntity be = handler.be;
        if (be.estado() == TinturasBlockEntity.Estado.TINIENDO) {
            return Text.translatable("femclothes.tinturas.hint.tiniendo");
        }
        if (!be.getSalida().isEmpty()) {
            return Text.translatable("femclothes.tinturas.hint.listo");
        }
        if (!be.hayFijadas()) {
            return Text.translatable("femclothes.tinturas.hint.vacio");
        }
        return Text.translatable("femclothes.tinturas.hint.fijado");
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
        dibujarEsquema(context);
    }

    /** Esquema de la prenda actual — mismos PNG y coordenadas que {@code ModeladoScreen#dibujarEsquema}; Pollera y Capa tienen los suyos. */
    private void dibujarEsquema(DrawContext context) {
        int cat = this.handler.be.categoria().ordinal();
        if (cat >= TEXTURE_ESQUEMA.length) return;
        int ex = this.x + M_MEDIO, ey = this.y + ESQUEMA_Y;
        context.drawTexture(TEXTURE_ESQUEMA[cat], ex, ey, 0, 0, ESQUEMA_ANCHO, ESQUEMA_ALTO, ESQUEMA_ANCHO, ESQUEMA_ALTO);
    }

    /** Entrada/Salida grandes: mismo truco de escala 1.5x que {@code ModeladoScreen#esSlotGrande}. */
    private static boolean esSlotGrande(int localX, int localY) {
        return localY == TinturasScreenHandler.SLOT_Y_IO
                && (localX == M_MEDIO + TinturasScreenHandler.ENTRADA_X || localX == M_MEDIO + TinturasScreenHandler.SALIDA_X);
    }

    @Override
    protected void drawSlot(DrawContext context, net.minecraft.screen.slot.Slot slot) {
        if (!esSlotGrande(slot.x, slot.y)) {
            super.drawSlot(context, slot);
            return;
        }
        context.getMatrices().push();
        context.getMatrices().translate(slot.x + 8, slot.y + 8, 0);
        context.getMatrices().scale(1.5f, 1.5f, 1f);
        context.getMatrices().translate(-(slot.x + 8), -(slot.y + 8), 0);
        super.drawSlot(context, slot);
        context.getMatrices().pop();
    }

    @Override
    protected boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
        if (width == 16 && height == 16 && esSlotGrande(x, y)) {
            return super.isPointWithinBounds(x - 8, y - 8, 32, 32, pointX, pointY);
        }
        return super.isPointWithinBounds(x, y, width, height, pointX, pointY);
    }

    /**
     * Distancia horizontal o vertical del patrón (2026-09-30): de pegados
     * (50 %) al triple (300 %), de a 5 %. Mismo mecanismo que los CMYK:
     * cuantiza y solo manda el click cuando cambia el escalón.
     */
    private class DistanciaSlider extends SliderWidget {
        private final boolean horizontal;
        private int ultimoNivelEnviado;

        DistanciaSlider(int x, int y, int width, int height, boolean horizontal, float distancia) {
            super(x, y, width, height, Text.empty(), 0);
            this.horizontal = horizontal;
            sincronizar(distancia);
            setTooltip(Tooltip.of(Text.translatable(horizontal
                    ? "femclothes.tinturas.tooltip.distancia_h" : "femclothes.tinturas.tooltip.distancia_v")));
        }

        private int nivel() {
            return Math.round((float) (this.value * (com.femclothes.render.DistribucionPatron.NIVELES - 1)));
        }

        void sincronizar(float distancia) {
            int nivel = com.femclothes.render.DistribucionPatron.nivelDeDistancia(distancia);
            this.value = nivel / (double) (com.femclothes.render.DistribucionPatron.NIVELES - 1);
            this.ultimoNivelEnviado = nivel;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int porcentaje = Math.round(com.femclothes.render.DistribucionPatron.distanciaDeNivel(nivel()) * 100);
            this.setMessage(Text.translatable(horizontal
                    ? "femclothes.tinturas.distancia_h" : "femclothes.tinturas.distancia_v", porcentaje));
        }

        @Override
        protected void applyValue() {
            int nivel = nivel();
            if (nivel == ultimoNivelEnviado) return;
            ultimoNivelEnviado = nivel;
            clickBoton((horizontal ? TinturasBlockEntity.BTN_DISTANCIA_H_BASE : TinturasBlockEntity.BTN_DISTANCIA_V_BASE) + nivel);
        }
    }

    /** Un slider C/M/Y/K — cuantiza a NIVELES_MEZCLA pasos y solo manda el click cuando ese nivel cambia. */
    private class CanalSlider extends SliderWidget {
        private final int canal;
        private int ultimoNivelEnviado;

        CanalSlider(int x, int y, int width, int height, int canal, int nivelInicial) {
            super(x, y, width, height, Text.empty(), nivelInicial / (double) (TinturasBlockEntity.NIVELES_MEZCLA - 1));
            this.canal = canal;
            this.ultimoNivelEnviado = nivelInicial;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int nivel = Math.round((float) (this.value * (TinturasBlockEntity.NIVELES_MEZCLA - 1)));
            int porcentaje = Math.round(nivel * 100f / (TinturasBlockEntity.NIVELES_MEZCLA - 1));
            this.setMessage(Text.literal(NOMBRE_CANAL[canal] + ": " + porcentaje + "%"));
        }

        @Override
        protected void applyValue() {
            int nivel = Math.round((float) (this.value * (TinturasBlockEntity.NIVELES_MEZCLA - 1)));
            if (nivel == ultimoNivelEnviado) return;
            ultimoNivelEnviado = nivel;
            clickBoton(handler.be.botonMezcla(canal, nivel));
        }

        /**
         * Slider y tanque en uno (2026-09-29, "la subli tiene indicador de
         * insumos, alguna forma de agregarsela a la tinturas fusionandola
         * de alguna manera con los sliders?"): el canal del slider es el
         * tanque — se llena con el color de la tinta según lo cargado
         * (mismas barras que la Sublimadora) y a la derecha dice "n/64", en
         * rojo si está vacío. El mango de madera marca la mezcla. La
         * Transparencia no gasta tinta: canal vacío, sin número.
         */
        @Override
        public void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            int x0 = getX(), y0 = getY(), w = getWidth(), h = getHeight();
            c.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, 0xFF2A180C);
            c.fill(x0, y0, x0 + w, y0 + h, 0xFF8A7556);
            boolean tanque = canal < 4;
            int carga = tanque ? handler.be.carga(canal) : 0;
            if (tanque && carga > 0) {
                int lleno = w * carga / TinturasBlockEntity.CARGA_MAXIMA;
                c.fill(x0, y0, x0 + lleno, y0 + h, COLOR_TANQUE[canal]);
                c.fill(x0, y0, x0 + lleno, y0 + 1, 0x40FFFFFF);   // brillo del líquido
            }
            // Mango: mismo recorrido que SliderWidget (8 px de ancho).
            int mx = x0 + (int) (this.value * (w - 8));
            EstiloPergamino.fondoBoton(c, mx, y0, 8, h, isHovered() || isFocused(), this.active);

            var fuente = MinecraftClient.getInstance().textRenderer;
            int ty = y0 + (h - 8) / 2;
            Text m = getMessage();
            // El texto pasa por encima del mango: sombra oscura para que se lea sobre cualquier tinta.
            c.drawText(fuente, m, x0 + 5, ty + 1, 0xFF2A180C, false);
            c.drawText(fuente, m, x0 + 4, ty, EstiloPergamino.TEXTO_CLARO, false);
            if (tanque) {
                String n = carga + "/" + TinturasBlockEntity.CARGA_MAXIMA;
                int nx = x0 + w - 4 - fuente.getWidth(n);
                c.drawText(fuente, n, nx + 1, ty + 1, 0xFF2A180C, false);
                c.drawText(fuente, n, nx, ty, carga == 0 ? 0xFFFF5A48 : EstiloPergamino.TEXTO_CLARO, false);
            }
        }

        /** Refleja el nivel del cuadradito recién seleccionado SIN mandar un click. */
        void sincronizarDesdeServidor(int nivel) {
            this.value = nivel / (double) (TinturasBlockEntity.NIVELES_MEZCLA - 1);
            this.ultimoNivelEnviado = nivel;
            updateMessage();
        }
    }

    /**
     * Una de las 3 muestras de color de la capa (Fase 2, 2026-09-28):
     * rellena con su color si está prendida (tachada si no), marco dorado
     * si es la que editan los sliders, y una "C" si es la del contorno.
     */
    private static class BotonMuestra extends ButtonWidget {
        int color;
        boolean prendida, editando, contorno;

        BotonMuestra(int indice, int x, int y, PressAction accion) {
            super(x, y, 48, 20, Text.literal(String.valueOf(indice + 1)), accion, DEFAULT_NARRATION_SUPPLIER);
        }

        @Override
        protected void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            if (prendida) {
                dibujarMuestra(c, x + 1, y + 1, x + w - 1, y + h - 1, color);
            } else {
                c.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFB9A27A);
                for (int i = 0; i < w - 2; i++) c.fill(x + 1 + i, y + 1 + i * (h - 2) / (w - 2), x + 2 + i, y + 2 + i * (h - 2) / (w - 2), 0xFF6B4E2A);
            }
            c.drawBorder(x, y, w, h, editando ? 0xFFFFD24C : (isHovered() ? 0xFFC79A4B : 0xFF2A180C));
            if (editando) c.drawBorder(x + 1, y + 1, w - 2, h - 2, 0xFFFFD24C);
            var fuente = MinecraftClient.getInstance().textRenderer;
            // Texto con sombra, se lee sobre cualquier color.
            c.drawTextWithShadow(fuente, getMessage().getString() + (contorno ? " C" : ""), x + 4, y + 6, 0xFFFFFFFF);
        }
    }

    /**
     * Chincheta de un cuadradito — misma arte y animación que
     * {@code ModeladoScreen.BotonChincheta}: hueca = no fijado, llena =
     * fijado (entra al diseño que se aplica).
     */
    static class BotonChincheta extends ButtonWidget {
        private boolean fijado;
        private long cambioMs = -1000;

        BotonChincheta(PressAction accion) {
            super(0, 0, 16, 16, Text.empty(), accion, DEFAULT_NARRATION_SUPPLIER);
        }

        void actualizar(boolean nuevo) {
            if (nuevo != fijado) cambioMs = net.minecraft.util.Util.getMeasuringTimeMs();
            fijado = nuevo;
        }

        @Override
        protected void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            boolean animando = net.minecraft.util.Util.getMeasuringTimeMs() - cambioMs < 160;
            int cuadro = animando ? 1 : fijado ? 2 : 0;
            int x = getX(), y = getY();
            c.getMatrices().push();
            c.getMatrices().translate(0, 0, 400);
            if (isHovered() && active) c.fill(x + 2, y + 2, x + 14, y + 14, 0x33FFFFFF);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            c.drawTexture(TEXTURE_CHINCHETA[cuadro], x + 2, y + 2, 0, 0, 12, 12, 12, 12);
            c.getMatrices().pop();
        }
    }
}
