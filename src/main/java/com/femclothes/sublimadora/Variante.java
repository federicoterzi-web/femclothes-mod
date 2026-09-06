package com.femclothes.sublimadora;

import com.femclothes.Femclothes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * El corte de una prenda: tres ejes independientes.
 *
 * Antes era un enum de cuatro valores fijos, y no escalaba. Sumar mangas
 * largas y escotes multiplicaba las combinaciones: tres largos por cuatro
 * mangas por tres cuellos son 36, y con una receta por combinacion y color
 * serian 576 recetas — ademas de imposibles de expresar en una grilla de 3x3.
 *
 * Por eso los ejes NO se craftean: se craftea una prenda y se la reforma en
 * el telar. Ademas de ahorrar recetas, deja rehacer una prenda que ya tenes
 * sin volver a gastar lana, y sin perder la estampa.
 */
public record Variante(Largo largo, Manga manga, Cuello cuello) {

    /** Cuanto baja la tela sobre las 12 filas del torso. */
    public enum Largo implements StringIdentifiable {
        CROP("crop", 5),
        NORMAL("normal", 9),
        LARGO("largo", 12);

        public final String clave;
        public final int filas;

        Largo(String clave, int filas) {
            this.clave = clave;
            this.filas = filas;
        }

        @Override public String asString() { return clave; }
    }

    /** Cuanto baja la manga sobre las 12 filas del brazo. */
    public enum Manga implements StringIdentifiable {
        SIN("sin", 0),
        CORTA("corta", 4),
        TRES_CUARTOS("tres_cuartos", 8),
        LARGA("larga", 12);

        public final String clave;
        public final int filas;

        Manga(String clave, int filas) {
            this.clave = clave;
            this.filas = filas;
        }

        @Override public String asString() { return clave; }
    }

    /**
     * El escote.
     *
     * A esta resolucion el pecho tiene 8 de ancho por 12 de alto, asi que el
     * escote se juega en dos o tres filas. La V es la que mejor se lee: dos
     * diagonales que se cortan al medio se distinguen de lejos. Un escote
     * redondo profundo se confundiria con el normal.
     */
    public enum Cuello implements StringIdentifiable {
        REDONDO("redondo"),
        V("v"),
        POLERA("polera");

        public final String clave;

        Cuello(String clave) {
            this.clave = clave;
        }

        @Override public String asString() { return clave; }
    }

    /** Lo que sale de la mesa de crafteo antes de pasar por el telar. */
    public static final Variante BASE = new Variante(Largo.NORMAL, Manga.CORTA, Cuello.REDONDO);

    public static final Codec<Variante> CODEC = RecordCodecBuilder.create(i -> i.group(
            StringIdentifiable.createCodec(Largo::values).optionalFieldOf("largo", Largo.NORMAL)
                    .forGetter(Variante::largo),
            StringIdentifiable.createCodec(Manga::values).optionalFieldOf("manga", Manga.CORTA)
                    .forGetter(Variante::manga),
            StringIdentifiable.createCodec(Cuello::values).optionalFieldOf("cuello", Cuello.REDONDO)
                    .forGetter(Variante::cuello)
    ).apply(i, Variante::new));

    public static final PacketCodec<ByteBuf, Variante> PACKET_CODEC = PacketCodec.tuple(
            net.minecraft.network.codec.PacketCodecs.indexed(i -> Largo.values()[i], Enum::ordinal),
            Variante::largo,
            net.minecraft.network.codec.PacketCodecs.indexed(i -> Manga.values()[i], Enum::ordinal),
            Variante::manga,
            net.minecraft.network.codec.PacketCodecs.indexed(i -> Cuello.values()[i], Enum::ordinal),
            Variante::cuello,
            Variante::new);

    /** Las 36 combinaciones, para generar recursos y para cachear. */
    public static List<Variante> todas() {
        List<Variante> out = new ArrayList<>();
        for (Largo l : Largo.values()) {
            for (Manga m : Manga.values()) {
                for (Cuello c : Cuello.values()) out.add(new Variante(l, m, c));
            }
        }
        return out;
    }

    /** Nombre de archivo de este corte: largo_manga_cuello. */
    public String clave() {
        return largo.clave + "_" + manga.clave + "_" + cuello.clave;
    }

    public boolean tieneMangas() {
        return manga != Manga.SIN;
    }

    public Identifier texturaCuerpo() {
        return Identifier.of(Femclothes.MOD_ID, "textures/entity/cuerpo_" + clave() + ".png");
    }

    public Identifier modeloItem() {
        return Identifier.of(Femclothes.MOD_ID, "item/corte_" + clave());
    }

    /**
     * Como se llama la prenda.
     *
     * Un solo nombre para 36 cortes las volveria indistinguibles en el
     * inventario, y 36 nombres serian ilegibles. Se nombra por el rasgo que
     * mas la define y el resto va al tooltip.
     */
    public String nombre() {
        if (largo == Largo.CROP) return "croptop";
        if (manga == Manga.SIN) return "musculosa";
        if (cuello == Cuello.POLERA) return "polera";
        if (largo == Largo.LARGO) return "remeron";
        return "remera";
    }
}
