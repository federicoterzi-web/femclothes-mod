# FemClothes — Resumen de prendas y recetas (para retomar en cualquier lado)

Mod de Minecraft Fabric 1.21.1, Loader 0.18.4. Ropa craftable, teñible,
que se dibuja pegada al cuerpo del jugador (no como armadura ancha).

> Última actualización: 2026-09-04.

## Las prendas

| Prenda | Estado | Mecanismo |
|---|---|---|
| **Medias color pleno** (`socks_solid`) | ✅ migrada y con patrones | Trinket, slot `socks/pair`, dibujada sobre la pierna real |
| Medias con raya (`socks_stripe_top`) | ⚠️ obsoleta — reemplazada por patrón en el telar | Ítem sin receta, `TwoToneArmorRenderProvider` |
| Medias alternadas (`socks_stripe_alt`) | ⚠️ obsoleta — idem | Ítem sin receta, `TwoToneArmorRenderProvider` |
| Medias 3/4 (`socks_34`) | placeholder | Slot cosmético (Cosmetic Armor Updated) |
| Medias de red (`fishnet_socks`) | placeholder, no dyeable | Pendiente: pasar a patrón "fishnet" |
| Shorts | placeholder | Pendiente migrar a `BodyPartTrinketRenderer` |
| Croptop | placeholder con relleno de piel | Pendiente migrar a `BodyPartTrinketRenderer` |
| Traje de maid | placeholder, set completo en una textura | Sin definir |
| Buzo oversize | placeholder | Se queda con geometría ANCHA a propósito |
| Calentadores de brazo | ✅ funcional | Trinket, slot custom `arms/armwarmer` |

## Cómo funciona el sistema hoy

**Craftear** define la prenda y su color BASE. **El telar** agrega patrón
y color de patrón después.

### Crafteo de medias
6 lanas del MISMO color, columnas izquierda y central:
```
A A .
A A .
A A .
```
Una receta por cada uno de los 16 colores de lana (16 archivos). Cada una
setea `minecraft:dyed_color` en el resultado vía el campo `"components"`.
Los RGB usados son los `DyeColor.getFireworkColor()` de vanilla, los
mismos que usa el telar — así craftear rojo y teñir de rojo dan el color
idéntico.

### El telar
**Se usa el Telar VANILLA, no hay bloque propio.** Click derecho sobre un
Telar con una prenda o un patrón de FemClothes en la mano → abre la UI de
ropa. Con cualquier otra cosa en la mano → Telar normal de banderas.

- Prenda + tinte (sin patrón) → re-tiñe el color BASE, como el cuero.
- Prenda + tinte + patrón → aplica el patrón con el tinte como color de
  patrón, sin tocar el color base. El patrón NO se consume, el tinte sí.

## Componentes de datos

- `minecraft:dyed_color` (vanilla) — color BASE.
- `femclothes:pattern_id` (`Identifier`) — qué patrón está aplicado.
  Ausente = prenda lisa.
- `femclothes:pattern_color` (`int` RGB) — color del patrón.

## Convención de texturas de patrón

```
textures/models/armor/patterns/<prenda>/<patrón>.png
```
Ej: `patterns/socks/stripe_top.png`. Es POR PRENDA y no global porque la
máscara depende del UV map: una raya sobre las medias cae en píxeles
distintos que la misma raya sobre un croptop. Agregar un patrón a una
prenda = soltar un PNG ahí, sin tocar código.

Si la máscara falta, `ClothingTextureCache.tintedWithPattern` cae a prenda
lisa en vez de mostrar el cuadrado de textura faltante.

## Decisiones de arquitectura (por qué está armado así)

1. **Recetas JSON estáticas, no código Java.** El enfoque de
   `SpecialCraftingRecipe` se abandonó (ver lección 8 abajo). El truco es
   el campo `"components"` en el resultado de una receta shaped normal.

2. **Las prendas "pegadas al cuerpo" usan Trinkets con renderer propio**
   (`BodyPartTrinketRenderer`) que dibuja directo sobre las ModelPart
   reales del jugador, en vez de la geometría de armadura (más ancha).

3. **El buzo oversize es la única prenda que se queda con geometría ancha
   a propósito** (Cosmetic Armor Updated + Armor Model API).

4. **El telar es el vanilla, vía `UseBlockCallback`.** Se descartó tanto
   el Mixin sobre `LoomScreenHandler` (los slots son clases anónimas
   internas, y `LoomScreen` del cliente dibuja previsualización de bandera
   y listado scrolleable de patrones — habría que parchear las dos cosas)
   como el bloque propio (duplicaba un bloque que ya casi no se usa).
   `UseBlockCallback` es un hook público y estable de Fabric API.

