# FemClothes

Mod de Minecraft **Fabric 1.21.1** (Loader 0.18.4). Ropa craftable, teñible y
compatible con cualquier tono de piel, que se dibuja **pegada al cuerpo** del
jugador y no como armadura ancha — más una sublimadora que le estampa fotos.

Este es el mapa del proyecto. El detalle profundo de la máquina está aparte,
en [docs/SUBLIMADORA.md](docs/SUBLIMADORA.md).

> **Diseño en curso**: el rediseño de la geometría de prendas (cortes, fit,
> polleras, pantalones acampanados) está en [docs/PRENDAS.md](docs/PRENDAS.md),
> y las mesas de tinturas/sastrería con su automatización en
> [docs/MAQUINAS.md](docs/MAQUINAS.md). La **fase 1** —sistema de capas,
> cuerpo base y selector de región— ya está implementada; el resto es diseño.
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
| **Remera** (`remera`) | ✅ 45 cortes, teñible, patrones, estampable | Trinket, slot `torso/prenda`, dibujada sobre el cuerpo real |
| **Medias color pleno** (`socks_solid`) | ✅ 5 largos, patrones, teñible, **estampable** | Trinket, slot `socks/pair`, dibujada sobre la pierna real |
| **Pantalón** (`pantalon`) | ✅ 5 largos + 3 tiros, teñible, sin patrón todavía | Trinket, slot `piernas/exterior`, primera prueba real del layering (capa arriba de la media) |
| **Calientabrazos** (`calientabrazos`) | ✅ cobertura (5, molde de manga reusado) + tiro (3, molde de pantalón reusado), teñible, sin patrón todavía | Trinket, slot custom `arms/armwarmer`, dibujada sobre el brazo real (`Capa.MANGA_INTERIOR`, bajo la manga de la remera) |
| Medias 3/4 (`socks_34`) | placeholder | Slot cosmético (Cosmetic Armor Updated) |
| Medias de red (`fishnet_socks`) | placeholder, no teñible | Pendiente: pasar a patrón "fishnet" |
| Traje de maid | placeholder, set completo en una textura | Sin definir |
| Buzo oversize | placeholder | Se queda con geometría ANCHA a propósito |

Las cuatro primeras están en el sistema nuevo: `GarmentFeatureRenderer`
dibuja TODA la ropa del mod desde un solo lugar (ver "El sistema de capas"
más abajo). El resto sigue en `ClothingArmorItem`, con geometría de armadura.

**El croptop se retiró.** Era un chestplate del pipeline viejo, nunca se le
dibujó el arte —salía en damero— y desde que la remera tiene el eje de largo,
el corte crop hace lo mismo pero teñible, estampable y sobre la geometría del
cuerpo. Sacarlo destruye los que hubiera en mundos guardados.

### El slot de torso es de a una prenda — por ahora

`torso/prenda` acepta una sola cosa por vez. La razón original ya no vale:
era que dos prendas sobre el mismo pedazo de cuerpo se pisaban, y **eso lo
resolvió el sistema de capas**. Queda como está hasta que existan las prendas
que lo justifican (binder, corpiño), que es cuando se parte en
`torso/interior` + `torso/exterior` — ver PRENDAS.md §1.

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
| Remera + tinte + patrón | Igual que cualquier prenda: aplica el patrón con ese color |
| Remera SOLA | Le saca el patrón, igual que cualquier prenda |

Un botón debajo del panel cicla **Ambas / Izquierda / Derecha**, y todo lo de
arriba respeta esa elección — salvo la remera, que siempre es un solo color y
un solo patrón para toda la prenda (`Garment.regionesDe(PATRON)` declara
`ENTERA`), así que el botón no le hace nada.

⚠️ **El patrón de remera vive en el telar por ahora, como interino.**
`RegionResolver`/`PATTERN_ID` no son específicos de ninguna prenda —el dato
ya funcionaba para remera sin tocar nada—, lo que faltaba era la función del
telar (`reformarRemera`). Cuando exista la Estación de tintes de
`MAQUINAS.md` la aplicación se muda ahí; el render y el dato no cambian.

