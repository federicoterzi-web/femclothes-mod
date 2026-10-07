package com.femclothes.client;

import com.femclothes.render.GarmentFeatureRenderer;
import com.femclothes.sublimadora.Estampa;
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
 * Pantalla de la Sublimadora — v2 (2026-09-28, "cambiemos la gui de la
 * sublimadora para hacerla sintonizar con sus bloques hermanos"): mismo
 * pergamino de 560x408 y mismo esqueleto que {@link TinturasScreen}:
 * <ul>
 *   <li>Izquierda: visor 3D + Vista + Nombre + Guardar diseño.</li>
 *   <li>Centro: Categoría, dibujo de la prenda (el ítem real, Frente |
 *   Espalda) con un slot de foto y una chincheta por cara, cinturón
 *   Entrada -> Salida con Prensar encima de la flecha, casilleros de
 *   diseño y los controles de la cara elegida (escala, X, Y, ángulo).</li>
 *   <li>Derecha: tanques de tinta CMYK y papel, guía de pasos y almacén de
 *   fotos.</li>
 * </ul>
 * Click en el slot de foto de una cara la elige (los controles editan esa);
 * su chincheta la fija — solo se estampan las caras fijadas con foto.
 */
public class SublimadoraScreen extends HandledScreen<SublimadoraScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of("femclothes", "textures/gui/container/sublimadora.png");
    private static final int ANCHO = 560;
    private static final int ALTO = 408;
    private static final int M_MEDIO = SublimadoraScreenHandler.M_MEDIO;
    private static final int M_DERECHA = SublimadoraScreenHandler.M_DERECHA;
    private static final int[][] FOTO_POS = SublimadoraScreenHandler.FOTO_POS;
    /** Cuánto se agranda el ítem de la prenda en el dibujo (16 px -> 64). */
    private static final float ESCALA_PRENDA = 4f;

    private ButtonWidget btnCategoria;
    private ButtonWidget btnEscala, btnX, btnY, btnAngulo;
    private ButtonWidget btnCara;
    private ButtonWidget btnSimetria;
    private ButtonWidget btnVista;
    private ButtonWidget btnPrensar;
    private ButtonWidget btnGuardarDiseno;
    private net.minecraft.client.gui.widget.TextFieldWidget txtNombreDiseno;
    private final BotonDiseno[] btnDisenos = new BotonDiseno[SublimadoraBlockEntity.DISENOS_MAXIMO];
    private final TinturasScreen.BotonChincheta[] btnChinchetas = new TinturasScreen.BotonChincheta[2];
    /** Máscaras (2026-10-02): lista de capas, subir/bajar/borrar y qué mueven los controles. */
    private final ButtonWidget[] btnCapas = new ButtonWidget[com.femclothes.sublimadora.CapaEstampa.MAXIMO];
    private ButtonWidget btnCapaSubir, btnCapaBajar, btnCapaBorrar, btnEditar;
    private static final int CAPAS_Y = 274;

    public SublimadoraScreen(SublimadoraScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = ANCHO;
        this.backgroundHeight = ALTO;
        this.titleX = M_MEDIO;
        this.titleY = 9;
        this.playerInventoryTitleX = M_MEDIO;
        this.playerInventoryTitleY = 314;
    }

    /** Botón pergamino chico con tooltip, ya agregado a la pantalla — mismo helper que TinturasScreen. */
    private ButtonWidget boton(int x, int y, int w, Text texto, String tooltip, int id) {
        ButtonWidget b = new EstiloPergamino.BotonPergamino(this.x + x, this.y + y, w, 16, texto, btn -> clickBoton(id));
        b.setTooltip(Tooltip.of(Text.translatable(tooltip)));
        this.addDrawableChild(b);
        return b;
    }

    /** Un control ‹ valor › de 116 de ancho: menos, valor (click = más) y más. */
    private ButtonWidget control(int x, int y, String tooltip, int menos, int mas) {
        boton(x, y, 14, Text.literal("<"), tooltip, menos);
        ButtonWidget valor = boton(x + 15, y, 86, Text.empty(), tooltip, mas);
        boton(x + 102, y, 14, Text.literal(">"), tooltip, mas);
        return valor;
    }

    @Override
    protected void init() {
        EstiloPergamino.usarTema(EstiloPergamino.Tema.ORO);
        super.init();

        btnCategoria = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO, this.y + 20, SublimadoraScreenHandler.M_MEDIO_ANCHO, 14,
                Text.literal(""), b -> clickBoton(SublimadoraBlockEntity.BTN_CATEGORIA));
        btnCategoria.setTooltip(Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.categoria")));
        this.addDrawableChild(btnCategoria);

        // Izquierda: Vista, Nombre y Guardar diseño — mismas coordenadas que Tintes.
        btnVista = new EstiloPergamino.BotonPergamino(this.x + PREVIEW_X1_LOCAL, this.y + 218, 86, 16, Text.literal(""),
                b -> anguloVista = Math.floorMod(Math.round(anguloVista) + 90, 360));
        btnVista.setTooltip(Tooltip.of(Text.translatable("femclothes.preview.tooltip.vista")));
        this.addDrawableChild(btnVista);

        txtNombreDiseno = new net.minecraft.client.gui.widget.TextFieldWidget(
                this.textRenderer, this.x + 8, this.y + 238, 86, 14, Text.translatable("femclothes.tinturas.nombre_diseno"));
        txtNombreDiseno.setMaxLength(24);
        txtNombreDiseno.setPlaceholder(Text.translatable("femclothes.tinturas.nombre_diseno"));
        this.addDrawableChild(txtNombreDiseno);

        btnGuardarDiseno = new EstiloPergamino.BotonPergamino(this.x + 8, this.y + 256, 86, 16,
                Text.translatable("femclothes.tinturas.boton.guardar_diseno"), b -> {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new com.femclothes.sublimadora.GuardarDisenoSublimadoraPayload(this.handler.be.getPos(), txtNombreDiseno.getText()));
            txtNombreDiseno.setText("");
        });
        btnGuardarDiseno.setTooltip(Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.guardar_diseno")));
        this.addDrawableChild(btnGuardarDiseno);

        // Escanear estampa (2026-10-02, "agregar un boton de escanear estampa... abre
        // explorador para seleccionar imagen y produce una imagen de camerapture"):
        // la foto queda en Frente si está vacío, si no en Espalda.
        ButtonWidget btnEscanear = new EstiloPergamino.BotonPergamino(this.x + 8, this.y + 276, 86, 16,
                Text.translatable("femclothes.sublimadora.boton.escanear"),
                b -> EscanerEstampaCliente.escanear(this.handler.be.getPos()));
        btnEscanear.setTooltip(Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.escanear")));
        this.addDrawableChild(btnEscanear);

        // Prensar, ENCIMA de la flecha Entrada -> Salida — como Teñir en Tintes.
        btnPrensar = boton(M_MEDIO + 96, 184, 48, Text.translatable("femclothes.sublimadora.boton.prensar"),
                "femclothes.sublimadora.tooltip.prensar", SublimadoraBlockEntity.BTN_PRENSAR);

        // Diseños guardados: mismo Y=214 que Tintes y la Modeladora.
        for (int i = 0; i < btnDisenos.length; i++) {
            int idx = i;
            btnDisenos[i] = new BotonDiseno(idx, this.x + M_MEDIO + i * 20, this.y + 214, 18, 14,
                    b -> clickBoton(SublimadoraBlockEntity.BTN_CARGAR_DISENO_BASE + idx),
                    () -> clickBoton(SublimadoraBlockEntity.BTN_BORRAR_DISENO_BASE + idx));
            this.addDrawableChild(btnDisenos[i]);
        }

        // Controles de la cara elegida: dos por fila.
        btnEscala = control(M_MEDIO, 232, "femclothes.sublimadora.tooltip.escala",
                SublimadoraBlockEntity.BTN_ESCALA_MENOS, SublimadoraBlockEntity.BTN_ESCALA_MAS);
        btnX = control(M_MEDIO + 124, 232, "femclothes.sublimadora.tooltip.x",
                SublimadoraBlockEntity.BTN_X_MENOS, SublimadoraBlockEntity.BTN_X_MAS);
        btnY = control(M_MEDIO, 252, "femclothes.sublimadora.tooltip.y",
                SublimadoraBlockEntity.BTN_Y_MENOS, SublimadoraBlockEntity.BTN_Y_MAS);
        btnAngulo = control(M_MEDIO + 124, 252, "femclothes.sublimadora.tooltip.angulo",
                SublimadoraBlockEntity.BTN_ANGULO_MENOS, SublimadoraBlockEntity.BTN_ANGULO_MAS);
        // Cara también por botón (además de tocar su slot).
        btnCara = boton(M_MEDIO, 272, 116, Text.empty(), "femclothes.sublimadora.tooltip.cara", SublimadoraBlockEntity.BTN_SELECCION);
        // Simetría lateral (2026-09-28, "simetria lateral para medias y
        // cubrebrazos"): solo se ve en las prendas de a pares.
        btnSimetria = boton(M_MEDIO + 124, 272, 116, Text.empty(), "femclothes.sublimadora.tooltip.simetria",
                SublimadoraBlockEntity.BTN_SIMETRIA);

        // Capas con máscara (2026-10-02, "que se pueda guardar varias layers 12
        // quizas entre frente y atras"): columna derecha, debajo del almacén.
        for (int i = 0; i < btnCapas.length; i++) {
            int idx = i;
            btnCapas[i] = new EstiloPergamino.BotonPergamino(this.x + M_DERECHA + (i % 6) * 27, this.y + CAPAS_Y + (i / 6) * 16,
                    26, 14, Text.empty(), b -> clickBoton(SublimadoraBlockEntity.BTN_CAPA_BASE + idx));
            this.addDrawableChild(btnCapas[i]);
        }
        btnCapaSubir = boton(M_DERECHA, CAPAS_Y + 34, 24, Text.literal("▲"), "femclothes.sublimadora.tooltip.capa_subir",
                SublimadoraBlockEntity.BTN_CAPA_SUBIR);
        btnCapaBajar = boton(M_DERECHA + 26, CAPAS_Y + 34, 24, Text.literal("▼"), "femclothes.sublimadora.tooltip.capa_bajar",
                SublimadoraBlockEntity.BTN_CAPA_BAJAR);
        btnCapaBorrar = boton(M_DERECHA + 52, CAPAS_Y + 34, 24, Text.literal("✕"), "femclothes.sublimadora.tooltip.capa_borrar",
                SublimadoraBlockEntity.BTN_CAPA_BORRAR);
        btnEditar = boton(M_DERECHA + 80, CAPAS_Y + 34, 82, Text.empty(), "femclothes.sublimadora.tooltip.editar",
                SublimadoraBlockEntity.BTN_EDITAR);

        // Una chincheta por cara, arriba a la derecha de su slot — se
        // dibujan a mano en render(), encima de todo (mismo criterio que Tintes).
        for (int i = 0; i < 2; i++) {
            int idx = i;
            btnChinchetas[i] = new TinturasScreen.BotonChincheta(b -> clickBoton(SublimadoraBlockEntity.BTN_CHINCHETA_BASE + idx));
            this.addSelectableChild(btnChinchetas[i]);
        }
    }

    private void clickBoton(int id) {
        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

    // ── visor 3D: arrastrar para girar, ruedita para zoom (igual que Tintes) ──
    private static final int PREVIEW_X1_LOCAL = 8, PREVIEW_Y1_LOCAL = 18, PREVIEW_X2_LOCAL = 94, PREVIEW_Y2_LOCAL = 214;
    /** En GRADOS, no radianes — ver el javadoc de {@link PreviewJugador}. */
    private float anguloVista = 0f;
    private float zoomVista = 1f;
    private static final float ZOOM_MIN = 0.5f, ZOOM_MAX = 2.5f;
    private boolean arrastrandoPreview = false;

    private boolean dentroDePreview(double mouseX, double mouseY) {
        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    /** La cara (0 frente, 1 espalda) cuyo slot de foto está bajo el mouse, o -1. */
    private int caraDelSlot(net.minecraft.screen.slot.Slot slot) {
        if (slot == null || slot.inventory != handler.be) return -1;
        int i = slot.getIndex();
        return i == 0 || i == 1 ? i : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && dentroDePreview(mouseX, mouseY)) arrastrandoPreview = true;
        // Tocar el slot de foto de una cara la ELIGE, además de lo que haga
        // el slot con la foto — mismo criterio que los cuadraditos de Tintes.
        int cara = caraDelSlot(this.focusedSlot);
        if (cara >= 0 && cara != handler.be.getSeleccion().ordinal()) {
            clickBoton(SublimadoraBlockEntity.BTN_ELEGIR_CARA_BASE + cara);
        }
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

    /** Sin esto, escribir una "e" en el nombre del diseño cierra la pantalla (mismo motivo que TinturasScreen). */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return (!txtNombreDiseno.keyPressed(keyCode, scanCode, modifiers) && !txtNombreDiseno.isActive())
                ? super.keyPressed(keyCode, scanCode, modifiers) : true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refrescar();
        super.render(context, mouseX, mouseY, delta);
        dibujarPreview(context, mouseX, mouseY);
        for (TinturasScreen.BotonChincheta b : btnChinchetas) b.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    /**
     * Visor 3D con la prenda y la estampa de lo FIJADO más la cara que se
     * está editando — sin prenda cargada, la de la categoría elegida.
     */
    private void dibujarPreview(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity jugador = client.player;
        if (jugador == null) return;

        ItemStack vista = handler.be.prendaDeVistaPrevia(true);
        // El banner no se viste: se ve en el recuadro de la foto.
        final ItemStack prenda = vista.getItem() instanceof net.minecraft.item.BannerItem ? ItemStack.EMPTY : vista;
        List<ItemStack> prendas = new ArrayList<>(GarmentFeatureRenderer.equipadas(jugador));
        if (!prenda.isEmpty()) {
            prendas.removeIf(s -> s.getItem().getClass() == prenda.getItem().getClass());
            prendas.add(prenda);
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
        dibujarPrendas(context);
        // Marco del slot de máscara, dibujado por código (el fondo no lo trae).
        int mx = this.x + SublimadoraScreenHandler.MASCARA_X, my = this.y + SublimadoraScreenHandler.MASCARA_Y;
        context.fill(mx - 1, my - 1, mx + 17, my + 17, 0xFF6B4E2A);
        context.fill(mx, my, mx + 16, my + 16, 0xFF8A7556);
    }

    /**
     * El dibujo de la prenda: el ÍTEM real (la de la Entrada con su color,
     * si no la lista, si no la de la categoría) agrandado, una vez por
     * cara, detrás de su slot de foto. Después se vuelve a pintar el marco
     * del slot desde la textura, que la prenda tapó.
     */
    private void dibujarPrendas(DrawContext context) {
        SublimadoraBlockEntity be = handler.be;
        ItemStack prenda = !be.getRemera().isEmpty() ? be.getRemera()
                : !be.getSalida().isEmpty() ? be.getSalida() : new ItemStack(be.categoria());
        // Un banner se ve con la foto puesta, a 16x, mientras se ajusta (2026-10-06).
        if (prenda.getItem() instanceof net.minecraft.item.BannerItem) prenda = be.prendaDeVistaPrevia(true);
        for (int cara = 0; cara < 2; cara++) {
            int sx = FOTO_POS[cara][0], sy = FOTO_POS[cara][1];
            float cx = this.x + sx + 8, cy = this.y + sy + 8;
            context.getMatrices().push();
            // Atrás de todo (z negativo): el ítem de la foto se dibuja después, encima.
            context.getMatrices().translate(cx - 8 * ESCALA_PRENDA, cy - 8 * ESCALA_PRENDA, -120);
            context.getMatrices().scale(ESCALA_PRENDA, ESCALA_PRENDA, 1f);
            context.drawItem(prenda, 0, 0);
            context.getMatrices().pop();
            context.drawTexture(TEXTURE, this.x + sx - 1, this.y + sy - 1, sx - 1, sy - 1, 18, 18, ANCHO, ALTO);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX,
                this.playerInventoryTitleY, EstiloPergamino.TEXTO, false);

        SublimadoraBlockEntity be = handler.be;
        int sel = be.getSeleccion().ordinal();
        for (int cara = 0; cara < 2; cara++) {
            int sx = FOTO_POS[cara][0], sy = FOTO_POS[cara][1];
            Text rotulo = Text.translatable("femclothes.sublimadora.rotulo." + Estampa.Cara.values()[cara].clave);
            context.drawText(this.textRenderer, rotulo, sx + 8 - this.textRenderer.getWidth(rotulo) / 2,
                    SublimadoraScreenHandler.ESQUEMA_Y + 8, EstiloPergamino.TEXTO, false);
            if (cara == sel) {
                context.drawBorder(sx - 2, sy - 2, 20, 20, 0xFFFFD24C);
                context.drawBorder(sx - 3, sy - 3, 22, 22, 0xFF6B4E2A);
            }
        }

        // Qué se está editando y si esa cara sale en la prenda.
        Text estado = Text.translatable(be.caraFijada(be.getSeleccion())
                ? "femclothes.tinturas.estado.fijada" : "femclothes.tinturas.estado.borrador");
        context.drawText(this.textRenderer, Text.translatable("femclothes.tinturas.editando",
                Text.translatable("femclothes.sublimadora.rotulo." + be.getSeleccion().clave), estado),
                M_MEDIO, 296, EstiloPergamino.TEXTO, false);

        dibujarTanques(context);
        dibujarCapas(context);
        context.drawTextWrapped(this.textRenderer, hint(), M_DERECHA + 6, 150, 162, EstiloPergamino.TEXTO);
    }

    /** Título de la lista de capas, la elegida resaltada y el rótulo del slot de máscara. */
    private void dibujarCapas(DrawContext c) {
        SublimadoraBlockEntity be = handler.be;
        c.drawText(this.textRenderer, Text.translatable("femclothes.sublimadora.capas", be.capas().size()),
                M_DERECHA + 2, CAPAS_Y - 11, EstiloPergamino.TEXTO, false);
        int elegida = be.capaElegida();
        if (elegida >= 0 && elegida < btnCapas.length) {
            int bx = M_DERECHA + (elegida % 6) * 27, by = CAPAS_Y + (elegida / 6) * 16;
            c.drawBorder(bx - 1, by - 1, 28, 16, 0xFFFFD24C);
        }
        com.femclothes.sublimadora.FormaMascara forma = be.formaMascara();
        Text rotulo = forma == null ? Text.translatable("femclothes.sublimadora.mascara")
                : Text.translatable("femclothes.sublimadora.mascara").append(": ")
                        .append(Text.translatable("femclothes.sublimadora.forma." + forma.asString()));
        c.drawText(this.textRenderer, rotulo, SublimadoraScreenHandler.MASCARA_X + 22,
                SublimadoraScreenHandler.MASCARA_Y + 4, EstiloPergamino.TEXTO, false);
    }

    /** Colores de las barras: C, M, Y, K y el papel. */
    private static final int[] COLOR_TANQUE = { 0xFF1FB3D6, 0xFFD6287F, 0xFFE8C21E, 0xFF2A2A2A, 0xFFF3EEDF };
    private static final String[] LETRA_TANQUE = { "C", "M", "Y", "K" };

    /** Columna derecha: un tanque por tinta y uno de papel, con barra y "n/64". */
    private void dibujarTanques(DrawContext c) {
        SublimadoraBlockEntity be = handler.be;
        int x0 = M_DERECHA + 6;
        c.drawText(this.textRenderer, Text.translatable("femclothes.sublimadora.tanques"), x0, 20, EstiloPergamino.TEXTO, false);
        for (int i = 0; i < 5; i++) {
            int y = 34 + i * 16;
            int cantidad = i < 4 ? Math.round(be.getTinta(i) * SublimadoraBlockEntity.CARGA_MAXIMA) : be.getPapel();
            Text letra = i < 4 ? Text.literal(LETRA_TANQUE[i]) : Text.translatable("femclothes.sublimadora.tanque.papel");
            c.drawText(this.textRenderer, letra, x0, y, EstiloPergamino.TEXTO, false);
            int bx0 = x0 + 34, bx1 = x0 + 120;
            c.fill(bx0 - 1, y - 1, bx1 + 1, y + 9, 0xFF2A180C);
            c.fill(bx0, y, bx1, y + 8, 0xFF8A7556);
            int lleno = (bx1 - bx0) * cantidad / SublimadoraBlockEntity.CARGA_MAXIMA;
            if (lleno > 0) c.fill(bx0, y, bx0 + lleno, y + 8, COLOR_TANQUE[i]);
            c.drawText(this.textRenderer, cantidad + "/" + SublimadoraBlockEntity.CARGA_MAXIMA, bx1 + 4, y,
                    cantidad == 0 ? 0xFFB02A1A : EstiloPergamino.TEXTO, false);
        }
    }

    private Text hint() {
        SublimadoraBlockEntity be = handler.be;
        if (be.getEstado() == SublimadoraBlockEntity.Estado.PRENSANDO) {
            return Text.translatable("femclothes.sublimadora.hint.prensando");
        }
        if (!be.getSalida().isEmpty()) {
            return Text.translatable("femclothes.sublimadora.hint.listo");
        }
        if (be.hayMascara() && be.getFotoCargada(be.getSeleccion()) != null) {
            return Text.translatable("femclothes.sublimadora.hint.mascara");
        }
        if (be.getFotoCargada(Estampa.Cara.FRENTE) == null && be.getFotoCargada(Estampa.Cara.ESPALDA) == null
                && be.capas().isEmpty()) {
            return Text.translatable("femclothes.sublimadora.hint.sin_foto");
        }
        if (!be.hayFijadas() && be.capas().isEmpty()) {
            return Text.translatable("femclothes.sublimadora.hint.ajustar");
        }
        if (be.getRemera().isEmpty()) {
            return Text.translatable("femclothes.sublimadora.hint.sin_prenda");
        }
        return Text.translatable("femclothes.sublimadora.hint.fijado");
    }

    private void refrescar() {
        SublimadoraBlockEntity be = handler.be;

        btnCategoria.setMessage(Text.translatable("femclothes.modelado.categoria",
                Text.translatable("femclothes.modelado.categoria." + claveCategoria(be.categoria()))));
        btnEscala.setMessage(Text.translatable("femclothes.sublimadora.escala", Math.round(be.getEscalaBorrador() * 100)));
        btnX.setMessage(Text.translatable("femclothes.sublimadora.posicion.x", Math.round(be.getXBorrador() * 100)));
        btnY.setMessage(Text.translatable("femclothes.sublimadora.posicion.y", Math.round(be.getYBorrador() * 100)));
        btnAngulo.setMessage(Text.translatable("femclothes.sublimadora.angulo", Math.round(be.getAnguloBorrador())));
        btnCara.setMessage(Text.translatable("femclothes.sublimadora.gui.cara",
                Text.translatable("femclothes.sublimadora.cara." + be.getSeleccion().clave)));
        btnVista.setMessage(Text.translatable("femclothes.preview.vista",
                Text.translatable(PreviewJugador.nombreVista(anguloVista))));
        btnSimetria.visible = SublimadoraBlockEntity.admiteSimetria(be.categoria());
        btnSimetria.setMessage(Text.translatable("femclothes.modelado.simetria",
                Text.translatable(be.simetria() ? "femclothes.si" : "femclothes.no")));

        boolean enReposo = be.getEstado() == SublimadoraBlockEntity.Estado.REPOSO;
        btnPrensar.active = enReposo && !be.getRemera().isEmpty() && be.getSalida().isEmpty();

        for (int cara = 0; cara < 2; cara++) {
            TinturasScreen.BotonChincheta b = btnChinchetas[cara];
            // Centro de la chincheta: arriba a la derecha del slot (como la remera en Tintes).
            b.setPosition(this.x + FOTO_POS[cara][0] + 17 - 8, this.y + FOTO_POS[cara][1] - 1 - 8);
            Estampa.Cara c = Estampa.Cara.values()[cara];
            b.actualizar(be.caraFijada(c));
            boolean comoCapa = be.hayMascara() && be.getFotoCargada(c) != null;
            b.setTooltip(Tooltip.of(Text.translatable(comoCapa ? "femclothes.sublimadora.tooltip.chincheta_capa"
                    : be.caraFijada(c)
                    ? "femclothes.sublimadora.tooltip.chincheta_quitar" : "femclothes.sublimadora.tooltip.chincheta_fijar",
                    Text.translatable("femclothes.sublimadora.rotulo." + c.clave))));
        }

        // Capas con máscara (2026-10-02).
        java.util.List<com.femclothes.sublimadora.CapaEstampa> capas = be.capas();
        for (int i = 0; i < btnCapas.length; i++) {
            ButtonWidget b = btnCapas[i];
            if (i >= capas.size()) {
                b.active = false;
                b.setMessage(Text.literal("·"));
                b.setTooltip(null);
                continue;
            }
            com.femclothes.sublimadora.CapaEstampa capa = capas.get(i);
            b.active = enReposo;
            b.setMessage(Text.literal((i + 1) + (capa.cara() == Estampa.Cara.FRENTE ? "F" : "E")));
            b.setTooltip(Tooltip.of(Text.translatable("femclothes.sublimadora.tooltip.capa", i + 1,
                    Text.translatable("femclothes.sublimadora.forma." + capa.mascara().forma().asString()),
                    Text.translatable("femclothes.sublimadora.rotulo." + capa.cara().clave))));
        }
        int elegida = be.capaElegida();
        boolean hayElegida = enReposo && elegida >= 0 && elegida < capas.size();
        btnCapaSubir.active = hayElegida && elegida < capas.size() - 1;
        btnCapaBajar.active = hayElegida && elegida > 0;
        btnCapaBorrar.active = hayElegida;
        btnEditar.active = enReposo && be.hayMascara();
        btnEditar.setMessage(Text.translatable(be.editarMascaraElegido()
                ? "femclothes.sublimadora.boton.editar_mascara" : "femclothes.sublimadora.boton.editar_imagen"));

        int guardados = be.disenos().size();
        btnGuardarDiseno.active = be.hayFijadas() && guardados < SublimadoraBlockEntity.DISENOS_MAXIMO;
        for (int i = 0; i < btnDisenos.length; i++) {
            String nombre = be.nombreDiseno(i);
            btnDisenos[i].active = nombre != null;
            btnDisenos[i].setTooltip(nombre == null ? null : Tooltip.of(Text.translatable(
                    "femclothes.tinturas.tooltip.casillero_diseno", nombre)));
        }
    }

    /** Reusa las mismas claves de traducción que Modeladora/Tinturas — mismas 4 categorías. */
    private static String claveCategoria(net.minecraft.item.Item item) {
        if (item == com.femclothes.sublimadora.ModItems.REMERA) return "remera";
        if (item == com.femclothes.item.FemclothesItems.SOCKS_SOLID) return "medias";
        if (item == com.femclothes.item.FemclothesItems.PANTALON) return "pantalon";
        return "calientabrazos";
    }

    // ── Entrada/Salida grandes: mismo truco de escala 1.5x que TinturasScreen#esSlotGrande ──
    private static boolean esSlotGrande(int localX, int localY) {
        return localY == SublimadoraScreenHandler.SLOT_Y_IO
                && (localX == M_MEDIO + SublimadoraScreenHandler.ENTRADA_X || localX == M_MEDIO + SublimadoraScreenHandler.SALIDA_X);
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
}
