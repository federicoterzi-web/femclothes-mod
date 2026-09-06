# Sistema de prendas — diseño

> **Estado: la FASE 1 está implementada** (§1, §3 y §7); §2 y §4–§6 siguen
> siendo diseño. Es la bajada de una charla de diseño (2026-09-06) para que se
> pueda retomar sin el historial. El mapa general está en
> [FEMCLOTHES.md](../FEMCLOTHES.md); las máquinas de confección en
> [MAQUINAS.md](MAQUINAS.md).

Objetivo: pasar de "ropa ceñida que no es armadura" a un guardarropa
personalizable de verdad —cortes, fit, prendas acampanadas— sin que el mod
se vuelva inmantenible ni la GUI se llene de sliders.

---

## 1. El bloqueo actual: dos prendas en la misma parte del cuerpo — ✅ RESUELTO

Hoy cada renderer (`BodyPartTrinketRenderer`, `RemeraTrinketRenderer`) dibuja
la ModelPart real del cuerpo con una textura que es **tela donde hay tela +
piel reconstruida en todo lo demás**, opaca, dilatada 0.3. El último que
dibuja tapa al anterior por completo.

Para una prenda por parte del cuerpo es perfecto: la media reconstruye el
muslo desnudo de arriba. Pero medias y shorts van los dos en las piernas, y
**el que se dibuje último hace desaparecer al otro**. Esto bloquea migrar
shorts, el traje de maid, y cualquier layering.

### Solución: capa de piel separada + orden de capa explícito — ✅ hecho

**Camino A del FEMCLOTHES.md, con la pieza que faltaba (el ordinal).**

Implementado como `GarmentFeatureRenderer` (un solo punto de dibujo para toda
la ropa), `Capa` (los ordinales), `Parte`, `Garment`/`Garments` (servidor) y
`Pieza`/`PiezasDePrenda` (cliente). El sustrato es el cuerpo base de §7.

- **Sustrato de piel reconstruida**: un solo renderer dibuja la piel desnuda
  reconstruida por parte de cuerpo, **una sola vez**, cuando hay cualquier
  prenda equipada ahí. Dilatación **0.30**. Neutraliza la ropa pintada de la
  skin y da la piel de base.
- **Cada prenda dibuja SOLO su tela** (alpha 0 en el resto), todas a la
  **misma dilatación (0.32)**. Donde una prenda tiene tela es opaca y gana;
  donde no, se ve la capa de abajo o el sustrato de piel.
- **Ordinal `layer` por prenda**: piel=0, medias=10, shorts=20, pollera=30,
  ruedo de remerón=40… Define quién va arriba cuando se solapan.

Ventajas: sin Mixins, incremental, escala a N prendas, y **de yapa arregla
el midriff** — una musculosa deja ver piel real abajo sin que cada prenda
rellene su propio hueco.

**Trampa:** apilar dilataciones distintas (0.30 / 0.32 / 0.34) deja un
"anillo de árbol" en la silueta. Por eso toda la tela va a la MISMA
dilatación; solo el sustrato de piel un pelín adentro (0.30) para que nunca
asome más allá de una prenda.

### ~~El torso se queda con ComposedSkin~~ — reemplazado

Esto era anterior al cuerpo base. Hoy el sustrato cubre TODAS las partes por
igual, y a `ComposedSkin` solo le quedó **borrar la segunda capa de la skin**
donde manda una prenda —que sigue siendo imprescindible por 3D Skin Layers—.
Ya no reconstruye piel en ningún lado.

La única herencia de la regla vieja: **con el cuerpo derivado de la skin real,
el torso se pinta hasta la cintura y no más abajo**, porque la cintura del
pantalón va pintada en las últimas filas del torso. Con un cuerpo curado no
aplica.

### El torso también es multi-capa — pendiente

