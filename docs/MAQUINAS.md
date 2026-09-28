# Máquinas de confección — diseño

> **Estado: DISEÑO, nada de esto está implementado.** Bajada de una charla de
> diseño (2026-09-06). El sistema de prendas está en [PRENDAS.md](PRENDAS.md);
> la sublimadora ya existente en [SUBLIMADORA.md](SUBLIMADORA.md).

Reemplaza el hook del telar (`UseBlockCallback`, decisión #4 de
FEMCLOTHES.md) por bloques propios. El telar vuelve a ser solo banderas.

---

## 1. Por qué bloques propios y no el telar

El hook del telar funciona pero es un parche: intercepta el click derecho,
tiene la rama frágil "¿esto es prenda o bandera?", y va a morder cuando
Minecraft toque `LoomScreenHandler` o la animación de la UI de banderas.

*Reformar una prenda* no duplica nada vanilla —no existe "recortá esta
prenda" en el juego— así que un bloque de sastrería no duplica un bloque
poco usado como pasaba con el telar/tinte.

Y con los cortes por explotar (fit, flare, largo + params de pollera), un
telar hackeado con moldes que ciclan de a uno a ciegas no es buen hogar.

---

## 2. Los bloques

| Bloque | Qué hace | Estado |
|---|---|---|
| **Mesa de tinturas** | superficie: color (tanque 16) + patrón (biblioteca) | nuevo |
| **Mesa de sastrería** | forma: corte, fit, flare, largo, manga, cuello | nuevo |
| **Sublimadora** | fotos: prensa CMYK, ceremonia de 20s | ya existe |

Más la **mesa de crafteo vanilla** (define la prenda + color base).

Se eligió **modular** (3 bloques en línea) sobre una mesa de confección
fusionada, porque con automatización (sección 5) la pinta de cadena de
montaje —cada bloque una transformación— tiene sentido que sin automatización
no tenía.

### Por qué no CMYK para el tinte

El mezclador CMYK da color libre pero pesa: sliders (justo lo que el mod
evita — los 3 presets de la sublimadora existen *porque* no quisieron
controles finos) o una rueda de color (widget pesado), y no matchea el
crafteo. Además el color arbitrario **ya está cubierto**: Camerapture estampa
cualquier imagen, un full print de un rectángulo teal te da ese teal.

Si en algún momento se quieren más de 16: mezcla **estilo armadura de cuero**
(tirás 2-3 tintes, se promedian). Mecánica 100% vanilla, sin picker.

---

## 3. Interacción: GUI rara, aplicación instantánea

Esto resuelve la tensión "cuánta GUI". La GUI se usa **poco** (configurás el
bloque una vez); la acción común es **instantánea**.

| gesto | qué hace |
|---|---|
| click **derecho con prenda** | aplica el config activo del bloque a la prenda |
| click **derecho mano vacía** | abre la GUI de config |
| **shift + derecho** | cicla el preset / color activo sin abrir nada |

**No** click izquierdo — interceptar el ataque en un bloque es frágil y un
misclick se siente mal.

El bloque **canta su config** en el modelo, para distinguir una fila de
mesas de un vistazo:
- **mesa de tinturas**: un parche del color activo pintado (tintindex, como
  el cuero).
- **mesa de sastrería**: una prendita plegada chiquita arriba mostrando el
  corte activo.

El config es **estado del block entity (NBT)** → cada bloque colocado queda
fijo a un look. ¿Dos looks? Dos bloques. Esto habilita filas de estaciones
pre-configuradas.

---

## 4. Contenido de cada mesa

### Mesa de tinturas

- **Tanque de color**: 16 slots, uno por `DyeColor`. Tirás lana/tinte de ese
  color → carga (hasta N usos). Recarga por tolva (auto-sort de tintes al
  tanque que corresponde).
- **Biblioteca de patrones**: aprende ítems de patrón (`ClothingPatternItem`).
  Se los pasás una vez, el bloque los recuerda para siempre (como el telar no
  consume banderas).
- **GUI**: elegís color activo (paleta de 16) + patrón activo.
- Aplicar consume una dosis del color activo; setea `minecraft:dyed_color` +
  `femclothes:pattern_id` / `pattern_color`.

### Mesa de sastrería

Dos zonas en la GUI:

1. **Slots de moldes de eje** (`manga`, `cuello`, `flare`, `largo`, `fit`…):
   son las llaves. Desbloquean qué ejes podés tocar. Sin el molde, el eje
   aparece gris.
