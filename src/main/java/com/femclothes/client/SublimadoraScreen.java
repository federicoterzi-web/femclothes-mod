package com.femclothes.client;

import com.femclothes.render.GarmentFeatureRenderer;
import com.femclothes.sublimadora.SublimadoraBlockEntity;
import com.femclothes.sublimadora.SublimadoraScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de escala/posición/cara de la Sublimadora — a pedido
 * (2026-09-19, "hace gui con preview con escalado y flechitas para
 * posicion y asi sacamos los controles del frente"). Mismo esqueleto que
 * {@link TinturasScreen} (preview 3D a la izquierda, controles al medio,
 * flechitas ‹ › en vez de un solo botón que solo avanza): acá hay 2 slots
 * de FOTO (Frente/Espalda) + desde 2026-09-21 uno de entrada y uno de
 * salida para la remera (ver {@code SublimadoraScreenHandler}) — el
 * click derecho directo sobre el bloque sigue andando igual para las dos
 * cosas. Desde 2026-09-21 ("slots") también tiene columna derecha: 9
 * slots de almacén de fotos, sin arte propio (mismo fondo gris que ya
 * quedaba de sobra ahí, ver el comentario de {@code ANCHO} debajo).
 */
public class SublimadoraScreen extends HandledScreen<SublimadoraScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of("femclothes", "textures/gui/container/sublimadora.png");
    // A pedido (2026-09-20, "hagamos las gui del mismo tamaño"): mismo
    // ANCHO/ALTO que Tinturas/Modeladora, aunque esta pantalla no use la
    // columna de la derecha — queda un margen gris de sobra a propósito,
    // uniformidad ante que aprovechar hasta el último pixel.
    private static final int ANCHO = 482;
    private static final int ALTO = 264;
    private static final int M_MEDIO = SublimadoraScreenHandler.M_MEDIO;

    private ButtonWidget btnCara;
    private ButtonWidget btnEscalaAtras, btnEscala, btnEscalaAdelante;
    private ButtonWidget btnXAtras, btnX, btnXAdelante;
    private ButtonWidget btnYAtras, btnY, btnYAdelante;
    private ButtonWidget btnAnguloAtras, btnAngulo, btnAnguloAdelante;
    private ButtonWidget btnFijar;
    private ButtonWidget btnCategoria;
    private ButtonWidget btnVista;
    private final ButtonWidget[] btnFijadas = new ButtonWidget[SublimadoraBlockEntity.FIJADAS_MAXIMO];

    public SublimadoraScreen(SublimadoraScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = ANCHO;
        this.backgroundHeight = ALTO;
        this.titleX = M_MEDIO;
        this.titleY = 6;
        this.playerInventoryTitleX = M_MEDIO;
        this.playerInventoryTitleY = 170;
    }

    @Override
    protected void init() {
        super.init();

        btnCara = ButtonWidget.builder(Text.literal(""), b -> clickBoton(SublimadoraBlockEntity.BTN_SELECCION))
                .dimensions(this.x + M_MEDIO, this.y + 18, 162, 16)
                .tooltip(Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.cara")))
                .build();
        this.addDrawableChild(btnCara);

        Tooltip ttEscala = Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.escala"));
        btnEscalaAtras = ButtonWidget.builder(Text.literal("<"), b -> clickBoton(SublimadoraBlockEntity.BTN_ESCALA_MENOS))
                .dimensions(this.x + M_MEDIO, this.y + 38, 14, 16)
                .tooltip(ttEscala)
                .build();
        this.addDrawableChild(btnEscalaAtras);
        btnEscala = ButtonWidget.builder(Text.literal(""), b -> clickBoton(SublimadoraBlockEntity.BTN_ESCALA_MAS))
                .dimensions(this.x + M_MEDIO + 15, this.y + 38, 132, 16)
                .tooltip(ttEscala)
                .build();
        this.addDrawableChild(btnEscala);
        btnEscalaAdelante = ButtonWidget.builder(Text.literal(">"), b -> clickBoton(SublimadoraBlockEntity.BTN_ESCALA_MAS))
                .dimensions(this.x + M_MEDIO + 148, this.y + 38, 14, 16)
                .tooltip(ttEscala)
                .build();
        this.addDrawableChild(btnEscalaAdelante);

        Tooltip ttX = Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.x"));
        btnXAtras = ButtonWidget.builder(Text.literal("<"), b -> clickBoton(SublimadoraBlockEntity.BTN_X_MENOS))
                .dimensions(this.x + M_MEDIO, this.y + 58, 14, 16)
                .tooltip(ttX)
                .build();
        this.addDrawableChild(btnXAtras);
        btnX = ButtonWidget.builder(Text.literal(""), b -> clickBoton(SublimadoraBlockEntity.BTN_X_MAS))
                .dimensions(this.x + M_MEDIO + 15, this.y + 58, 132, 16)
                .tooltip(ttX)
                .build();
        this.addDrawableChild(btnX);
        btnXAdelante = ButtonWidget.builder(Text.literal(">"), b -> clickBoton(SublimadoraBlockEntity.BTN_X_MAS))
                .dimensions(this.x + M_MEDIO + 148, this.y + 58, 14, 16)
                .tooltip(ttX)
                .build();
        this.addDrawableChild(btnXAdelante);

        Tooltip ttY = Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.y"));
        btnYAtras = ButtonWidget.builder(Text.literal("<"), b -> clickBoton(SublimadoraBlockEntity.BTN_Y_MENOS))
                .dimensions(this.x + M_MEDIO, this.y + 78, 14, 16)
                .tooltip(ttY)
                .build();
        this.addDrawableChild(btnYAtras);
        btnY = ButtonWidget.builder(Text.literal(""), b -> clickBoton(SublimadoraBlockEntity.BTN_Y_MAS))
                .dimensions(this.x + M_MEDIO + 15, this.y + 78, 132, 16)
                .tooltip(ttY)
                .build();
        this.addDrawableChild(btnY);
        btnYAdelante = ButtonWidget.builder(Text.literal(">"), b -> clickBoton(SublimadoraBlockEntity.BTN_Y_MAS))
                .dimensions(this.x + M_MEDIO + 148, this.y + 78, 14, 16)
                .tooltip(ttY)
                .build();
        this.addDrawableChild(btnYAdelante);

        // Rotación libre (a pedido, 2026-09-20, "posibilidad de rotarla"):
        // mismo mecanismo ‹ › que el ángulo de patrón de TinturasScreen,
        // pasos de 15°.
        Tooltip ttAngulo = Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.angulo"));
        btnAnguloAtras = ButtonWidget.builder(Text.literal("<"), b -> clickBoton(SublimadoraBlockEntity.BTN_ANGULO_MENOS))
                .dimensions(this.x + M_MEDIO, this.y + 98, 14, 16)
                .tooltip(ttAngulo)
                .build();
        this.addDrawableChild(btnAnguloAtras);
        btnAngulo = ButtonWidget.builder(Text.literal(""), b -> clickBoton(SublimadoraBlockEntity.BTN_ANGULO_MAS))
                .dimensions(this.x + M_MEDIO + 15, this.y + 98, 132, 16)
                .tooltip(ttAngulo)
                .build();
        this.addDrawableChild(btnAngulo);
        btnAnguloAdelante = ButtonWidget.builder(Text.literal(">"), b -> clickBoton(SublimadoraBlockEntity.BTN_ANGULO_MAS))
                .dimensions(this.x + M_MEDIO + 148, this.y + 98, 14, 16)
                .tooltip(ttAngulo)
                .build();
        this.addDrawableChild(btnAnguloAdelante);

        // "Save por cada prenda" (a pedido, 2026-09-19): fijar guarda la
        // escala/x/y/ángulo de las DOS caras bajo la CATEGORÍA activa;
        // cada fijada de abajo la aplica de nuevo con un click (y
        // clickear la ya seleccionada la borra, mismo criterio que las
        // fijadas de TinturasScreen). Comparte la fila con Categoría
        // (2026-09-21, "le falta el boton de ciclado de settings de las
        // distintas prendas") — antes de esto solo se podía ver/guardar
        // el seteo de LO QUE HUBIERA CARGADO en ese momento.
        btnFijar = ButtonWidget.builder(Text.translatable("femclothes.tinturas.fijar"),
                        b -> clickBoton(SublimadoraBlockEntity.BTN_FIJAR))
                .dimensions(this.x + M_MEDIO, this.y + 118, 78, 16)
                .tooltip(Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.fijar")))
                .build();
        this.addDrawableChild(btnFijar);

        btnCategoria = ButtonWidget.builder(Text.literal(""), b -> clickBoton(SublimadoraBlockEntity.BTN_CATEGORIA))
                .dimensions(this.x + M_MEDIO + 82, this.y + 118, 80, 16)
                .tooltip(Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.categoria")))
                .build();
        this.addDrawableChild(btnCategoria);

        // Un solo botón que cicla las 4 perspectivas — a pedido (2026-09-20,
        // "arreglame el boton de back... yo haria un solo boton que cicle
        // las 4 perspectivas"), mismo mecanismo que TinturasScreen (ver
        // PreviewJugador): abajo del preview no entra (los 2 slots de foto
        // arrancan en y=172), así que va en la columna de controles, en el
        // huequito entre las fijadas y el inventario del jugador.
        btnVista = ButtonWidget.builder(Text.literal(""),
                        b -> anguloVista = Math.floorMod(Math.round(anguloVista) + 90, 360))
                .dimensions(this.x + M_MEDIO, this.y + 156, 162, 14)
                .tooltip(Tooltip.of(Text.translatable("femclothes.preview.tooltip.vista")))
                .build();
        this.addDrawableChild(btnVista);

        for (int i = 0; i < btnFijadas.length; i++) {
            int id = SublimadoraBlockEntity.BTN_FIJADA_BASE + i;
            int idx = i;
            btnFijadas[i] = ButtonWidget.builder(Text.literal(Integer.toString(i + 1)), b -> clickBoton(id))
                    .dimensions(this.x + M_MEDIO + idx * 20, this.y + 136, 18, 18)
                    .build();
            this.addDrawableChild(btnFijadas[i]);
        }
    }

    private void clickBoton(int id) {
        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

    // ── rotación del preview a mano — mismo mecanismo que TinturasScreen ──
    private static final int PREVIEW_X1_LOCAL = 8, PREVIEW_Y1_LOCAL = 18, PREVIEW_X2_LOCAL = 94, PREVIEW_Y2_LOCAL = 164;
    /** En GRADOS, no radianes — ver el javadoc de {@link PreviewJugador}. */
    private float anguloVista = 0f;
    private boolean arrastrandoPreview = false;
    /**
     * Zoom del preview — a pedido (2026-09-21, "se puede agregar un zoom a
     * la preview?"): rueda del mouse arriba de la caja del preview,
     * multiplica el {@code size} fijo (35) que ya usaba
     * {@link PreviewJugador#dibujar}. Rango acotado para no poder alejar
     * tanto que el jugador desaparezca ni acercar tanto que reviente el
     * scissor de la caja.
     */
    private float zoomVista = 1f;
    private static final float ZOOM_MIN = 0.5f, ZOOM_MAX = 2.5f;

    private boolean dentroDePreview(double mouseX, double mouseY) {
        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && dentroDePreview(mouseX, mouseY)) arrastrandoPreview = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) arrastrandoPreview = false;
        return super.mouseReleased(mouseX, mouseY, button);
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (arrastrandoPreview) {
            anguloVista = Math.floorMod(Math.round(anguloVista + (float) deltaX * 1.15f), 360);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refrescar();
        super.render(context, mouseX, mouseY, delta);
        dibujarPreview(context, mouseX, mouseY);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    /**
     * Visor 3D de la remera cargada CON la estampa de las dos caras ya
     * aplicada (con la escala/posición actuales, ambas en vivo, no solo la
     * cara que se está editando) — mismo mecanismo que
     * {@code TinturasScreen#dibujarPreview}.
     */
    private void dibujarPreview(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity jugador = client.player;
        if (jugador == null) return;

        SublimadoraBlockEntity be = handler.be;
        ItemStack remera = be.prendaDeVistaPrevia();

        List<ItemStack> prendas = new ArrayList<>(GarmentFeatureRenderer.equipadas(jugador));
        if (!remera.isEmpty()) {
            ItemStack prendaFinal = remera;
            prendas.removeIf(s -> s.getItem().getClass() == prendaFinal.getItem().getClass());
            prendas.add(prendaFinal);
        }

        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        GarmentFeatureRenderer.previewOverride = prendas;
        try {
            PreviewJugador.dibujar(context, jugador, x1, y1, x2, y2, Math.round(35 * zoomVista), anguloVista, (float) mouseY);
        } finally {
            GarmentFeatureRenderer.previewOverride = null;
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
        dibujarSeparadores(context);
    }

    /** Mismo criterio que {@code ModeladoScreen#dibujarSeparadores} — 2026-09-23, "esta todo muy apretado". */
    private void dibujarSeparadores(DrawContext context) {
        int gris = 0xFF808080;
        context.fill(this.x + M_MEDIO - 9, this.y + 16, this.x + M_MEDIO - 8, this.y + ALTO - 16, gris);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        // Guía de flujo — mismo mecanismo y motivo que
        // ModeladoScreen#hint (esta pantalla no tenía ninguna todavía,
        // 2026-09-23 "el orden de pasos no es claro"). No hay ninguna fila
        // libre en la columna del medio (confirmado: título→cara→escala→
        // x→y→ángulo→fijar/categoría→fijadas→vista→inventario, sin hueco),
        // así que va en la columna derecha, que ya queda vacía a
        // propósito (ver javadoc de la clase).
        context.drawText(this.textRenderer, hint(), M_MEDIO + 182, 20, 0x404040, false);
    }

    private Text hint() {
        SublimadoraBlockEntity be = handler.be;
        if (be.getEstado() == SublimadoraBlockEntity.Estado.PRENSANDO) {
            return Text.translatable("femclothes.sublimadora.hint.prensando");
        }
        if (!be.getSalida().isEmpty()) {
            return Text.translatable("femclothes.sublimadora.hint.listo");
        }
        if (be.getFotoCargada(com.femclothes.sublimadora.Estampa.Cara.FRENTE) == null
                && be.getFotoCargada(com.femclothes.sublimadora.Estampa.Cara.ESPALDA) == null) {
            return Text.translatable("femclothes.sublimadora.hint.sin_foto");
        }
        if (be.getRemera().isEmpty()) {
            return Text.translatable("femclothes.sublimadora.hint.ajustar");
        }
        return Text.translatable("femclothes.sublimadora.hint.fijado");
    }

    private void refrescar() {
        SublimadoraBlockEntity be = handler.be;

        btnCara.setMessage(Text.translatable("femclothes.sublimadora.gui.cara",
                Text.translatable("femclothes.sublimadora.cara." + be.getSeleccion().clave)));

        btnEscala.setMessage(Text.translatable("femclothes.sublimadora.escala",
                Math.round(be.getEscalaBorrador() * 100)));
        btnX.setMessage(Text.translatable("femclothes.sublimadora.posicion.x",
                Math.round(be.getXBorrador() * 100)));
        btnY.setMessage(Text.translatable("femclothes.sublimadora.posicion.y",
                Math.round(be.getYBorrador() * 100)));
        btnAngulo.setMessage(Text.translatable("femclothes.sublimadora.angulo",
                Math.round(be.getAnguloBorrador())));

        btnVista.setMessage(Text.translatable("femclothes.preview.vista",
                Text.translatable(PreviewJugador.nombreVista(anguloVista))));

        btnCategoria.setMessage(Text.translatable("femclothes.modelado.categoria",
                Text.translatable("femclothes.modelado.categoria." + claveCategoria(be.categoria()))));

        int seleccionada = be.fijadaSeleccionada();
        int cantidad = be.fijadas().size();
        for (int i = 0; i < btnFijadas.length; i++) {
            // Solo clickeables las que ya existen o la próxima vacía (para
            // fijar ahí) — más allá de eso no hay nada que aplicar.
            btnFijadas[i].active = i <= cantidad;
            btnFijadas[i].setMessage(Text.literal(i == seleccionada ? "[" + (i + 1) + "]" : Integer.toString(i + 1)));
        }
    }

    /** Reusa las mismas claves de traducción que Modeladora/Tinturas — mismas 4 categorías. */
    private static String claveCategoria(net.minecraft.item.Item item) {
        if (item == com.femclothes.sublimadora.ModItems.REMERA) return "remera";
        if (item == com.femclothes.item.FemclothesItems.SOCKS_SOLID) return "medias";
        if (item == com.femclothes.item.FemclothesItems.PANTALON) return "pantalon";
        return "calientabrazos";
    }
}