La decisión vieja "`El slot de torso es de a una prenda`" es **anterior** al
sistema de capas — que es justo lo que la hace innecesaria. Con el ordinal,
el torso gana una segunda capa igual que las piernas:

- `torso/interior` → binder (faja), corpiño, musculosa de abajo. Teñible.
- `torso/exterior` → remera, top.

El **binder** hoy es solo una textura (banda compresora / top deportivo)
sobre el torso plano del modelo. Si algún día se agrega geometría de pecho,
gana un flag que la suprime — **eso NO es fase 1**.

### ~~La zona de piel reconstruida sigue al corte~~ — el problema se disolvió

Esto pedía que `SkinRegions` pasara a ser `f(prenda, variante)`, calculado
desde la máscara del corte, para que cualquier escote expusiera la piel
correcta. **Con el cuerpo base no hace falta nada de eso**: la prenda deja
transparente lo que no cubre y abajo está el cuerpo entero. El escote más
profundo del mundo no necesita una línea de código.

`SkinRegions` quedó como `f(partes cubiertas)`: qué rectángulos de la segunda
capa de la skin hay que borrar. Un croptop y un remerón borran lo mismo.

### Mapa de regiones y slots

| región | slots (capas) | prendas |
|---|---|---|
| cabeza | — (sin slot, es una pieza de la polera) | banda de polera |
| torso | `torso/interior`, `torso/exterior` | binder/corpiño · remera/top |
| brazos | `brazos` | mangas (incl. armwarmers, ver §5) |
| piernas | `piernas/media`, `piernas/exterior` | medias/fishnet/leggings · shorts/pollera/pantalón |
| pies | `pies` | calzado |

Dentro y entre slots de una región, el **ordinal `layer`** decide el orden
de dibujo.

---

## 2. El modelo: prenda = conjunto de piezas `(región, fuente, capa)`

Una prenda deja de estar atada a UN slot y a UNA parte del cuerpo. Es un
conjunto de piezas. El slot (`torso/prenda`, `socks/pair`…) sigue existiendo
para el inventario y la exclusividad, pero lo que se **renderiza** es el
conjunto de piezas.

### Fuentes de geometría

| fuente | qué es | anima |
|---|---|---|
| `BODY_PART` | ModelPart real dilatada 0.32, pintada (lo de hoy) | gratis |
| `BODY_PART` + dilatación alta (0.5–1.0) | "relajado": la tela despegada un poco del cuerpo | gratis |
| `FLARE_MESH(cintura, ruedo, largo, gajos)` | cono truncado / paneles radiales, colgado de un bone de ancla | manual (swing tipo capa) |
| `TUBE_MESH(arriba, abajo, largo)` | tubo cónico holgado, colgado de un bone de pierna. Es un `FLARE_MESH` invertido | manual |
| `OVERSIZE_MESH` | cubo ancho (el buzo) | gratis-ish |

**"Flojo o ajustado" NO es un slider** — es un switch entre fuentes. El
sistema `BODY_PART` se eligió porque es *automáticamente correcto* con
cualquier animación; todo lo flojo pierde eso y hay que animarlo a mano.

### Generalizar el renderer

`BodyPartTrinketRenderer` y `RemeraTrinketRenderer` hoy hacen casi lo mismo.
Fusionarlos en un `BodyPartGarmentRenderer(partes[], resolvedorDeTextura,
capa)`. La remera pasa a ser una config de eso. Hacerlo junto con la
migración de shorts, que ya toca esos archivos.

---

## 3. Selector de región — normalizar izq/der y frente/espalda — ✅ HECHO (salvo el widget)

Hoy "qué lado toco" aparece distinto en cada lugar: el telar cicla
"Ambas/Izq/Der", la sublimadora tiene un selector físico frente/espalda, los
componentes se llaman `right_dyed_color` (inglés) y `estampa_frente`
(español). Es **el mismo concepto**: selector de región objetivo.

### Dos ejes de región

