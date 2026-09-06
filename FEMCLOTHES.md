# FemClothes

Mod de Minecraft **Fabric 1.21.1** (Loader 0.18.4). Ropa craftable, teñible y
compatible con cualquier tono de piel, que se dibuja **pegada al cuerpo** del
jugador y no como armadura ancha — más una sublimadora que le estampa fotos.

Este es el mapa del proyecto. El detalle profundo de la máquina está aparte,
en [docs/SUBLIMADORA.md](docs/SUBLIMADORA.md).

> **Diseño en curso (no implementado)**: el rediseño del sistema de capas y
> geometría de prendas (cortes, fit, polleras, pantalones acampanados) está en
> [docs/PRENDAS.md](docs/PRENDAS.md), y las mesas de tinturas/sastrería con su
> automatización en [docs/MAQUINAS.md](docs/MAQUINAS.md). Los dos resuelven el
> "Problema abierto" de más abajo y reemplazan el hook del telar.
>
> **[docs/CANAL.md](docs/CANAL.md)** es la bitácora entre sesiones de Claude.
> `git pull` y leelo ANTES de trabajar; agregá una entrada y subí el
> `canal-version` DESPUÉS.

> **Toolchain**: Fabric Loom **1.15.3** + Gradle **9.2.0**.
> En Windows, `gradlew.bat` desde PowerShell — el `./gradlew` de Bash falla
> por finales de línea CRLF.
>
> `gradlew.bat runClient` levanta el cliente. Entra siempre como `H0p3san`,
> con nombre fijo puesto en `build.gradle`: sin eso Loom estrena un
> `Player###` al azar en cada arranque y, como el UUID offline se deriva del
> nombre, cada sesión entra al mundo como un jugador distinto y con el
> inventario vacío.

**Un solo mod, un solo namespace.** La sublimadora empezó como un mod aparte
con su propio jar; hoy es el paquete `com.femclothes.sublimadora` del mismo
jar, y todo vive bajo `femclothes:`. El paquete sigue separado porque el
ciclo de prensado no tiene nada que ver con el resto de las prendas.

---

## Las prendas

| Prenda | Estado | Mecanismo |
|---|---|---|
| **Remera** (`remera`) | ✅ 36 cortes, teñible, estampable | Trinket, slot `torso/prenda`, dibujada sobre el cuerpo real |
| **Medias color pleno** (`socks_solid`) | ✅ patrones, teñible, **estampable** | Trinket, slot `socks/pair`, dibujada sobre la pierna real |
| **Calentadores de brazo** (`armwarmers`) | ✅ funcional | Trinket, slot custom `arms/armwarmer` |
| Medias 3/4 (`socks_34`) | placeholder | Slot cosmético (Cosmetic Armor Updated) |
| Medias de red (`fishnet_socks`) | placeholder, no teñible | Pendiente: pasar a patrón "fishnet" |
| Shorts | placeholder | Pendiente migrar a `BodyPartTrinketRenderer` |
| Traje de maid | placeholder, set completo en una textura | Sin definir |
| Buzo oversize | placeholder | Se queda con geometría ANCHA a propósito |

Las tres primeras están en el sistema nuevo (`BodyPartTrinketRenderer` o el
renderer propio de la remera). El resto sigue en `ClothingArmorItem`, con
geometría de armadura.

**El croptop se retiró.** Era un chestplate del pipeline viejo, nunca se le
dibujó el arte —salía en damero— y desde que la remera tiene el eje de largo,
el corte crop hace lo mismo pero teñible, estampable y sobre la geometría del
cuerpo. Sacarlo destruye los que hubiera en mundos guardados.

### El slot de torso es de a una prenda

`torso/prenda` acepta una sola cosa por vez, a propósito: dos prendas
dibujadas sobre el mismo pedazo de cuerpo se pisarían, así que en vez de
resolver el solapamiento se eligió que no puedan coexistir.

Los iconos de los huecos vacíos van en `textures/gui/slot/`, en gris y al 45%
de alpha. Dos cosas aprendidas ahí: apuntar a la textura del ítem deja un
ícono a todo color que se lee como si ya hubiera algo equipado, y **un slot
sin campo `icon` hace que Trinkets pida `minecraft:textures/.png` en cada
frame** con el inventario abierto.

