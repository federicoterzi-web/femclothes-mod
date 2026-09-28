# Cobertura de torso vs. extremidad — categorías + asimetría izq/der

Borrador para pensar y editar. Nada de esto está implementado todavía.

## Contexto

Reorganizar los "moldes de largo" del mod en dos categorías conceptuales,
mostradas como tal en el almacén de la Mesa de Modelado:

- **Cobertura de torso**: largo de remera (`Variante.Largo`) + tiro de
  pantalón (`PantalonTiro`) — las dos tocan el cuboide de TORSO, una desde
  arriba (hombro hacia abajo), la otra desde abajo (cintura hacia arriba).
- **Cobertura de extremidad**: largo de pantalón (`PantalonLargo`), largo de
  medias (`MediasLargo`), cobertura de calientabrazos
  (`CalientabrazosItem.cobertura`) — y manga de remera en la categoría, con
  la limitación técnica anotada abajo. **Puede ser asimétrica**: izquierda,
  derecha, o ambas — algo que hoy NO existe para forma/largo (solo el COLOR
  es asimétrico hoy, vía `RegionResolver`).

Decisión ya tomada: **no fusionar los enums** de cada prenda (quedan
`PantalonLargo`/`MediasLargo`/`Variante.Manga` tal cual, con sus propios
valores/nombres/texturas) — la categorización es solo de organización en el
almacén/UI de la Modeladora. La asimetría reusa el MISMO patrón
izquierda-primaria/derecha-override que ya usa `RegionResolver` para color.

## Restricción técnica encontrada

Revisado `PiezasDelMod.java`:
- `pantalon()`, `medias()`, `calientabrazos()`: cada uno calcula la textura
  base **una sola vez** y la reusa para las piezas de ambos lados — cambiar
  a resolver la base por lado es un cambio chico y mecánico, los tres ya
  componen pieza-por-pieza.
- `remera()`: usa **un solo archivo monolítico**
  (`cuerpo_<largo>_<manga>_<cuello>.png`) que ya trae torso + las dos
  mangas + cuello horneados juntos en un PNG por combinación. Partir la
  manga por lado necesitaría descomponer ese esquema — es un proyecto de
  render aparte, no entra en este pase.

**Alcance de este pase**: asimetría real para pantalón-piernas, medias y
calientabrazos (los tres ya componen per-lado). La manga de remera queda
categorizada como "extremidad" en el almacén/UI pero sigue siendo
AMBAS-únicamente por ahora.

## Cambios de datos (mismo patrón que ya existe)

Mirror exacto de `RIGHT_DYED_COLOR`/`RegionResolver.colorBase` para cada eje
de extremidad que hoy es un solo valor sin lado:

- `RIGHT_PANTALON_LARGO` (`Optional<PantalonLargo>`)
- `RIGHT_MEDIAS_LARGO` (`Optional<MediasLargo>`)
- `RIGHT_CALIENTABRAZOS_COBERTURA` (`Optional<Variante.Manga>`)

Izquierda sigue siendo el componente primario ya existente; derecha
ausente = hereda de la izquierda (igual regla que color). Getters/setters
Lado-aware nuevos en cada clase dueña del eje, con los métodos actuales
(sin lado) quedando como atajo para IZQUIERDA — no rompen los call-sites
existentes (Telar viejo, tooltips).

## Render (`PiezasDelMod.java`)

`pantalon()`, `medias()`, `calientabrazos()`: la textura base pasa a
calcularse dos veces, una por lado, leyendo el valor Lado-aware nuevo. El
resto de cada método ya recibe `lado` — solo cambia cuál base le entra a
cada llamada. La pieza de TORSO del pantalón (banda de tiro) no cambia —
el tiro nunca fue bilateral y sigue sin serlo.

## `ComboCorte` y `PrendaModelado`

Se agrega un campo `Lado lado` (default `AMBAS`) a `ComboCorte`, usado solo
por los tres campos de extremidad; los campos de torso lo ignoran (siempre
aplican entero, como hoy).

---

## Alternativas de organización del almacén (para pensar/elegir)

El borrador original proponía **A**. Documento las otras variantes que se
me ocurrieron para que se puedan comparar y editar antes de decidir.

### A — Rangos de slots fijos en una sola grilla