El patrón se pinta con **una sola máscara compartida por los 36 cortes**
(`textures/models/armor/patterns/remera/<patrón>.png`, del tamaño del corte
más grande), recortada gratis por el alfa de cada corte: un croptop nunca
muestra patrón más allá de su propio ruedo porque ahí la base ya es
transparente. Evita el problema de "N prendas × M patrones" que
[PRENDAS.md §6](docs/PRENDAS.md) marca — ver `EstampaTextures.aplicarPatron`.

**Pendiente, a propósito**: el ÍCONO de la remera no muestra el patrón
todavía (sí lo hacen las medias, con un modelo de dos capas). Remera tiene 36
modelos de ítem —uno por corte— y sumarle una segunda capa tintada a cada
uno es un trabajo de arte aparte, no de código.

Ni el patrón ni el molde se consumen, igual que un patrón de estandarte
vanilla.

### El corte de la remera: tres ejes

| eje | valores | filas de tela |
|---|---|---|
| largo | crop · normal · largo | 5 · 9 · 12 de las 12 del torso |
| mangas | sin · cortas · 3/4 · siete octavos · largas | 0 · 4 · 8 · 10 · 12 de las 12 del brazo |
| cuello | redondo · en V · polera | — |

Son **45 combinaciones** (antes 36 — `siete_octavos` se sumó a mangas, ver
"El eje cobertura" más abajo), y por eso el corte **no se craftea**: una
receta por combinación y color sería carísima, y ninguna grilla de 3x3
distingue una manga 3/4 de una larga. Se craftea la remera base y el corte
se cambia después en el telar. El molde de **largo** fija un valor
(`MoldeLargoRemeraItem`, uno por valor); los de **manga** y **cuello**
siguen ciclando (`MoldeItem`). El molde no toca la estampa, así que una
remera ya impresa se puede reformar sin perder la foto: justamente lo que
no dejaría hacer una receta, que tendría que fabricar el ítem de cero.

El nombre del ítem sale del rasgo que más lo define (croptop, musculosa,
polera, remerón, remera) y el resto del corte va al tooltip. 36 nombres serían
ilegibles y uno solo dejaría prendas muy distintas indistinguibles.

### El eje "cobertura": largo en pantalón, medias y remera

Tres prendas bilaterales miden lo mismo —cuánto de una extremidad tapan— con
la misma escala de 5 pasos: **extra corto/corto/mediano/largo/extralargo**.
Cada una le pone SUS nombres reales a esos 5 pasos, pero **no comparten
ítems**: cada prenda tiene su propio set de moldes, todos con la misma
filosofía —FIJAN un valor, no ciclan— porque con 5 pasos ciclar significa
hasta cuatro clicks a ciegas para llegar al que querés.

| prenda | eje | valores (extra corto → extralargo) | filas de las 12 |
|---|---|---|---|
| **Pantalón** | `PantalonLargo` | pantalón · tres cuartos · bermudas · shorts · ropa interior | 12·9·7·4·2 (cuenta DESDE la cintura hacia abajo) |
| **Medias** | `MediasLargo` | cancán · 3/4 · rodilla · medias · zoquetes | 10·8·6·4·2 (cuenta DESDE el tobillo hacia arriba — ¡al revés!) |
| **Remera (manga)** | `Variante.Manga` | larga · siete octavos · tres cuartos · corta · sin | 12·10·8·4·0 de las 12 del brazo |

⚠️ **Pantalón y medias llenan en direcciones OPUESTAS.** Un pantalón nace en
la cintura y crece hacia el tobillo; una media nace en el tobillo y crece
hacia la cintura. Confundir la dirección al generar una textura nueva deja el
hueco del lado equivocado.

**Calzoncillos, slip y tanga se fusionaron en `ROPA_INTERIOR`** (un solo
valor): son parecidos en largo y así el eje de pantalón queda en exactos 5
pasos, igual que medias.