---

## Cómo se personaliza

**Craftear** define la prenda y su color BASE. **El telar** cambia patrón,
color de patrón, corte y color. **La sublimadora** imprime fotos.

### Crafteo

Medias: 6 lanas del mismo color, columnas izquierda y derecha.

```
A . A
A . A
A . A
```

Remera: 8 lanas del mismo color.

```
L . L
L L L
L L L
```

Una receta por cada uno de los 16 colores. Los RGB son los
`DyeColor.getFireworkColor()` de vanilla, los mismos que usa el telar, así
que craftear rojo y teñir de rojo dan el color idéntico.

### El telar

**Se usa el Telar VANILLA, no hay bloque propio.** Click derecho con una
prenda, un patrón o un molde en la mano abre la UI de ropa; con cualquier
otra cosa, el Telar normal de banderas.

| Slots | Resultado |
|---|---|
| Prenda + tinte | Re-tiñe el color BASE, no toca el patrón |
| Prenda + tinte + patrón | Aplica el patrón con ese color, no toca la base |
| Prenda SOLA | Le saca el patrón y la deja lisa |
| Remera + molde | Avanza un eje del corte |
| Remera + molde + tinte | Las dos cosas de una pasada |

Un botón debajo del panel cicla **Ambas / Izquierda / Derecha**, y todo lo de
arriba respeta esa elección.

Ni el patrón ni el molde se consumen, igual que un patrón de estandarte
vanilla.

### El corte de la remera: tres ejes

| eje | valores | filas de tela |
|---|---|---|
| largo | crop · normal · largo | 5 · 9 · 12 de las 12 del torso |
| mangas | sin · cortas · 3/4 · largas | 0 · 4 · 8 · 12 de las 12 del brazo |
| cuello | redondo · en V · polera | — |

Son **36 combinaciones**, y por eso el corte **no se craftea**: una receta por
combinación y color serían 576, y ninguna grilla de 3x3 distingue una manga
3/4 de una larga. Se craftea la remera base y el corte se cambia después en el
telar, con un **molde por eje** que *cicla* su valor — tres ítems en vez de
diez. El molde no toca la estampa, así que una remera ya impresa se puede
reformar sin perder la foto: justamente lo que no dejaría hacer una receta,
que tendría que fabricar el ítem de cero.

El nombre del ítem sale del rasgo que más lo define (croptop, musculosa,
polera, remerón, remera) y el resto del corte va al tooltip. 36 nombres serían
ilegibles y uno solo dejaría prendas muy distintas indistinguibles.

### Piernas independientes, sin duplicar ítems

Componentes `RIGHT_DYED_COLOR`, `RIGHT_PATTERN_ID` y `RIGHT_PATTERN_COLOR`,
todos OPCIONALES: ausentes, la derecha usa lo de la izquierda. Así un par
parejo no guarda nada extra y las 16 recetas siguen valiendo sin tocarlas.

`ClothingStyle` centraliza la resolución por lado. ⚠️ Trampa que ya costó un
bug: para tocar SOLO la izquierda hay que llamar antes a `pinRight()`, porque
si no la derecha sigue heredando y cambian las dos.

### Reconocer el patrón sin ponerse la prenda

- **Ícono**: modelo de dos capas + `ItemColorProvider`. Capa 0 la prenda con
  el color base, capa 1 las rayas con el color del patrón. Sin patrón, la capa
  1 se pinta igual que la base y desaparece.
- **Tooltip**: el nombre del patrón, escrito EN el color del patrón. Si las
  piernas difieren, las lista por separado.

---

## La sublimadora

