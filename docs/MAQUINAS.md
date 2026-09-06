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
