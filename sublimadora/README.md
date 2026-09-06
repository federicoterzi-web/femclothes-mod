# Sublimadora — mod aparte, mismo repo

Prensa térmica que estampa fotos de [Camerapture](https://modrinth.com/mod/camerapture)
sobre remeras en blanco. Es un **mod independiente** (`sublimadora`), no
parte de FemClothes: se construye como subproyecto de Gradle y sale como
su propio jar.

Lo único que comparten es el slot de torso de Trinkets, y lo comparten sin
depender uno del otro — ver abajo.

## El ciclo

1. Tapa abierta. Click con un **tinte** carga ese tanque (hasta 16 de cada
   uno de los cuatro CMYK). Click con una **remera** la apoya en la plancha.
   Click con una **foto** la carga en la cara que tenga elegida el selector.

   Dos controles en el frente de la base: el **selector** de cara (arriba
   frente, abajo espalda) y el **slider de modo**. El selector dice de qué
   cara hablamos y el slider qué se le hace, así se puede dejar full print
   adelante y logo atrás y estampar las dos en una sola pasada. Cuesta una
   dosis de cada color **por cara**: hacer las dos juntas ahorra el ciclo,
   no la tinta.

   Si lo que tenés en la mano no entra —el tanque lleno, una remera cuando
   ya hay una— el click **no se consume** y pasa a abrir o cerrar la tapa.
   Al revés se la tragaba y con un tinte en la mano no había forma de
   cerrar la máquina.
2. Click con la mano vacía cierra la tapa. **Cerrar la tapa es lo que
   dispara el prensado**, y ahí se consume la foto más una carga de cada
   color.
3. Veinte segundos (400 ticks) con el LED rojo parpadeando, un beep por
   parpadeo y vapor saliendo por la junta.
4. Poof de descarga, y ocho ticks después el LED verde con la campanita.
5. Tapa abierta, click con la mano vacía y sale la remera estampada.

Si al cerrar la tapa no puede arrancar, lo dice en la barra de acción en
vez de quedarse muda — el silencio era indistinguible de estar rota.
Agachado, el click descarga lo que haya puesto, para poder corregir. Y
mientras prensa **la tapa queda trabada**: sin eso se podía abrir en medio
del ciclo y ver la remera flotando con el prensado corriendo por debajo.

Con la tapa abierta, la foto cargada se dibuja **apoyada sobre la remera**.
Es el único indicio de que hay una puesta, y muestra cuál. No es un hueso
del modelo porque la textura cambia con cada foto y un hueso tiene una
sola, la del atlas: va como quad después del modelo, con la misma rotación
que GeckoLib le aplica al bloque.

Ese dibujo usa `getFotoVisible()` y no `getFotoCargada()`: el prensado
consume la foto en el mismo tick en que se cierra la tapa, pero la tapa
tarda **seis ticks** en bajar, así que sin arrastre el papel se evaporaba
con la plancha todavía abierta.

La remera es **un solo ítem**. Lo que cambia son dos componentes,
`estampa_frente` y `estampa_espalda`; sin ninguno está en blanco. Cada uno
guarda el UUID de la foto y no la imagen, así el `ItemStack` pesa lo mismo
con o sin estampa y la imagen sigue viviendo en el almacenamiento de
Camerapture.

**Una cara por ciclo.** Estampar frente y espalda cuesta dos pasadas y dos
juegos de tinta. No es una limitación técnica: la máquina prensa un lado a la
vez, igual que una de verdad. Una remera con el frente hecho vuelve a entrar
para imprimirle la espalda, y si intentás pisar una cara que ya tiene dibujo
la rechaza en vez de sobreescribirla en silencio.

```java
record Estampa(UUID foto, float escala, float x, float y)
```

`escala` y la posición van **normalizadas al área imprimible**, no en
píxeles: así el mismo número sirve para el ícono del inventario, para la
remera en la plancha y para cuando se dibuje sobre el cuerpo, que son tres
resoluciones distintas.

Los valores los elige un **slider vertical en el frente de la máquina** —
click con la mano vacía en la parte baja del frente lo corre. Tres presets,
que cubren los casos reales sin obligar a pelear con controles finos a
ciegas gastando tinta y veinte segundos por intento:

| posición | modo | qué hace |
|---|---|---|
| abajo | `LOGO` | chico, arriba a la derecha |
| medio | `CENTRADA` | la estampa clásica |
| arriba | `COMPLETO` | full print, cubre la prenda entera |

**El full print va enmascarado contra la silueta.** No alcanza con dibujar
un cuadrado sobre la remera: tiene forma de T, y cualquier rectángulo que
llegue a las mangas se sale del contorno por las esquinas. `EstampaTextures`
compone al vuelo una textura de 16x16 con `alfa = alfa_foto × alfa_prenda`,
y la foto se recorta desde el centro para llenar en vez de encogerse para
entrar — que es lo que uno quiere cuando la consigna es que no quede tela
sin estampar.

Dos cosas de esa composición que no son obvias:

- **Los píxeles de la foto se le piden de vuelta a la GPU.** La foto no es
  un recurso del resource pack, se registra en runtime, así que no hay de
  dónde leerla en disco. El tamaño se le pregunta a OpenGL y no al mod: la
  textura puede estar padeada y leer con el tamaño equivocado la corrompe.
- **`NativeImage` empaqueta ABGR, no ARGB.** Por eso se copia el color
  entero y sólo se rearma el byte de alfa.

Si la composición falla —la textura todavía no subió, por ejemplo— cae a un
rectángulo limitado al torso. Más chico, pero nunca fuera del contorno.

**Los PNG con transparencia funcionan**, verificado de punta a punta. El
alfa sobrevive el viaje por Camerapture aunque comprima en WebP con pérdida.
Del lado nuestro la estampa se dibuja con `getEntityTranslucent` y no con
`getEntityCutout`: cutout hace alfa **binario** y se come cualquier degradé,
que es justo lo que tiene un logo con bordes suavizados.

El componente viejo `picture_id` sigue registrado para leer las remeras de
mundos guardados, donde la estampa era un UUID pelado siempre centrado
adelante. Nada lo escribe.

## Conseguirla y levantarla

Se craftea con hierro, un pistón y un horno: el bastidor, la prensa que baja
y el calor.

```
I I I          L · L
I P I          L L L     8 lanas del mismo color
I F I          L L L     → remera de ese color
```

La remera sale en los **16 colores**, uno por color de lana. El color viaja
en el componente `dyed_color` de vanilla, puesto desde la receta: no hace
falta código ni un ítem por color. Los RGB son los `DyeColor.getFireworkColor()`,
los mismos que usa el telar de FemClothes, así que craftear rojo y teñir de
rojo dan el color idéntico.

El teñido pasa por tres caminos distintos: el **ícono** lo tiñe vanilla con
un proveedor de color, el **cuerpo** se multiplica al componer la textura, y
la **remera de la plancha** queda blanca porque sale del atlas de GeckoLib.

⚠️ El tinte de ítem es **ARGB y el alfa cuenta**. Devolver un RGB pelado deja
el ítem invisible; vanilla usa `-1` para "sin tinte". Ya costó un rato en
este repo.

Al teñir el cuerpo, la tela se multiplica **antes** de pintar las estampas:
la foto se imprime sobre la prenda ya teñida y no se tiñe con ella, igual
que en una sublimadora de verdad.

Se pica **con la mano** en unos 3.7 s, o con pico de hierro en menos de uno.
No lleva `requiresTool()` a propósito: con esa bandera la mano no dropeaba
nada, y para una máquina que puede tener una remera estampada adentro eso es
cruel. Igual está en `mineable/pickaxe` para que el pico sea la herramienta
rápida.

Al romperla **devuelve lo que tenga adentro** —remera, foto, o la estampada
sin retirar— como un cofre. Sin eso el block entity se iba y con él sus
`ItemStack`, sin aviso.

**La tinta cargada viaja adentro del ítem.** Es el mismo mecanismo con el que
una caja de shulker se lleva su contenido: `collectImplicitComponents` al
romper, `applyImplicitComponents` al colocar, y la loot table copia el
componente sola con `minecraft:copy_components`. El ítem lo dice en el
tooltip, porque si no dos sublimadoras idénticas —una llena y otra vacía— no
se podrían distinguir hasta colocarlas.

Detalle que hay que hacer a mano: `removeFromCopiedStackNbt` borra esas
claves del NBT copiado. Si no, la tinta viajaría **dos veces** —en el
componente y en el NBT del block entity— y dos máquinas con la misma tinta
no apilarían entre sí.

Lo que no vuelve es la tinta al romper: son cuatro tanques de hasta 16 dosis
y no hay ítem que represente una dosis suelta, así que reintegrarla sería
inventar tintes de la nada. Por eso viaja en el ítem en vez de devolverse.

## Editar el modelo en Blockbench

```
src/main/resources/assets/sublimadora/
├── geo/sublimadora.geo.json               ← File > Open, modo Bedrock Block
├── animations/sublimadora.animation.json  ← pestaña Animate > Load
└── textures/block/sublimadora_atlas.png   ← panel Textures > +
```

**Sólo esa copia.** `build/resources/` y `bin/` tienen copias que se pisan
solas en cada build; si editás ahí, perdés el trabajo. Las dos están en
`.gitignore`: si dudás, la buena es la que git te muestra como modificada.

El formato Bedrock **no tiene campo de textura**, por eso Blockbench abre
el modelo en gris. Hay que cargar el atlas a mano. Para no repetirlo,
*File > Save Project* como `.bbmodel` y después trabajar sobre ese —
pero ojo: **el juego lee el `.geo.json`**, así que al terminar hay que
*File > Export > Bedrock Geometry* encima del original. Guardar el
proyecto no alcanza.

Nombres que el código busca y **no** se pueden renombrar:

| hueso | lo usa |
|---|---|
| `led_rojo`, `led_verde` | `SublimadoraGeoModel` los prende y apaga con `setHidden` |
| `palanca` | el pomo del slider de modo; se corre en Y |
| `palanca_guia` | su riel. Es un hueso aparte a propósito: adentro de `palanca` se desplazaría junto con el pomo |
| `selector` | el pomo del selector de cara |
| `selector_guia` | su riel |
| `remera` | idem, según haya una cargada |
| `ink_c`, `ink_m`, `ink_y`, `ink_k` | escala en Y según la tinta restante |
| `tapa` | la animación de abrir y cerrar |

Los zócalos negros (`led_zocalo_rojo`, `led_zocalo_verde`) son cubos
comunes dentro de `tapa`: esos movelos libremente.

## El atlas

Un solo PNG de 64x64 para todo el modelo, más
`sublimadora_atlas_glowmask.png` del mismo tamaño donde los píxeles
marcados se dibujan a luz plena. GeckoLib resuelve el sufijo `_glowmask`
solo, a partir del nombre de la textura.

Zonas con dueño, para no pisarlas:

| zona | quién |
|---|---|
| `(16,48)` | LED rojo — también marcada en el glowmask |
| `(32,48)` | LED verde — idem |
| `(48,48)`–`(58,48)` | caras laterales de la remera, **vacía a propósito** |
| `(0,48)` | barra de tinta negra |
| `(0,32)`–`(1,35)` | pista de las cuatro barras, **gris a propósito** |
| `(48,16)`–`(58,26)` | la remera vista desde arriba |
| `(16,19)` | el píxel más oscuro, lo usan los zócalos |

La pista gris tampoco es decorativa. Era `(25,27,30)` y la barra negra es
`(33,34,38)`: ocho puntos por canal, o sea la barra dibujándose sobre un
fondo de su mismo color. Un gris medio y no claro, porque contra uno claro
el amarillo perdía fuerza.

Esa franja vacía en `y=48` no es un olvido: las cuatro caras laterales de
la remera la muestrean, y con un texel opaco quedaba un marco blanco
cuadrado rodeando una remera con forma de T. La loza tiene 0.42 de alto
—menos de medio píxel— pero el render igual le dibuja una línea. Se
arregló en el atlas y no en la geometría **para que sobreviva al próximo
export de Blockbench**.

## Trampas que ya costaron tiempo

### GeckoLib está fijado en 4.7.7 a propósito

4.8.4 **no dibuja cubos con `uv_size` parcial por cara**, que es como está
hecho todo el modelo. Confirmado con un A/B. Y no se puede subir a 4.9+:
pide Loom 1.17.13 y el proyecto está en 1.15.3.

### La tapa aparecía abierta al cargar el chunk

El controlador pedía la *transición* en vez de la *pose*. `cerrar` arranca
en −104 grados, o sea abierta, así que un bloque cerrado aparecía abierto
y se cerraba solo delante del jugador. La animación pedida tiene que ser
**estable entre cambios de estado**: si cada frame se pide la transición,
GeckoLib la reinicia para siempre.

### El vapor usa `WHITE_SMOKE`, no `CLOUD`

`PlayerCloudParticle` busca al jugador más cercano dentro de 2 bloques y
arrastra la partícula hacia la altura de sus **pies** un 20% por tick.
Como para usar la máquina hay que estar al lado, el vapor se venía abajo
por más velocidad hacia arriba que se le pusiera. `WhiteSmokeParticle`
además tiene gravedad −0.1, o sea flotabilidad: sube sola.

Otras dos cosas del sistema de partículas que no son obvias:

- **`count = 0` no significa "ninguna"**. Con `count > 0` los tres deltas
  son dispersión de posición y la velocidad es azar gaussiano por `speed`
  en las tres direcciones. Con `count = 0` sale una sola partícula y los
  deltas son su velocidad.
- **Colisionan con bloques.** Nacían en un círculo de radio 0.48 que cae
  entero adentro del cubo, y salían expulsadas para cualquier lado. Ahora
  nacen sobre una de las cuatro caras, a 0.62 del centro.

Y la fricción de 0.96 hace que una partícula recorra unas 13 veces su
velocidad inicial antes de frenar: velocidades que parecen chiquitas la
mandan a varios bloques.

### El ícono del inventario

Lo dibuja GeckoLib con el mismo `.geo.json` que el bloque. Antes era un
modelo vanilla escrito a mano en paralelo, que nunca se enteraba de lo
editado en Blockbench.

El `-5` en el `display` del modelo de ítem no es tanteo: vanilla centra
con `translate(-0.5,-0.5,-0.5)` y GeckoLib suma `translate(0.5, 0.51,
0.5)`. En X y Z se cancelan, pero el modelo va de `y=0` a `y=16` —apoyado
en el origen, no centrado en él— así que su centro queda medio bloque
arriba. Adentro del `scale` eso son `0.51 * 0.625 * 16 = 5.1` píxeles.

### La remera del inventario no la dibujamos nosotros

`RemeraItemRenderer` no reimplementa el aspecto de la remera: le pide al
`ItemRenderer` el modelo `item/remera_base` —un `item/generated` normal— y
recién después le pega la foto adelante. Hacerla a mano habría costado el
relieve y el sombreado que vanilla le da gratis a cualquier ítem plano.

Dos cosas que hay que saber si se toca:

- **`renderItem` vuelve a hacer `translate(-0.5,-0.5,-0.5)`** por su cuenta, y
  vanilla ya lo hizo antes de llamar al renderer builtin. Sin compensarlo la
  remera sale corrida un bloque entero.
- **`item/remera_base` no lo referencia nadie**, así que Minecraft no lo
  hornea. Hay que pedirlo con `ModelLoadingPlugin.addModels` o el renderer no
  lo encuentra y la remera se ve invisible.

### Dos superficies coplanares parpadean de lejos

Pasó dos veces: la estampa sobre el ítem y la foto sobre la plancha. En los
dos casos había puesto la capa de arriba **exactamente** sobre la de abajo
—`0.0313` contra la cara del modelo en `0.03125`, cinco cienmilésimas— y de
cerca el z-buffer todavía las distinguía, pero de lejos pierde precisión y
se pelean. Hay que dejar al menos medio píxel de separación.

### El orden de llegada de dos paquetes no está garantizado

Al cerrar la tapa viajan por separado el cambio de estado del bloque y el
NBT del block entity. El arrastre que mantiene la foto a la vista arrancaba
al detectar la tapa cerrándose, así que si el NBT sin foto llegaba primero
la foto desaparecía, y un tick después el arrastre la hacía volver: un
parpadeo.

Se arregla no dependiendo del orden. El contador se **recarga** mientras
haya foto en vez de arrancar en la transición, así en el momento en que la
foto se va ya está lleno, llegue lo que llegue primero.

### La remera puesta: el layout de skin y el lienzo

La prenda del cuerpo es una textura en **layout de skin** a 4x
(`RemeraTrinketRenderer.ESCALA`), donde cada región es una cara de una
`ModelPart`. No tiene nada que ver con el sprite de 16x16 del ítem, y
confundir los dos es el error más fácil de cometer.

Hay una plantilla dibujada en `calibracion/plantilla_skin.png` con cada
región etiquetada. Vocabulario acordado para hablar de una manga: **superior,
delantera, trasera, interna, externa**, más el puño, que no se pinta.

⚠️ **Interna y externa no están en la misma columna en las dos mangas.** El
desdoblado ordena siempre derecha / frente / izquierda / atrás en coordenadas
del MODELO, y como un brazo está en −X y el otro en +X, eso las invierte. En
la manga derecha la externa es la primera columna del bloque; en la izquierda
es la tercera. Es lo que hace que un cambio "simétrico" salga espejado en un
solo brazo.

El full print no le da a cada cara su propio recorte: eso dejaba las mangas
con un pedazo suelto que no continuaba el torso. Hay un **lienzo virtual de
24x14** que es la prenda desenrollada de frente, y cada cara toma su ventana:

```
        0    4        8              16      20    24
  y 0   │    │ supD   │   hombros    │ supI  │     │   banda del hombro
  y 2   │extD│ mangaD │    TORSO     │mangaI │extI │
  y 14  └────┴────────┴──────────────┴───────┴─────┘
```

Los dos números salieron de bugs concretos. **24 de ancho** porque las caras
que envuelven —las externas— necesitan columna propia: sin ellas pedían el
mismo rectángulo que las delanteras y se veían idénticas. **14 de alto**
porque las caras horizontales necesitan una banda propia arriba: sin ella la
superior de una manga pedía el mismo rectángulo que su delantera y salía
repetida, nada más que rotada por la orientación de la cara.

Cada cara está partida en mitad delantera y trasera, así un estampado de un
solo lado no se desborda al otro por arriba del hombro o por el costado. La
única entera es el ruedo, que es la boca de abajo y no se ve.

### Tres flags que parecen uno solo pero no lo son

Cada vez que se parte una cara queda a la vista que son independientes, y
tenerlos mezclados hacía que arreglar una cara rompiera otra:

| flag | qué decide |
|---|---|
| `atras` | de qué estampa es la cara |
| `espejar` | eje horizontal invertido — sólo las que se **miran** desde atrás |
| `espejarV` | eje vertical invertido — sólo las caras **horizontales** traseras |

Pertenecer a la espalda no implica verse desde atrás: la mitad trasera de un
costado es de la estampa de atrás pero se mira de perfil. Y verse desde atrás
no implica estar invertida en vertical: eso les pasa sólo a las tapas de
arriba, donde la mitad trasera tiene que continuar hacia la espalda y su eje
v corre al revés.

### Herramientas de calibración

`calibracion/patron_calibracion.png` es una imagen 16:12 para subir con
Camerapture y estampar en full print. El tono dice la columna, el brillo la
fila, una diagonal negra cruza de esquina a esquina —si el motivo continúa de
una cara a la otra, la línea sigue derecha— y una F blanca gigante delata
cualquier espejado porque es asimétrica en los dos ejes.

`calibracion/simulacion_cuerpo.png` es la misma cuenta hecha fuera del juego:
sirve para saber si lo que se ve mal está en el render o en la tabla de caras.

### El modelo se dibuja espejado en X

GeckoLib dibuja el modelo con el eje X invertido. Cualquier control que se
ubique **por posición del click** necesita esa vuelta: la palanca de modo
está en `x = +6` del modelo pero cae del lado `-x` del bloque, así que los
dos controles del frente respondían al revés de donde se ven.

### El sello de build

El mod loguea al arrancar la fecha del build, que `processResources`
escribe en `sublimadora_build.txt`:

```
[sublimadora] Sublimadora cargada - build 2026-09-05 12:50:10
```

Comparada contra `build/resources/main/sublimadora_build.txt` contesta sin
discusión si el cliente abierto trae los últimos cambios. Existe porque
varias veces se probó durante minutos sobre un cliente desactualizado, y
eso es indistinguible de un cambio que no funciona.

Tiene que ser un **recurso** y no un hash de las clases: en el cliente de
desarrollo Loom remapea el jar de intermediary a nombres yarn, así que los
`.class` cambian de bytes aunque el código sea idéntico. Los recursos
pasan tal cual.

### Dependencias que Loom no desanida

Camerapture trae **webp4j** como jar anidado, y Loom no extrae los
anidados de `modLocalRuntime`: sacar una foto moría con
`NoClassDefFoundError: dev/matrixlab/webp4j/WebPCodec`. Va suelto en
`libs/`. Mismo caso que las librerías de 3D Skin Layers.

### Correr el cliente

`gradlew runClient` **sin los dos puntos** corre la tarea en todos los
subproyectos y levanta dos instancias, cada una con su propio mundo. Usar
siempre `gradlew.bat :runClient` desde la raíz.

## El slot de torso, compartido con FemClothes

El slot `torso/prenda` lo define FemClothes; la sublimadora sólo declara
su remera en el tag. **Los tags se fusionan entre datapacks**, así que
ninguno de los dos depende de que el otro esté instalado: sin FemClothes,
el tag de la sublimadora simplemente no aplica.

Que el croptop y la remera compartan slot es a propósito: dos prendas
dibujadas sobre el mismo pedazo de cuerpo se pisarían, así que en vez de
resolver el solapamiento se vuelven mutuamente excluyentes.

La remera **se dibuja puesta**, con su estampa. Trinkets es una dependencia
opcional, igual que Camerapture: sin ella la remera sigue siendo un ítem que
se craftea y se estampa, sólo que no se puede vestir.

> **Pendiente**: la estampa no se ve todavía sobre la remera apoyada en la
> plancha; eso es una capa de GeckoLib.

## Camerapture, sin depender de Camerapture

La foto se reconoce comparando el id registrado (`camerapture:picture`),
no importando la clase. El acceso al componente vive aislado en
`CameraptureCompat`, detrás de un `FabricLoader.isModLoaded`, para que la
JVM no lo resuelva si el mod no está. Compila y corre igual sin
Camerapture; simplemente no hay fotos que poner.