Reservar rangos de índice en la grilla plana de 18 slots (ej. slots 0-5
"Torso", 6-13 "Extremidad", 14-17 "Otros"), con `isValid()` restringiendo
qué tipo de molde entra en cada rango. Etiquetas de texto dibujadas encima
de cada rango (`drawForeground`, no horneadas en el PNG).

- 👍 Cambio más chico: mismo grid de 18 que ya existe, solo se le pone
  reglas y etiquetas.
- 👎 Los rangos quedan con tamaño fijo aunque una categoría tenga pocos
  moldes (ej. "Otros" con un solo eje, cuello) y otra necesite más lugar —
  hay que adivinar bien cuántos slots reservar por categoría de entrada.

### B — Pestañas (tabs) por categoría

Botones "Torso" / "Extremidad" / "Otros" arriba del almacén que cambian
qué slots están VISIBLES/activos, cada categoría con su propia sub-grilla
completa (podría ser más chica por categoría, ej. 6 slots cada una, ya que
no hay que verlas todas a la vez).

- 👍 Panel más compacto, cada categoría puede tener el tamaño que
  realmente necesita sin desperdiciar espacio.
- 👎 Más lógica de UI (estado de "pestaña activa", los slots ocultos
  igual existen del lado del servidor) — más superficie de bugs de layout,
  justo lo que costó esta sesión.

### C — Grilla plana sin restricción, solo color de acento

No tocar el almacén en absoluto (sigue siendo una grilla libre de 18) —
la "categoría" es puramente visual: un borde o tinte de color por slot
ocupado, reusando la convención de acento por eje que el mod ya tiene
(rojo/azul/verde/ámbar/violeta/magenta por tipo de molde — ver
`FEMCLOTHES.md` §"Un color de acento por EJE"). Un vistazo rápido muestra
qué es cada cosa sin restringir dónde se puede poner.

- 👍 Cero cambios de `isValid`/estructura, el menor riesgo de todos.
- 👎 No es realmente "categorizar el storage", es solo maquillaje — capaz
  no cumple la idea de fondo de separar de verdad.

### D — Sub-grillas separadas visualmente (todas visibles a la vez)

Como A, pero cada categoría es una mini-grilla con su propio marco/recuadro
en vez de ser un tramo indistinguible de una tira continua — más alto que
A (necesita más espacio vertical total) pero mucho más legible de un
vistazo, sin la complejidad de pestañas de B.

- 👍 El más claro visualmente sin agregar lógica de estado (tabs).
- 👎 El panel crece en altura — puede volver a las peleas de layout de
  esta sesión si no se mide con cuidado.

**Mi recomendación si hay que elegir una:** A para arrancar (motor ya
existe, menor riesgo) — se puede pasar a D después si A se siente
confuso una vez jugado, es un cambio principalmente visual/de coordenadas,
no de lógica de fondo.

## Archivos a tocar (una vez que se elija la organización)

- `FemclothesComponents.java`: 3 componentes `RIGHT_*` nuevos.
- `PantalonItem.java`, `MediasLargo.java`, `CalientabrazosItem.java`:
  overloads Lado-aware de largo/cobertura.
- `PiezasDelMod.java`: `pantalon()`/`medias()`/`calientabrazos()` resuelven
  la base por lado.
- `ComboCorte.java`: campo `Lado lado` + codec/packet-codec.
- `PrendaModelado.java`: usa los setters Lado-aware para los 3 campos de
  extremidad.
- `ModeladoBlockEntity.java`: organización del almacén (según la
  alternativa elegida), botón de ciclar Lado en el borrador.
- `client/ModeladoScreen.java`: botón de Lado, etiquetas/estructura de
  categoría según la alternativa elegida.
- `tools/generar_textura_modelado.py`: regenerar si la organización elegida
  cambia el fondo.
- Lang: nombres de categoría.

## Verificación

- `./gradlew.bat compileJava` limpio.
- **Preguntar antes de compilar y antes de lanzar el cliente** — está
  codeando en paralelo en IntelliJ, pidió que se le avise cada vez.
- En el juego: fijar cobertura de pantalón solo en la pierna izquierda,
  confirmar visualmente que la derecha queda con el largo anterior; repetir
  con medias y calientabrazos; confirmar que el molde de manga de remera
  sigue aplicando a ambos brazos por igual (limitación conocida, no bug).
