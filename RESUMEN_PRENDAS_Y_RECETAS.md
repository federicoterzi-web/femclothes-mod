# FemClothes — Resumen de prendas y recetas (para retomar en cualquier lado)

Mod de Minecraft Fabric 1.21.1, Loader 0.18.4. Ropa craftable, teñible,
que se dibuja pegada al cuerpo del jugador (no como armadura ancha).

> Última actualización: 2026-09-05.
>
> En este repo hay un segundo mod: `sublimadora/`, una prensa térmica que
> estampa fotos de Camerapture sobre remeras. Documentado aparte en
> [`sublimadora/README.md`](sublimadora/README.md).

## Las prendas

| Prenda | Estado | Mecanismo |
|---|---|---|
| **Medias color pleno** (`socks_solid`) | ✅ migrada y con patrones | Trinket, slot `socks/pair`, dibujada sobre la pierna real |
| Medias con raya (`socks_stripe_top`) | ⚠️ obsoleta — reemplazada por patrón en el telar | Ítem sin receta, `TwoToneArmorRenderProvider` |
| Medias alternadas (`socks_stripe_alt`) | ⚠️ obsoleta — idem | Ítem sin receta, `TwoToneArmorRenderProvider` |
| Medias 3/4 (`socks_34`) | placeholder | Slot cosmético (Cosmetic Armor Updated) |
| Medias de red (`fishnet_socks`) | placeholder, no dyeable | Pendiente: pasar a patrón "fishnet" |
| Shorts | placeholder | Pendiente migrar a `BodyPartTrinketRenderer` |
| Croptop | placeholder con relleno de piel | Slot `torso/prenda`; entra pero **no se dibuja**. Pendiente migrar a `BodyPartTrinketRenderer` |
| Traje de maid | placeholder, set completo en una textura | Sin definir |
| Buzo oversize | placeholder | Se queda con geometría ANCHA a propósito |
| Calentadores de brazo | ✅ funcional | Trinket, slot custom `arms/armwarmer` |

## Cómo funciona el sistema hoy

**Craftear** define la prenda y su color BASE. **El telar** agrega patrón,
color de patrón, y permite configurar cada pierna por separado.

### Crafteo de medias
6 lanas del MISMO color, columna izquierda y derecha, centro vacío:
```
A . A
A . A
A . A
```
Una receta por cada uno de los 16 colores (16 archivos), todas con la misma
clave de ingrediente en las dos columnas. Los RGB son los
`DyeColor.getFireworkColor()` de vanilla, los mismos que usa el telar, así
que craftear rojo y teñir de rojo dan el color idéntico.

### El telar
**Se usa el Telar VANILLA, no hay bloque propio.** Click derecho con una
prenda o un patrón de FemClothes en la mano abre la UI de ropa; con
cualquier otra cosa, el Telar normal de banderas.

| Slots | Resultado |
|---|---|
| Prenda + tinte | Re-tiñe el color BASE, no toca el patrón |
| Prenda + tinte + patrón | Aplica el patrón con ese color, no toca la base |
| Prenda SOLA | Le saca el patrón y la deja lisa |

Un botón debajo del panel cicla **Ambas / Izquierda / Derecha**, y todo lo
de arriba respeta esa elección.

### Piernas independientes, sin duplicar ítems
Componentes `RIGHT_DYED_COLOR`, `RIGHT_PATTERN_ID` y `RIGHT_PATTERN_COLOR`,
todos OPCIONALES: ausentes, la derecha usa lo de la izquierda. Así un par
parejo no guarda nada extra y las 16 recetas siguen valiendo sin tocarlas.

`ClothingStyle` centraliza la resolución por lado. Ojo con una trampa que ya
costó un bug: para tocar SOLO la izquierda hay que llamar antes a
`pinRight()`, porque si no la derecha sigue heredando y cambian las dos.

### Reconocer el patrón sin ponerse la prenda
- **Ícono**: modelo de dos capas + `ItemColorProvider`. Capa 0 la prenda con
  el color base, capa 1 las rayas con el color del patrón. Sin patrón, la
  capa 1 se pinta igual que la base y desaparece.