- `Lado { IZQUIERDA, DERECHA, AMBAS }` — todo lo bilateral: piernas, brazos.
- `Cara { FRENTE, ESPALDA, AMBAS }` — torso de remera para estampas.

**IZQUIERDA = la del jugador** (anatómica), no la del que mira. La resolución
textura/modelo ya está verificada (`PlayerEntityModel.getTexturedModelData`,
ver FEMCLOTHES.md §Texturas). El widget y el enum coinciden con eso.

### La prenda declara sus regiones por operación

```java
Set<Region> regionesDe(Operacion op)   // op ∈ TEÑIR, PATRON, CORTE, ESTAMPAR
```

| prenda | op | regiones |
|---|---|---|
| `socks_solid` | TEÑIR / PATRON | {IZQ, DER, AMBAS} |
| `remera` | TEÑIR | {AMBAS} (una remera, un color base) |
| `remera` | ESTAMPAR | {FRENTE, ESPALDA} |
| `armwarmers` | cualquiera | {IZQ, DER, AMBAS} |

### Un solo resolvedor — ✅ hecho

`RegionResolver` — clase única, `(ItemStack, Region, componente) → valor`.
**Absorbió `ClothingStyle`**, que ya no existe. Un detalle que salió mejor de
lo diseñado: el guard `fijarDerecha()` (el ex `pinRight`) lo llaman **los
propios setters**, no el que llama. Un guard que hay que acordarse de invocar
vuelve a fallar tarde o temprano, y ese fue exactamente el bug original.

La regla en un solo lado:

- **`Lado`**: la IZQUIERDA es primaria (guarda el valor). La DERECHA guarda
  *overrides opcionales*; ausente = hereda. Es lo que hoy hace el mod con
  `RIGHT_*` en medias, generalizado a toda prenda bilateral. El guard tipo
  `pinRight()` queda como **el único** camino para tocar un lado sin
  arrastrar el otro.
- **`Cara`**: cada cara su componente (`estampa_frente` / `estampa_espalda`).

### Un widget + el selector físico — PENDIENTE

El widget no está: el telar sigue con su botón que cicla, pero ahora cicla
`Lado` y no un enum propio. `Estampa.Cara` tampoco se colapsó todavía contra
`region.Cara` — son dos enums con el mismo nombre, y unificarlos toca 8
archivos de la sublimadora (el selector físico y su NBT).

- Estaciones con GUI (tinturas, sastrería): `RegionPickerWidget` — toma el
  `Set<Region>` válido y muestra exactamente esos botones.
- Sublimadora (sin GUI): el selector físico del modelo maneja el **mismo**
  valor de `Cara`.
- El valor vive en el **config NBT de la estación** (default AMBAS).
  Configurás una vez, metés prendas, se aplican a la región activa.

### `AMBAS` NO es uniforme — trampa

- **Teñir / patrón**: setea el valor primario y **borra el override** del
  otro lado → quedan iguales sin guardar data extra (el caso "parejo").
- **Estampar**: aplica a todas las caras pero **cuesta tinta por cara** (la
  sublimadora ya es así — "una pasada, dos juegos de tinta").

### Alternar la lateralidad

- **Espejar (swap izq↔der) de una capa**: acción de la estación. Intercambia
  el valor primario y el override de esa capa. Si la derecha heredaba,
  después hereda la izquierda. Un click.
- **Auto-espejo al aplicar**: un patrón asimétrico aplicado al otro lado se
  espeja para que "externo siga siendo externo". El proyector body-space
  (§6) lo hace solo; con PNGs por prenda, cada patrón lleva flag
  `simétrico` / `con lateralidad`.

### Rotar la prenda entera — `orientacion` — ✅ el dato y la resolución, hechos

Terminó siendo un **componente propio** (`femclothes:orientacion`) y no un
campo de `variante`: `variante` es de la remera y esto vale para cualquier
prenda. Se borra al volver a la normal, para que una prenda sin girar siga
apilando con otra igual.

