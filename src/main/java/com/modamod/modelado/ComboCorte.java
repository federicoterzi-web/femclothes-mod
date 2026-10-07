package com.modamod.modelado;

import com.modamod.item.Botamanga;
import com.modamod.item.Calce;
import com.modamod.item.PantalonTiro;
import com.modamod.item.PatronRed;
import com.modamod.region.Lado;
import com.modamod.sublimadora.Variante;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

import java.util.Optional;

/**
 * Lo que guarda un {@link MoldeDeCorteItem}: varios ejes de forma juntos en
 * un solo ítem físico, uno por familia de prenda. Cada campo es opcional —
 * un molde de corte no tiene por qué tocar todos a la vez, y aplicar sobre
 * una prenda solo escribe los campos presentes Y relevantes a esa prenda
 * (ver {@link PrendaModelado#aplicar}).
 *
 * <h2>Cobertura de torso vs. extremidad</h2>
 * {@code remeraLargo}/{@code remeraManga}/{@code remeraCuello} y
 * {@code pantalonTiro} son ejes de TORSO — un solo valor cada uno, sin
 * anclaje doble ni lado. Los tres ejes de EXTREMIDAD (pantalón-pierna,
 * medias, calientabrazos) en cambio son dos anclajes que se intersecan
 * (superior/inferior — ver {@code PantalonItem#filasVisibles} y análogos) Y
 * pueden ser asimétricos — de ahí {@link #lado}, que dice a qué pierna/brazo
 * aplica el anclaje que esta entrada puebla. Una entrada puebla UN anclajes
 * de UN eje; fijar superior y después inferior son dos entradas separadas
 * que se acumulan sobre la misma prenda (ver {@link PrendaModelado}).
 *
 * <p><b>2026-09-15</b>: {@code remeraLargo/manga/cuello} eran ANTES un solo
 * campo {@code remeraVariante} (el {@link Variante} entero de una) — bug
 * real encontrado jugando: como cada fijada de remera reemplazaba el
 * Variante COMPLETO, fijar el largo DESPUÉS de fijar la manga pisaba la
 * manga de vuelta a un default congelado (y viceversa) — dos fijadas de
 * remera competían en vez de acumularse, a diferencia de cómo ya
 * funcionaba pantalón/medias. Separarlos en 3 campos independientes,
 * aplicados por {@link PrendaModelado#aplicar} sobre el Variante ACTUAL de
 * la prenda (no un borrador congelado), arregla esto con el mismo patrón
 * que ya usan los demás ejes.
 *
 * <p><b>2026-09-16</b>: {@link #iconoOrigen} — a pedido, la GUI de la Mesa
 * (ver {@code ModeladoScreen}) quiere mostrar el ícono del molde que
 * produjo cada fijada en vez de un código de texto. El VALOR guardado
 * (ej. {@code Botamanga.RODILLA}) no alcanza para eso: el Molde de
 * Rango tiene menos escalones que {@link Botamanga}, así que la
 * traducción valor→ítem no es 1 a 1 ni reversible. Se guarda el
 * {@link Identifier} del ítem tal como estaba en el slot Activo al fijar
 * (ver {@code ModeladoBlockEntity#fijar}) — no participa de
 * {@link #estaVacio()} (es metadata de UI, no un eje real).
 *
 * <p><b>2026-09-20</b>: {@link #red} — molde de red/fishnet, transversal a
 * las 4 categorías igual que {@link #calce}, sin anclaje ni lado. Ver
 * {@link PatronRed}.
 *
 * <p><b>2026-09-24</b>: {@link #capaPatron} — pines "Materiales" del
 * esquema visual de remera (3 pines = 3 índices de capa, ver
 * {@code ModeladoBlockEntity#pinearMaterial}). Reusa el mismo sistema de
 * capas apiladas que ya escribe Tinturas ({@code RegionResolver.CapaPatron}/
 * {@code ponerPatron}, tope real {@code TinturasBlockEntity#CAPAS_MAXIMO}=3)
 * — acá solo se guarda el {@code patronId} del molde soltado en ese índice;
 * color/tamaño/ángulo/posición salen con sus defaults al aplicar (ver
 * {@link PrendaModelado#aplicar}), no hay edición fina desde la Modeladora
 * todavía (eso sigue siendo cosa de Tinturas).
 */