- **Tooltip**: el nombre del patrón, escrito EN el color del patrón. Si las
  piernas difieren, las lista por separado.

⚠️ El tinte de ítem es **ARGB y el alfa cuenta**. Devolver `0xRRGGBB` deja el
ítem invisible. Vanilla usa `-1` para "sin tinte".

### Resolución x2
Las prendas son **128x128**, el doble que la skin. Subir `textureWidth` solo
no alcanza: en `ModelPart.Cuboid` el rectángulo UV se calcula con el TAMAÑO
DEL CUBOIDE en unidades de modelo. El truco es armar el cuboide al doble y
escalar la parte a la mitad. Todo sale de `BodyPartTrinketRenderer.SCALE`.

La escala va DESPUÉS de `copyTransform`, que también copia `xScale/yScale/zScale`.

### Sombreado
`faceFactor` degrada a lo ancho de las caras frontal y trasera, de oscuro en
el borde interno a claro en el externo. **Las caras interiores no se ven con
el jugador parado**, así que sombrearlas solas no separaba nada: lo que
funciona es el borde de las caras que sí se ven. Se aplica tanto a la tela
como a la piel reconstruida.

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

## El slot de torso es compartido entre los dos mods

El croptop de FemClothes y la remera estampada de la sublimadora entran al
MISMO slot `torso/prenda`, y son mutuamente excluyentes. No es una
limitación: dos prendas dibujadas sobre el mismo pedazo de cuerpo se
pisarían, así que en vez de resolver el solapamiento se eligió que no puedan
coexistir.

El slot lo define FemClothes y cada mod declara su ítem en el tag. Los tags
se fusionan entre datapacks, así que ninguno depende de que el otro esté
instalado.

Los iconos de los huecos vacíos viven en `textures/gui/slot/`, en gris y al
45% de alpha. Dos cosas aprendidas ahí: apuntar a la textura del ítem deja un
ícono a todo color que se lee como si ya hubiera algo equipado, y un slot sin
campo `icon` hace que Trinkets pida `minecraft:textures/.png` en cada frame
con el inventario abierto.

## Lo que falta

1. **Arte de verdad.** Solo las medias tienen textura, y es de prueba
   (generada por script, en el scratchpad). Las otras 7 prendas no tienen
   nada.
2. **Migrar croptop y shorts** a `BodyPartTrinketRenderer` — BLOQUEADO, ver
   abajo.
3. Geometría Blockbench del buzo oversize (Armor Model API).
4. Recetas de shorts, croptop, traje de maid, medias de red.
5. ~~El Mixin de override de skin~~ — HECHO: `AbstractClientPlayerEntityMixin`
   engancha en `getSkinTextures()` al RETURN. El puente `HttpTextureAccessor`
   para 3D Skin Layers está escrito pero **nunca se probó en el juego**; sus
   líneas de `modLocalRuntime` siguen comentadas en `build.gradle`.

## ⚠️ Problema abierto: dos prendas en la misma parte del cuerpo

`composeGarment` rellena con tono de piel TODO lo que la prenda deja
transparente. Para una sola prenda por parte del cuerpo está perfecto: la
media reconstruye el muslo desnudo de arriba.

Pero medias y shorts van los dos en las piernas. Cada uno rellenaría la
pierna entera con piel más su propia tela, y **el que se dibuje último tapa
al otro por completo**. No es que se vea feo: desaparece una de las dos
prendas.

Esto BLOQUEA migrar shorts, y hay que resolverlo antes de tocar esa parte.
Dos caminos:

- **Capa de piel separada**: un solo renderer dibuja la piel reconstruida,
  con dilatación un poco menor que las prendas, y cada prenda queda
  transparente donde no tiene tela. Sin Mixins.
- **Override de skin** (el Mixin de `getSkinTextures()`): la piel se
  reconstruye una vez sobre la skin misma, y las prendas no saben nada de
  piel. Además es lo que hace falta para 3D Skin Layers.

El segundo resuelve las dos cosas de una, pero es el primer Mixin del
proyecto.

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
