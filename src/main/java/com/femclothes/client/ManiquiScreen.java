package com.femclothes.client;

import com.femclothes.maniqui.ManiquiBlockEntity;
import com.femclothes.maniqui.ManiquiScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Maniquí (2026-09-30): la grilla de 16 slots en el mismo lugar que el
 * Guardarropas (reusa su fondo), más Girar/Detener e "Intercambiar conmigo"
 * (la ropa del maniquí pasa a Trinkets y la tuya al maniquí). El preview de
 * la izquierda te muestra con la ropa del maniquí puesta — la figura real
 * ya se ve en el mundo.
 */
public class ManiquiScreen extends HandledScreen<ManiquiScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of("femclothes", "textures/gui/container/maniqui.png");
    private static final int ANCHO = 482;
    private static final int ALTO = 264;
    private static final int M_MEDIO = ManiquiScreenHandler.M_MEDIO;

    private static final int PREVIEW_X1_LOCAL = 8, PREVIEW_Y1_LOCAL = 18, PREVIEW_X2_LOCAL = 94, PREVIEW_Y2_LOCAL = 164;
    private float anguloVista = 0f;
    private ButtonWidget btnVista;
    private ButtonWidget btnGirar;

    public ManiquiScreen(ManiquiScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = ANCHO;
        this.backgroundHeight = ALTO;
        this.titleX = M_MEDIO;
        this.titleY = 8;
        this.playerInventoryTitleX = M_MEDIO;
        this.playerInventoryTitleY = 170;
    }

    @Override
    protected void init() {
        // Estilo del mod con el metal del mueble (2026-09-30, "armame la gui...
        // con el estilo del mod y los colores de sus acentos metalicos").
        EstiloPergamino.usarTema(EstiloPergamino.Tema.AZUL);
        super.init();

        btnVista = new EstiloPergamino.BotonPergamino(this.x + PREVIEW_X1_LOCAL, this.y + 166, 86, 16, Text.literal(""), b -> anguloVista = Math.floorMod(Math.round(anguloVista) + 90, 360));
        this.addDrawableChild(btnVista);

        btnGirar = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO, this.y + 104, 78, 16, Text.literal(""), b -> clickBoton(ManiquiBlockEntity.BTN_GIRAR));
        this.addDrawableChild(btnGirar);

        btnIntercambiar = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO + 82, this.y + 104, 80, 16, Text.translatable("femclothes.maniqui.intercambiar"), b -> clickBoton(ManiquiBlockEntity.BTN_INTERCAMBIAR));
        this.addDrawableChild(btnIntercambiar);

        // Poses y figura (2026-09-30).
        btnPose = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO, this.y + 124, 78, 16, Text.literal(""), b -> clickBoton(ManiquiBlockEntity.BTN_POSE));
        this.addDrawableChild(btnPose);
        btnFigura = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO + 82, this.y + 124, 80, 16, Text.literal(""), b -> clickBoton(ManiquiBlockEntity.BTN_FIGURA));
        this.addDrawableChild(btnFigura);
        // Busto de la figura (2026-10-02, "agregale la opcion de ponerle tetas").
        btnBusto = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO, this.y + 144, 78, 16, Text.literal(""), b -> clickBoton(ManiquiBlockEntity.BTN_BUSTO));
        this.addDrawableChild(btnBusto);
        // Candado (2026-10-04, "le pongamos un lock al maniqui").
        btnCandado = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO + 82, this.y + 144, 80, 16, Text.literal(""), b -> clickBoton(ManiquiBlockEntity.BTN_CANDADO));
        this.addDrawableChild(btnCandado);

        // A la derecha de los botones (M_MEDIO + 162) y de la columna de armadura.
        int sx = this.x + M_MEDIO + 170;
        int ancho = this.x + ANCHO - 8 - sx;
        for (int i = 0; i < sliders.length; i++) {
            sliders[i] = new SliderPose(sx, this.y + 18 + i * 14, ancho, 12, i);
            this.addDrawableChild(sliders[i]);
        }

        // Skin de un jugador por nombre (2026-10-04, "elegir skin escribiendo el nombre").
        campoSkin = new net.minecraft.client.gui.widget.TextFieldWidget(this.textRenderer, sx, this.y + 162, ancho - 32, 14,
                Text.translatable("femclothes.maniqui.skin.campo"));
        campoSkin.setMaxLength(16);
        campoSkin.setPlaceholder(Text.translatable("femclothes.maniqui.skin.campo"));
        this.addDrawableChild(campoSkin);
        btnSkin = new EstiloPergamino.BotonPergamino(sx + ancho - 30, this.y + 162, 30, 14, Text.translatable("femclothes.maniqui.skin.ir"), b -> {
            String nombre = campoSkin.getText().trim();
            if (!nombre.isEmpty()) {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new com.femclothes.maniqui.ElegirSkinManiquiPayload(handler.be.getPos(), nombre));
            }
        });
        this.addDrawableChild(btnSkin);

        // Emote de Emotecraft (2026-10-04): solo si el mod está instalado.
        if (EmotecraftCompat.disponible()) {
            btnEmote = new EstiloPergamino.BotonPergamino(sx, this.y + 178, ancho, 14, Text.literal(""), b -> siguienteEmote());
            this.addDrawableChild(btnEmote);
        }
    }

    private ButtonWidget btnEmote;

    /** Pasa por ninguno → cada emote cargado → ninguno. */
    private void siguienteEmote() {
        var lista = EmotecraftCompat.lista();
        java.util.UUID actual = handler.be.emote();
        int i = actual == null ? -1 : lista.indexOf(actual);
        String siguiente = i + 1 < lista.size() ? lista.get(i + 1).toString() : "";
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new com.femclothes.maniqui.ElegirEmoteManiquiPayload(handler.be.getPos(), siguiente));
    }

    private net.minecraft.client.gui.widget.TextFieldWidget campoSkin;
    private ButtonWidget btnSkin;
    private ButtonWidget btnCandado;

    private ButtonWidget btnIntercambiar;
    private ButtonWidget btnPose;
    private ButtonWidget btnBusto;
    private ButtonWidget btnFigura;
    private final SliderPose[] sliders = new SliderPose[com.femclothes.maniqui.PoseManiqui.ANGULOS];
    /** Mientras se arrastra un slider no se pisa con lo que llega del servidor. */
    private boolean arrastrando = false;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) arrastrando = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) arrastrando = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Un ángulo de la pose libre (2026-09-30, "pose libre por partes"): manda
     * el valor en grados por {@code PoseManiquiPayload}; el maniquí pasa a
     * pose Libre. Rango por eje en {@code PoseManiqui.RANGO}.
     */
    private final class SliderPose extends net.minecraft.client.gui.widget.SliderWidget {
        private final int indice;

        SliderPose(int x, int y, int ancho, int alto, int indice) {
            super(x, y, ancho, alto, Text.empty(), 0.5);
            this.indice = indice;
            this.value = aValor(handler.be.angulo(indice));
            updateMessage();
        }

        private float[] rango() { return com.femclothes.maniqui.PoseManiqui.RANGO[indice]; }

        private double aValor(float grados) {
            float[] r = rango();
            return (grados - r[0]) / (r[1] - r[0]);
        }

        private float grados() {
            float[] r = rango();
            return Math.round(r[0] + (float) value * (r[1] - r[0]));
        }

        void refrescar() {
            double v = aValor(handler.be.angulo(indice));
            if (Math.abs(v - value) > 0.001) {
                value = v;
                updateMessage();
            }
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("femclothes.maniqui.eje." + indice, (int) grados()));
        }

        /** Riel de madera oscura con filo del metal del tema y mango de madera (estilo del mod, 2026-09-30). */
        @Override
        public void renderWidget(DrawContext c, int mouseX, int mouseY, float delta) {
            int x0 = getX(), y0 = getY(), w = getWidth(), h = getHeight();
            c.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, 0xFF2A180C);
            c.fill(x0, y0, x0 + w, y0 + h, 0xFF5A4028);
            c.fill(x0, y0 + h - 1, x0 + w, y0 + h, EstiloPergamino.tema().claro);
            int mango = x0 + (int) (value * (w - 8));
            EstiloPergamino.fondoBoton(c, mango, y0, 8, h, isHovered(), active);
            var fuente = MinecraftClient.getInstance().textRenderer;
            Text m = getMessage();
            int tx = x0 + (w - fuente.getWidth(m)) / 2, ty = y0 + (h - 8) / 2;
            c.drawText(fuente, m, tx + 1, ty + 1, 0xFF2A180C, false);
            c.drawText(fuente, m, tx, ty, EstiloPergamino.TEXTO_CLARO, false);
        }

        @Override
        protected void applyValue() {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new com.femclothes.maniqui.PoseManiquiPayload(handler.be.getPos(), indice, grados()));
        }
    }

    private Text nombreFigura() {
        var be = handler.be;
        String pre = be.predefinida();
        if (pre != null) return Text.translatable("femclothes.maniqui.figura.pre", Text.translatable("femclothes.maniqui.skin." + pre));
        return switch (be.figura()) {
            case ManiquiBlockEntity.FIGURA_TU_SKIN -> Text.translatable("femclothes.maniqui.figura.skin");
            case ManiquiBlockEntity.FIGURA_JUGADOR -> Text.translatable("femclothes.maniqui.figura.jugador",
                    be.dueno() != null && be.dueno().name().isPresent() ? be.dueno().name().get() : "?");
            default -> Text.translatable("femclothes.maniqui.figura.maniqui");
        };
    }

    private void clickBoton(int id) {
        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        btnVista.setMessage(Text.translatable("femclothes.preview.vista",
                Text.translatable(PreviewJugador.nombreVista(anguloVista))));
        btnGirar.setMessage(Text.translatable(handler.be.girando()
                ? "femclothes.maniqui.detener" : "femclothes.maniqui.girar"));
        btnPose.setMessage(Text.translatable("femclothes.maniqui.pose",
                Text.translatable(handler.be.pose().traduccion())));
        btnFigura.setMessage(nombreFigura());
        // Sin permiso (candado cerrado de otro) todo queda de solo lectura (2026-10-04).
        boolean toca = handler.puedeTocar();
        for (ButtonWidget b : new ButtonWidget[] {btnGirar, btnPose, btnFigura, btnBusto, btnSkin, btnIntercambiar}) {
            if (b != null) b.active = toca;
        }
        for (SliderPose sl : sliders) sl.active = toca;
        campoSkin.setEditable(toca);
        if (btnEmote != null) {
            btnEmote.active = toca;
            java.util.UUID e = handler.be.emote();
            btnEmote.setMessage(e == null ? Text.translatable("femclothes.maniqui.emote.ninguno")
                    : Text.translatable("femclothes.maniqui.emote", EmotecraftCompat.nombre(e)));
        }
        btnCandado.active = com.femclothes.util.Candado.puedeAlternar(handler.estado());
        btnCandado.setMessage(Text.translatable(com.femclothes.util.Candado.cerrado(handler.estado())
                ? "femclothes.candado.cerrado" : "femclothes.candado.abierto"));
        btnBusto.setMessage(handler.be.busto() == 0 ? Text.translatable("femclothes.maniqui.busto.sin")
                : Text.translatable("femclothes.maniqui.busto", handler.be.busto(), com.femclothes.body.PerfilCuerpo.BUSTO_MAXIMO));
        if (!arrastrando) for (SliderPose s : sliders) s.refrescar();
        super.render(context, mouseX, mouseY, delta);
        dibujarPreview(context, mouseY);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    /**
     * La vista previa es la figura del maniquí (2026-10-02, "la vista previa
     * q tiene q generar es la gui es del maniquí no del player"): la misma
     * que en el bloque — pose, figura (maniquí o skin), busto, armadura y
     * ropa — dibujada por {@code ManiquiRenderer#dibujarFigura}.
     */
    private void dibujarPreview(DrawContext context, int mouseY) {
        // Sin permiso (2026-10-04, "alguien que no es dueño... la preview de su personaje usando el outfit"): se ve
        // el propio personaje con la ropa y la armadura del maniquí puestas, sin tocar nada.
        if (!handler.puedeTocar() && MinecraftClient.getInstance().player != null) {
            GuardarropasScreen.dibujarConOutfit(context, MinecraftClient.getInstance().player, handler.be,
                    handler.be.prendasPuestas(), this.x + PREVIEW_X1_LOCAL, this.y + PREVIEW_Y1_LOCAL,
                    this.x + PREVIEW_X2_LOCAL, this.y + PREVIEW_Y2_LOCAL, anguloVista, mouseY);
            return;
        }
        var dispatcher = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();
        if (!(dispatcher.get(handler.be) instanceof com.femclothes.maniqui.ManiquiRenderer renderer)) return;
        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        context.enableScissor(x1, y1, x2, y2);
        // La figura mide ~2 bloques: que entre con un poco de aire.
        float escala = (y2 - y1) * 0.86f / 2.05f;
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate((x1 + x2) / 2f, y2 - (y2 - y1) * 0.06f, 150f);
        matrices.scale(escala, -escala, escala);
        // Inclinación leve con el mouse, como la vista del jugador.
        float h = (y1 + y2) / 2f;
        float inclina = (float) Math.atan((h - mouseY) / 40f) * 10f;
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-inclina));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180f + anguloVista));
        net.minecraft.client.render.DiffuseLighting.method_34742();
        renderer.dibujarFigura(handler.be, matrices, context.getVertexConsumers(),
                net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE);
        context.draw();
        net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();
        matrices.pop();
        context.disableScissor();
    }


    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX,
                this.playerInventoryTitleY, EstiloPergamino.TEXTO, false);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
        GuardarropasScreen.marcoSombrero(context, this.x, this.y);
    }
}
