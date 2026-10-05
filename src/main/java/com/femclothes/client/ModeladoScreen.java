package com.femclothes.client;

import com.femclothes.modelado.ComboCorte;
import com.femclothes.modelado.ModeladoBlockEntity;
import com.femclothes.modelado.ModeladoScreenHandler;
import com.femclothes.render.GarmentFeatureRenderer;
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
 * Pantalla de la Mesa de Modelado. Fondo propio (generado por
 * tools/generar_textura_modelado.py). Apagada = editable (almacén, borrador,
 * fijar); prendida, la GUI ni siquiera llega a abrirse (ver
 * ModeladoBlock#onUse) — por eso acá no hace falta un modo de solo lectura.
 *
 * Panel de tres columnas, {@link #ANCHO}x{@link #ALTO} (no los 176x166
 * vanilla): visor 3D (izquierda, {@link #PX} de ancho) + config (medio,
 * margen {@link #M_MEDIO}: categoría/activo/prenda/salida/anclaje/lado/
 * fijar/fijadas + inventario del jugador) + storage (derecha, margen
 * {@link #M_DERECHA}: almacén compartido de 27 arriba, banco de 12 por
 * categoría abajo) — a pedido, "el storage guarda moldes y la
 * configuración se guarda en el medio".
 *
 * Ni el Activo ni el banco de storage-por-prenda se reposicionan al
 * cambiar de categoría: son slots FIJOS atados a un adaptador que mira a
 * {@code be.categoria()} en cada acceso (ver
 * {@code ModeladoScreenHandler#activoAdaptador}) — Slot.x/Slot.y son
 * {@code final} en vanilla, así que "ciclar por categoría" cambia A QUÉ
 * miran los mismos slots, no dónde están.
 *
 * Preview: visor 3D real ({@link PreviewJugador}) del jugador vistiendo la
 * prenda en construcción (slot PRENDA + fijadas ya aplicadas, ver
 * {@code ModeladoBlockEntity#previsualizar}) — no hace falta tenerla puesta
 * encima. Arrastre para girar fino + {@link #btnVista} para saltar entre
 * las 4 perspectivas (frente/derecha/espalda/izquierda) — a pedido
 * (2026-09-20), mismo mecanismo que {@code TinturasScreen}/
 * {@code SublimadoraScreen}, ya no sigue al mouse en vivo. Ver
 * {@link #dibujarPreview}.
 *
 * Sin botón de Encender: la máquina se prende sola al cerrar esta pantalla
 * (ver {@code ModeladoScreenHandler#onClosed}) y se apaga sola cuando
 * termina de procesar (ver {@code ModeladoBlockEntity#tick}) — a pedido,
 * "simplemente cuando se sale se enciende".
 */
public class ModeladoScreen extends HandledScreen<ModeladoScreenHandler> {

    private static final Identifier TEXTURE = Identifier.of("femclothes", "textures/gui/container/modelado.png");
    /** Esquema visual de remera con pines (2026-09-24) — ver {@link #dibujarEsquemaRemera}. */
    private static final Identifier[] TEXTURE_ESQUEMA = {
            Identifier.of("femclothes", "textures/gui/container/esquema_remera.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_pantalon.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_medias.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_calientabrazos.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_pollera.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_capa.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_sombrero.png"),
            Identifier.of("femclothes", "textures/gui/container/esquema_banda.png"),
    };
    /** Cuadros de la chincheta: 0 sin fijar (aguja a la vista), 1 a mitad de clavarse, 2 fijada (sin aguja). */
    private static final Identifier[] TEXTURE_CHINCHETA = {
            Identifier.of("femclothes", "textures/gui/container/chincheta_0.png"),
            Identifier.of("femclothes", "textures/gui/container/chincheta_1.png"),
            Identifier.of("femclothes", "textures/gui/container/chincheta_2.png"),
    };
    /** Ancho de la franja del visor 3D, a la izquierda. */
    private static final int PX = 100;
    private static final int M_MEDIO = 19 + PX;
    // 240 y no 162 (2026-09-24, "quiero que se agrande" — el panel del
    // esquema de remera quedaba tan chico que el pin de Calce chocaba con
    // el slot Prenda y había que correrlo a mano). Ver ESQUEMA_ANCHO/ALTO.
    private static final int M_MEDIO_ANCHO = 240;
    private static final int M_DERECHA = M_MEDIO + M_MEDIO_ANCHO + 20;
    private static final int ANCHO = M_DERECHA + 162 + 19;
    // 334 y no 264 (2026-09-24, mismo pedido de arriba): el esquema mide
    // 136 y ahora arranca en y=36 (debajo del botón de categoría, que lo
    // tapaba), y Prenda/Salida + Fijar comparten una fila propia debajo.
    private static final int ALTO = 334;

    private ButtonWidget btnCategoria;
    private ButtonWidget btnFijar;
    private ButtonWidget btnAnclaje, btnLado;
    private ButtonWidget btnSimetria;
    private ButtonWidget btnModelar;
    private ButtonWidget btnVista;
    private ButtonWidget btnGuardarDiseno;
    private net.minecraft.client.gui.widget.TextFieldWidget txtNombreDiseno;
    private final BotonChincheta[] btnPines = new BotonChincheta[ModeladoBlockEntity.PINES_POR_CATEGORIA];
    private final BotonDiseno[] btnDisenos = new BotonDiseno[ModeladoBlockEntity.DISENOS_MAXIMO];

    public ModeladoScreen(ModeladoScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = ANCHO;
        this.backgroundHeight = ALTO;
        this.titleX = M_MEDIO;
        this.titleY = 9;
        this.playerInventoryTitleX = M_MEDIO;
        this.playerInventoryTitleY = 236;
    }

    @Override
    protected void init() {
        EstiloPergamino.usarTema(EstiloPergamino.Tema.COBRE);
        super.init();

        btnCategoria = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO, this.y + 20, M_MEDIO_ANCHO, 14, Text.literal(""), b -> clickBoton(ModeladoBlockEntity.BTN_CATEGORIA));
        btnCategoria.setTooltip(Tooltip.of(Text.translatable("femclothes.modelado.tooltip.categoria")));
        this.addDrawableChild(btnCategoria);

        btnFijar = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO + 50, this.y + 176, M_MEDIO_ANCHO - 50, 16, Text.translatable("femclothes.modelado.boton.fijar"), b -> clickBoton(ModeladoBlockEntity.BTN_FIJAR));
        btnFijar.setTooltip(Tooltip.of(Text.translatable("femclothes.modelado.tooltip.fijar")));
        this.addDrawableChild(btnFijar);

        // Anclaje/Lado: únicos dos controles de ajuste que quedan (a
        // pedido) — antes competían con la fila de remera/calientabrazos.
        btnAnclaje = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO, this.y + 198, 117, 16, Text.literal(""), b -> clickBoton(ModeladoBlockEntity.BTN_ANCLAJE));
        btnAnclaje.setTooltip(Tooltip.of(Text.translatable("femclothes.modelado.tooltip.anclaje")));
        this.addDrawableChild(btnAnclaje);
        btnLado = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO + 123, this.y + 198, 117, 16, Text.literal(""), b -> clickBoton(ModeladoBlockEntity.BTN_LADO));
        btnLado.setTooltip(Tooltip.of(Text.translatable("femclothes.modelado.tooltip.lado")));
        this.addDrawableChild(btnLado);

        // Simetría de manga (2026-09-24, "agreguemos un boton de
        // simetria") — solo para REMERA, mismo hueco que Anclaje/Lado
        // (que para esa categoría ya están ocultos, ver refrescar()).
        btnSimetria = new EstiloPergamino.BotonPergamino(this.x + 8, this.y + 292, 86, 16, Text.literal(""), b -> clickBoton(ModeladoBlockEntity.BTN_SIMETRIA));
        btnSimetria.setTooltip(Tooltip.of(Text.translatable("femclothes.modelado.tooltip.simetria")));
        this.addDrawableChild(btnSimetria);

        // Modelar, ENCIMA de la flecha Entrada -> Salida (2026-09-28, "a
        // modeladora le agreguemos el boton modelar") — como Teñir y Prensar.
        btnModelar = new EstiloPergamino.BotonPergamino(this.x + M_MEDIO + 96, this.y + 184, 48, 16,
                Text.translatable("femclothes.modelado.boton.modelar"), b -> clickBoton(ModeladoBlockEntity.BTN_MODELAR));
        btnModelar.setTooltip(Tooltip.of(Text.translatable("femclothes.modelado.tooltip.modelar")));
        this.addDrawableChild(btnModelar);

        // Casilleros de diseño (2026-09-27, "que los slots donde se fijaban
        // sirvan para guardar el diseño completo de todos los pines como 1
        // 2 etc"): click izquierdo carga TODAS las fijadas de ese diseño,
        // click derecho lo borra. El nombre puesto en Guardar aparece acá
        // en el hover — ver BotonDiseno#mouseClicked.
        for (int i = 0; i < btnDisenos.length; i++) {
            int idx = i;
            btnDisenos[i] = new BotonDiseno(idx, this.x + M_MEDIO + i * 20, this.y + 214, 18, 14,
                    b -> clickBoton(ModeladoBlockEntity.BTN_CARGAR_DISENO_BASE + idx),
                    () -> clickBoton(ModeladoBlockEntity.BTN_BORRAR_DISENO_BASE + idx));
            this.addDrawableChild(btnDisenos[i]);
        }

        // Una chincheta por cada slot de corte de remera (2026-09-24, "una
        // chincheta en cada slot de corte... fijarlo con la chincheta y el
        // molde vuelve al almacen"): a la derecha del slot, sobre el dibujo.
        for (int i = 0; i < btnPines.length; i++) {
            btnPines[i] = new BotonChincheta(0, 0, b -> { });
            this.addSelectableChild(btnPines[i]); // se dibujan a mano en render(), encima del ícono fantasma
        }

        // Un solo botón que cicla las 4 perspectivas — a pedido (2026-09-20,
        // "el boton de ciclado de preview en las tres estaciones"), mismo
        // mecanismo que Tinturas/Sublimadora (ver PreviewJugador). Antes
        // esta pantalla seguía el mouse en vivo sin ningún control fijo;
        // ahora es arrastre + este botón, igual que las otras dos.
        btnVista = new EstiloPergamino.BotonPergamino(this.x + 8, this.y + 218, 86, 16, Text.literal(""), b -> anguloVista = Math.floorMod(Math.round(anguloVista) + 90, 360));
        btnVista.setTooltip(Tooltip.of(Text.translatable("femclothes.preview.tooltip.vista")));
        this.addDrawableChild(btnVista);

        // Casillero de nombre + Guardar (2026-09-27, "un casillero donde
        // poner el nombre para que save los guarde... con el nombre en el
        // hover"): el nombre viaja al servidor como paquete propio (no
        // entra en clickButton(int), ver GuardarDisenoPayload) porque es
        // texto libre, no un id — el resto de esta pantalla no lo necesita.
        txtNombreDiseno = new net.minecraft.client.gui.widget.TextFieldWidget(
                this.textRenderer, this.x + 8, this.y + 238, 86, 14, Text.translatable("femclothes.modelado.nombre_diseno"));
        txtNombreDiseno.setMaxLength(24);
        txtNombreDiseno.setPlaceholder(Text.translatable("femclothes.modelado.nombre_diseno"));
        this.addDrawableChild(txtNombreDiseno);

        btnGuardarDiseno = new EstiloPergamino.BotonPergamino(this.x + 8, this.y + 256, 86, 16, Text.translatable("femclothes.modelado.boton.guardar_diseno"), b -> {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new com.femclothes.modelado.GuardarDisenoPayload(this.handler.be.getPos(), txtNombreDiseno.getText()));
            txtNombreDiseno.setText("");
        });
        btnGuardarDiseno.setTooltip(Tooltip.of(Text.translatable("femclothes.modelado.tooltip.guardar_diseno")));
        this.addDrawableChild(btnGuardarDiseno);

        refrescar();
    }

    /**
     * Manda el click al SERVIDOR nada más — ya NO corre {@code be.onButtonClick}
     * también acá para "responder al instante". Ese doble-camino (mutar el
     * {@code be} del cliente Y separado mandarle el click al servidor) tenía
     * una carrera real: el sync periódico de propiedades del servidor podía
     * pisar la mutación optimista del cliente ANTES de que el servidor
     * terminara de procesar el click, así que un click rápido de Anclaje
     * seguido de Fijar podía fijar con el valor VIEJO aunque el botón ya
     * mostrara el nuevo — el bug reportado jugando ("se marcó anchor top
     * aunque estaba en bottom"). Los botones ya están gateados por
     * {@code .active} (ver {@link #refrescar}), así que no hace falta
     * replicar la validación acá: el servidor decide, como en cualquier
     * pantalla vanilla (Telar, Yunque, etc.).
     */
    void clickBoton(int id) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.interactionManager == null) return;
        client.interactionManager.clickButton(this.handler.syncId, id);
    }

    // ── rotación del preview a mano — mismo mecanismo que TinturasScreen/SublimadoraScreen ──
    // Y2 164->208 (2026-09-24, "quiero que se agrande"): la columna
    // izquierda no tenía nada más para hacer crecer cuando el panel del
    // medio se agrandó — sin esto quedaba un hueco vacío abajo del visor.
    private static final int PREVIEW_X1_LOCAL = 8, PREVIEW_Y1_LOCAL = 18, PREVIEW_X2_LOCAL = 94, PREVIEW_Y2_LOCAL = 214;
    /** En GRADOS, no radianes — ver el javadoc de {@link PreviewJugador}. */
    private float anguloVista = 0f;
    private boolean arrastrandoPreview = false;
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

    /**
     * Sin esto, escribir una "e" en {@link #txtNombreDiseno} cierra la
     * pantalla — {@code HandledScreen#keyPressed} cierra con la tecla de
     * inventario (E de fábrica) apenas el foco NO consume la tecla, y
     * {@code TextFieldWidget#keyPressed} solo consume teclas de control
     * (flechas, Ctrl+C/V, etc.), nunca letras sueltas (esas van por
     * {@code charTyped}). Mismo mecanismo que usa {@code AnvilScreen} para
     * su casillero de renombrar.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return (!txtNombreDiseno.keyPressed(keyCode, scanCode, modifiers) && !txtNombreDiseno.isActive())
                ? super.keyPressed(keyCode, scanCode, modifiers) : true;
    }

    /**
     * Layout FIJO — nada de mostrar/ocultar filas ni reposicionar botones.
     * Se probó (dos veces: primero ocultando filas enteras, después
     * reubicando lo de abajo para "tapar" el hueco) y las dos dejaban un
     * vacío en OTRO lado del panel — la última vez, entre la fila de
     * fijadas (que subía) y el inventario del jugador (que no se mueve,
     * está horneado en la textura de fondo). Con posiciones fijas no hay
     * hueco posible: lo que no aplica se ve gris (deshabilitado), no
     * desaparece. Sacrifica el "la GUI cambia sutil por prenda" a cambio de
     * que nunca más se desalinee.
     */
    private void refrescar() {
        ModeladoBlockEntity be = this.handler.be;
        boolean prendida = be.encendida();
        ModeladoBlockEntity.Categoria categoria = be.categoria();

        btnCategoria.setMessage(Text.translatable("femclothes.modelado.categoria",
                Text.translatable("femclothes.modelado.categoria." + categoria.name().toLowerCase(java.util.Locale.ROOT))));
        btnCategoria.active = !prendida;

        btnVista.setMessage(Text.translatable("femclothes.preview.vista",
                Text.translatable(PreviewJugador.nombreVista(anguloVista))));

        btnFijar.active = !prendida && !be.activoVacio();

        // El anclaje/lado no aplican a REMERA (un Variante entero, sin
        // anclaje) NI a un molde de corte DIRECTO (ya trae sus anclajes
        // horneados de fábrica, los ignora — bug reportado jugando: sin
        // esto quedaban prendidos aunque no hicieran nada con un preset
        // puesto). Dentro de PANTALON tampoco aplican si el molde es de
        // tiro en vez de largo, pero eso no vale la pena distinguir acá:
        // clickearlos ahí no hace nada al fijar, inofensivo.
        boolean esExtremidad = categoria != ModeladoBlockEntity.Categoria.REMERA && !be.activoEsComboDirecto();
        // Anclaje además queda afuera para PANTALON (2026-09-23,
        // "pantalones se fija solo el corte inferior" — perdió el
        // anclaje Superior). Lado SÍ sigue aplicando ahí: las dos
        // piernas pueden seguir teniendo largos distintos.
        boolean tieneAnclaje = esExtremidad && categoria != ModeladoBlockEntity.Categoria.PANTALON;

        btnAnclaje.active = !prendida && tieneAnclaje;
        btnAnclaje.setMessage(Text.translatable(be.anclaje() == ModeladoBlockEntity.Anclaje.SUPERIOR
                ? "femclothes.cobertura.superior_boton" : "femclothes.cobertura.inferior_boton"));

        btnLado.active = !prendida && esExtremidad;
        btnLado.setMessage(Text.translatable("femclothes.region." + be.ladoBorrador().clave()));

        // El esquema visual reemplaza Activo/Anclaje/Lado SOLO para REMERA
        // (2026-09-24, "un esquema de la prenda... arrastrar un patron a
        // un slot en el esquema") — mismo hueco de pantalla, no un hueco
        // nuevo (ver "Layout FIJO" más arriba: esto sustituye contenido,
        // no lo vacía). Simetría ocupa el hueco de Anclaje/Lado, al revés:
        // solo se ve para REMERA.
        // Con pines en las 4 categorías (2026-09-26) Activo/Fijar/Anclaje/Lado
        // quedaron sin uso: el esquema los reemplaza. Simetría aplica a todo
        // pin de lado (mangas, botas, cortes izq/der de medias y cubrebrazos).
        btnFijar.visible = false;
        btnModelar.active = be.puedeModelar();
        btnAnclaje.visible = false;
        btnLado.visible = false;
        btnSimetria.visible = true;
        btnSimetria.setMessage(Text.translatable("femclothes.modelado.simetria",
                Text.translatable(be.remeraSimetria() ? "femclothes.si" : "femclothes.no")));

        int guardados = be.disenosGuardados();
        btnGuardarDiseno.active = !prendida && !be.fijadas().isEmpty() && guardados < ModeladoBlockEntity.DISENOS_MAXIMO;
        txtNombreDiseno.active = !prendida;

        for (int i = 0; i < btnDisenos.length; i++) {
            String nombre = be.nombreDiseno(i);
            btnDisenos[i].setPosition(this.x + M_MEDIO + i * 20, this.y + 214);
            btnDisenos[i].setMessage(Text.literal(String.valueOf(i + 1)));
            btnDisenos[i].active = !prendida && nombre != null;
            btnDisenos[i].setTooltip(nombre == null ? null : Tooltip.of(Text.translatable(
                    "femclothes.modelado.tooltip.casillero_diseno", nombre)));
        }

        int cat = categoria.ordinal();
        for (int i = 0; i < btnPines.length; i++) {
            int p = cat * ModeladoBlockEntity.PINES_POR_CATEGORIA + i;
            boolean usable = ModeladoBlockEntity.ROLES[cat][i] != ModeladoBlockEntity.Rol.NINGUNO;
            boolean fijado = be.pinFijado(p);
            boolean conMolde = !be.getStack(ModeladoBlockEntity.pinSlot(p)).isEmpty();
            int[] c = ModeladoScreenHandler.PIN_BTN[cat][i];
            btnPines[i].setPosition(this.x + M_MEDIO + c[0] - 8, this.y + c[1] - 8);
            btnPines[i].pin = p;
            btnPines[i].visible = usable;
            btnPines[i].active = !prendida && (conMolde || fijado);
            btnPines[i].actualizar(fijado);
            ComboCorte combo = be.pinCombo(p);
            btnPines[i].setTooltip(Tooltip.of(fijado && combo != null
                    ? Text.translatable("femclothes.modelado.tooltip.chincheta_quitar", descripcion(combo))
                    : Text.translatable("femclothes.modelado.tooltip.chincheta")));
        }
    }

    /**
     * El ítem que produjo esta fijada (ver {@code ComboCorte#iconoOrigen})
     * — a pedido (2026-09-16): la fila de fijadas mostraba un código de
     * texto ("P^", "Mv"...); ahora muestra el ícono real del molde, con
     * una flechita de anclaje/lado superpuesta (ver {@link #anclajeDe}/
     * {@link #ladoDe}) en vez de codificarlo en texto.
     */
    private static ItemStack iconoDe(ComboCorte combo) {
        return combo.iconoOrigen()
                .map(net.minecraft.registry.Registries.ITEM::get)
                .filter(item -> item != null)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private static Text descripcion(ComboCorte combo) {
        StringBuilder sb = new StringBuilder();
        combo.remeraLargo().ifPresent(v -> sb.append("Remera largo: ").append(v.clave).append(' '));
        combo.remeraManga().ifPresent(v -> sb.append("Remera manga: ").append(v.clave).append(' '));
        combo.remeraCuello().ifPresent(v -> sb.append("Remera cuello: ").append(v.clave).append(' '));
        combo.pantalonTiro().ifPresent(v -> sb.append("Tiro: ").append(v.clave).append(' '));
        combo.pantalonLargoSuperior().ifPresent(v -> sb.append("Pantalón sup: ").append(v.clave).append(' '));
        combo.pantalonLargoInferior().ifPresent(v -> sb.append("Pantalón inf: ").append(v.clave).append(' '));
        combo.mediasLargoSuperior().ifPresent(v -> sb.append("Medias sup: ").append(v.clave).append(' '));
        combo.mediasLargoInferior().ifPresent(v -> sb.append("Medias inf: ").append(v.clave).append(' '));
        combo.calientabrazosCoberturaSuperior().ifPresent(v -> sb.append("Calient. sup: ").append(v.clave).append(' '));
        combo.calientabrazosCoberturaInferior().ifPresent(v -> sb.append("Calient. inf: ").append(v.clave).append(' '));
        combo.calce().ifPresent(v -> sb.append("Calce: ").append(v.clave).append(' '));
        combo.pollera().ifPresent(p -> {
            p.largo().ifPresent(v -> sb.append("Pollera largo: ").append(v.clave).append(' '));
            p.forma().ifPresent(v -> sb.append("Pollera forma: ").append(v.clave).append(' '));
            p.capaLargo().ifPresent(v -> sb.append("Capa largo: ").append(v.asString()).append(' '));
            p.capaRuedo().ifPresent(v -> sb.append("Capa ruedo: ").append(v.asString()).append(' '));
            p.capaCapucha().ifPresent(v -> sb.append(v ? "Con capucha " : "Sin capucha "));
            p.capaCuello().ifPresent(v -> sb.append(v ? "Cuello alto " : "Sin cuello "));
        });
        if (combo.lado() != com.femclothes.region.Lado.AMBAS) sb.append('(').append(combo.lado().clave()).append(')');
        return Text.literal(sb.toString().trim());
    }

    /** Los slots de prenda base y resultado son grandes: el ítem se dibuja a 1.5x dentro de su marco de 32x32. */
    private static boolean esSlotGrande(int localX, int localY) {
        return localY == ModeladoScreenHandler.SLOT_Y_IO
                && (localX == M_MEDIO + ModeladoScreenHandler.ENTRADA_X || localX == M_MEDIO + ModeladoScreenHandler.SALIDA_X);
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
     * {@code this.x}/{@code this.y}, NO recalculados — son el mismo valor
     * que {@code HandledScreen} ya usa para ubicar los slots. Recalcular acá
     * (como hacía la v1) corre el riesgo de un redondeo distinto en algún
     * caso de ventana/GUI Scale y desalinea el fondo de los slots reales,
     * que es justo lo que se reportó jugando.
     */
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // El overload de 7 args asume que el PNG es 256x256 (convención
        // vieja de Minecraft — loom.png vanilla SÍ está rellenado a 256x256
        // por eso ClothingLoomScreen nunca mostró este bug). El nuestro es
        // 300x300 real, así que hay que pasar el tamaño real de textura o
        // el fondo se muestrea con el UV equivocado y los slots dibujados
        // quedan corridos respecto a los Slot interactivos reales (que sí
        // usan coordenadas de pixel exactas) — el bug reportado jugando.
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0,
                this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);
        dibujarEsquema(context);
    }

    // Mismo tamaño/posición que tools/generar_esquema_remera.py — si se
    // toca el layout ahí, hay que tocarlo acá y en ModeladoScreenHandler
    // (las posiciones de los 8 Slot reales tienen que matchear los pines
    // dibujados acá).
    // 240x136 y no 162x92 (2026-09-24, "quiero que se agrande" — el cuadro
    // quedaba tan chico que el pin de Calce chocaba con el slot Prenda).
    // Misma proporción que el PNG real (1663x946, ratio 1.7589) así no se
    // deforma. Con este tamaño los 8 pines entran sin superponerse a nada
    // (remedidos por color sobre el PNG, ver el historial de esta sesión).
    private static final int ESQUEMA_ANCHO = 240, ESQUEMA_ALTO = 136;
    /** Debajo del botón de categoría (y=18..34) — antes arrancaba en 18 y el botón tapaba el pin de Cuello. */
    private static final int ESQUEMA_Y = 36;

    /**
     * Esquema visual de remera con 8 pines (2026-09-24, "un esquema de la
     * prenda... arrastrar un patron a un slot en el esquema", + "la idea
     * de materiales/textura es para agregar esas cosas ahi pongamos eso de
     * nuevo"). Asset pergamino/madera curado a pedido ("me gustaba mas el
     * estilo q te pase") — YA NO es el placeholder gris/bisel generado por
     * {@code tools/generar_esquema_remera.py} (ese script queda solo como
     * referencia histórica del mapeo de coordenadas). Pintado OPACO: tapa
     * el slot Activo horneado en modelado.png en esa misma zona, que para
     * REMERA ya no se usa (ver {@code ModeladoBlockEntity#isValid}). El PNG
     * real es de resolución bastante más alta que {@link #ESQUEMA_ANCHO} —
     * Minecraft lo reescala solo al dibujarlo en este cuadro (la
     * resolución del archivo no tiene que matchear el tamaño lógico), por
     * eso sale nítido. Los 8 pines de {@link ModeladoScreenHandler} están
     * alineados a mano con la posición real de cada pin dibujado acá — si
     * se reemplaza el PNG por otro con otra disposición, hay que remedir
     * esto.
     */
    private void dibujarEsquema(DrawContext context) {
        int ex = this.x + M_MEDIO, ey = this.y + ESQUEMA_Y;
        context.drawTexture(TEXTURE_ESQUEMA[this.handler.be.categoria().ordinal()], ex, ey, 0, 0,
                ESQUEMA_ANCHO, ESQUEMA_ALTO, ESQUEMA_ANCHO, ESQUEMA_ALTO);
    }

    /**
     * Visor 3D: renderiza al jugador vistiendo la prenda en construcción
     * (no la real que tiene puesta) mediante
     * {@link GarmentFeatureRenderer#previewOverride} — se arma la lista de
     * prendas a mostrar (lo que ya lleva puesto, menos cualquier pieza de
     * la MISMA clase que la de la Mesa, más la de la Mesa) justo antes del
     * único {@code drawEntity} y se limpia enseguida después, así que el
     * render normal del jugador en el mundo nunca ve este override.
     */
    private void dibujarPreview(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity jugador = client.player;
        if (jugador == null) return;

        ItemStack enConstruccion = this.handler.be.previsualizar();
        List<ItemStack> prendas = new ArrayList<>(GarmentFeatureRenderer.equipadas(jugador));
        boolean esSombrero = com.femclothes.render.AccesorioRenderer.es(enConstruccion);
        if (!enConstruccion.isEmpty() && !esSombrero) {
            prendas.removeIf(s -> s.getItem().getClass() == enConstruccion.getItem().getClass());
            prendas.add(enConstruccion);
        }

        int x1 = this.x + PREVIEW_X1_LOCAL, y1 = this.y + PREVIEW_Y1_LOCAL;
        int x2 = this.x + PREVIEW_X2_LOCAL, y2 = this.y + PREVIEW_Y2_LOCAL;
        GarmentFeatureRenderer.previewOverride = prendas;
        GarmentFeatureRenderer.sombreroOverride = esSombrero ? enConstruccion : null;
        try {
            PreviewJugador.dibujar(context, jugador, x1, y1, x2, y2, Math.round(35 * zoomVista), anguloVista, (float) mouseY);
        } finally {
            GarmentFeatureRenderer.previewOverride = null;
            GarmentFeatureRenderer.sombreroOverride = null;
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Título e "Inventario" en marrón oscuro (el super los dibuja en 0x404040,
        // pensado para el panel gris de antes).
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX,
                this.playerInventoryTitleY, EstiloPergamino.TEXTO, false);
        // Preview stub: una línea entre el almacén y la fila activo/prenda/
        // salida, que además guía el flujo paso a paso — "no sé cómo hacerla
        // recortar" era justamente esto: ni una pista de qué falta hacer.
        dibujarRotulosStorage(context);
        dibujarFantasmasDePines(context);
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
            String clave = switch (rol) {
                case NINGUNO, PERS_IZQ2, PERS_IZQ3, PERS_DER2, PERS_DER3 -> null;
                // las columnas de 3 llevan UN título arriba, no uno por slot (los slots están casi pegados)
                case PERS_IZQ1 -> "pers_izq";
                case PERS_DER1 -> "pers_der";
                default -> rol.name().toLowerCase(java.util.Locale.ROOT);
            };
            if (clave == null) continue;
            Text t = Text.translatable("femclothes.modelado.rol." + clave);
            int[] pos = ModeladoScreenHandler.PIN_POS[cat][i];
            float ancho = this.textRenderer.getWidth(t) * escala;
            // El borde derecho del texto queda en el centro del slot: la chincheta ocupa la esquina superior derecha.
            float x0 = Math.max(M_MEDIO + 26, Math.min(M_MEDIO + pos[0] + 10 - ancho, M_MEDIO + 214 - ancho));
            context.getMatrices().push();
            context.getMatrices().translate(x0, pos[1] - 7, 0);
            context.getMatrices().scale(escala, escala, 1f);
            context.fill(-2, -1, this.textRenderer.getWidth(t) + 2, 9, 0x99E8D2A4);
            context.drawText(this.textRenderer, t, 0, 0, 0x3B2410, false);
            context.getMatrices().pop();
        }
    }

    /**
     * Ícono fantasma en cada pin con el corte ya fijado (2026-09-25, "quiero
     * que quede visible el patron cuando esta pineado, capaz un icono
     * fantasma"): el molde volvió al almacén y el slot quedó vacío, así que
     * se dibuja el ítem que lo produjo, desvaído con un velo del color del
     * pergamino. Coordenadas locales (drawForeground ya está trasladado).
     */
    private void dibujarFantasmasDePines(DrawContext context) {
        ModeladoBlockEntity be = this.handler.be;
        int cat = be.categoria().ordinal();
        for (int i = 0; i < ModeladoBlockEntity.PINES_POR_CATEGORIA; i++) {
            int p = cat * ModeladoBlockEntity.PINES_POR_CATEGORIA + i;
            if (ModeladoBlockEntity.ROLES[cat][i] == ModeladoBlockEntity.Rol.NINGUNO) continue;
            if (!be.pinFijado(p) || !be.getStack(ModeladoBlockEntity.pinSlot(p)).isEmpty()) continue;
            ComboCorte combo = be.pinCombo(p);
            if (combo == null) continue;
            ItemStack icono = iconoDe(combo);
            if (icono.isEmpty()) continue;
            int x = M_MEDIO + ModeladoScreenHandler.PIN_POS[cat][i][0], y = ModeladoScreenHandler.PIN_POS[cat][i][1];
            context.drawItem(icono, x, y);
            // drawItem dibuja en z=150: el velo tiene que ir más arriba o queda debajo del ícono
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 200);
            context.fill(x, y, x + 16, y + 16, 0x5CD9B98A);
            context.getMatrices().pop();
        }
    }

    /**
     * Rótulos de las 2 grillas de storage (2026-09-24, a pedido de las
     * reglas nuevas de guardado por categoría — "en el almacen de prendas
     * van los moldes que sirven para mas de una prenda" / "en los slots de
     * moldes de remera solo entran moldes que sirvan solo para la
     * remera"): sin esto no había forma de saber por qué un molde entraba
     * en una grilla y no en la otra. Mismo criterio que {@link #hint()}
     * (texto plano, sin arte nueva).
     */
    private void dibujarRotulosStorage(DrawContext context) {
        ModeladoBlockEntity.Categoria categoria = this.handler.be.categoria();
        Text porPrenda = Text.translatable("femclothes.modelado.porprenda",
                Text.translatable("femclothes.modelado.categoria." + categoria.name().toLowerCase(java.util.Locale.ROOT)));
        context.drawText(this.textRenderer, Text.translatable("femclothes.modelado.almacen"), M_DERECHA, 9,EstiloPergamino.TEXTO, false);
        context.drawText(this.textRenderer, porPrenda, M_DERECHA, 76, EstiloPergamino.TEXTO, false);
    }

    private Text hint() {
        ModeladoBlockEntity be = this.handler.be;
        if (be.estado() == ModeladoBlockEntity.Estado.PROCESANDO) {
            return Text.translatable("femclothes.modelado.estado.procesando");
        }
        if (!be.getStack(ModeladoBlockEntity.SALIDA).isEmpty()) {
            return Text.translatable("femclothes.modelado.hint.listo");
        }
        if (be.encendida()) {
            return Text.translatable("femclothes.modelado.hint.esperando");
        }
        if (be.activoVacio()) {
            return Text.translatable("femclothes.modelado.hint.vacio");
        }
        if (be.fijadas().isEmpty()) {
            return Text.translatable("femclothes.modelado.hint.ajustar");
        }
        return Text.translatable("femclothes.modelado.hint.fijado");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refrescar();
        super.render(context, mouseX, mouseY, delta);
        dibujarPreview(context, mouseX, mouseY);
        // Las chinchetas van ENCIMA de todo lo demás (incluido el ícono
        // fantasma de drawForeground) — antes quedaban atrás del patrón.
        for (BotonChincheta b : btnPines) {
            if (b.visible) b.render(context, mouseX, mouseY, delta);
        }
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    /**
     * Chincheta de un pin de remera: dibujada a mano (sin arte nueva).
     * Hueca = hay molde puesto, todavía sin fijar; rosa llena = corte ya
     * fijado (el molde volvió al almacén).
     */
    private static class BotonChincheta extends ButtonWidget {
        boolean fijado;
        int pin;
        private long cambioMs = -1000;

        BotonChincheta(int x, int y, PressAction onPress) {
            super(x, y, 16, 16, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
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
            if (isHovered() && active) c.fill(x + 2, y + 2, x + 14, y + 14, 0x33FFFFFF);
            // Por encima de ítems (z=150) y del velo fantasma (z=200): antes
            // "a veces aparecía debajo, a veces arriba" según hubiera un ícono.
            c.getMatrices().push();
            c.getMatrices().translate(0, 0, 400);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            c.setShaderColor(1f, 1f, 1f, active ? 1f : 0.55f);
            c.drawTexture(TEXTURE_CHINCHETA[cuadro], x + 2, y + 2, 0, 0, 12, 12, 12, 12);
            c.setShaderColor(1f, 1f, 1f, 1f);
            c.getMatrices().pop();
        }
    }

}
