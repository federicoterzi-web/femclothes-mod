# Sublimadora — bloque de Minecraft (Java · GeckoLib)

Prensa térmica de sublimación, 16×16×16, con tapa abisagrada que se abre como un cofre.

## Qué hay acá

    pack.mcmeta                                  pack_format 34 (1.21.x) — ajustalo a tu versión
    assets/sublimadora/geo/                      sublimadora.geo.json — geometría Bedrock (GeckoLib)
    assets/sublimadora/animations/               abrir · abierta · cerrar · cerrada
    assets/sublimadora/textures/block/           sublimadora_atlas.png (64×64) + las 13 tiles de 16×16
    assets/sublimadora/blockstates/              variante sin GeckoLib (open=false/true × 4 rotaciones)
    assets/sublimadora/models/block/             modelos JSON de esa variante
    assets/sublimadora/models/item/              modelo de inventario
    assets/sublimadora/lang/                     es_ar y en_us
    src/main/java/com/ejemplo/sublimadora/       bloque, GeoBlockEntity, GeoModel, renderer

## GeckoLib (camino principal)

Huesos: `root` → `base` (fija) y `tapa`, con pivote en `[0, 11, 8]` — la bisagra del
borde trasero, a la altura de la plancha. La animación gira solo la tapa: −104° en
0.35 s con un leve rebase al final; `cerrar` vuelve en 0.3 s con un golpecito de
asentamiento. `cerrada` y `abierta` son las poses en loop que sostienen cada extremo.

El controlador de `SublimadoraBlockEntity` lee la propiedad `OPEN` del estado y
encadena `abrir → abierta` o `cerrar → cerrada`. Las dos quedan además registradas
como triggerable, si preferís dispararlas desde el server con
`triggerAnim("tapa", "abrir")` — hay una línea comentada en `onUse` para eso.

Para editar geometría o animación: Blockbench, modo **Bedrock Block**, abrí el
`.geo.json` y cargá el `.animation.json` en la pestaña Animate.

## Display CMYK

En el costado **oeste** (izquierdo, con el frente hacia el norte): bisel oscuro, visor
negro y cuatro barras verticales — cián, magenta, amarillo y negro — sobre riel claro.
El canal K es negro real (`#17171a`); se lee por contraste contra el riel gris claro,
no por ser blanco.

Las barras **no** son textura pintada: cada una es un hueso propio del `.geo.json`
(`ink_c`, `ink_m`, `ink_y`, `ink_k`) con el pivote en su base, y
`SublimadoraGeoModel#setCustomAnimations` les escala el eje Y con el nivel de tinta
(0 = vacía, 1 = llena). Escalar desde el pivote inferior hace que se llenen de abajo
hacia arriba; con nivel 0 el hueso se oculta.

El dato vive en el block entity: `float[] tinta` por canal, persistido en NBT y
sincronizado al cliente con `toUpdatePacket` / `toInitialChunkDataNbt`. `tick(...)`
suaviza el valor mostrado y `getNivelInterpolado` lo interpola entre ticks, así la
barra baja con transición en vez de saltar. Métodos útiles: `consumir(0.02f)` (gasta
los cuatro canales; devuelve `false` si alguno está vacío) y `setTinta(canal, 1f)`
para un cartucho nuevo. En `onUse`, cerrar la tapa cuenta como prensado y consume.

Para que el ticker corra, registrá el `BlockEntityType` con
`SublimadoraBlockEntity::tick` como ticker en `ModBlocks`.

## Lo que tenés que completar

- **Dependencia**: GeckoLib 4.x para tu loader y versión.
- **`ModBlocks`**: registro del bloque, del `BlockEntityType` (`SUBLIMADORA_ENTITY`) y
  del renderer — `BlockEntityRendererFactories.register(..., ctx -> new SublimadoraRenderer())`.
  Es específico de Fabric / NeoForge, por eso no viene escrito.
- **Mappings**: el código está contra Yarn 1.21 y GeckoLib 4.x; los nombres cambian
  entre versiones y entre Yarn y Mojmap.
- Si la tapa abre hacia el frente en vez de hacia atrás, invertí el signo de la
  rotación en `sublimadora.animation.json` (−104 → 104).
- Ítem de inventario: el modelo JSON alcanza. Si querés el modelo GeckoLib también en
  la mano, el `BlockItem` tiene que implementar `GeoItem` y llevar su `GeoItemRenderer`.

## Variante sin GeckoLib

Los `blockstates` y los modelos JSON siguen ahí: `sublimadora` (cerrada) y
`sublimadora_open` (tapa parada atrás). El cambio es instantáneo, sin interpolación, y
no necesita ninguna dependencia. Usalos si querés un resource pack a secas.

## Geometría (unidades de 1/16 de bloque)

    patas          y 0–2      4 tacos de 3×2×3 en las esquinas
    zócalo         y 2–3      marco oscuro
    cuerpo         y 3–9      madera, manija al frente, rejillas a los costados
    marco sup.     y 9–10     marco oscuro
    plancha        y 10–11    base térmica, remera encima
    tapa           y 11–15.5  madera con borde oscuro, panel de control al frente
    bisagra        z 16       eje de rotación de la tapa