Falta lo que lo usa: espejar la máscara al renderizar, y la acción de estación.

Campo chico: `{ girado: bool, espejado: bool }`. **No destructivo.**

- `girado` → frente↔espalda (remera al revés: estampa y escote pasan atrás)
- `espejado` → izq↔der para **toda** la prenda de una

El `RegionResolver` lo aplica **al resolver**, no permuta datos guardados: si
`girado`, cuando el render pide `FRENTE` devuelve `espalda`; si `espejado`,
pide `IZQUIERDA` y da `der` + espeja la máscara. Vale para render, ícono y
tooltip. Se saca el flag y vuelve a la normalidad.

La **geometría no rota** — una remera es casi simétrica frente/espalda, así
que "al revés" es remapear la superficie, no la malla. Prendas asimétricas
de forma (buzo con capucha, vestido con cola) declaran
`puedeGirarse() = false`.

### Lo que habilita

Cortes **asimétricos** salen gratis: manga 3/4 en un brazo y larga en el
otro, flare distinto por pierna. `femclothes:variante` pasaría a tener split
por `Lado` como ya lo tienen las medias (mismos componentes opcionales,
mismo `RegionResolver`).

---

## 4. Los cortes de remera — cómo construirlos de verdad

El data-model de los 3 ejes ya existe (36 combos, `femclothes:variante`).
Falta que las variantes se **vean** bien; hoy son todas generadas por script.

### Mangas — `sin / cortas / 3-4 / largas`

El más simple. Puro texture: qué filas del brazo tienen tela (0 / 4 / 8 / 12
de las 12). Layout de skin.

- El **borde** de la manga necesita una fila de dobladillo/sombra + una de
  highlight, o queda cortado con tijera.
- 3/4 vs larga son 4 texels. Se distingue bien a 8× (512px), era ilegible a
  2× — de ahí la subida de resolución.
- Candidato a resolver primero. Se puede dejar generado por script si el
  script pinta el dobladillo.

### Cuello — `redondo / V / escote / polera`

Textura, pero es una **forma** (máscara de silueta) en las filas de arriba
del torso frontal, no un conteo. Una textura base "torso con tela hasta el
cuello" + la variante como máscara de recorte. Mismo mecanismo que los
patrones (`textures/models/armor/patterns/`), pero recorta en vez de teñir.

| valor | forma | filas que recorta |
|---|---|---|
| redondo | arco cerca de la base del cuello | ~2 |
| V | dos diagonales al centro | ~4 |
| escote | scoop / corazón / plunge — más profundo | 5–8, decisión de diseño |
| polera | sin escote + banda en la cabeza (ver abajo) | 0 |

- La espalda es siempre redonda. Todo lo demás es solo adelante.
- La piel que queda a la vista abajo del recorte la resuelve **el sustrato
  de piel / `SkinRegions` calculado desde esta máscara** (§1) — no hay
  código nuevo por cada profundidad de escote.

### Polera (turtleneck) — la excepción de geometría

Acá se rompe "pegada al cuerpo con la ModelPart del torso": la tela **sube
por el cuello**, y eso no es el torso.

- **Opción elegida**: usar la ModelPart de la **cabeza**, pintando tela solo
  en la banda inferior (~2 filas de abajo del cubo, todo alrededor),
  dilatada. Barato, encaja en el renderer si acepta "cabeza" como parte.
- Alternativa (más laburo): cilindro de cuello propio, enganchado al head
  bone.

**Polera = (torso sin escote) + (banda en la cabeza).** Dos piezas. Es el
**primer caso real de prenda multi-parte** y sirve de ensayo para el maid.

### Largo — `crop / normal / largo`

- crop y normal: filas del torso, puro texture.
- **`largo`**: 12/12 filas del torso. La decisión es si además toca la
  cadera.