Prensa térmica que estampa fotos de [Camerapture](https://modrinth.com/mod/camerapture)
sobre prendas en blanco: remeras y medias. Tres modos de estampa (logo,
centrada, full print), dos caras por prenda, tinta CMYK.

Todo el detalle —el ciclo, el modelo GeckoLib, el lienzo de estampado, las
trampas de render— está en [docs/SUBLIMADORA.md](docs/SUBLIMADORA.md).

Lo único que hay que saber desde acá: **qué prendas acepta lo dice
`ModItems.esEstampable`**, y esa lista tiene que coincidir con la de
`EstampaTextures.prendaDe`, que es la que sabe desdoblarlas. La primera vive
del lado del servidor a propósito; la segunda es código de cliente y tocarla
en un dedicado cargaría `NativeImage` y `MinecraftClient`.

---

## Texturas: DOS layouts distintos según el renderer

Esta es la trampa más fácil de pisar del proyecto. El layout depende de con
qué renderer se dibuja la prenda, y **no son intercambiables**:

- **Prendas del sistema nuevo** (`socks_solid`, `remera`): layout de **SKIN DE
  JUGADOR**, porque el renderer dibuja las ModelPart reales. Las dos piernas
  están en regiones SEPARADAS: derecha en `uv(0,16)`, izquierda en
  `uv(16,48)`; los brazos, derecho en `uv(40,16)` e izquierdo en `uv(32,48)`.
  Verificado en `PlayerEntityModel.getTexturedModelData`.

- **Prendas que siguen en `ClothingArmorItem`** (shorts, maid, buzo): layout
  de ARMADURA vanilla, tipo `leather_layer_1.png`.

Una textura 64x32 de armadura usada en una prenda de Trinkets hace que la
pierna izquierda samplee fuera de la imagen. Antes de dibujar, mirar con qué
renderer está registrada la prenda en `FemclothesClient`.

### Resolución 8×

Las prendas del sistema nuevo son **512x512**, ocho veces la skin. Subir
`textureWidth` solo no alcanza: en `ModelPart.Cuboid` el rectángulo UV se
calcula con el **TAMAÑO DEL CUBOIDE** en unidades de modelo, no con el tamaño
de la textura. El truco es armar el cuboide a 8× y escalar la parte a 1/8.
Sale de `BodyPartTrinketRenderer.SCALE` y de `RemeraTrinketRenderer.ESCALA`,
que valen lo mismo.

Fueron 2× hasta que las medias se volvieron estampables: a esa escala una cara
de pierna eran 8x24 texels y ninguna foto se lee ahí.

La escala va DESPUÉS de `copyTransform`, que también copia
`xScale/yScale/zScale`.

### Máscaras de patrón

```
textures/models/armor/patterns/<prenda>/<patrón>.png
```

Ej: `patterns/socks/stripe_top.png`. Es **por prenda** y no global porque la
máscara depende del UV map: una raya sobre las medias cae en píxeles distintos
que la misma raya sobre otra prenda. Agregar un patrón = soltar un PNG ahí,
sin tocar código. Si la máscara falta, la prenda cae a lisa en vez de mostrar
el cuadrado de textura faltante.

### Sombreado

`faceFactor` degrada a lo ancho de las caras frontal y trasera, de oscuro en
el borde interno a claro en el externo. **Las caras interiores no se ven con
el jugador parado**, así que sombrearlas solas no separaba nada: lo que
funciona es el borde de las que sí se ven. Se aplica tanto a la tela como a la
piel reconstruida.

### Estado del arte

Todo lo que hay es generado por script, no dibujado: las medias, sus máscaras
de patrón, y los 36 cortes de remera con su ícono, su textura de cuerpo y su
modelo. Las prendas placeholder no tienen nada.

---

## Componentes de datos

| Componente | Qué guarda |
|---|---|
| `minecraft:dyed_color` (vanilla) | Color BASE |
| `femclothes:pattern_id` | Qué patrón está aplicado. Ausente = lisa |
| `femclothes:pattern_color` | Color del patrón |
| `femclothes:right_dyed_color` / `right_pattern_id` / `right_pattern_color` | Overrides de la pierna derecha. Ausentes = hereda de la izquierda |
| `femclothes:variante` | El corte de la remera: largo, manga, cuello |
| `femclothes:estampa_frente` / `_espalda` | La foto de cada cara |
| `femclothes:picture_id` | El componente viejo de estampa. Solo se lee |
| `femclothes:cargas` | La tinta que viaja dentro de la máquina levantada |

⚠️ El tinte de ítem es **ARGB y el alfa cuenta**. Devolver `0xRRGGBB` deja el
ítem invisible; vanilla usa `-1` para "sin tinte".

---

## Decisiones de arquitectura

1. **Recetas JSON estáticas, no código Java.** `SpecialCraftingRecipe` se
   abandonó. El truco es el campo `"components"` en el resultado de una receta
   shaped normal.

2. **Las prendas "pegadas al cuerpo" usan Trinkets con renderer propio**
   (`BodyPartTrinketRenderer`), que dibuja directo sobre las ModelPart reales
   del jugador en vez de la geometría de armadura, más ancha.

3. **El buzo oversize es la única prenda que se queda con geometría ancha a
   propósito** (Cosmetic Armor Updated + Armor Model API).

4. **El telar es el vanilla, vía `UseBlockCallback`.** Se descartó tanto el
   Mixin sobre `LoomScreenHandler` (los slots son clases anónimas internas, y
   `LoomScreen` dibuja previsualización de bandera y listado scrolleable de
   patrones — habría que parchear las dos cosas) como un bloque propio
   (duplicaba un bloque que ya casi no se usa). `UseBlockCallback` es un hook
   público y estable de Fabric API.

5. **Lo que varía va en componentes, no en ítems.** 36 cortes × 16 colores ×
   estampas serían miles de ítems; son uno solo con datos encima.

---

## Dependencias

- **Trinkets** (original, emilyploszaj) `3.10.0`
- **Cardinal Components API** `6.1.3` — la pide Cosmetic Armor Updated
- **Cosmetic Armor Updated** `1.7.0`
- **GeckoLib** — fijado en `4.7.7` a propósito, ver docs/SUBLIMADORA.md
- **Camerapture** — opcional, `suggests`. La sublimadora anda sin él
- **Armor Model API** `1.0.0+1.21.1` — como **jar local** en `libs/`

### Armor Model API va como archivo local

El coordinate de Modrinth Maven no resolvió con ninguna variante de versión.
En vez de seguir adivinando: bajar el jar del
[release `1.0.0+1.21.1`](https://github.com/FabricExtras/ArmorModelAPI/releases)
(el que NO dice `-neoforge`), **renombrarlo a
`armor-model-api-1.0.0-1.21.1.jar`** —con guión en vez de `+`— y ponerlo en
`libs/`. El `+` en el nombre, combinado con espacios en la ruta en Windows,
rompe el cálculo de hash de Gradle/Loom. Por la misma razón la carpeta del
proyecto no puede tener espacios ni paréntesis.

### ArmorRenderLib no existe para 1.21.x

Ni el proyecto original (se quedó en 1.19.2) ni el fork "Directors Cut" (en
1.20.1) tienen build para 1.21.x. Se sacó del proyecto: nunca fue más que una
envoltura fina sobre el `ArmorRenderer` que ya trae Fabric API
(`net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer`), así que no se
perdió funcionalidad.

---

## Color de la piel expuesta

Muchas prendas dejan piel a la vista: la panza de un croptop, el muslo sobre
la media, el brazo de una musculosa. **No alcanza con dejar alpha 0** para que
se vea el jugador debajo: la skin casi siempre tiene ropa pintada en el torso,
así que un croptop mostraría la remera pintada de la skin y no piel. Hay que
repintar esas zonas con el tono real.

`SkinRegions` dice qué parte de la skin toca cada prenda, `SkinToneSampler`
saca la paleta y `ComposedSkin` la repinta, una sola vez, sobre la skin misma
(vía `AbstractClientPlayerEntityMixin`).

La regla del torso: **la prenda manda de los hombros a la cintura, el pantalón
de la cintura para abajo.** Repintar el torso completo borraba la cintura del
pantalón, que en una skin va pintada en las últimas filas del TORSO y no en
las piernas.

### Trampas que ya costaron tiempo

1. **`PlayerSkinTexture` extiende `ResourceTexture`, no
   `NativeImageBackedTexture`.** El cast que sugería una nota vieja falla para
   toda skin real. Lo que funciona es leer los píxeles de vuelta desde la GPU
   con `NativeImage.loadFromTextureImage()`, en el hilo de render y cacheado
   por Identifier. Está en `SkinTextureAccess`.
2. **El punto (44,20) NO es "la parte interna del antebrazo".** En el layout
   64x64 es la esquina superior de la cara frontal del brazo derecho — el
   hombro, que suele estar tapado por la manga.
3. **`NativeImage` empaqueta ABGR, no ARGB.** El rojo está en los bits bajos.

### Z-fighting: no es un problema

`RenderLayer.getArmorCutoutNoCull` ya incluye
`.layering(VIEW_OFFSET_Z_LAYERING)`, que escala la model-view a 0.99975586
antes de dibujar, más `LEQUAL_DEPTH_TEST`. Dibujar sobre la ModelPart base del
jugador no produce z-fighting.

La dilatación de 0.3 que sí hay **no es por eso**: es para ganarle en
occlusión a la segunda capa de la skin.

---

## ⚠️ Problema abierto: dos prendas en la misma parte del cuerpo

`composeGarment` rellena con tono de piel TODO lo que la prenda deja
transparente. Para una sola prenda por parte del cuerpo está perfecto: la
media reconstruye el muslo desnudo de arriba.

Pero medias y shorts van los dos en las piernas. Cada uno rellenaría la pierna
entera con piel más su propia tela, y **el que se dibuje último tapa al otro
por completo**. No es que se vea feo: desaparece una de las dos prendas.

Esto BLOQUEA migrar shorts. Dos caminos:

- **Capa de piel separada**: un solo renderer dibuja la piel reconstruida, con
  dilatación un poco menor que las prendas, y cada prenda queda transparente
  donde no tiene tela. Sin Mixins.
- **Reconstruir sobre la skin** (lo que ya hace `ComposedSkin` para el torso):
  las prendas no saben nada de piel. Es además lo que hace falta para 3D Skin
  Layers.

---

## 3D Skin Layers (mod del server, id `skinlayers3d`)

Convierte la capa externa de la skin en geometría 3D real. Findings de la
inspección del jar 1.11.2:

- **Rompe nuestras prendas si no lo prevemos**: si la skin tiene remera en la
  capa externa, esa remera pasa a ser volumen 3D *alrededor* del cuerpo y la
  prenda queda tapada adentro. Por eso `SkinRegions` borra esas zonas de la
  capa externa.
- **Lee la skin de `AbstractClientPlayerEntity.getSkinTextures().texture()`**
  (vía `PlayerUtil.getPlayerSkin`). Un Mixin en `PlayerEntityRenderer.getTexture`
  NO alcanzaría, 3DSL no se enteraría.
- **Cachea por Identifier y reconstruye cuando cambia** (`getCurrentSkin` /
  `clearMeshes`).
- **Solo extruye píxeles sólidos** (`create3DMesh` → `SolidPixelWrapper.wrapBox`
  → `TextureData.isSolid`). Alpha 0 en la capa externa = sin geometría 3D.
- **PERO `SkinUtil.getTexture` solo sabe leer dos cosas**: recursos del
  ResourceManager, o texturas que implementen su interfaz
  `HttpTextureAccessor`. Una textura registrada en runtime no es ninguna de las
  dos y devuelve null — o sea, un skin override ingenuo le APAGA las capas 3D
  al jugador. La salida es que nuestra clase de textura implemente
  `HttpTextureAccessor`, aislada tras `FabricLoader.isModLoaded("skinlayers3d")`.
- La API pública (`SkinLayersAPI`) no sirve: sus setters son globales de un
  solo dueño.

**Escrito pero nunca probado en el juego.** Las líneas de `modLocalRuntime`
siguen comentadas en `build.gradle`.

---

## Lo que falta

1. **Arte de verdad.** Todo lo que hay es generado por script.
2. **La prenda sobre la plancha de la sublimadora** no muestra la estampa ni
   el teñido, y con medias adentro igual se ve una remera: esa prenda es parte
   del atlas de GeckoLib de la máquina, no del sistema de texturas.
3. **El ícono de las medias estampadas** no muestra la foto. La remera tiene
   un renderer propio para eso (`RemeraItemRenderer`); las medias usan un
   modelo normal.
4. **Migrar shorts** a `BodyPartTrinketRenderer` — BLOQUEADO, ver arriba.
5. **Deduplicar `RemeraTrinketRenderer` contra `BodyPartTrinketRenderer`**,
   que hoy hacen casi lo mismo. Se podía recién desde la fusión de los mods.
6. **Geometría Blockbench del buzo oversize** (Armor Model API). Hay un TODO
   en `FemclothesClient` marcando dónde va el registro.
7. **Probar el puente de 3D Skin Layers.**