2. **Grilla tipo cofre** (3-5 slots, sin scroll) de **moldes de corte
   guardados**.

Flujo: en la GUI armás un combo usando los ejes desbloqueados → botón
**guardar** → sale un **molde de corte** (ítem físico, como un patrón de
estandarte, encodea el combo entero). Lo metés en la grilla, elegís el
activo, click a las prendas.

Molde de corte físico (vs solo NBT del bloque): **se comparte y se vende**
("te paso el molde del cuello en V crop"), cualquier mesa lee cualquier
molde, y **no es "un ítem por combo crafteado"** (se fabrica una vez a mano
desde el config).

Aplicar setea `femclothes:variante`. No consume material (o aguja+hilo si se
quiere un sink blando).

### Categorías de patrones de modelado — la arquitectura definitiva

> Bajada de una charla de diseño (2026-09-08), después de una sesión larga
> de calientabrazos que terminó confundiendo cobertura y tiro entre sí (ver
> `docs/CANAL.md` v14-v15). Esto es la referencia que resuelve esa
> confusión — **antes de tocar el eje tiro/cobertura de nuevo, leer esto
> primero**, no la bitácora de la sesión anterior.

Los patrones de modelado **no son exclusivos de una prenda**. La idea es
que existan categorías reutilizables entre prendas — hoy hay cuatro
prendas principales (remera, pantalón, medias, cubrebrazos/mangas
independientes), y la filosofía es evitar un patrón por combinación de
prenda×eje (nada de "patrón de manga corta" + "patrón de media corta" +
"patrón de pantalón corto" por separado): un solo patrón abstracto de
**Cobertura**, reutilizable, cuya interpretación depende de la región
corporal y de un **Anclaje** que configura la mesa, no el patrón.

#### Cobertura

Cuánto cubre una prenda de una región corporal. Reutilizable entre
remera (mangas), cubrebrazos, pantalón, medias. Niveles (0 a 5):

| nivel | significado |
|---|---|
| 0 | sin cobertura |
| 1 | corto |
| 2 | hasta articulación / medio |
| 3 | tres cuartos |
| 4 | siete octavos |
| 5 | completo |

**La diferencia entre prendas NO está en el patrón — está en el Anclaje.**
La Mesa de Modelado tiene una configuración de **Anclaje de Cobertura**,
que decide desde qué punta de la región corporal arranca la cobertura y
hacia dónde crece:

| anclaje | arranca en | crece hacia |
|---|---|---|
| **Superior** | arriba de la región | abajo |
| **Inferior** | abajo de la región | arriba |

| prenda | región | anclaje |
|---|---|---|
| Manga de remera | hombro → muñeca | Superior |
| Cubrebrazos | muñeca → hombro | Inferior |
| Pantalón / calza | cintura-cadera → tobillo | Superior |
| Media | tobillo → pierna | Inferior |

**El Anclaje NO es un patrón independiente** (no es un ítem que el
jugador elige) — es una configuración técnica fija de la mesa por
prenda/región, igual que hoy `Variante.Manga` (remera, ancla arriba) y
`MediasLargo`/`PantalonLargo`/`CalientabrazosItem.cobertura` (ancla
abajo/arriba según corresponda) son el mismo concepto de "cobertura" con
direcciones fijas distintas por código, no por elección del jugador.