**El problema del ruedo que cuelga:** si pintás el largo extra en la
ModelPart de la pierna superior, esa tela se mueve **con cada pierna por
separado** → caminando el ruedo se abre en dos como un culotte, no cuelga.
Un ruedo que cuelga de verdad pende del **torso** y es geometría propia (el
problema de la pollera).

Entonces:

- **B1 — remerón ceñido** (elegido para el eje `largo`): tela pintada en las
  ~4 filas de arriba del muslo, pegada, se mueve con la pierna porque *está*
  pegada. Va con la tesis del mod. Ruedo con curva leve (más largo a los
  costados) + dobladillo, no un corte recto.
- **B2 — ruedo suelto / vestido**: geometría acampanada colgada del bone del
  torso. **NO es un valor del eje `largo`** — es una prenda aparte, en el
  bucket de excepciones junto al buzo.

Con B1, `largo` confirma que **una prenda dibuja partes fuera de su slot**
(slot `torso/prenda`, renderiza torso + piernas superiores, capa arriba de
los shorts). Es la generalización del punto 2. Requiere el sistema de capas
(punto 1) para no pisar shorts.

---

## 5. Prendas nuevas

### Regla: dos excepciones explícitas a "pegada al cuerpo"

1. **Ancho** — el buzo oversize. `OVERSIZE_MESH`.
2. **Acampanado** — pollera, vestido, campana de pantalón. `FLARE_MESH`. Una
   pollera no puede ser la ModelPart de la pierna: vuela.

Documentar esto como **regla de diseño**, no como deuda técnica.

### Pollera — recta Y con vuelo son la misma malla

`FLARE_MESH(cintura, ruedo, largo, gajos)`:

- `ruedo ≈ cintura` → tubo → **pollera lápiz**
- `ruedo` un poco mayor → **A-line**
- `ruedo >> cintura` → cono → **pollera circular**

Un generador, el molde `flare` escala `ruedo` de 0 a 1.

**El comportamiento sí cambia:**

| | swing | clipping de piernas |
|---|---|---|
| circular | gajos rotan con la velocidad, se abre al girar | ninguno — la malla es tan ancha que la pierna nunca la alcanza |
| A-line | moderado | poco |
| lápiz | gajos casi trabados, cuelga rígido | **severo si es larga** |

**Cómo esquivar el clipping:** la malla **no abraza ninguna pierna**. Se
ancla a un **hueso sintético de pelvis** (punto medio de las dos piernas, a
la altura de la cintura), cuelga desde ahí, las piernas se mueven adentro.
Correcto para la circular; para la lápiz queda un toque "barril" (no marca
el muslo) pero **nunca clippea**. Trade-off estándar.

**La lápiz corta no necesita malla:** una minifalda tubo ajustada *es un
short sin entrepierna* → `BODY_PART` en las piernas superiores, textura de
tubo, sin partir el inseam. Clippea un poco al abrir mucho el paso, igual
que los shorts.

**El renderer switchea solo:**
- `flare = 0` **y** `largo` corto → `BODY_PART` (lápiz ceñida de verdad)
- `flare > 0` **o** `largo` medio/largo → `FLARE_MESH` anclada a pelvis

El jugador nunca ve la costura, solo mueve el molde.

### Pantalones acampanados

- muslo → `BODY_PART` (ajustado)
- pantorrilla → `FLARE_MESH` chiquito anclado abajo de la rodilla (la campana)

### Babuchas (harem)

- muslo → `TUBE_MESH` (holgado, ancho arriba)
- tobillo → `BODY_PART` (el puño ajustado)

Dos piezas.

### Botamanga = eje de corte del pantalón, NO un slot

El puño del pantalón (doblado / elástico / abierto / caído) es un **eje** de
la familia pantalón, guardado en `variante`. No es una prenda ni un slot
aparte.

### Mangas / armwarmers — un garment, un eje de banda

