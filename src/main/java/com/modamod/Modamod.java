package com.modamod;

import com.modamod.body.ComandoCuerpo;
import com.modamod.body.PerfilesDeCuerpo;
import com.modamod.garment.PrendasDelMod;
import com.modamod.item.ModamodComponents;
import com.modamod.item.ModamodItems;
import com.modamod.screen.ModamodScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Modamod implements ModInitializer {

    public static final String MOD_ID = "modamod";

    @Override
    public void onInitialize() {
        ModamodItems.init();
        ModamodComponents.init();
        // Que items son prendas del sistema de capas. Va DESPUES de los
        // items: registra por instancia, no por id.
        PrendasDelMod.init();
        PerfilesDeCuerpo.init();
        ComandoCuerpo.init();
        com.modamod.body.RedCuerpo.init();
        com.modamod.util.RedTwirl.init();
        com.modamod.util.RedCapucha.init();
        com.modamod.ropa.Cosmeticos.init();
        com.modamod.ropa.RedRopa.init();
        ModamodScreenHandlers.init();
        com.modamod.modelado.ModeladoMod.register();
        com.modamod.estilista.EstilistaMod.register();
        com.modamod.telar.TelarMod.register();
        com.modamod.tinturas.TinturasMod.register();
        com.modamod.guardarropas.GuardarropasMod.register();
        com.modamod.maniqui.ManiquiMod.register();
        com.modamod.cinta.CintaMod.register();
        com.modamod.estilado.EstiladoMod.register();
        registrarPestanaCreativa();
    }

    /**
     * Sin esto, la ÚNICA forma de conseguir cualquiera de estos ítems era
     * ya saber la receta de memoria — ninguno aparecía en el buscador
     * creativo. Se notó recién con calientabrazos ("me faltan"), pero el
     * agujero es de TODO `ModamodItems` (pantalón, medias, los 16 moldes,
     * los 3 patrones): `SublimadoraMod` solo agrega los ítems de su propio
     * paquete (remera + sus 2 moldes cíclicos).
     *
     * <p>Pestaña PROPIA desde 2026-09-28 ("meteme todas las cosas del mod en
     * una pestaña"): antes todo se colaba en la vanilla de Bloques
     * funcionales, mezclado con lo de Minecraft. Incluye también lo de la
     * Sublimadora (que antes agregaba {@code SublimadoraMod} por su cuenta).
     * Las entradas se arman recién al abrir la pestaña, así que referenciar
     * ítems de otro ModInitializer acá no depende del orden de carga.
     */
    public static final ItemGroup PESTANA = FabricItemGroup.builder()
            .icon(() -> new ItemStack(com.modamod.sublimadora.ModItems.REMERA))
            .displayName(Text.translatable("itemGroup.modamod"))
            .entries((contexto, entries) -> {
            // Máquinas primero.
            entries.add(com.modamod.modelado.ModeladoMod.MODELADO_BLOCK_ITEM);
            entries.add(com.modamod.telar.TelarMod.TELAR_ITEM);
            entries.add(com.modamod.estilista.EstilistaMod.ESTILISTA_ITEM);
            entries.add(com.modamod.tinturas.TinturasMod.TINTURAS_BLOCK_ITEM);
            entries.add(com.modamod.sublimadora.ModBlocks.SUBLIMADORA_ITEM);
            entries.add(com.modamod.guardarropas.GuardarropasMod.GUARDARROPAS_BLOCK_ITEM);
            entries.add(com.modamod.maniqui.ManiquiMod.MANIQUI_BLOCK_ITEM);
            entries.add(com.modamod.estilado.EstiladoMod.ESTILADO_BLOCK_ITEM);
            entries.add(com.modamod.cinta.CintaMod.CINTA_ITEM);
            entries.add(com.modamod.cinta.CintaMod.EMPALME_ITEM);
            // Máquinas creativas (2026-10-01): sin espera ni insumos, cargadas al colocarlas.
            entries.add(com.modamod.modelado.ModeladoMod.MODELADO_CREATIVA_ITEM);
            entries.add(com.modamod.telar.TelarMod.TELAR_CREATIVA_ITEM);
            entries.add(com.modamod.estilista.EstilistaMod.ESTILISTA_CREATIVA_ITEM);
            entries.add(com.modamod.tinturas.TinturasMod.TINTURAS_CREATIVA_ITEM);
            entries.add(com.modamod.sublimadora.ModBlocks.SUBLIMADORA_CREATIVA_ITEM);
            entries.add(com.modamod.estilado.EstiladoMod.ESTILADO_CREATIVA_ITEM);
            // Kits (2026-10-01, antes /modamod debug patrones|moldes|insumos).
            for (ItemStack kit : com.modamod.util.KitsCreativos.todos()) entries.add(kit);
            // Prendas.
            entries.add(com.modamod.sublimadora.ModItems.REMERA);
            entries.add(ModamodItems.CHAQUETA);
            // Apliques (2026-10-01): los 3 moldes y retazos de prueba con colores
            // de fábrica, hasta que Tintes los tiña (fase 4).
            entries.add(ModamodItems.MOLDE_APLIQUE_MONO);
            entries.add(ModamodItems.MOLDE_APLIQUE_MARIPOSA);
            entries.add(ModamodItems.MOLDE_APLIQUE_FLOR);
            entries.add(ModamodItems.MOLDE_TEXTURA_FRUNCIDO);
            entries.add(ModamodItems.MOLDE_TEXTURA_ACOLCHADO);
            entries.add(ModamodItems.ESTROGENOS);
            entries.add(com.modamod.aplique.RetazoApliqueItem.conColores(
                    new ItemStack(ModamodItems.RETAZO_APLIQUE), 0xE878A8, 0xF8D860, 0x58A868));
            entries.add(com.modamod.aplique.RetazoApliqueItem.conColores(
                    new ItemStack(ModamodItems.RETAZO_APLIQUE), 0x78B8E8, 0xF2F2F6, 0x2E4A80));
            entries.add(com.modamod.aplique.RetazoApliqueItem.conColores(
                    new ItemStack(ModamodItems.RETAZO_APLIQUE), 0xC82828, 0x1E1E22, 0xE8C050));
            entries.add(ModamodItems.SOCKS_34);
            entries.add(ModamodItems.SOCKS_SOLID);
            entries.add(ModamodItems.FISHNET_SOCKS);
            entries.add(ModamodItems.PANTALON);
            entries.add(ModamodItems.POLLERA);
            entries.add(ModamodItems.TINTE_MEZCLA);
            // Los moldes por-eje ESPECÍFICOS de pantalón-largo y medias se
            // sacaron (a pedido, "sintetizar todos en esos dos moldes
            // aunque cada prenda tenga su propia medida") — reemplazados
            // por el molde de RANGO unificado de abajo, que sirve para las
            // 4 categorías de extremidad a la vez. Siguen registrados (el
            // Telar viejo los sigue usando), solo dejan de listarse acá.
            // El de TIRO también se saca ahora (a pedido, "fusionar largo
            // de remera y tiro de pantalón en cobertura de torso") —
            // reemplazado por el molde de TORSO unificado de abajo.
            entries.add(ModamodItems.CALIENTABRAZOS);
            entries.add(ModamodItems.CAPA);
            entries.add(ModamodItems.SOMBRERO_BRUJA);
            // Banda (2026-10-05): un cinto y un choker de fábrica, hasta que lleguen los moldes de la Modeladora.
            entries.add(ModamodItems.BANDA);
            entries.add(ModamodItems.MOLDE_CORREA_LISA);
            entries.add(ModamodItems.MOLDE_CORREA_CADENA);
            entries.add(ModamodItems.MOLDE_CORREA_CADENA_FINA);
            entries.add(ModamodItems.MOLDE_CORREA_OJALILLOS);
            entries.add(ModamodItems.MOLDE_CORREA_CORDON);
            net.minecraft.item.ItemStack choker = new net.minecraft.item.ItemStack(ModamodItems.BANDA);
            com.modamod.item.BandaItem.setZona(choker, com.modamod.item.BandaZona.CUELLO);
            com.modamod.item.BandaItem.setAncho(choker, com.modamod.item.BandaAncho.FINO);
            entries.add(choker);
            // Variantes de fábrica hasta que lleguen los moldes de la Modeladora (tanda 2).
            for (var par : new Object[][] {
                    {com.modamod.item.SombreroAla.CORTA, com.modamod.item.SombreroPunta.RECTA},
                    {com.modamod.item.SombreroAla.ANCHA, com.modamod.item.SombreroPunta.DOBLADA},
                    {com.modamod.item.SombreroAla.CORTA, com.modamod.item.SombreroPunta.DOBLADA}}) {
                net.minecraft.item.ItemStack variante = new net.minecraft.item.ItemStack(ModamodItems.SOMBRERO_BRUJA);
                com.modamod.item.SombreroBrujaItem.setAla(variante, (com.modamod.item.SombreroAla) par[0]);
                com.modamod.item.SombreroBrujaItem.setPunta(variante, (com.modamod.item.SombreroPunta) par[1]);
                entries.add(variante);
            }
            entries.add(ModamodItems.MAID_OUTFIT);
            // El Buzo Oversize viejo (armadura) se reemplazó por el hoodie
            // (2026-09-30): sigue registrado para no romper mundos, fuera de la pestaña.
            entries.add(ModamodItems.PATTERN_STRIPE_TOP);
            entries.add(ModamodItems.PATTERN_STRIPE_ALT);
            entries.add(ModamodItems.PATTERN_TRIPLE_STRIPE);
            entries.add(ModamodItems.PATTERN_CORAZONES);
            entries.add(ModamodItems.PATTERN_ESTRELLAS);
            entries.add(ModamodItems.PATTERN_LUNARES);
            entries.add(ModamodItems.PATTERN_VICHY);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_DE_CORTE);
            // Moldes de la Sublimadora (manga y cuello por valor).
            entries.add(com.modamod.sublimadora.ModItems.MOLDE_CUELLO_REDONDO);
            entries.add(com.modamod.sublimadora.ModItems.MOLDE_CUELLO_V);
            for (var m : com.modamod.sublimadora.ModItems.MOLDES_MASCARA) entries.add(m);
            entries.add(com.modamod.sublimadora.ModItems.MOLDE_CUELLO_POLERA);
            entries.add(com.modamod.sublimadora.ModItems.MOLDE_CUELLO_CUADRADO);
            entries.add(com.modamod.sublimadora.ModItems.MOLDE_CUELLO_CORAZON);
            entries.add(com.modamod.sublimadora.ModItems.MOLDE_CUELLO_CAMISA);
            // Los 8 presets de combo directo (cobertura de torso/extremidad)
            // se sacaron de la pestaña — traen los anclajes horneados de
            // fábrica e ignoran Anclaje/Lado por completo, lo que generaba
            // confusión al mezclarse con el flujo real de moldes por-eje.
            // Siguen registrados (no se borra nada), solo dejan de listarse.
            // Molde de rango unificado (a pedido): mismos 5, sirven para
            // pantalón/medias/calientabrazos/manga de remera, cada una
            // traduciéndolo a su propia escala real al fijar.
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RANGO_CERO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RANGO_MINIMO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RANGO_CORTO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RANGO_MEDIO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RANGO_MEDIOLARGO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RANGO_LARGO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RANGO_MAXIMO);
            // Molde de calce (a pedido): transversal a las 4 categorías,
            // controla la dilatación de la geometría 3D, no recorta tela.
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CALCE_PEGADO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CALCE_AJUSTADO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CALCE_NORMAL);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CALCE_SUELTO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CALCE_OVERSIZE);
            // Molde de red (a pedido): transversal a las 4 categorías,
            // perfora la tela ya compuesta, no recorta filas.
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_FINA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_GRUESA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_HEXAGONAL);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_PERFORADA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_ENCAJE);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_RAYAS);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_ESCOCESA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_ARNES_X);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_ARNES_TIRANTES);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_ARNES_BANDAS);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RED_LISA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_POLLERA_CAMPANA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_POLLERA_TABLEADA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_POLLERA_TUBO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_POLLERA_GLOBO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_POLLERA_CIRCULAR);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_VOLADO_RECTO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_VOLADO_CIRCULAR);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CAPA_RUEDO_RECTO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CAPA_RUEDO_REDONDEADO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CAPA_RUEDO_COLA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CAPA_CON_CAPUCHA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CAPA_SIN_CAPUCHA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CAPA_CUELLO_ALTO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CAPA_SIN_CUELLO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_SOMBRERO_ALA_ANCHA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_SOMBRERO_ALA_CORTA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_SOMBRERO_PUNTA_RECTA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_SOMBRERO_PUNTA_DOBLADA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_BANDA_ZONA_CINTURA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_BANDA_ZONA_CUELLO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RUEDO_RECTO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RUEDO_AJUSTADO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RUEDO_CAMPANA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RUEDO_ONDULADO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RUEDO_FESTONEADO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_RUEDO_PICO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CHAQUETA_FRENTE_CERRADA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CHAQUETA_FRENTE_ABIERTA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CHAQUETA_CAPUCHA_CON);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CHAQUETA_CAPUCHA_SIN);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CHAQUETA_SOLAPA_NINGUNA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CHAQUETA_SOLAPA_PICO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CHAQUETA_SOLAPA_REDONDA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_CHAQUETA_SOLAPA_CHAL);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_BANDA_ANCHO_FINO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_BANDA_ANCHO_MEDIO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_BANDA_ANCHO_ANCHO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_BANDA_HERRAJE_NINGUNO);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_BANDA_HERRAJE_PLACA);
            entries.add(com.modamod.modelado.ModeladoMod.MOLDE_BANDA_HERRAJE_ARO);
            })
            .build();

    private static void registrarPestanaCreativa() {
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "modamod"), PESTANA);
    }
}
