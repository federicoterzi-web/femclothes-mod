package com.femclothes.modelado;

import com.femclothes.item.Calce;
import com.femclothes.item.CalientabrazosItem;
import com.femclothes.item.FemclothesItems;
import com.femclothes.item.MediasLargo;
import com.femclothes.item.PantalonItem;
import com.femclothes.item.PatronRed;
import com.femclothes.sublimadora.ModItems;
import com.femclothes.sublimadora.RemeraItem;
import net.minecraft.item.ItemStack;

/**
 * Aplica un {@link ComboCorte} sobre una prenda — el mismo dispatch por tipo
 * que antes vivía en {@code ClothingLoomScreenHandler}, factorizado acá para
 * que la Mesa de Modelado (y el Telar viejo, que llama a este mismo método
 * ahora) no dupliquen la lógica de cada eje.
 *
 * Cada campo del combo se ignora si no aplica al tipo de prenda en mano — el
 * mismo molde de corte físico sirve para cualquier prenda, cada una toma
 * solo lo suyo. Los ejes de EXTREMIDAD (pantalón-pierna, medias,
 * calientabrazos) además respetan {@link ComboCorte#lado} — cada entrada
 * puebla el anclaje superior O inferior de UN lado (o ambos).
 */
public final class PrendaModelado {

    /** Devuelve una copia con el combo aplicado, o la prenda original si no cambió nada. */
    public static ItemStack aplicar(ItemStack prenda, ComboCorte combo) {
        if (combo.estaVacio() || prenda.isEmpty()) {
            return prenda;
        }

        ItemStack out = prenda.copyWithCount(prenda.getCount());
        boolean cambio = false;
        var lado = combo.lado();

        // Calce: transversal a las 4, sin anclaje ni lado (a diferencia de
        // los demás ejes, no recorta filas — cambia la dilatación de la
        // geometría 3D, ver CuerpoGeometria).
        if (combo.calce().isPresent() && (out.getItem() instanceof RemeraItem
                || out.getItem() instanceof PantalonItem
                || out.getItem() == FemclothesItems.SOCKS_SOLID
                || out.getItem() instanceof CalientabrazosItem)) {
            Calce.escribir(out, combo.calce().get());
            cambio = true;
        }

        // Red: mismo criterio transversal que Calce — perfora la textura ya
        // compuesta en vez de recortar filas (ver PatronRed, PiezasDelMod).
        if (combo.red().isPresent() && (out.getItem() instanceof RemeraItem
                || out.getItem() instanceof PantalonItem
                || out.getItem() == FemclothesItems.SOCKS_SOLID
                || out.getItem() instanceof CalientabrazosItem)) {
            PatronRed.escribir(out, combo.red().get());
            cambio = true;
        }

        if (out.getItem() instanceof RemeraItem
                && (combo.remeraLargo().isPresent() || combo.remeraCuello().isPresent())) {
            // Parchea SOLO los ejes presentes sobre el Variante ACTUAL de la
            // prenda (no un valor congelado) — antes esto reemplazaba el
            // Variante entero, así que fijar largo después de manga pisaba
            // la manga de vuelta a un default; ahora cada fijada preserva
            // lo que las anteriores ya escribieron, igual que pantalón/medias.
            com.femclothes.sublimadora.Variante actual = RemeraItem.variante(out);
            com.femclothes.sublimadora.Variante nuevo = new com.femclothes.sublimadora.Variante(
                    combo.remeraLargo().orElse(actual.largo()),
                    actual.manga(),
                    combo.remeraCuello().orElse(actual.cuello()));
            out.set(ModItems.VARIANTE, nuevo);
            cambio = true;
        }
        // Manga: eje aparte (2026-09-24, "vamos con mangas distintas") —
        // respeta combo.lado() en vez de ir siempre adentro del Variante
        // completo (izquierda/derecha independientes, ver RemeraItem#setManga).
        if (out.getItem() instanceof RemeraItem && combo.remeraManga().isPresent()) {
            RemeraItem.setManga(out, lado, combo.remeraManga().get());
            cambio = true;
        }
        // Materiales/Calado (2026-09-24): un pin = una capa de patrón en un
        // índice puntual. Lee las capas YA puestas en la prenda y reemplaza
        // (o agrega, si el índice cae justo después de la última) solo esa
        // — así 3 pines independientes arman hasta 3 capas apiladas, en vez
        // de que cada fijada pise a las otras (ponerPatron(..., List) exige
        // la lista completa de una, no soporta "agregar una sola").
        if (combo.capaPatron().isPresent() && (out.getItem() instanceof RemeraItem
                || out.getItem() instanceof PantalonItem
                || out.getItem() == FemclothesItems.SOCKS_SOLID
                || out.getItem() instanceof CalientabrazosItem)) {
            var ci = combo.capaPatron().get();
            var patronItem = com.femclothes.item.ClothingPatternItem.porId(ci.patronId());
            var forma = patronItem != null ? patronItem.forma : com.femclothes.render.PatronGenerador.Forma.ALTERNADO;
            var nueva = new com.femclothes.region.RegionResolver.CapaPatron(
                    ci.patronId(), 0xFFFFFF, com.femclothes.item.TamanoPatron.GRANDE, 0f, 0.5f, forma, false);
            var actuales = new java.util.ArrayList<>(
                    com.femclothes.region.RegionResolver.capasAplicadas(out, lado));
            int idx = Math.min(ci.indice(), actuales.size());
            if (idx < actuales.size()) actuales.set(idx, nueva);
            else actuales.add(nueva);
            if (actuales.size() > 3) actuales = new java.util.ArrayList<>(actuales.subList(0, 3));
            com.femclothes.region.RegionResolver.ponerPatron(out, lado, actuales);
            cambio = true;
        }
        if (out.getItem() instanceof PantalonItem) {
            if (combo.pantalonTiro().isPresent()) {
                PantalonItem.setTiro(out, combo.pantalonTiro().get());
                cambio = true;
            }
            if (combo.pantalonLargoSuperior().isPresent()) {
                PantalonItem.setLargoSuperior(out, lado, combo.pantalonLargoSuperior().get());
                cambio = true;
            }
            if (combo.pantalonLargoInferior().isPresent()) {
                PantalonItem.setLargoInferior(out, lado, combo.pantalonLargoInferior().get());
                cambio = true;
            }
        }
        if (out.getItem() == FemclothesItems.SOCKS_SOLID) {
            if (combo.mediasLargoSuperior().isPresent()) {
                MediasLargo.setSuperior(out, lado, combo.mediasLargoSuperior().get());
                cambio = true;
            }
            if (combo.mediasLargoInferior().isPresent()) {
                MediasLargo.setInferior(out, lado, combo.mediasLargoInferior().get());
                cambio = true;
            }
        }
        if (out.getItem() instanceof CalientabrazosItem) {
            if (combo.calientabrazosCoberturaSuperior().isPresent()) {
                CalientabrazosItem.setCoberturaSuperior(out, lado, combo.calientabrazosCoberturaSuperior().get());
                cambio = true;
            }
            if (combo.calientabrazosCoberturaInferior().isPresent()) {
                CalientabrazosItem.setCoberturaInferior(out, lado, combo.calientabrazosCoberturaInferior().get());
                cambio = true;
            }
        }

        return cambio ? out : prenda;
    }

    private PrendaModelado() {}
}