`arms/armwarmer` pasa a ser el slot `brazos`. El garment **`mangas`** cubre
una **banda** del brazo definida por dos filas (0 = muñeca … 12 = hombro):

| preset | banda |
|---|---|
| manga larga | 12→0 |
| manga 3/4 | 12→4 |
| manga corta | 12→8 |
| armwarmer | 8→0 |
| armwarmer largo | 11→0 |

**Armwarmers no es un tipo de prenda** — es este preset. El render se
comparte con la manga de la remera (misma tela sobre la ModelPart del
brazo). Neto: menos piezas que hoy (armwarmers tiene renderer + slot +
lógica propios que se colapsan acá).

### Calzado — slot nuevo `pies`

Región nueva. El modelo vanilla no tiene "pie" separado (es la base del cubo
de la pierna inferior), así que:
- zapatilla ceñida / media-zapato → `BODY_PART`, las ~3 filas de abajo de la
  pierna inferior + una suela chica de geometría debajo.
- botas / plataformas / cualquier calzado con forma → malla propia
  (`OVERSIZE_MESH` chico), es la excepción de "ancho".

### La malla se genera, no se dibuja

Igual que los 36 cortes: un generador paramétrico a partir de ~5 números
(radio cintura, radio ruedo, largo, gajos, ángulo de flare) escupe el
modelo. Encaja con el ADN del proyecto (todo generado por script) y hace que
"20% más de vuelo" sea un número, no un Blockbench nuevo.

**Los gajos radiales permiten swing barato:** cada gajo rota según la
velocidad del jugador, como una capa. Swing proporcional a `flare` — la
lápiz casi no se mueve, la circular se abre.

---

## 6. Personalizable sin explotar en combos

### `femclothes:variante` agnóstico de prenda

Hoy `variante` es `{largo, manga, cuello}` hardcodeado para remera. Cada
familia tiene sus ejes:

| familia | ejes |
|---|---|
| remera | largo · manga · cuello |
| medias | largo (tobillo/media/rodilla/muslo) · puño |
| pollera | flare · largo |
| pantalón | fit · largo (short/capri/full/campana) · tiro · botamanga |
| mangas | banda |
| calzado | tipo · caña |

`variante` pasa a ser un `Map<Eje, Valor>`. Cada prenda declara
`List<Eje> ejes()` (nombre, valores ordenados, qué región/geometría afecta).

- **Moldes de eje** = llaves **universales**. El molde `manga` desbloquea el
  eje en *cualquier* prenda que lo tenga.
- **Molde de corte** = spec **parcial**. Al aplicarlo, la prenda toma los
  ejes que entiende e ignora el resto. Un molde "manga larga + cuello V" de
  una remera, aplicado a un vestido de maid, le setea manga y cuello si el
  maid los tiene.
- **Migración**: `variante` genérico tiene que leer el formato viejo
  (remeras en mundos guardados con `{largo, manga, cuello}`). Decidir el
  formato ANTES de implementar la fase de cortes.

Los ejes **NO son ítems**. Se cambian en la **mesa de sastrería** (ver
MAQUINAS.md). El molde de corte es un ítem físico (se comparte, se vende),
fabricado **una vez a mano** desde el config — no es "un ítem por combo".

### Patrones de tinte — no autorar N prendas × M patrones

1. **Agrupar prendas por topología de región**, no por prenda:
   - torso: remera, croptop, musculosa, polera, maid-top
   - pierna: medias, fishnet, leggings, pantalón ajustado, minifalda tubo
   - brazo: mangas
   - malla acampanada: pollera con vuelo, campana, babucha (UV de cono
     desplegado, set propio)

   Los patrones se autoran **por grupo**. De N×M a ~4×M.