**`MediasLargo.CANCAN` (10 filas) es idéntico byte a byte a la textura
`socks_solid_layer_1.png` de siempre** — verificado, no es una aproximación.
Es el default (sin componente = CANCAN), así que una media de un mundo viejo
se sigue viendo exactamente igual.

Los moldes de largo son **por valor, no cíclicos**, para las tres familias:
`MoldePantalonItem` (5), `MoldeMediaItem` (5, nuevo), y el de remera
(`MoldeLargoRemeraItem`, 3 — crop/normal/largo) es un eje APARTE, no
unificado con este (se evaluó y se descartó: quedan como conceptos
independientes, cada uno con su propio set de moldes).

El **tiro** del pantalón (`PantalonTiro`: corto/medio/largo — cuánto sube la
cintura sobre el torso, pintado en runtime, no en el PNG) es OTRO eje
independiente de `PantalonLargo`: uno decide hasta dónde llega la pierna, el
otro decide dónde arranca la cintura. Se pueden combinar libremente.

**Remera llegó al mismo diseño de 5 pasos** con `SIETE_OCTAVOS` (10 filas,
"un corte un poco más arriba del puño"), insertado entre `TRES_CUARTOS` y
`LARGA`. La remera pasa de 36 a **45 combinaciones** (3 largo × 5 manga × 3
cuello). Se generaron las 9 texturas nuevas (3 largo × 3 cuello, manga fija
en el valor nuevo) **derivándolas** de las 9 "larga" ya existentes —
recortando (alfa 0) las últimas 2 filas de manga y agregando una fila de
dobladillo en el nuevo borde, con el MISMO tono de dobladillo
`(188,188,198)` que ya usaba el generador original en el puño real, para que
no se note la costura entre lo generado por script y lo derivado a mano.
No hizo falta tocar `EstampaTextures`: `caras(Variante)` ya calculaba todo a
partir de `manga().filas` como variable, sin ningún valor hardcodeado — el
sistema de estampado es genérico de por sí.

⚠️ **Insertar un valor en el MEDIO de un enum de `Variante` es seguro** para
mundos guardados: el `CODEC` persistente serializa por NOMBRE
(`StringIdentifiable`), no por posición. Solo el `PACKET_CODEC` (sync de
red, no persistente) usa ordinal, y no necesita sobrevivir entre sesiones.

⚠️ **El ícono es UNO SOLO para los 5+5+3 valores de pantalón/medias/tiro**, a
propósito — esos NO tienen generador de sprite por valor. La manga de
remera es la excepción: sus 9 íconos nuevos SÍ existen, pero como
**placeholder** (reusan el ícono de manga larga del mismo largo/cuello, sin
arte propia todavía — visualmente indistinguibles de su combinación "larga"
hasta que alguien dibuje el sprite real).

**Un color de acento por EJE, no por prenda.** Los 3 moldes originales de
remera ya tenían esta convención sin que estuviera escrita: misma silueta de
"molde de papel" (`molde_largo.png`), un color de acento distinto por eje
— largo=rojo, manga=azul, cuello=verde. Los 13 moldes de pantalón/tiro/
medias la habían roto sin querer, reusando el rojo de largo-de-remera para
los tres a la vez. Se corrigió recoloreando (mismo script de un tinte,
Pillow) la MISMA silueta con un acento nuevo por eje: `molde_pantalon.png`
(ámbar) para el largo de pantalón, `molde_tiro.png` (violeta) para el tiro
—de pantalón Y calientabrazos, mismo ítem, mismo acento—, `molde_media.png`
(magenta) para el largo de medias. El eje sigue siendo lo que decide el
color, no la prenda: por eso tiro tiene un solo acento aunque sirva para dos
prendas distintas.

