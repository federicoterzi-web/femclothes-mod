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
    /** Matrices de la última vista previa (por parte). */
    private final Map<Parte, Matrix4f> poses = new EnumMap<>(Parte.class);
    /** Las mallas de la pollera y la capa del último dibujo de la vista, en pantalla (2026-10-02). */
    private final Map<String, com.femclothes.render.MallaCapturada> mallas = new java.util.HashMap<>();
    @Nullable private Text aviso;

    private final ButtonWidget[] btnApliques = new ButtonWidget[Aplique.MAXIMO_POR_PRENDA];
    private ButtonWidget btnGiro, btnEscala, btnQuitar, btnSacudir;
    /** Controles del aplique de objeto (2026-10-04): arriba de la vista, solo con uno elegido. */
    private ButtonWidget btnObjModo, btnObjVariante;
    private final ButtonWidget[] btnObjInclinar = new ButtonWidget[4];
    private SliderBlandura sliderBlandura;

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
        btnTextura = boton(X_DER, 132, 96, Text.empty(), "femclothes.estilado.tooltip.textura",
                () -> clickBoton(EstiladoBlockEntity.BTN_TEXTURA));
        // Sacudir (2026-10-04, "boton de sacudir... alternar mover el muñeco"): prende y apaga el vaivén.
        btnSacudir = boton(X_DER + 98, 132, 64, Text.empty(), "femclothes.estilado.tooltip.sacudir",
                () -> sacudiendo = !sacudiendo);
        // Aplique de objeto (2026-10-04, "poder rotar el modelo y seleccionar si item o bloque"): en la franja
        // de arriba de la vista; solo se ven con un aplique de objeto elegido.
        int by = PY1 + 3;
        btnObjModo = boton(PX1 + 4, by, 58, Text.empty(), "femclothes.estilado.tooltip.obj_modo",
                () -> clickBoton(EstiladoBlockEntity.BTN_OBJ_MODO));
        btnObjVariante = boton(PX1 + 64, by, 58, Text.empty(), "femclothes.estilado.tooltip.obj_variante",
                () -> clickBoton(EstiladoBlockEntity.BTN_OBJ_VARIANTE));
        int[] ids = { EstiladoBlockEntity.BTN_OBJ_INCLINAR_X_MAS, EstiladoBlockEntity.BTN_OBJ_INCLINAR_X_MENOS,
                EstiladoBlockEntity.BTN_OBJ_INCLINAR_Y_MENOS, EstiladoBlockEntity.BTN_OBJ_INCLINAR_Y_MAS };
        String[] glifos = { "▲", "▼", "◀", "▶" };
        for (int i = 0; i < 4; i++) {
            int id = ids[i];
            btnObjInclinar[i] = boton(PX1 + 126 + i * 17 + (i >= 2 ? 4 : 0), by, 16, Text.literal(glifos[i]),
                    i < 2 ? "femclothes.estilado.tooltip.obj_inclinar_x" : "femclothes.estilado.tooltip.obj_inclinar_y",
                    () -> clickBoton(id));
        }
        // Blandura del aplique elegido (2026-10-04, "1 slider de blandura").
        sliderBlandura = new SliderBlandura(this.x + X_DER, this.y + 150, 162, 11);
        sliderBlandura.setTooltip(Tooltip.of(Text.translatable("femclothes.estilado.tooltip.blandura")));
        this.addDrawableChild(sliderBlandura);
        // Mesa creativa (2026-10-01): elegir cualquier molde sin tenerlo.
        ButtonWidget moldeCreativo = boton(X_DER + 98, 44, 64, Text.translatable("femclothes.estilado.siguiente_molde"),
                "femclothes.estilado.tooltip.siguiente_molde", () -> clickBoton(EstiladoBlockEntity.BTN_SIGUIENTE_MOLDE));
        moldeCreativo.visible = com.femclothes.util.MaquinaCreativa.es(handler.be);
    }

    private ButtonWidget btnTextura;

    /** Cuánto se mueve como tela el aplique elegido: 0 % = rígido, 100 % = muy suelto, de a 10 %. */
    private final class SliderBlandura extends net.minecraft.client.gui.widget.SliderWidget {
        private int pasoEnviado = -1;

        SliderBlandura(int x, int y, int ancho, int alto) {
            super(x, y, ancho, alto, Text.empty(), 0.5);
            updateMessage();
        }

        private int paso() {
            return (int) Math.round(value * EstiladoBlockEntity.BLANDURA_PASOS);
        }

        /** Sigue al aplique elegido (salvo mientras se lo arrastra). */
        void refrescar(@Nullable Aplique a) {
            active = a != null;
            if (a != null && !isFocused() && pasoEnviado < 0) {
                double v = Math.round(a.blandura() * EstiladoBlockEntity.BLANDURA_PASOS) / (double) EstiladoBlockEntity.BLANDURA_PASOS;
                if (Math.abs(v - value) > 1e-3) value = v;
            }
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("femclothes.estilado.blandura",
                    paso() * 100 / EstiladoBlockEntity.BLANDURA_PASOS));
        }

        @Override
        protected void applyValue() {
            int paso = paso();
            if (paso == pasoEnviado) return;
            pasoEnviado = paso;
            clickBoton(EstiladoBlockEntity.BTN_BLANDURA_BASE + paso);
        }

        @Override
        public void onRelease(double mx, double my) {
            super.onRelease(mx, my);
            pasoEnviado = -1;
        }

        /** Riel de madera con mango, como los sliders de pose del Maniquí. */
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
            int tx = x0 + (w - fuente.getWidth(m)) / 2, ty = y0 + (h - 8) / 2 + 1;
            c.drawText(fuente, m, tx + 1, ty + 1, 0xFF2A180C, false);
            c.drawText(fuente, m, tx, ty, EstiloPergamino.TEXTO_CLARO, false);
        }
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
        // Una armadura (2026-10-02, "extender apliques para toda armadura o
        // wearable"): puesta de mentira en el inventario del jugador SOLO del
        // cliente durante este dibujo, como la vista previa del Guardarropas.
        var armadura = jugador.getInventory().armor;
        ItemStack[] antes = new ItemStack[armadura.size()];
        for (int i = 0; i < antes.length; i++) antes[i] = armadura.get(i);
        net.minecraft.entity.EquipmentSlot slot = delMod ? null : slotDe(prenda);
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
            GarmentFeatureRenderer.capturaPoses = null;
            GarmentFeatureRenderer.capturaMallas = null;
            for (int i = 0; i < antes.length; i++) armadura.set(i, antes[i]);
        }
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
                         Aplique.Superficie superficie) {
        Toque(Parte parte, float x, float y, float z, Direction cara, float profundidad) {
            this(parte, x, y, z, cara, profundidad, Aplique.Superficie.CAJA);
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

    @Nullable
    private Toque tocar(double mx, double my) {
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
            if (button == 0) {
                Toque t = tocar(mx, my);
                EstiladoBlockEntity be = handler.be;
                boolean objeto = !be.getStack(EstiladoBlockEntity.SLOT_OBJETO).isEmpty();
                if (be.getStack(EstiladoBlockEntity.SLOT_PRENDA).isEmpty()) aviso = Text.translatable("femclothes.estilado.aviso.prenda");
                else if (!objeto && be.getStack(EstiladoBlockEntity.SLOT_MOLDE).getItem() instanceof com.femclothes.item.MoldeTexturaItem)
                    aviso = Text.translatable("femclothes.estilado.aviso.textura");
                else if (!objeto && be.getStack(EstiladoBlockEntity.SLOT_MOLDE).isEmpty()) aviso = Text.translatable("femclothes.estilado.aviso.molde");
                else if (!objeto && !(be.getStack(EstiladoBlockEntity.SLOT_RETAZO).getItem() instanceof com.femclothes.aplique.RetazoApliqueItem)
                        && !com.femclothes.util.MaquinaCreativa.es(be)) aviso = Text.translatable("femclothes.estilado.aviso.retazo");
                else if (be.apliques().size() >= Aplique.MAXIMO_POR_PRENDA) aviso = Text.translatable("femclothes.estilado.aviso.lleno");
                else if (t == null) aviso = Text.translatable("femclothes.estilado.aviso.fuera");
                else {
                    aviso = null;
                    ClientPlayNetworking.send(new PonerApliquePayload(be.getPos(), t.parte().ordinal(),
                            t.x(), t.y(), t.z(), t.cara().ordinal(), t.superficie().ordinal()));
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private List<ButtonWidget> objetoBotones() {
        List<ButtonWidget> l = new java.util.ArrayList<>(List.of(btnObjModo, btnObjVariante));
        l.addAll(List.of(btnObjInclinar));
        return l;
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
        sliderBlandura.refrescar(hay ? apliques.get(sel) : null);
        com.femclothes.aplique.ObjetoAplique obj = hay ? apliques.get(sel).objeto() : null;
        for (ButtonWidget b : objetoBotones()) b.visible = obj != null;
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
        context.drawText(this.textRenderer, Text.translatable("femclothes.estilado.slot.objeto"), X_DER + 77, y + 30, EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, Text.translatable("femclothes.estilado.apliques", handler.be.apliques().size(),
                Aplique.MAXIMO_POR_PRENDA), X_DER + 102, EstiladoScreenHandler.Y_SLOTS + 4, EstiloPergamino.TEXTO, false);
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
        context.fill(ox - 1, oy - 1, ox + 17, oy + 17, 0xFF2A180C);
        context.fill(ox, oy, ox + 16, oy + 16, 0xFF6B5A78);
    }
}