2. **Largo plazo — patrones en espacio del cuerpo + proyector.** Un patrón
   deja de ser un PNG y pasa a ser una spec ("rayas horizontales cada 20%",
   "banda vertical centrada"). Un script la proyecta al UV de cualquier
   prenda y genera la máscara. **Rotar sale gratis** (rotás en body-space),
   **prenda nueva sale gratis**. El path `<prenda>/<patrón>.png` queda como
   salida.

### Combinar patrones

`pattern_id` / `pattern_color` pasan a ser una **lista corta** (tope 3-4
capas, no 6 como banderas), compositadas en orden con su color. De los
primitivos que ya hay (rayas, raya vertical, tres rayas) salen varsity,
tartán, etc.

- **Ícono**: el truco de 2 capas no estira solo → runtime-composed (reusa el
  compositor de estampas) o tope fijo de 4 con un tintindex por capa.
- **UI**: aplicar patrón = agrega capa. Shift-aplicar = saca la última.
  Prenda sola = las saca todas.
- Encamina al pipeline unificado `base → patrón(es) → estampa → sombreado`,
  donde la estampa es otra capa más de la lista.

**Rotar máscaras: NO.** Van pegadas al UV del cuerpo — rotar el PNG rompe en
las costuras del cubo. Las orientaciones se hacen como máscaras separadas
(o salen del proyector body-space).

---

## 7. Cuerpo base — reemplaza la reconstrucción de piel — ✅ HECHO (salvo la GUI y el arte)

Hoy `SkinToneSampler` / `ComposedSkin` / `SkinRegions` reconstruyen la piel
desnuda leyendo la skin real, con toda la lista de "trampas que costaron
tiempo" de FEMCLOTHES.md. Un **cuerpo base elegido a mano** borra eso.

### Qué es

- **Set curado** de texturas en layout de skin: build plano, con abdominales,
  con curvas, pecho aplanado (binder), etc. Set fijo, **no por prenda**.
  Implementado a **1×** (una skin común de 64×64) y no a 8× como la tela: el
  cuerpo nunca recibe una foto ni un patrón, así que no necesita más
  resolución que un pixel de skin. Las dos escalas conviven sin desalinearse
  — ver "El cuerpo va a 1×" en FEMCLOTHES.md.
- Trae **ropa interior básica** baked, o engancha con los slots
  `torso/interior` + `piernas/interior` (§1) — viene un default, se cambia
  (slip / boxer / bralette). El censurado es el fallback si no elegís nada.
- **Tono paramétrico**: textura en tono neutro + recolor por rampa (sombras
  / medios / luces). El default se **deriva sampleando la skin real UNA
  vez** en el setup; después se edita a mano.

### Reemplaza el sustrato de fase 1

El sustrato de §1 deja de "reconstruir" y **ES** el cuerpo base elegido. UV
conocido, regiones desnudas conocidas. `SkinRegions` pasa a ser "qué parte
del cuerpo base asoma por esta prenda" — trivial. **Es parte de fase 1.**

### Primera interacción — PENDIENTE

La primera vez que crafteás/equipás una prenda del mod, se abre una GUI:
elegís cuerpo base, tono, ropa interior, preview.

**Lo que sí está**: el perfil (`PerfilCuerpo`) como attachment persistente,
sincronizado a **todos** los clientes y no solo al dueño —los demás también
tienen que dibujarte el mismo cuerpo—, con `copyOnDeath`. Mientras no haya
GUI se cambia con `/femclothes cuerpo|tono|interior|reset|ver`.

**Lo que falta además de la GUI**: el arte. `textures/entity/cuerpo/<id>.png`
(mapa de sombras, se multiplica) e `interior_<ropa>.png` (se compone encima)
son los dos **opcionales**; sin ellos sale el cuerpo liso sombreado por cara,
que es lo que ya producía la reconstrucción vieja. O sea que hoy los cinco
cuerpos del set se ven iguales — el sistema anda, el arte es lo que los
distingue.

### No romper la filosofía

