package com.femclothes;

import com.femclothes.body.ComandoCuerpo;
import com.femclothes.body.PerfilesDeCuerpo;
import com.femclothes.garment.PrendasDelMod;
import com.femclothes.item.FemclothesComponents;
import com.femclothes.item.FemclothesItems;
import com.femclothes.screen.FemclothesScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Femclothes implements ModInitializer {

    public static final String MOD_ID = "femclothes";

    @Override
    public void onInitialize() {
        FemclothesItems.init();
        FemclothesComponents.init();
        // Que items son prendas del sistema de capas. Va DESPUES de los
        // items: registra por instancia, no por id.
        PrendasDelMod.init();
        PerfilesDeCuerpo.init();
        ComandoCuerpo.init();
        com.femclothes.body.RedCuerpo.init();
        com.femclothes.util.RedTwirl.init();
        com.femclothes.util.RedCapucha.init();
        com.femclothes.util.DebugMaquinas.init();
        FemclothesScreenHandlers.init();
        com.femclothes.modelado.ModeladoMod.register();
        com.femclothes.tinturas.TinturasMod.register();
        com.femclothes.guardarropas.GuardarropasMod.register();
        com.femclothes.maniqui.ManiquiMod.register();
        com.femclothes.estilado.EstiladoMod.register();
        registrarPestanaCreativa();
    }

    /**
     * Sin esto, la ÚNICA forma de conseguir cualquiera de estos ítems era
     * ya saber la receta de memoria — ninguno aparecía en el buscador
     * creativo. Se notó recién con calientabrazos ("me faltan"), pero el
     * agujero es de TODO `FemclothesItems` (pantalón, medias, los 16 moldes,
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
            .icon(() -> new ItemStack(com.femclothes.sublimadora.ModItems.REMERA))
            .displayName(Text.translatable("itemGroup.femclothes"))
            .entries((contexto, entries) -> {
            // Máquinas primero.
            entries.add(com.femclothes.modelado.ModeladoMod.MODELADO_BLOCK_ITEM);
            entries.add(com.femclothes.tinturas.TinturasMod.TINTURAS_BLOCK_ITEM);
            entries.add(com.femclothes.sublimadora.ModBlocks.SUBLIMADORA_ITEM);
            entries.add(com.femclothes.guardarropas.GuardarropasMod.GUARDARROPAS_BLOCK_ITEM);
            entries.add(com.femclothes.maniqui.ManiquiMod.MANIQUI_BLOCK_ITEM);
            entries.add(com.femclothes.estilado.EstiladoMod.ESTILADO_BLOCK_ITEM);
            // Prendas.
            entries.add(com.femclothes.sublimadora.ModItems.REMERA);
            entries.add(FemclothesItems.CHAQUETA);
            // Apliques (2026-10-01): los 3 moldes y retazos de prueba con colores
            // de fábrica, hasta que Tintes los tiña (fase 4).
            entries.add(FemclothesItems.MOLDE_APLIQUE_MONO);
            entries.add(FemclothesItems.MOLDE_APLIQUE_MARIPOSA);
            entries.add(FemclothesItems.MOLDE_APLIQUE_FLOR);
            entries.add(FemclothesItems.MOLDE_TEXTURA_FRUNCIDO);
            entries.add(FemclothesItems.MOLDE_TEXTURA_ACOLCHADO);
            entries.add(FemclothesItems.ESTROGENOS);
            entries.add(com.femclothes.aplique.RetazoApliqueItem.conColores(
                    new ItemStack(FemclothesItems.RETAZO_APLIQUE), 0xE878A8, 0xF8D860, 0x58A868));
            entries.add(com.femclothes.aplique.RetazoApliqueItem.conColores(
                    new ItemStack(FemclothesItems.RETAZO_APLIQUE), 0x78B8E8, 0xF2F2F6, 0x2E4A80));
            entries.add(com.femclothes.aplique.RetazoApliqueItem.conColores(
                    new ItemStack(FemclothesItems.RETAZO_APLIQUE), 0xC82828, 0x1E1E22, 0xE8C050));
            entries.add(FemclothesItems.SOCKS_34);
            entries.add(FemclothesItems.SOCKS_SOLID);
            entries.add(FemclothesItems.FISHNET_SOCKS);
            entries.add(FemclothesItems.PANTALON);
            entries.add(FemclothesItems.POLLERA);
            entries.add(FemclothesItems.TINTE_MEZCLA);
            // Los moldes por-eje ESPECÍFICOS de pantalón-largo y medias se
            // sacaron (a pedido, "sintetizar todos en esos dos moldes
            // aunque cada prenda tenga su propia medida") — reemplazados
            // por el molde de RANGO unificado de abajo, que sirve para las
            // 4 categorías de extremidad a la vez. Siguen registrados (el
            // Telar viejo los sigue usando), solo dejan de listarse acá.
            // El de TIRO también se saca ahora (a pedido, "fusionar largo
            // de remera y tiro de pantalón en cobertura de torso") —
            // reemplazado por el molde de TORSO unificado de abajo.
            entries.add(FemclothesItems.CALIENTABRAZOS);
            entries.add(FemclothesItems.CAPA);
            entries.add(FemclothesItems.MAID_OUTFIT);
            // El Buzo Oversize viejo (armadura) se reemplazó por el hoodie
            // (2026-09-30): sigue registrado para no romper mundos, fuera de la pestaña.
            entries.add(FemclothesItems.PATTERN_STRIPE_TOP);
            entries.add(FemclothesItems.PATTERN_STRIPE_ALT);
            entries.add(FemclothesItems.PATTERN_TRIPLE_STRIPE);
            entries.add(FemclothesItems.PATTERN_CORAZONES);
            entries.add(FemclothesItems.PATTERN_ESTRELLAS);
            entries.add(FemclothesItems.PATTERN_LUNARES);
            entries.add(FemclothesItems.PATTERN_VICHY);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_DE_CORTE);
            // Moldes de la Sublimadora (manga y cuello por valor).
            entries.add(com.femclothes.sublimadora.ModItems.MOLDE_MANGA);
            entries.add(com.femclothes.sublimadora.ModItems.MOLDE_CUELLO_REDONDO);
            entries.add(com.femclothes.sublimadora.ModItems.MOLDE_CUELLO_V);
            entries.add(com.femclothes.sublimadora.ModItems.MOLDE_CUELLO_POLERA);
            // Los 8 presets de combo directo (cobertura de torso/extremidad)
            // se sacaron de la pestaña — traen los anclajes horneados de
            // fábrica e ignoran Anclaje/Lado por completo, lo que generaba
            // confusión al mezclarse con el flujo real de moldes por-eje.
            // Siguen registrados (no se borra nada), solo dejan de listarse.
            // Molde de rango unificado (a pedido): mismos 5, sirven para
            // pantalón/medias/calientabrazos/manga de remera, cada una
            // traduciéndolo a su propia escala real al fijar.
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RANGO_MINIMO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RANGO_CORTO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RANGO_MEDIO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RANGO_MEDIOLARGO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RANGO_LARGO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RANGO_MAXIMO);
            // Molde de torso unificado (a pedido): mismos 3, sirven para
            // largo de remera y tiro de pantalón.
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_TORSO_CORTO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_TORSO_MEDIO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_TORSO_LARGO);
            // Molde de calce (a pedido): transversal a las 4 categorías,
            // controla la dilatación de la geometría 3D, no recorta tela.
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CALCE_PEGADO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CALCE_AJUSTADO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CALCE_NORMAL);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CALCE_SUELTO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CALCE_OVERSIZE);
            // Molde de red (a pedido): transversal a las 4 categorías,
            // perfora la tela ya compuesta, no recorta filas.
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_FINA);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_GRUESA);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_HEXAGONAL);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_PERFORADA);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_ENCAJE);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_RAYAS);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_ESCOCESA);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_ARNES_X);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_ARNES_TIRANTES);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_ARNES_BANDAS);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_RED_LISA);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_POLLERA_CAMPANA);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_POLLERA_TABLEADA);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CAPA_RUEDO_RECTO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CAPA_RUEDO_REDONDEADO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CAPA_CON_CAPUCHA);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CAPA_SIN_CAPUCHA);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CAPA_CUELLO_ALTO);
            entries.add(com.femclothes.modelado.ModeladoMod.MOLDE_CAPA_SIN_CUELLO);
            })
            .build();

    private static void registrarPestanaCreativa() {
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "femclothes"), PESTANA);
    }
}