**Tooltip con lore, no solo "En el telar, con una prenda".** Cada molde
ahora dice arriba "Molde" (dorado) y una línea "Se usa en: X" con la(s)
prenda(s) reales — clave para que compartir moldes (manga, tiro) no
confunda cuál sirve para qué. Los patrones (`ClothingPatternItem`, antes sin
tooltip propio) ganaron lo mismo con "Patrón" (celeste, color opuesto a
molde a propósito) y su propia lista de prendas (remera, medias — pantalón
y calientabrazos todavía no aceptan patrón, por diseño). Helper compartido:
`PrendaLore.seUsaEn(...)`.

**Ningún ítem de `FemclothesItems` aparecía en el buscador creativo** —
pantalón, medias, calientabrazos, los 16 moldes y los 3 patrones solo eran
alcanzables sabiendo la receta de memoria (`SublimadoraMod` únicamente
agrega los de SU paquete: remera + sus 2 moldes cíclicos). Se notó cuando
faltó calientabrazos recién agregado; se agregaron TODOS los de
`FemclothesItems` a la pestaña `ItemGroups.FUNCTIONAL`, en
`Femclothes.onInitialize`.

### 💡 Idea anotada, SIN implementar: eje "fit"

El dueño tiró la idea de un cuarto eje transversal, además de cobertura/
tiro/borde: **fit** — cuán ajustada o suelta cae la tela sobre el cuerpo,
3 pasos: **skin tight / regular / oversize**. `PRENDAS.md` ya lo tenía
listado hace tiempo como eje de pantalón únicamente (línea "pantalón | fit
· largo² · tiro · botamanga"); la idea nueva es generalizarlo cross-prenda,
mismo criterio que ya se hizo con cobertura y tiro. Sin resolver todavía:
qué prendas lo usan, si comparte molde o es una familia por prenda, y cómo
se ve en la geometría (¿silueta distinta por Superficie, o solo un
recorte/dilatación distinta de la tela?). No tocar hasta que se hable el
concepto, mismo protocolo que el eje "Borde" (ver más abajo).

### Calientabrazos: 4ta prenda del eje, PRIMERA que comparte moldes

`Calientabrazos` (ex `armwarmers` — el ítem más viejo del mod, nunca
enganchado al sistema de capas) es la prenda base de BRAZO, análoga a las
medias en la pierna. A diferencia de pantalón/medias/remera (línea 170: "no
comparten ítems"), acá el dueño pidió explícitamente lo contrario — **cero
moldes nuevos**, reusar los que ya existen:

| eje | qué mueve | molde reusado |
|---|---|---|
| cobertura (5 pasos) | cuánto brazo tapa la tela, desde la MUÑECA hacia arriba | `MOLDE_MANGA` — el mismo cíclico de la manga de remera (`MoldeItem.Eje.MANGA`) |
| tiro (3 pasos) | otra banda, independiente, desde el HOMBRO hacia abajo | `MOLDE_TIRO_*` — el mismo fijo del tiro de pantalón |

Mismo molde físico, pero **componente propio por prenda**
(`CALIENTABRAZOS_COBERTURA`/`CALIENTABRAZOS_TIRO`, tipos `Variante.Manga`/
`PantalonTiro` reusados) — la manga de una remera puesta y la cobertura de
un calientabrazos puesto no se pisan entre sí. `MoldeItem` ganó un helper
público `siguienteManga(Variante.Manga)` para ciclar sin pasar por un
`Variante` completo.

⚠️ **Tiro NO pinta un parche en el torso — pinta una SEGUNDA banda en el
BRAZO, anclada en la punta opuesta a cobertura.** Cobertura llena desde la
muñeca hacia arriba; tiro llena desde el hombro hacia abajo, sobre el
MISMO archivo, en runtime, ignorando el alfa que cobertura ya haya puesto.
Con las dos cortas queda un hueco de piel en el medio del brazo a
propósito — "mangas custom en paralelo" (muñequera + hombrera sueltas), no
necesariamente un tubo continuo. El primer intento (pintar el tiro como
banda de cintura pero en el `Parte.TORSO`, copiando el mecanismo de
`pintarCintura` de pantalón tal cual) estaba mal: la manga real y el
parche del torso no tenían por qué tocarse -cobertura corta deja piel
antes de llegar al hombro-, y el parche pintaba las 4 caras del torso
entero (pecho Y espalda a la vez) — reportado jugando como "pinta todo el
pecho y la espalda". Corregido: `pintarBandaHombro` vive en
`PiezasDelMod.java` y pinta sobre `LayoutSkin.base(Parte.BRAZO_*).caras()`,
no sobre `Parte.TORSO`. Sin archivo propio para cobertura `SIN` (nunca
hacía falta antes): se parte de "larga" como base y se borra la manga
entera primero (`borrarManga`) para que el tiro no herede tela de muñeca
que cobertura dijo que no hay.

`Capa.MANGA_INTERIOR` (15, nueva) dibuja debajo de la manga de la remera —
mismo rol que `MEDIA` para la pierna. Las 4 texturas (`SIN` no genera
archivo) se derivaron de la manga de `cuerpo_normal_larga_redondo.png`
truncando desde el HOMBRO (dirección opuesta a como remera trunca las
suyas, desde la muñeca) — ver `tools/derivar_banda.py`, el primer script de
derivación que se guarda en el repo (antes eran ad-hoc, ver v9-v11 de
`docs/CANAL.md`).

⚠️ **El eje "Borde"** (forma de escote/cintura/borde superior — generalización
de "cuello" para pantalón/medias/calientabrazos) se discutió en la misma
sesión pero **no se implementó**: no tiene componente, no tiene molde, no
asumir que existe.

### Piernas independientes, sin duplicar ítems

Componentes `RIGHT_DYED_COLOR`, `RIGHT_PATTERN_ID` y `RIGHT_PATTERN_COLOR`,
todos OPCIONALES: ausentes, la derecha usa lo de la izquierda. Así un par
parejo no guarda nada extra y las 16 recetas siguen valiendo sin tocarlas.

`RegionResolver` centraliza la resolución por lado, y ahora también el eje
frente/espalda y la orientación. ⚠️ La trampa que costó un bug —tocar SOLO la
izquierda arrastraba la derecha, que heredaba— ya no se puede pisar: el guard
`fijarDerecha()` lo llaman los propios setters. Un guard que hay que acordarse
de invocar vuelve a fallar tarde o temprano.

### El selector de región

"Ambas / izquierda / derecha" y "frente / espalda" son **el mismo concepto**:
a qué pedazo de la prenda apunta una operación. Estaba escrito distinto en
cada lugar (el telar ciclaba su propio enum, la sublimadora tiene un selector
físico, los componentes se llamaban `right_dyed_color` en inglés pero
`estampa_frente` en castellano).

Ahora son `Lado` y `Cara`, los dos `Region`, y cada prenda declara
`regionesDe(Operacion)` — por operación y no de una vez, porque no coinciden:
una remera se tiñe entera pero se estampa por cara.

⚠️ **`AMBAS` no es uniforme.** Al teñir, setea el primario y BORRA el override
del otro lado (un par parejo no ocupa data extra). Al estampar, aplica a las
dos caras pero **cuesta tinta por cara**.

`Orientacion` (girado / espejado) se aplica **al resolver**, no permutando
datos guardados: sacar el flag devuelve la prenda exactamente a como estaba.

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

- **Prendas del sistema nuevo** (`socks_solid`, `pantalon`, `remera`): layout de
  **SKIN DE JUGADOR**, porque el renderer dibuja cajas con la geometría del
  jugador. `LayoutSkin` y `CajaSkin` calculan dónde cae cada cara — antes eran
  rectángulos escritos a mano y había que contar píxeles sobre la plantilla.
  Las dos piernas están en regiones SEPARADAS: derecha en `uv(0,16)`,
  izquierda en `uv(16,48)`; los brazos, derecho en `uv(40,16)` e izquierdo en
  `uv(32,48)`. Verificado en `PlayerEntityModel.getTexturedModelData`.

- **Prendas que siguen en `ClothingArmorItem`** (maid, buzo, fishnet, socks_34):
  layout de ARMADURA vanilla, tipo `leather_layer_1.png`.

Una textura 64x32 de armadura usada en una prenda del sistema nuevo hace que
la pierna izquierda samplee fuera de la imagen. Antes de dibujar, mirar si la
prenda está registrada en `PiezasDelMod` (sistema nuevo) o sigue siendo un
`ClothingArmorItem`.

### Resolución 8×

Las prendas del sistema nuevo son **512x512**, ocho veces la skin. Subir
`textureWidth` solo no alcanza: en `ModelPart.Cuboid` el rectángulo UV se
calcula con el **TAMAÑO DEL CUBOIDE** en unidades de modelo, no con el tamaño
de la textura. El truco es armar el cuboide a 8× y escalar la parte a 1/8.
Sale de `CuerpoGeometria.ESCALA`. Eran dos constantes con el mismo valor en
dos renderers distintos hasta que el sistema de capas los fusionó — y tenían
que coincidir sin que nada lo verificara.

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

2. **Las prendas "pegadas al cuerpo" usan Trinkets para el inventario, pero
   NO para dibujarse.** Un solo `GarmentFeatureRenderer` dibuja toda la ropa
   del mod, porque el orden de capa lo tenemos que decidir nosotros y no el
   order de los slots de Trinkets.

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

## El cuerpo base: qué hay debajo de la ropa

Muchas prendas dejan piel a la vista: la panza de un croptop, el muslo sobre
la media, el brazo de una musculosa. **No alcanza con dejar alpha 0** para que
se vea el jugador debajo: la skin casi siempre tiene ropa pintada en el torso,
así que un croptop mostraría la remera pintada de la skin y no piel.

La respuesta es un **cuerpo base**: una geometría propia, con las mismas cajas
que el jugador, dibujada **abajo de toda la ropa** y dilatada 0.30. Cada
prenda dibuja SOLO su tela y deja transparente lo demás; el hueco lo llena el
cuerpo.

| clase | qué hace |
|---|---|
| `PerfilCuerpo` | qué cuerpo, qué tono y qué ropa interior eligió el jugador |
| `PerfilesDeCuerpo` | dónde vive: attachment persistente y sincronizado |
| `CuerpoBaseTextures` | compone la textura del cuerpo, en layout de skin a **1×** |
| `SkinRegions` + `ComposedSkin` | le borran a la skin la segunda capa donde manda una prenda |

### El cuerpo va a 1×, no a 8×

La tela (medias, remera) va a 8× porque se sublima: una foto necesita
resolución. El cuerpo **nunca** recibe una foto ni un patrón — nada más fino
que un pixel de skin — así que dibujarlo a 8× serían 64 veces los texels para
pintar exactamente lo mismo. `CuerpoGeometria.ESCALA_CUERPO = 1`.

Que las dos superficies tengan escalas distintas no las desalinea: en
`ModelPart.Cuboid` la fracción de UV que ocupa una cara es
`(tamaño × S) / (64 × S)`, la misma para cualquier `S` — se cancela sola. El
cuerpo y la tela son dos cajas separadas con su propia textura; el borde de
una prenda lo define el alfa de SU textura, no un texel del cuerpo de abajo.

⚠️ **Ojo con lo que esto implica**: como la fracción de UV no depende de `S`,
`CuerpoGeometria.ESCALA_*` **no controla el detalle real de nada por sí
sola** — es solo la convención que usa el código que SÍ decide un tamaño real
de imagen (`CuerpoBaseTextures`, `ClothingTextureCache`). La geometría
renderiza igual de bien una textura de 64×64 que una de 512×512 atada al
mismo `Superficie`; el ahorro de memoria sale de encoger la imagen real, no
de re-declarar la geometría a otra escala. Ver "Tres resoluciones de tela" más
abajo, que es exactamente ese lugar.

De yapa: el arte del cuerpo se pinta como una skin común de 64×64 en
cualquier editor de skins, y una textura pasa de 1&nbsp;MB a 16&nbsp;KB.

**El default no le cambia el cuerpo a nadie**: `CuerpoBase.SKIN_REAL` con el
tono derivado de la propia skin del jugador, sampleado UNA vez con
`SkinToneSampler`. El set curado (plano, atlético, curvy, binder) son
alternativas, no un reemplazo.

Antes de esto, cada prenda **rellenaba su hueco con piel** ella misma. Eso
andaba con una prenda por parte del cuerpo y se rompía con dos: cada una
pintaba la pierna entera y el que dibujaba último hacía desaparecer al otro.

El arte del cuerpo (`textures/entity/cuerpo/*.png`) y el de la ropa interior
(`interior_*.png`) son **opcionales**: sin ellos sale un cuerpo liso sombreado
por cara, que es exactamente lo que producía la reconstrucción vieja. Se
enchufan soltando un PNG, igual que un patrón.

La regla del torso sigue viva, pero acotada: **con el cuerpo derivado de la
skin, la prenda manda de los hombros a la cintura y el pantalón de la cintura
para abajo.** Pintar el torso completo borraba la cintura del pantalón, que en
una skin va pintada en las últimas filas del TORSO y no en las piernas. Con un
cuerpo curado no aplica: ese cuerpo es dueño de su cintura.

### Tres resoluciones de tela, no una

Igual que el cuerpo, la tela de una prenda tampoco necesita siempre 8×. Solo
lo necesita si la sublimadora la va a estampar con una foto — un color plano
o un patrón de rayas no ganan nada con esa resolución, y con muchos jugadores
puestos a la vez en un server poblado esa es memoria de video que no hace
falta gastar (la composición es 100% del lado cliente: el servidor nunca toca
un píxel, así que esto no es carga de CPU del servidor, es VRAM de cada
cliente que ve jugadores puestos).

| nivel | escala | cuándo |
|---|---|---|
| `ESCALA_TELA_LISA` | 2× | sin patrón y sin foto |
| `ESCALA_TELA_PATRON` | 4× | con patrón, sin foto |
| `ESCALA_TELA` | 8× | con foto (la única que de verdad necesita esa resolución) |

**No se compone a menos resolución desde el principio.** `ClothingTextureCache`
sigue tiñendo, aplicando el patrón y sombreando por cara a la resolución
NATIVA de la textura base — tocar esa matemática por el ahorro arriesgaría el
sombreado y el recorte de la estampa (justo el caso que sí necesita 8×) por
nada. Lo que hace `reducirSiHaceFalta` es encoger la imagen YA compuesta, con
`NativeImage.resizeSubRectTo` (STB, no vecino-más-cercano: el degradé de
`faceFactor` sobrevive mezclado, no cortado en bandas).

Hoy esto vive solo en `ClothingTextureCache` (medias). **Remera queda
pendiente**: pasa por `EstampaTextures`, un compositor separado con su propia
matemática de píxeles (el lienzo virtual, las tablas de caras, el recorte del
full print) que `SUBLIMADORA.md` marca como frágil — se toca en una pasada
aparte, con más cuidado.

### La GUI de primera interacción todavía no está

El perfil se cambia por ahora con `/femclothes cuerpo|tono|interior|reset|ver`.
La GUI que se abre la primera vez que te ponés una prenda es lo que va arriba
de esto.

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

## El sistema de capas

Antes cada prenda se registraba como `TrinketRenderer` y se dibujaba sola, con
una textura opaca que era *tela + piel reconstruida en todo lo demás*. El
último que dibujaba tapaba al anterior **por completo** — no se veía feo,
desaparecía una de las dos prendas. Eso bloqueaba shorts, el maid y cualquier
layering.

Ahora hay **un solo punto de dibujo** para toda la ropa del mod,
`GarmentFeatureRenderer`, enganchado con
`LivingEntityFeatureRendererRegistrationCallback` (hook público de Fabric API,
sin Mixins). Por cada parte del cuerpo dibuja:

1. el **cuerpo base**, una vez, dilatado **0.30**;
2. las piezas de esa parte ordenadas por el ordinal `Capa`, todas a **0.32**.

**Toda la tela va a la MISMA dilatación.** Apilar 0.30 / 0.32 / 0.34 deja un
"anillo de árbol" en la silueta: se ve el canto de cada prenda alrededor de la
de abajo. Solo el cuerpo va un pelín adentro, para no asomar nunca por el
borde de una prenda.

| clase | qué es |
|---|---|
| `Parte` | cabeza, torso, y brazos/piernas por separado |
| `Capa` | el ordinal: cuerpo 0, interior 5, media 10, short 20, remera 25, pollera 30, ruedo 40, calzado 50 |
| `Garment` / `Garments` | qué items son prendas y qué partes gobiernan (lado servidor) |
| `Pieza` / `PiezasDePrenda` | cómo se ve cada una (lado cliente) |
| `CuerpoGeometria` | las cajas, fusión de los dos renderers viejos — dos `Superficie` (cuerpo a 1×, tela a 8×) |

Los ordinales **se comparan solo dentro de una misma `Parte`**: que la remera
sea 25 y el short 20 no significa nada, nunca comparten píxel.

⚠️ **El cuerpo base va donde la prenda MANDA, no donde tiene tela.** Las partes
salen de `Garment.partes()`, no de las piezas dibujadas: una musculosa no
dibuja nada en los brazos pero igual tiene que taparle las mangas pintadas de
la skin. Por lo mismo, **no declarar una parte hasta que haya tela ahí**:
declararla le borra al jugador la capa externa de la skin a cambio de nada.

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
4. **La GUI de primera interacción del cuerpo base.** Hoy el perfil se cambia
   solo por comando (`/femclothes ...`).
5. **Arte del cuerpo base**: `textures/entity/cuerpo/<cuerpo>.png` e
   `interior_<ropa>.png`. Sin ellos el cuerpo sale liso sombreado por cara,
   que es lo que había antes — o sea que no se perdió nada, pero es lo que
   hace que el set curado se distinga entre sí.
6. ~~**Migrar shorts**~~ — hecho, y creció a `pantalon`: Trinket, slot
   `piernas/exterior`, 5 largos + 3 tiros vía `PantalonLargo`/`PantalonTiro`,
   se dibuja en `Capa.PIERNA_EXTERIOR` (20) arriba de la media (`Capa.MEDIA`,
   10). Medias ganó su propio eje de largo (`MediasLargo`, 5 valores) con el
   mismo mecanismo. Sin patrón todavía en ninguna de las dos, e ícono único
   por prenda para todos los valores (§"El eje cobertura"). Con el layering
   probado, fishnet y socks_34 caen con el mismo mecanismo.
7. **Geometría Blockbench del buzo oversize** (Armor Model API). Hay un TODO
   en `FemclothesClient` marcando dónde va el registro.
8. **Probar el puente de 3D Skin Layers.**
9. **Colapsar `Estampa.Cara` en `region.Cara`.** Quedaron dos enums con el
   mismo nombre y casi el mismo contenido; unificarlos toca 8 archivos de la
   sublimadora (el selector físico y su NBT), y no era parte de la fase 1.
10. **Máscaras de patrón para remera** (`textures/models/armor/patterns/remera/*.png`).
    El código ya compone el patrón (`EstampaTextures.aplicarPatron`); sin el
    PNG cae a la prenda tenida lisa, igual que cualquier máscara faltante.
11. **Ícono de remera con patrón.** Necesita una segunda capa tintada por
    cada uno de los 36 modelos de corte — trabajo de arte, no de código.
12. **Migrar el patrón de remera del telar a la Estación de tintes**, cuando
    `MAQUINAS.md` deje de ser diseño. El dato (`RegionResolver`) y el render
    no cambian; solo `reformarRemera` se borra.