**"Usar mi skin real"** queda como una de las opciones del cuerpo base. El
set curado son alternativas, no un reemplazo. El default sigue siendo
"derivado de tu skin".

---

## 8. 3D Skin Layers — las prendas también son 3D

Con 3DSL prendido el jugador se ve voxelado y una prenda dibujada plana
sobre la ModelPart queda como panqueque al lado. La respuesta **parte por
fuente de geometría** (§2).

### Ajustadas (`BODY_PART`) → que las extruda 3DSL

En vez de dibujar la tela plana, se **compone la tela DENTRO de la capa
externa de la skin** y 3DSL la extruye sola, en su estilo exacto.

- **3DSL presente**: `ComposedSkin` extendido — pinta la tela de la prenda
  (no solo piel desnuda) en la capa externa, en torso Y piernas Y brazos.
  `SkinRegions` pasa de *borrar* la ropa pintada de la skin a *reemplazarla*
  con la tela de la prenda.
- **3DSL ausente**: el cuerpo base plano/dilatado como fallback (§7).

### Flojas (`FLARE_MESH`, `OVERSIZE_MESH`) → ya son 3D

Pollera, campana, buzo ya tienen volumen. Solo hay que **estilarlas blocky**
(facetas duras) para que peguen con el look de 3DSL. No necesitan 3DSL.

### Lo que esto implica

- 3DSL deja de ser "puente de compat" y pasa a ser **camino de render de
  primera clase**. `SkinLayersCompat` / `HttpTextureAccessor` (escrito,
  sin probar) se vuelve **crítico**.
- **Costo**: cada cambio de prenda/tinte/corte → recompone la skin
  (NativeImage, por jugador) + 3DSL rehace su mesh. **Async / con debounce**,
  sí o sí.
- **Fase 1 tiene que anticiparlo**: el sustrato/cuerpo base debe diseñarse
  de forma que la ruta "componer en la capa externa + 3DSL extruye" no
  obligue a romperlo después.

---

## 9. Roadmap (dos tracks paralelos después de la fase 1)

**Fase 1 — Sistema de capas + cuerpo base. ✅ HECHA.** Sustrato = cuerpo base
elegido + ordinal de capa + `RegionResolver`. `SkinRegions` no terminó siendo
paramétrico por corte: con el cuerpo base ese problema se disolvió (ver §1).
El punto 3 de acá abajo se adelantó, porque el ordinal exige un único punto de
dibujo. Quedan afuera de la fase 1, a propósito: la **GUI de primera
interacción**, el **arte del cuerpo base** y el **`RegionPickerWidget`**.

Después, dos tracks independientes:

### Track prendas
2. Migrar shorts al sistema nuevo (prueba el layering). Después fishnet y
   socks_34 caen solos. **Ya no está bloqueado.**
3. ~~Fusionar `RemeraTrinketRenderer` + `BodyPartTrinketRenderer`~~ — hecho en
   la fase 1 (`CuerpoGeometria` + `GarmentFeatureRenderer`).
4. Mangas (banda) con dobladillo. Cuello redondo/V/escote como máscaras de
   silueta.
5. Polera como prenda de dos piezas (torso-sin-escote + banda-en-cabeza) —
   ensayo de multi-parte.
6. Backend `FLARE_MESH` + generador paramétrico. Probar con **solo la
   pollera** hasta que swing y clipping estén bien.
7. El resto de lo acampanado sale de los moldes (pantalón campana, babucha).
   Maid como prenda multi-parte. Calzado.
8. Buzo oversize (geometría Blockbench, Armor Model API).
9. Puente 3DSL de verdad (§8) — probar en juego con el mod del server.

### Track máquinas
Ver [MAQUINAS.md](MAQUINAS.md). No depende de la fase 1 — una mesa solo
setea componentes.

### Arte
Paralelo a todo. Hoy es 100% generado por script. El **set de cuerpos base**
(§7) es arte fijo, una vez, no por prenda.
