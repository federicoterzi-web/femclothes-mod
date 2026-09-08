package com.femclothes.garment;

import com.femclothes.item.FemclothesItems;
import com.femclothes.region.Operacion;
import com.femclothes.region.Region;
import com.femclothes.sublimadora.ModItems;
import net.minecraft.item.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * Que es cada prenda del mod, del lado que el servidor tambien necesita.
 *
 * Es un solo archivo y no un metodo en cada Item a proposito: asi la tabla de
 * "quien acepta que operacion en que region" se lee entera de un vistazo, que
 * es lo que hace falta para no volver a tener el mismo concepto escrito
 * distinto en cada estacion.
 */
public final class PrendasDelMod {

    private PrendasDelMod() {}

    public static void init() {
        Garments.registrar(FemclothesItems.SOCKS_SOLID, MEDIAS);
        Garments.registrar(FemclothesItems.PANTALON, PANTALON);
        Garments.registrar(ModItems.REMERA, REMERA);
        Garments.registrar(FemclothesItems.CALIENTABRAZOS, CALIENTABRAZOS);
    }

    /**
     * Medias: bilaterales de punta a punta.
     *
     * Son la unica prenda que ya tenia el per-lado antes del RegionResolver
     * (los componentes RIGHT_*), y por eso son la referencia de como se
     * comporta el eje Lado.
     */
    private static final Garment MEDIAS = new Garment() {
        @Override
        public Set<Region> regionesDe(Operacion op) {
            return switch (op) {
                case TENIR, PATRON, ESTAMPAR -> BILATERAL;
                // Las medias todavia no tienen ejes de corte. Cuando los
                // tengan (largo tobillo/media/rodilla/muslo, puño) esto pasa
                // a BILATERAL tambien: nada impide una media hasta la rodilla
                // y la otra al tobillo.
                case CORTE -> Set.of();
            };
        }

        @Override
        public Set<Parte> partes(ItemStack stack) {
            return EnumSet.of(Parte.PIERNA_IZQ, Parte.PIERNA_DER);
        }
    };

    /**
     * Pantalón: la prenda larga de pierna, y la primera prueba real del
     * sistema de capas (era "shorts" — ver FEMCLOTHES.md).
     *
     * Va en {@code piernas/exterior}, un slot DISTINTO del de las medias
     * ({@code socks/pair}) — pueden estar puestas las dos a la vez. Quien
     * decide que se dibuja arriba es {@code Capa.PIERNA_EXTERIOR} (20)
     * contra {@code Capa.MEDIA} (10), no el order de los slots de Trinkets.
     *
     * `CORTE` es `ENTERA` (como remera): el largo es UN valor para todo el
     * pantalón, no por pierna — nadie tiene una pierna en bermudas y la otra
     * en tanga. `TENIR` sigue siendo `BILATERAL`, sin cambios respecto de
     * cuando era shorts: cada pierna puede tener su propio color.
     *
     * Sin patrón todavía: a propósito, mismo motivo que shorts no lo tenía
     * — se suma después con el mismo mecanismo genérico de medias/remera.
     */
    private static final Garment PANTALON = new Garment() {
        @Override
        public Set<Region> regionesDe(Operacion op) {
            return switch (op) {
                case TENIR -> BILATERAL;
                case CORTE -> ENTERA;
                case PATRON, ESTAMPAR -> Set.of();
            };
        }

        @Override
        public Set<Parte> partes(ItemStack stack) {
            return EnumSet.of(Parte.PIERNA_IZQ, Parte.PIERNA_DER);
        }
    };

    /**
     * Calientabrazos: la prenda base de brazo, análoga a medias en la pierna.
     *
     * `CORTE` es `ENTERA`: cobertura y tiro son un valor para toda la prenda,
     * no por brazo (nadie tiene un brazo con muñequera y el otro con manga
     * larga). `TENIR` sigue siendo `BILATERAL`, cada brazo su color, igual
     * que pantalón cada pierna.
     *
     * `partes()` es siempre los dos brazos, nunca TORSO — igual que pantalón
     * NO declara TORSO pese a pintar ahí una pieza por el tiro: una parte
     * declarada se cubre con cuerpo base, y eso taparía el torso entero sin
     * remera puesta a cambio de una banda de un par de filas.
     */
    private static final Garment CALIENTABRAZOS = new Garment() {
        @Override
        public Set<Region> regionesDe(Operacion op) {
            return switch (op) {
                case TENIR -> BILATERAL;
                case CORTE -> ENTERA;
                case PATRON, ESTAMPAR -> Set.of();
            };
        }

        @Override
        public Set<Parte> partes(ItemStack stack) {
            return EnumSet.of(Parte.BRAZO_IZQ, Parte.BRAZO_DER);
        }
    };

    /**
     * Remera: un color base para toda la prenda, pero dos caras para estampar.
     *
     * Es el ejemplo de por que las regiones se declaran POR OPERACION. Tenir
     * medio torso no tiene sentido —es una remera, tiene un color—, pero
     * estampar frente y espalda por separado es justamente lo que hace la
     * sublimadora. Un solo set para las dos operaciones dejaria botones que
     * no hacen nada en una de las dos estaciones.
     */
    private static final Garment REMERA = new Garment() {
        @Override
        public Set<Region> regionesDe(Operacion op) {
            return switch (op) {
                case TENIR, PATRON, CORTE -> ENTERA;
                case ESTAMPAR -> CARAS;
            };
        }

        /**
         * El torso siempre; los brazos SIEMPRE tambien, tenga mangas o no.
         *
         * Que una musculosa declare los brazos parece de mas y es justamente
         * lo contrario: no dibuja tela ahi, pero tiene que taparle las mangas
         * pintadas de la skin. Sin eso, una musculosa muestra la manga de la
         * remera que el jugador tiene dibujada, no el brazo.
         */
        @Override
        public Set<Parte> partes(ItemStack stack) {
            // El remeron va a sumar PIERNA_IZQ/DER cuando el ruedo se dibuje
            // de verdad (punto 4 del roadmap). NO se declaran todavia: una
            // parte declarada se cubre con cuerpo base y se le borra la capa
            // externa de la skin, asi que declararla sin tela ahi le borraria
            // el pantalon pintado al jugador a cambio de nada.
            return EnumSet.of(Parte.TORSO, Parte.BRAZO_IZQ, Parte.BRAZO_DER);
        }
    };
}
