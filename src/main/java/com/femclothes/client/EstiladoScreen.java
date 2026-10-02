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
    private boolean arrastrando = false;
    /** Matrices de la última vista previa (por parte). */
    private final Map<Parte, Matrix4f> poses = new EnumMap<>(Parte.class);
    @Nullable private Text aviso;

    private final ButtonWidget[] btnApliques = new ButtonWidget[Aplique.MAXIMO_POR_PRENDA];
    private ButtonWidget btnGiro, btnEscala, btnQuitar;

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
            btnApliques[i] = boton(X_DER + i * 26, 66, 22, Text.literal(Integer.toString(i + 1)),
                    "femclothes.estilado.tooltip.aplique", () -> clickBoton(id));
        }
        boton(X_DER, 88, 14, Text.literal("<"), "femclothes.estilado.tooltip.giro", () -> clickBoton(EstiladoBlockEntity.BTN_GIRO_ATRAS));
        btnGiro = boton(X_DER + 15, 88, 64, Text.empty(), "femclothes.estilado.tooltip.giro", () -> clickBoton(EstiladoBlockEntity.BTN_GIRO));
        boton(X_DER + 80, 88, 14, Text.literal(">"), "femclothes.estilado.tooltip.giro", () -> clickBoton(EstiladoBlockEntity.BTN_GIRO));
        boton(X_DER, 108, 14, Text.literal("<"), "femclothes.estilado.tooltip.escala", () -> clickBoton(EstiladoBlockEntity.BTN_ESCALA_ATRAS));
        btnEscala = boton(X_DER + 15, 108, 64, Text.empty(), "femclothes.estilado.tooltip.escala", () -> clickBoton(EstiladoBlockEntity.BTN_ESCALA));
        boton(X_DER + 80, 108, 14, Text.literal(">"), "femclothes.estilado.tooltip.escala", () -> clickBoton(EstiladoBlockEntity.BTN_ESCALA));
        btnQuitar = boton(X_DER + 98, 88, 64, Text.translatable("femclothes.estilado.quitar"),
                "femclothes.estilado.tooltip.quitar", () -> clickBoton(EstiladoBlockEntity.BTN_QUITAR));
        boton(X_DER + 98, 108, 31, Text.literal("⟲"), "femclothes.estilado.tooltip.vista",
                () -> anguloVista = Math.floorMod(Math.round(anguloVista) - 45, 360));
        boton(X_DER + 131, 108, 31, Text.literal("⟳"), "femclothes.estilado.tooltip.vista",
                () -> anguloVista = Math.floorMod(Math.round(anguloVista) + 45, 360));
        // Textura de tela (2026-10-01, relieve): con un Molde de textura en el slot del molde.
        btnTextura = boton(X_DER, 132, 162, Text.empty(), "femclothes.estilado.tooltip.textura",
                () -> clickBoton(EstiladoBlockEntity.BTN_TEXTURA));
        // Mesa creativa (2026-10-01): elegir cualquier molde sin tenerlo.
        ButtonWidget moldeCreativo = boton(X_DER + 98, 44, 64, Text.translatable("femclothes.estilado.siguiente_molde"),
                "femclothes.estilado.tooltip.siguiente_molde", () -> clickBoton(EstiladoBlockEntity.BTN_SIGUIENTE_MOLDE));
        moldeCreativo.visible = com.femclothes.util.MaquinaCreativa.es(handler.be);
    }

    private ButtonWidget btnTextura;

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
        GarmentFeatureRenderer.previewOverride = prenda.isEmpty() ? List.of() : List.of(prenda);
        GarmentFeatureRenderer.capturaPoses = captura;
        try {
            // mouseY en el centro: la vista no se inclina con el mouse (el click necesita una pose quieta).
            PreviewJugador.dibujar(context, jugador, x1, y1, x2, y2, 78, anguloVista, (y1 + y2) / 2f);
        } finally {
            GarmentFeatureRenderer.previewOverride = null;
            GarmentFeatureRenderer.capturaPoses = null;
        }
        poses.clear();
        poses.putAll(captura);
    }

    /** Resultado del click: parte, punto sobre la caja sin inflar (px) y cara. */
    private record Toque(Parte parte, float x, float y, float z, Direction cara, float profundidad) {}

    @Nullable
    private Toque tocar(double mx, double my) {
        ItemStack prenda = handler.be.getStack(EstiladoBlockEntity.SLOT_PRENDA);
        PlayerEntity jugador = MinecraftClient.getInstance().player;
        if (prenda.isEmpty() || jugador == null || poses.isEmpty()) return null;
        boolean slim = MinecraftClient.getInstance().player.getSkinTextures().model() == SkinTextures.Model.SLIM;
        Toque mejor = null;
        com.femclothes.render.relieve.BustoRender.Busto busto =
                com.femclothes.render.GarmentFeatureRenderer.bustoDe(jugador, 0f, false);
        for (Pieza pieza : PiezasDePrenda.de(prenda, jugador)) {
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
        }
        return mejor;
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
        if (dentroDeVista(mx, my)) {
            if (button == 1) {
                arrastrando = true;
                return true;
            }
            if (button == 0) {
                Toque t = tocar(mx, my);
                EstiladoBlockEntity be = handler.be;
                if (be.getStack(EstiladoBlockEntity.SLOT_PRENDA).isEmpty()) aviso = Text.translatable("femclothes.estilado.aviso.prenda");
                else if (be.getStack(EstiladoBlockEntity.SLOT_MOLDE).getItem() instanceof com.femclothes.item.MoldeTexturaItem)
                    aviso = Text.translatable("femclothes.estilado.aviso.textura");
                else if (be.getStack(EstiladoBlockEntity.SLOT_MOLDE).isEmpty()) aviso = Text.translatable("femclothes.estilado.aviso.molde");
                else if (be.getStack(EstiladoBlockEntity.SLOT_RETAZO).isEmpty() && !com.femclothes.util.MaquinaCreativa.es(be)) aviso = Text.translatable("femclothes.estilado.aviso.retazo");
                else if (be.apliques().size() >= Aplique.MAXIMO_POR_PRENDA) aviso = Text.translatable("femclothes.estilado.aviso.lleno");
                else if (t == null) aviso = Text.translatable("femclothes.estilado.aviso.fuera");
                else {
                    aviso = null;
                    ClientPlayNetworking.send(new PonerApliquePayload(be.getPos(), t.parte().ordinal(),
                            t.x(), t.y(), t.z(), t.cara().ordinal()));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
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
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
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
        btnGiro.active = btnEscala.active = btnQuitar.active = hay;
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
        context.drawText(this.textRenderer, Text.translatable("femclothes.estilado.apliques", handler.be.apliques().size(),
                Aplique.MAXIMO_POR_PRENDA), X_DER + 80, EstiladoScreenHandler.Y_SLOTS + 4, EstiloPergamino.TEXTO, false);
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
    }
}
