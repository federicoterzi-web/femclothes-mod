# Sistema de prendas — diseño

> **Estado: DISEÑO, nada de esto está implementado.** Es la bajada de una
> charla de diseño (2026-09-06) para que se pueda retomar sin el historial.
> El mapa general está en [FEMCLOTHES.md](../FEMCLOTHES.md); las máquinas de
> confección en [MAQUINAS.md](MAQUINAS.md).

Objetivo: pasar de "ropa ceñida que no es armadura" a un guardarropa
personalizable de verdad —cortes, fit, prendas acampanadas— sin que el mod
se vuelva inmantenible ni la GUI se llene de sliders.

---

## 1. El bloqueo actual: dos prendas en la misma parte del cuerpo

Hoy cada renderer (`BodyPartTrinketRenderer`, `RemeraTrinketRenderer`) dibuja
la ModelPart real del cuerpo con una textura que es **tela donde hay tela +
piel reconstruida en todo lo demás**, opaca, dilatada 0.3. El último que
dibuja tapa al anterior por completo.

Para una prenda por parte del cuerpo es perfecto: la media reconstruye el
muslo desnudo de arriba. Pero medias y shorts van los dos en las piernas, y
**el que se dibuje último hace desaparecer al otro**. Esto bloquea migrar
shorts, el traje de maid, y cualquier layering.

### Solución: capa de piel separada + orden de capa explícito

**Camino A del FEMCLOTHES.md, con la pieza que faltaba (el ordinal).**

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

### El torso se queda con ComposedSkin

`ComposedSkin` ya reconstruye el torso sobre la skin misma y lo necesita 3D
Skin Layers (ver FEMCLOTHES.md). Piernas y brazos usan el sustrato separado.
Si algún día 3DSL tiene que respetar las piernas, ahí se unifica todo en
ComposedSkin.

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

## 3. Selector de región — normalizar izq/der y frente/espalda

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

### Un solo resolvedor

`RegionResolver` — clase única, `(ItemStack, Region, componente) → valor`.
**Absorbe `ClothingStyle`.** La regla en un solo lado:

- **`Lado`**: la IZQUIERDA es primaria (guarda el valor). La DERECHA guarda
  *overrides opcionales*; ausente = hereda. Es lo que hoy hace el mod con
  `RIGHT_*` en medias, generalizado a toda prenda bilateral. El guard tipo
  `pinRight()` queda como **el único** camino para tocar un lado sin
  arrastrar el otro.
- **`Cara`**: cada cara su componente (`estampa_frente` / `estampa_espalda`).

### Un widget + el selector físico

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

### Cuello redondo vs en V

Textura, pero es una **forma** en las ~3-4 filas de arriba del torso
frontal, no un conteo.

- **Redondo**: un arco cerca de la base del cuello.
- **En V**: dos diagonales al centro. Cuánto baja es decisión de diseño.
- Comparten todo menos esas filas → una textura base "torso con tela hasta
  el cuello" + las variantes de cuello como **máscara de silueta**. Mismo
  mecanismo que los patrones (`textures/models/armor/patterns/`), pero de
  recorte en vez de color.
- La espalda del cuello es igual en las dos (redonda). La V es solo adelante.

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

Los ejes **NO son ítems**. Van en un componente estilo `variante`. Se cambian
en la **mesa de sastrería** (ver MAQUINAS.md).

- **Molde por eje** (`fit`, `flare`, `largo`, `manga`, `cuello`…): son las
  llaves de progresión. Sin el molde, ese eje aparece gris en la GUI.
- **Molde de corte** = combo completo guardado, un ítem físico (se comparte,
  se vende). Se fabrica **una vez a mano** desde el config; no es "un ítem
  por combo crafteado".

36 cortes × 16 colores × flare × largo × estampas = miles de combinaciones,
un solo ítem con datos encima.

---

## 7. Roadmap (dos tracks paralelos después de la fase 1)

**Fase 1 — Sistema de capas.** Sustrato de piel + ordinal. Es el desbloqueo
de todo lo demás. Sin dependencias.

Después, dos tracks independientes:

### Track prendas
2. Migrar shorts al sistema nuevo (prueba el layering). Después fishnet y
   socks_34 caen solos.
3. Fusionar `RemeraTrinketRenderer` + `BodyPartTrinketRenderer` en
   `BodyPartGarmentRenderer`.
4. Mangas con dobladillo (script). Cuello redondo/V como máscaras de
   silueta.
5. Polera como prenda de dos piezas (torso-sin-escote + banda-en-cabeza) —
   ensayo de multi-parte.
6. Backend `FLARE_MESH` + generador paramétrico. Probar con **solo la
   pollera** hasta que swing y clipping estén bien.
7. El resto de lo acampanado sale de los moldes (pantalón campana, babucha).
   Maid como prenda multi-parte.
8. Buzo oversize (geometría Blockbench, Armor Model API). Puente 3DSL.

### Track máquinas
Ver [MAQUINAS.md](MAQUINAS.md). No depende de la fase 1 — una mesa solo
setea componentes.

### Arte
Paralelo a todo. Hoy es 100% generado por script.