## Lo que falta

1. **TODAS las texturas.** No hay una sola en el proyecto — las prendas se
   ven como cuadrado de textura faltante y los patrones caen a "lisa".
   Este es el bloqueante real.
2. **Color de piel expuesta** — ver sección abajo, sin resolver.
3. Migrar croptop y shorts a `BodyPartTrinketRenderer`.
4. Borrar `socks_stripe_top` / `socks_stripe_alt` / `TwoToneArmorRenderProvider`.
5. Geometría Blockbench del buzo oversize (Armor Model API).
6. Recetas de shorts, croptop, traje de maid, medias de red.

## Color de la piel expuesta

Muchas prendas dejan piel a la vista (panza del croptop, muslos con
shorts, pierna sobre la media). No alcanza con dejar alpha 0 para que se
vea el jugador debajo: **la skin del jugador casi siempre tiene ropa
pintada en el torso**, así que un croptop mostraría la remera pintada de
la skin, no piel. Por eso hay que repintar esas zonas con el tono real.

**Estado: el sampleo funciona.** `SkinToneSampler` usa MODA (color más
frecuente) sobre la cara frontal de la cabeza más los dos antebrazos, con
los UV corregidos para skins slim (Alex, brazos de 3px).

Dos trampas que ya costaron tiempo y NO hay que repetir:

1. **`PlayerSkinTexture` extiende `ResourceTexture`, no
   `NativeImageBackedTexture`.** El cast que sugería la nota vieja falla
   para toda skin real. La forma que funciona es leer los píxeles de vuelta
   desde la GPU con `NativeImage.loadFromTextureImage()`, en el hilo de
   render, cacheado por Identifier.
2. **El punto (44,20) NO es "la parte interna del antebrazo".** En el
   layout 64x64 es la esquina superior de la cara frontal del brazo
   derecho — el hombro, que suele estar tapado por la manga.

## 3D Skin Layers (mod del server, id `skinlayers3d`)

Convierte la capa externa de la skin en geometría 3D real. Findings de la
inspección del jar 1.11.2:

- **Rompe nuestras prendas si no lo previmos**: si la skin tiene remera en
  la capa externa, esa remera pasa a ser volumen 3D *alrededor* del cuerpo
  y el croptop (dibujado sobre la ModelPart base) queda tapado adentro.
- **Lee la skin de `AbstractClientPlayerEntity.getSkinTextures().texture()`**
  (vía `PlayerUtil.getPlayerSkin`). Si alguna vez hacemos un Mixin de skin
  override, tiene que ser ahí — enganchar en `PlayerEntityRenderer.getTexture`
  NO alcanza, 3DSL no se enteraría.
- **Cachea por Identifier y reconstruye cuando cambia** (`getCurrentSkin` /
  `clearMeshes`). Una skin override con otro Identifier regenera sus meshes.
- **Solo extruye píxeles sólidos** (`create3DMesh` → `SolidPixelWrapper.wrapBox`
  → `TextureData.isSolid`). Alpha 0 en la capa externa = sin geometría 3D.
- **PERO `SkinUtil.getTexture` solo sabe leer dos cosas**: recursos del
  ResourceManager, o texturas que implementen su interfaz
  `HttpTextureAccessor`. Una textura registrada en runtime no es ninguna de
  las dos y devuelve null — o sea, un skin override ingenuo le APAGA las
  capas 3D al jugador. La salida es que nuestra clase de textura implemente
  `HttpTextureAccessor`, aislada tras `FabricLoader.isModLoaded("skinlayers3d")`.
- La API pública (`SkinLayersAPI`) no sirve para esto: sus setters
  (`setupMeshTransformerProvider`, `setupBoxBuilder`, `setLayerTransformer`)
  son globales de un solo dueño.

**Idea sin implementar:** si la prenda se compone dentro de la capa externa
de la skin en vez de dibujarse como geometría aparte, 3DSL le da volumen
real gratis, y sin el mod se ve plana (degradación limpia). Eso partiría la
arquitectura en prendas pegadas al cuerpo (compuestas en la skin) y prendas
holgadas (geometría propia vía Armor Model API).

## Nota sobre z-fighting: NO es un problema

`RenderLayer.getArmorCutoutNoCull` ya incluye `.layering(VIEW_OFFSET_Z_LAYERING)`,
que escala la model-view a 0.99975586 antes de dibujar, más
`LEQUAL_DEPTH_TEST`. Dibujar sobre la ModelPart base del jugador no produce
z-fighting. No hace falta dilatar nada.