public record ComboCorte(
        Optional<Variante.Largo> remeraLargo,
        Optional<Variante.Manga> remeraManga,
        Optional<Variante.Cuello> remeraCuello,
        Optional<PantalonTiro> pantalonTiro,
        Optional<Botamanga> pantalonLargoSuperior,
        Optional<Botamanga> pantalonLargoInferior,
        Optional<Botamanga> mediasLargoSuperior,
        Optional<Botamanga> mediasLargoInferior,
        Optional<Variante.Manga> calientabrazosCoberturaSuperior,
        Optional<Variante.Manga> calientabrazosCoberturaInferior,
        Optional<Calce> calce,
        Optional<PatronRed> red,
        Optional<CapaIndexada> capaPatron,
        Lado lado,
        Optional<Identifier> iconoOrigen,
        Optional<PolleraCorte> pollera
) {

    /**
     * Ejes de la pollera (2026-09-29, "quiero poder hacerle distintos
     * largos" + forma campana/tableada): van juntos en un solo campo porque
     * el codec de registros admite hasta 16.
     */
    public record PolleraCorte(Optional<com.modamod.item.PolleraLargo> largo,
                               Optional<com.modamod.item.PolleraForma> forma,
                               // La capa (2026-09-29) viaja en el mismo campo: el
                               // codec de ComboCorte ya está en su tope de 16.
                               Optional<com.modamod.item.CapaLargo> capaLargo,
                               Optional<com.modamod.item.CapaRuedo> capaRuedo,
                               Optional<Boolean> capaCapucha,
                               Optional<Boolean> capaCuello,
                               // El sombrero de bruja (2026-10-05) también: el codec ya está en su tope de 16.
                               Optional<com.modamod.item.SombreroAla> sombreroAla,
                               Optional<com.modamod.item.SombreroPunta> sombreroPunta,
                               // La banda (cinto y choker, 2026-10-05) también.
                               Optional<com.modamod.item.BandaZona> bandaZona,
                               Optional<com.modamod.item.BandaAncho> bandaAncho,
                               Optional<com.modamod.item.BandaHerraje> bandaHerraje,
                               // Los volados de la pollera (2026-10-05): del borde de abajo y de toda la pollera.
                               Optional<com.modamod.item.PolleraVolado> voladoRuedo,
                               Optional<com.modamod.item.PolleraVolado> voladoTodo,
                               // El ruedo de un borde libre (2026-10-07, fusiona el borde de la pollera y el remate de la chaqueta).
                               Optional<RuedoCorte> ruedo,
                               // La chaqueta (2026-10-07): frente, capucha y solapa en un solo campo (tope de 16 campos).
                               Optional<ChaquetaCorte> chaqueta) {

        /** Los 14 campos de antes de la chaqueta. */
        public PolleraCorte(Optional<com.modamod.item.PolleraLargo> largo,
                            Optional<com.modamod.item.PolleraForma> forma,
                            Optional<com.modamod.item.CapaLargo> capaLargo,
                            Optional<com.modamod.item.CapaRuedo> capaRuedo,
                            Optional<Boolean> capaCapucha,
                            Optional<Boolean> capaCuello,
                            Optional<com.modamod.item.SombreroAla> sombreroAla,
                            Optional<com.modamod.item.SombreroPunta> sombreroPunta,
                            Optional<com.modamod.item.BandaZona> bandaZona,
                            Optional<com.modamod.item.BandaAncho> bandaAncho,
                            Optional<com.modamod.item.BandaHerraje> bandaHerraje,
                            Optional<com.modamod.item.PolleraVolado> voladoRuedo,
                            Optional<com.modamod.item.PolleraVolado> voladoTodo,
                            Optional<RuedoCorte> ruedo) {
            this(largo, forma, capaLargo, capaRuedo, capaCapucha, capaCuello, sombreroAla, sombreroPunta, bandaZona,
                    bandaAncho, bandaHerraje, voladoRuedo, voladoTodo, ruedo, Optional.empty());
        }

        /** Los 13 campos de antes del ruedo. */
        public PolleraCorte(Optional<com.modamod.item.PolleraLargo> largo,
                            Optional<com.modamod.item.PolleraForma> forma,
                            Optional<com.modamod.item.CapaLargo> capaLargo,
                            Optional<com.modamod.item.CapaRuedo> capaRuedo,
                            Optional<Boolean> capaCapucha,
                            Optional<Boolean> capaCuello,
                            Optional<com.modamod.item.SombreroAla> sombreroAla,
                            Optional<com.modamod.item.SombreroPunta> sombreroPunta,
                            Optional<com.modamod.item.BandaZona> bandaZona,
                            Optional<com.modamod.item.BandaAncho> bandaAncho,
                            Optional<com.modamod.item.BandaHerraje> bandaHerraje,
                            Optional<com.modamod.item.PolleraVolado> voladoRuedo,
                            Optional<com.modamod.item.PolleraVolado> voladoTodo) {
            this(largo, forma, capaLargo, capaRuedo, capaCapucha, capaCuello, sombreroAla, sombreroPunta, bandaZona,
                    bandaAncho, bandaHerraje, voladoRuedo, voladoTodo, Optional.empty(), Optional.empty());
        }
        public PolleraCorte(Optional<com.modamod.item.PolleraLargo> largo,
                            Optional<com.modamod.item.PolleraForma> forma) {
            this(largo, forma, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty());
        }

        public PolleraCorte(Optional<com.modamod.item.PolleraLargo> largo,
                            Optional<com.modamod.item.PolleraForma> forma,
                            Optional<com.modamod.item.CapaLargo> capaLargo,
                            Optional<com.modamod.item.CapaRuedo> capaRuedo,
                            Optional<Boolean> capaCapucha,
                            Optional<Boolean> capaCuello) {
            this(largo, forma, capaLargo, capaRuedo, capaCapucha, capaCuello, Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        public PolleraCorte(Optional<com.modamod.item.PolleraLargo> largo,
                            Optional<com.modamod.item.PolleraForma> forma,
                            Optional<com.modamod.item.CapaLargo> capaLargo,
                            Optional<com.modamod.item.CapaRuedo> capaRuedo,
                            Optional<Boolean> capaCapucha,
                            Optional<Boolean> capaCuello,
                            Optional<com.modamod.item.SombreroAla> sombreroAla,
                            Optional<com.modamod.item.SombreroPunta> sombreroPunta) {
            this(largo, forma, capaLargo, capaRuedo, capaCapucha, capaCuello, sombreroAla, sombreroPunta,
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        /** Los 11 campos de antes de los volados (la banda, 2026-10-05). */
        public PolleraCorte(Optional<com.modamod.item.PolleraLargo> largo,
                            Optional<com.modamod.item.PolleraForma> forma,
                            Optional<com.modamod.item.CapaLargo> capaLargo,
                            Optional<com.modamod.item.CapaRuedo> capaRuedo,
                            Optional<Boolean> capaCapucha,
                            Optional<Boolean> capaCuello,
                            Optional<com.modamod.item.SombreroAla> sombreroAla,
                            Optional<com.modamod.item.SombreroPunta> sombreroPunta,
                            Optional<com.modamod.item.BandaZona> bandaZona,
                            Optional<com.modamod.item.BandaAncho> bandaAncho,
                            Optional<com.modamod.item.BandaHerraje> bandaHerraje) {
            this(largo, forma, capaLargo, capaRuedo, capaCapucha, capaCuello, sombreroAla, sombreroPunta,
                    bandaZona, bandaAncho, bandaHerraje, Optional.empty(), Optional.empty());
        }

        boolean vacio() {
            return largo.isEmpty() && forma.isEmpty() && capaLargo.isEmpty() && capaRuedo.isEmpty()
                    && capaCapucha.isEmpty() && capaCuello.isEmpty()
                    && sombreroAla.isEmpty() && sombreroPunta.isEmpty()
                    && bandaZona.isEmpty() && bandaAncho.isEmpty() && bandaHerraje.isEmpty()
                    && voladoRuedo.isEmpty() && voladoTodo.isEmpty() && ruedo.isEmpty() && chaqueta.isEmpty();
        }

        static final Codec<PolleraCorte> CODEC = RecordCodecBuilder.create(i -> i.group(
                StringIdentifiable.createCodec(com.modamod.item.PolleraLargo::values).optionalFieldOf("largo")
                        .forGetter(PolleraCorte::largo),
                StringIdentifiable.createCodec(com.modamod.item.PolleraForma::values).optionalFieldOf("forma")
                        .forGetter(PolleraCorte::forma),
                StringIdentifiable.createCodec(com.modamod.item.CapaLargo::values).optionalFieldOf("capa_largo")
                        .forGetter(PolleraCorte::capaLargo),
                StringIdentifiable.createCodec(com.modamod.item.CapaRuedo::values).optionalFieldOf("capa_ruedo")
                        .forGetter(PolleraCorte::capaRuedo),
                Codec.BOOL.optionalFieldOf("capa_capucha").forGetter(PolleraCorte::capaCapucha),
                Codec.BOOL.optionalFieldOf("capa_cuello").forGetter(PolleraCorte::capaCuello),
                StringIdentifiable.createCodec(com.modamod.item.SombreroAla::values).optionalFieldOf("sombrero_ala")
                        .forGetter(PolleraCorte::sombreroAla),
                StringIdentifiable.createCodec(com.modamod.item.SombreroPunta::values).optionalFieldOf("sombrero_punta")
                        .forGetter(PolleraCorte::sombreroPunta),
                StringIdentifiable.createCodec(com.modamod.item.BandaZona::values).optionalFieldOf("banda_zona")
                        .forGetter(PolleraCorte::bandaZona),
                StringIdentifiable.createCodec(com.modamod.item.BandaAncho::values).optionalFieldOf("banda_ancho")
                        .forGetter(PolleraCorte::bandaAncho),
                StringIdentifiable.createCodec(com.modamod.item.BandaHerraje::values).optionalFieldOf("banda_herraje")
                        .forGetter(PolleraCorte::bandaHerraje),
                StringIdentifiable.createCodec(com.modamod.item.PolleraVolado::values).optionalFieldOf("volado_ruedo")
                        .forGetter(PolleraCorte::voladoRuedo),
                StringIdentifiable.createCodec(com.modamod.item.PolleraVolado::values).optionalFieldOf("volado_todo")
                        .forGetter(PolleraCorte::voladoTodo),
                RuedoCorte.CODEC.optionalFieldOf("ruedo").forGetter(PolleraCorte::ruedo),
                ChaquetaCorte.CODEC.optionalFieldOf("chaqueta").forGetter(PolleraCorte::chaqueta)
        ).apply(i, PolleraCorte::new));
    }

    /** Los ejes propios de la chaqueta (2026-10-07): frente, capucha (true = con) y solapa. */
    public record ChaquetaCorte(Optional<com.modamod.item.ChaquetaFrente> frente, Optional<Boolean> capucha,
                                Optional<com.modamod.item.ChaquetaSolapa> solapa) {
        static final Codec<ChaquetaCorte> CODEC = RecordCodecBuilder.create(i -> i.group(
                StringIdentifiable.createCodec(com.modamod.item.ChaquetaFrente::values).optionalFieldOf("frente")
                        .forGetter(ChaquetaCorte::frente),
                Codec.BOOL.optionalFieldOf("capucha").forGetter(ChaquetaCorte::capucha),
                StringIdentifiable.createCodec(com.modamod.item.ChaquetaSolapa::values).optionalFieldOf("solapa")
                        .forGetter(ChaquetaCorte::solapa)
        ).apply(i, ChaquetaCorte::new));
    }

    /** El ruedo de UN borde libre (2026-10-07): qué zona y qué remate. Cada pin de ruedo carga uno. */
    public record RuedoCorte(com.modamod.item.ZonaRuedo zona, com.modamod.item.Ruedo ruedo, boolean espejo) {
        static final Codec<RuedoCorte> CODEC = RecordCodecBuilder.create(i -> i.group(
                StringIdentifiable.createCodec(com.modamod.item.ZonaRuedo::values).fieldOf("zona").forGetter(RuedoCorte::zona),
                StringIdentifiable.createCodec(com.modamod.item.Ruedo::values).fieldOf("ruedo").forGetter(RuedoCorte::ruedo),
                Codec.BOOL.optionalFieldOf("espejo", false).forGetter(RuedoCorte::espejo)
        ).apply(i, RuedoCorte::new));
    }

    public static ComboCorte chaquetaFrente(com.modamod.item.ChaquetaFrente v) {
        return conChaqueta(new ChaquetaCorte(Optional.of(v), Optional.empty(), Optional.empty()));
    }

    public static ComboCorte chaquetaCapucha(boolean v) {
        return conChaqueta(new ChaquetaCorte(Optional.empty(), Optional.of(v), Optional.empty()));
    }

    public static ComboCorte chaquetaSolapa(com.modamod.item.ChaquetaSolapa v) {
        return conChaqueta(new ChaquetaCorte(Optional.empty(), Optional.empty(), Optional.of(v)));
    }

    private static ComboCorte conChaqueta(ChaquetaCorte c) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(c)));
    }

    public static ComboCorte polleraLargo(com.modamod.item.PolleraLargo v) {
        return VACIO.conPollera(new PolleraCorte(Optional.of(v), Optional.empty()));
    }

    public static ComboCorte polleraForma(com.modamod.item.PolleraForma v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.of(v)));
    }

    public static ComboCorte capaLargo(com.modamod.item.CapaLargo v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.of(v),
                Optional.empty(), Optional.empty(), Optional.empty()));
    }

    public static ComboCorte capaRuedo(com.modamod.item.CapaRuedo v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(v), Optional.empty(), Optional.empty()));
    }

    public static ComboCorte capaCapucha(boolean v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(v), Optional.empty()));
    }

    public static ComboCorte capaCuello(boolean v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(v)));
    }

    public static ComboCorte sombreroAla(com.modamod.item.SombreroAla v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(v), Optional.empty()));
    }

    public static ComboCorte sombreroPunta(com.modamod.item.SombreroPunta v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(v)));
    }

    /** Un molde de ruedo puesto en el pin de {@code zona}. */
    public static ComboCorte ruedo(com.modamod.item.ZonaRuedo zona, com.modamod.item.Ruedo v, boolean espejo) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(new RuedoCorte(zona, v, espejo))));
    }

    public static ComboCorte voladoRuedo(com.modamod.item.PolleraVolado v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(v), Optional.empty()));
    }

    public static ComboCorte voladoTodo(com.modamod.item.PolleraVolado v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(v)));
    }

    public static ComboCorte bandaZona(com.modamod.item.BandaZona v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(v),
                Optional.empty(), Optional.empty()));
    }

    public static ComboCorte bandaAncho(com.modamod.item.BandaAncho v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(v), Optional.empty()));
    }

    public static ComboCorte bandaHerraje(com.modamod.item.BandaHerraje v) {
        return VACIO.conPollera(new PolleraCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(v)));
    }

    public ComboCorte conPollera(PolleraCorte p) {
        return new ComboCorte(remeraLargo, remeraManga, remeraCuello, pantalonTiro,
                pantalonLargoSuperior, pantalonLargoInferior, mediasLargoSuperior, mediasLargoInferior,
                calientabrazosCoberturaSuperior, calientabrazosCoberturaInferior, calce, red,
                capaPatron, lado, iconoOrigen, Optional.of(p));
    }

    /** Un molde de patrón en un índice de capa (0..2) — ver {@link #capaPatron}. */
    public record CapaIndexada(int indice, Identifier patronId) {}

    public static final ComboCorte VACIO = new ComboCorte(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(),
            Lado.AMBAS, Optional.empty(), Optional.empty());

    // ── factories: un campo poblado por llamada, evita el positional de 15 args ──

    public static ComboCorte remeraLargo(Variante.Largo v) {
        return new ComboCorte(Optional.of(v), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                Lado.AMBAS, Optional.empty(), Optional.empty());
    }

    public static ComboCorte remeraManga(Variante.Manga v) {
        return remeraManga(v, Lado.AMBAS);
    }

    /** Manga por lado (2026-09-24, "vamos con mangas distintas") — ver {@code RemeraItem#setManga}. */
    public static ComboCorte remeraManga(Variante.Manga v, Lado lado) {
        return new ComboCorte(Optional.empty(), Optional.of(v), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                lado, Optional.empty(), Optional.empty());
    }

    public static ComboCorte remeraCuello(Variante.Cuello v) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.of(v), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                Lado.AMBAS, Optional.empty(), Optional.empty());
    }

    public static ComboCorte tiro(PantalonTiro t) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(t),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                Lado.AMBAS, Optional.empty(), Optional.empty());
    }

    public static ComboCorte pantalonSuperior(Botamanga v, Lado lado) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(v), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                lado, Optional.empty(), Optional.empty());
    }

    public static ComboCorte pantalonInferior(Botamanga v, Lado lado) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(v), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                lado, Optional.empty(), Optional.empty());
    }

    public static ComboCorte mediasSuperior(Botamanga v, Lado lado) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(v), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                lado, Optional.empty(), Optional.empty());
    }

    public static ComboCorte mediasInferior(Botamanga v, Lado lado) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(v),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                lado, Optional.empty(), Optional.empty());
    }

    public static ComboCorte calientabrazosSuperior(Variante.Manga v, Lado lado) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(v), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                lado, Optional.empty(), Optional.empty());
    }

    public static ComboCorte calientabrazosInferior(Variante.Manga v, Lado lado) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(v), Optional.empty(), Optional.empty(),
                Optional.empty(),
                lado, Optional.empty(), Optional.empty());
    }

    /** Calce — a pedido, transversal a las 4 categorías, sin anclaje ni lado. */
    public static ComboCorte calce(Calce v) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(v), Optional.empty(),
                Optional.empty(),
                Lado.AMBAS, Optional.empty(), Optional.empty());
    }

    /** Red — a pedido (2026-09-20), transversal a las 4 categorías, sin anclaje ni lado. */
    public static ComboCorte red(PatronRed v) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(v),
                Optional.empty(),
                Lado.AMBAS, Optional.empty(), Optional.empty());
    }

    /** Material en un índice de capa (2026-09-24) — transversal, sin lado (remera es AMBAS-únicamente para patrón). */
    public static ComboCorte capaPatron(int indice, Identifier patronId) {
        return capaPatron(indice, patronId, Lado.AMBAS);
    }

    /** Personalización por lado (medias/calientabrazos, 2026-09-26): la capa va solo a la pierna/brazo de {@code lado}. */
    public static ComboCorte capaPatron(int indice, Identifier patronId, Lado lado) {
        return new ComboCorte(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(new CapaIndexada(indice, patronId)),
                lado, Optional.empty(), Optional.empty());
    }

    /**
     * Presets de prueba "cobertura de torso/extremidad" (ítems genéricos,
     * un nivel abstracto aplicado a todas las prendas de esa categoría a la
     * vez) — reusan el anclaje HISTÓRICO de cada prenda: torso siempre
     * entero (remera+tiro); extremidad usa el anclaje que ya tenía cada una
     * antes de este eje (pantalón=superior, medias=inferior,
     * calientabrazos=superior), la anterior queda en su default (abierta).
     */
    public static ComboCorte coberturaTorso(Variante.Largo remeraLargo, PantalonTiro tiro) {
        return new ComboCorte(
                Optional.of(remeraLargo), Optional.empty(), Optional.empty(),
                Optional.of(tiro),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                Lado.AMBAS, Optional.empty(), Optional.empty());
    }

    public static ComboCorte coberturaExtremidad(Botamanga pantalon, Botamanga medias, Variante.Manga calientabrazos) {
        return new ComboCorte(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(),
                Optional.of(pantalon), Optional.empty(),
                Optional.empty(), Optional.of(medias),
                Optional.of(calientabrazos), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty(),
                Lado.AMBAS, Optional.empty(), Optional.empty());
    }

    /** Copia con {@link #iconoOrigen} puesto — ver {@code ModeladoBlockEntity#fijar}. */
    public ComboCorte conIcono(Identifier id) {
        return new ComboCorte(remeraLargo, remeraManga, remeraCuello, pantalonTiro,
                pantalonLargoSuperior, pantalonLargoInferior, mediasLargoSuperior, mediasLargoInferior,
                calientabrazosCoberturaSuperior, calientabrazosCoberturaInferior, calce, red,
                capaPatron, lado,
                Optional.of(id), pollera);
    }

    public boolean estaVacio() {
        return remeraLargo.isEmpty() && remeraManga.isEmpty() && remeraCuello.isEmpty()
                && pantalonTiro.isEmpty()
                && pantalonLargoSuperior.isEmpty() && pantalonLargoInferior.isEmpty()
                && mediasLargoSuperior.isEmpty() && mediasLargoInferior.isEmpty()
                && calientabrazosCoberturaSuperior.isEmpty() && calientabrazosCoberturaInferior.isEmpty()
                && calce.isEmpty() && red.isEmpty() && capaPatron.isEmpty()
                && (pollera.isEmpty() || pollera.get().vacio());
    }

    private static final Codec<Lado> LADO_CODEC = Codec.STRING.xmap(Lado::valueOf, Enum::name);

    private static final Codec<CapaIndexada> CAPA_INDEXADA_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("indice").forGetter(CapaIndexada::indice),
            Identifier.CODEC.fieldOf("patron_id").forGetter(CapaIndexada::patronId)
    ).apply(i, CapaIndexada::new));

    public static final Codec<ComboCorte> CODEC = RecordCodecBuilder.create(i -> i.group(
            StringIdentifiable.createCodec(Variante.Largo::values).optionalFieldOf("remera_largo")
                    .forGetter(ComboCorte::remeraLargo),
            StringIdentifiable.createCodec(Variante.Manga::values).optionalFieldOf("remera_manga")
                    .forGetter(ComboCorte::remeraManga),
            StringIdentifiable.createCodec(Variante.Cuello::values).optionalFieldOf("remera_cuello")
                    .forGetter(ComboCorte::remeraCuello),
            StringIdentifiable.createCodec(PantalonTiro::values).optionalFieldOf("pantalon_tiro")
                    .forGetter(ComboCorte::pantalonTiro),
            StringIdentifiable.createCodec(Botamanga::values).optionalFieldOf("pantalon_largo_superior")
                    .forGetter(ComboCorte::pantalonLargoSuperior),
            StringIdentifiable.createCodec(Botamanga::values).optionalFieldOf("pantalon_largo_inferior")
                    .forGetter(ComboCorte::pantalonLargoInferior),
            StringIdentifiable.createCodec(Botamanga::values).optionalFieldOf("medias_largo_superior")
                    .forGetter(ComboCorte::mediasLargoSuperior),
            StringIdentifiable.createCodec(Botamanga::values).optionalFieldOf("medias_largo_inferior")
                    .forGetter(ComboCorte::mediasLargoInferior),
            StringIdentifiable.createCodec(Variante.Manga::values).optionalFieldOf("calientabrazos_cobertura_superior")
                    .forGetter(ComboCorte::calientabrazosCoberturaSuperior),
            StringIdentifiable.createCodec(Variante.Manga::values).optionalFieldOf("calientabrazos_cobertura_inferior")
                    .forGetter(ComboCorte::calientabrazosCoberturaInferior),
            StringIdentifiable.createCodec(Calce::values).optionalFieldOf("calce")
                    .forGetter(ComboCorte::calce),
            StringIdentifiable.createCodec(PatronRed::values).optionalFieldOf("red")
                    .forGetter(ComboCorte::red),
            CAPA_INDEXADA_CODEC.optionalFieldOf("capa_patron")
                    .forGetter(ComboCorte::capaPatron),
            LADO_CODEC.optionalFieldOf("lado", Lado.AMBAS).forGetter(ComboCorte::lado),
            Identifier.CODEC.optionalFieldOf("icono_origen").forGetter(ComboCorte::iconoOrigen),
            PolleraCorte.CODEC.optionalFieldOf("pollera").forGetter(ComboCorte::pollera)
    ).apply(i, ComboCorte::new));

    public static final PacketCodec<ByteBuf, ComboCorte> PACKET_CODEC = PacketCodec.of(
            (combo, buf) -> {
                writeOptEnum(buf, combo.remeraLargo);
                writeOptEnum(buf, combo.remeraManga);
                writeOptEnum(buf, combo.remeraCuello);
                writeOptEnum(buf, combo.pantalonTiro);
                writeOptEnum(buf, combo.pantalonLargoSuperior);
                writeOptEnum(buf, combo.pantalonLargoInferior);
                writeOptEnum(buf, combo.mediasLargoSuperior);
                writeOptEnum(buf, combo.mediasLargoInferior);
                writeOptEnum(buf, combo.calientabrazosCoberturaSuperior);
                writeOptEnum(buf, combo.calientabrazosCoberturaInferior);
                writeOptEnum(buf, combo.calce);
                writeOptEnum(buf, combo.red);
                buf.writeBoolean(combo.capaPatron.isPresent());
                combo.capaPatron.ifPresent(c -> {
                    buf.writeInt(c.indice());
                    Identifier.PACKET_CODEC.encode(buf, c.patronId());
                });
                buf.writeByte(combo.lado.ordinal());
                buf.writeBoolean(combo.iconoOrigen.isPresent());
                combo.iconoOrigen.ifPresent(id -> Identifier.PACKET_CODEC.encode(buf, id));
                buf.writeBoolean(combo.pollera.isPresent());
                combo.pollera.ifPresent(p -> {
                    writeOptEnum(buf, p.largo());
                    writeOptEnum(buf, p.forma());
                    writeOptEnum(buf, p.capaLargo());
                    writeOptEnum(buf, p.capaRuedo());
                    writeOptBool(buf, p.capaCapucha());
                    writeOptBool(buf, p.capaCuello());
                    writeOptEnum(buf, p.sombreroAla());
                    writeOptEnum(buf, p.sombreroPunta());
                    writeOptEnum(buf, p.bandaZona());
                    writeOptEnum(buf, p.bandaAncho());
                    writeOptEnum(buf, p.bandaHerraje());
                    writeOptEnum(buf, p.voladoRuedo());
                    writeOptEnum(buf, p.voladoTodo());
                    buf.writeBoolean(p.ruedo().isPresent());
                    p.ruedo().ifPresent(r -> {
                        buf.writeByte(r.zona().ordinal());
                        buf.writeByte(r.ruedo().ordinal());
                        buf.writeBoolean(r.espejo());
                    });
                    buf.writeBoolean(p.chaqueta().isPresent());
                    p.chaqueta().ifPresent(c -> {
                        writeOptEnum(buf, c.frente());
                        writeOptBool(buf, c.capucha());
                        writeOptEnum(buf, c.solapa());
                    });
                });
            },
            buf -> new ComboCorte(
                    readOptEnum(buf, Variante.Largo.values()),
                    readOptEnum(buf, Variante.Manga.values()),
                    readOptEnum(buf, Variante.Cuello.values()),
                    readOptEnum(buf, PantalonTiro.values()),
                    readOptEnum(buf, Botamanga.values()),
                    readOptEnum(buf, Botamanga.values()),
                    readOptEnum(buf, Botamanga.values()),
                    readOptEnum(buf, Botamanga.values()),
                    readOptEnum(buf, Variante.Manga.values()),
                    readOptEnum(buf, Variante.Manga.values()),
                    readOptEnum(buf, Calce.values()),
                    readOptEnum(buf, PatronRed.values()),
                    buf.readBoolean()
                            ? Optional.of(new CapaIndexada(buf.readInt(), Identifier.PACKET_CODEC.decode(buf)))
                            : Optional.empty(),
                    Lado.values()[buf.readByte()],
                    buf.readBoolean() ? Optional.of(Identifier.PACKET_CODEC.decode(buf)) : Optional.empty(),
                    buf.readBoolean() ? Optional.of(new PolleraCorte(
                            readOptEnum(buf, com.modamod.item.PolleraLargo.values()),
                            readOptEnum(buf, com.modamod.item.PolleraForma.values()),
                            readOptEnum(buf, com.modamod.item.CapaLargo.values()),
                            readOptEnum(buf, com.modamod.item.CapaRuedo.values()),
                            readOptBool(buf), readOptBool(buf),
                            readOptEnum(buf, com.modamod.item.SombreroAla.values()),
                            readOptEnum(buf, com.modamod.item.SombreroPunta.values()),
                            readOptEnum(buf, com.modamod.item.BandaZona.values()),
                            readOptEnum(buf, com.modamod.item.BandaAncho.values()),
                            readOptEnum(buf, com.modamod.item.BandaHerraje.values()),
                            readOptEnum(buf, com.modamod.item.PolleraVolado.values()),
                            readOptEnum(buf, com.modamod.item.PolleraVolado.values()),
                            buf.readBoolean() ? Optional.of(new RuedoCorte(
                                    com.modamod.item.ZonaRuedo.values()[buf.readByte()],
                                    com.modamod.item.Ruedo.values()[buf.readByte()],
                                    buf.readBoolean())) : Optional.empty(),
                            buf.readBoolean() ? Optional.of(new ChaquetaCorte(
                                    readOptEnum(buf, com.modamod.item.ChaquetaFrente.values()),
                                    readOptBool(buf),
                                    readOptEnum(buf, com.modamod.item.ChaquetaSolapa.values()))) : Optional.empty()))
                            : Optional.empty()));

    private static void writeOptBool(ByteBuf buf, Optional<Boolean> value) {
        buf.writeByte(value.isEmpty() ? 0 : value.get() ? 2 : 1);
    }

    private static Optional<Boolean> readOptBool(ByteBuf buf) {
        byte b = buf.readByte();
        return b == 0 ? Optional.empty() : Optional.of(b == 2);
    }

    private static <E extends Enum<E>> void writeOptEnum(ByteBuf buf, Optional<E> value) {
        buf.writeBoolean(value.isPresent());
        if (value.isPresent()) buf.writeByte(value.get().ordinal());
    }

    private static <E extends Enum<E>> Optional<E> readOptEnum(ByteBuf buf, E[] values) {
        return buf.readBoolean() ? Optional.of(values[buf.readByte()]) : Optional.empty();
    }
}