> **Actualizado 2026-09-15 — YA IMPLEMENTADO, esta nota queda como
> historial.** `docs/PRODUCCION_TEXTIL.md` pedía lo contrario a lo de
> arriba — el jugador SÍ elige el anclaje en la Mesa de Modelado — y ese
> punto ganó. Terminó siendo más que un selector Superior/Inferior: cada
> eje de extremidad (pantalón-pierna, medias, calientabrazos/manga) tiene
> **DOS anclajes simultáneos** (superior e inferior) que se INTERSECAN —
> ver `ModeladoBlockEntity.Anclaje`, `PantalonItem#filasVisibles`/
> `MediasLargo#filasVisibles`/`CalientabrazosItem#filasVisibles`, y el
> recorte en runtime de `PiezasDelMod#recortarFilas`. El render por
> anclaje opuesto (lo que esta nota decía que faltaba) se resolvió
> recortando en runtime el archivo de cobertura COMPLETA en vez de
> pre-generar un PNG por combinación — no hicieron falta "más filas de
> tela distintas por dirección" como se especulaba acá.
>
> La Mesa de Modelado (`com.femclothes.modelado`) tiene: un botón de
> **Categoría** que cicla entre las 4 prendas de extremidad + remera
> (reemplaza "el molde define el eje" por "el botón define el eje, el
> molde aporta el valor"), botones de **Anclaje** y **Lateralidad**
> (izquierda/derecha/ambas, mismo patrón bilateral que el color), un
> **Molde de Rango** unificado de 5 valores (Mínimo/Corto/Medio/Largo/
> Máximo) que sirve para las 4 categorías de extremidad a la vez
> traduciéndose a la escala real de cada una, un visor 3D en vivo del
> jugador vistiendo la prenda en construcción, y storage partido en
> compartido (27) + por-categoría (12, cicla con el mismo botón). Detalle
> completo de la implementación y de los bugs reales que costó encontrar
> en el camino: `docs/CANAL.md` v17.

#### Tiro

Altura de una prenda INFERIOR respecto de la cintura/cadera. Niveles:
Alto, Medio, Bajo.

**Aplica solo a pantalón y calza (futuras prendas inferiores similares).
NO aplica a medias ni a cubrebrazos.** Esto es lo que la sesión anterior
no tenía claro — el tiro de calientabrazos que se probó y rediseñó dos
veces en v14 no debería haber existido: tiro es un eje de PANTALÓN, punto.

#### Forma de cuello

Geometría del escote: redondo, en V, cuadrado, alto. Aplica a prendas
superiores (remera y futuras similares). No aplica a pantalón, medias ni
cubrebrazos.

#### Fit / Ajuste

Qué tan ajustada o voluminosa es la prenda respecto del cuerpo: Tight,
Regular, Loose, Oversize. Altamente reutilizable — aplica a las 4 prendas
(remera, pantalón, medias, cubrebrazos) y a las futuras. Es la misma idea
que ya se había anotado suelta en `FEMCLOTHES.md` como "eje fit,
sin implementar" — esto la formaliza dentro del sistema de categorías.

#### Compatibilidad de patrones por prenda

| Prenda | Cobertura | Tiro | Cuello | Fit |
|---|---|---|---|---|
| Remera | Sí | No | Sí | Sí |
| Pantalón | Sí | Sí | No | Sí |
| Medias | Sí | No | No | Sí |
| Cubrebrazos | Sí | No | No | Sí |

Cada prenda declara qué categorías acepta — mismo mecanismo que ya existe
para `Region`/`Operacion` en `RegionResolver`/`Garment`, extendido a
categorías de eje en vez de solo bilateral/estampa.

### Estación de tintes — separada de la forma

La Estación de Tintes (la "Mesa de tinturas" de más arriba) modifica
**solo apariencia cromática**, nunca forma — usa Patrones de Tinte
(rayas alternadas, una raya superior, tres rayas, etc.), que **no
contienen color propio**: el patrón define la distribución, el color lo
elige el tinte por separado. El mismo patrón "rayas alternadas" sirve
para rojo+blanco, azul+negro, verde+amarillo — ya es así hoy con
`ClothingPatternItem` (`PATTERN_STRIPE_TOP`/`PATTERN_STRIPE_ALT`/
`PATTERN_TRIPLE_STRIPE`, aplicados con un tinte en el telar), esta sección
solo formaliza el nombre de la estación futura que lo reemplaza.

### Flujo general del sistema completo

```
PRENDA BASE
    ↓
MESA DE MODELADO       (forma: cobertura · tiro · cuello · fit)
    ↓
ESTACIÓN DE TINTES      (patrones cromáticos + colores)
    ↓
SUBLIMADORA              (imagen concreta, viene de Camerapture)
    ↓
PRENDA FINAL
```

Separación estricta: **Modelado** cambia forma/geometría, **Tintes**
cambia color/diseño cromático, **Sublimación** aplica una imagen concreta
(no un patrón abstracto — a diferencia de Modelado y Tintes, la
Sublimadora no trabaja con categorías reutilizables, trabaja con la foto
que se cargó).

### Sublimadora

Ya existe. Ver [SUBLIMADORA.md](SUBLIMADORA.md). En la línea modular va
última: `... → sastrería → sublimadora → cofre`.

---

## 5. Automatización — line modular sin tolvas en el medio

Le va perfecto a la identidad del mod ("más una prensa que una armadura").
Una prensa **es** una máquina de producción.

### Auto-io: las máquinas se pasan los ítems solas

Cada máquina tiene **facing** (como un horno) +:
- **auto-output** a la cara de adelante (empuja la prenda terminada al
  inventario de enfrente — puede ser la próxima máquina directamente)
- **auto-pull** de atrás

Con las dos, la línea es literalmente:

```
cofre → [tinturas] → [sastrería] → [sublimadora] → cofre
```

Bloques **pegados, en fila, mirando todos para el mismo lado**. Cero tolvas
intermedias. Tolvas vanilla solo al principio (prendas en blanco), al final
(juntar), y en las caras libres para recargar tanques.

- Delay por ítem: ~1-2s en tinturas/sastrería (que se sienta trabajo, no
  máquina de lag), 20s+ el ciclo de la sublimadora = tope de throughput.
- `SidedInventory` + salida de comparador (lee cuán lleno el buffer / el
  tanque, para lógica de apagado).
- El **tanque de tinte es el sink**: producción en masa cuesta tinte en
  masa.
- "no vanilla": bloques que mueven ítems solos no lo es. Pero ya hay block
  entities custom con GeckoLib. Un tick handler que transfiere inventario es
  trivial. Opt-in si se quiere: toggle en la GUI, o requiere señal de
  redstone para el modo auto.

### Sublimadora — modo auto

No reemplaza la ceremonia, la agrega:
- Cuando le entra prenda + tiene tinta + tiene una **foto recordada**,
  arranca el ciclo solo. La tapa igual baja (es el prensado), la dispara la
  máquina en vez del click.
- **Foto recordada por cara**: guarda el UUID de la última foto cargada y la
  reusa → no necesitás 64 ítems de foto para 64 remeras. Una foto en la
  tolva actualiza la recordada.
- El uso a mano mantiene TODA la ceremonia (feedback si no arranca, agachado
  descarga, tapa trabada).

### Tolva horizontal como bloque aparte

Solo si se quiere el look de fábrica con vueltas / ramificar líneas. Para la
cadena recta no hace falta si las máquinas se pasan la posta.

---

## 6. Convención de caras — EL ESTÁNDAR DE MÁQUINA DEL MOD

Toda máquina nueva sigue esto. Mirás el bloque y sabés cómo se cablea.

| Cara | Función |
|---|---|
| **Frente** | interfaz humana — controles manuales. Te parás acá para operar a mano. |
| **Atrás** | suministro — gauges de tanque + recarga (tolva con auto-sort). En una línea dejás corredor trasero para el suministro, por eso los gauges van acá. |
| **Costado izquierdo** (mirando el frente) | entrada de la línea |
| **Costado derecho** | salida de la línea |
| **Arriba** | la tapa / apoyar la prenda a mano |

- **Convención de lados igual para las 3 máquinas** → una fila recta funciona
  sin pensar.
- **¿Flippable?** Un click con llave/destornillador que espeja entrada↔salida,
  para que la línea corra en cualquier dirección. Empezar **fijo** (izq→der),
  agregar si hace falta.
- **Puertos en el modelo**: un hueco/rodillos en el costado de entrada, una
  rampa/bandeja en el de salida (~2 bones chicos por máquina). La sublimadora
  ya es GeckoLib; tinturas y sastrería pueden ser modelos simples salvo que
  se quieran animar los rodillos.
- **Máquinas pegadas**: la salida de A toca la entrada de B → transferencia
  bloque-a-bloque, sin tolva. Diseñar los puertos para que **enganchen
  visualmente** en la cara compartida.
- **La sublimadora hoy** tiene los controles al frente y el contador CMYK
  aparte → mover el CMYK a la cara de atrás, controles al frente, I/O por los
  costados. Es la que fija la convención.

---

## 7. Roadmap máquinas

Independiente de la fase 1 de PRENDAS.md (una mesa solo setea componentes).

1. **Mesa de tinturas** — tanque 16 + biblioteca de patrones + GUI + aplicar
   por click derecho. Sacar el hook del telar acá.
2. **Mesa de sastrería** — moldes de eje (gates) + grilla de moldes de corte
   + GUI de armado + guardar. Depende de que `femclothes:variante` ya lea
   todos los ejes nuevos (coordina con track prendas).
3. **Convención de caras** en las 3 (incluido reacomodar la sublimadora) +
   modelos con puertos.
4. **Auto-io** (facing + auto-output + auto-pull) + `SidedInventory` +
   comparador.
5. **Sublimadora modo auto** (foto recordada por cara).
6. Tolva horizontal, si se quiere.
