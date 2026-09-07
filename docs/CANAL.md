<!-- canal-version: 10 -->
# Canal — bitácora entre sesiones de Claude

Este archivo es el **canal de comunicación asincrónico** entre las distintas
sesiones de Claude que tocan este mod (distintas cuentas, distintas máquinas).
El repo está en GitHub; cuando la compu del dueño está en reposo, esto sigue
online.

## Protocolo

**Al empezar a trabajar:**
1. `git pull`
2. Leé este archivo de arriba para abajo hasta tu última entrada conocida.
3. Mirá el `canal-version` en la primera línea. Si subió desde la última vez
   que lo viste, hay algo nuevo.

**Al terminar (o al dejar algo a medias):**
1. Agregá una entrada **arriba de todo** en la sección Bitácora, con el
   formato de abajo.
2. Subí el `canal-version` de la primera línea en 1.
3. `git add -A && git commit && git push`.

**Formato de entrada:**

```
## vN — AAAA-MM-DD — <cuenta o apodo> · <máquina o contexto>

**Hecho:** …
**Próximo:** …
**Para el otro Claude:** … (preguntas, bloqueos, "no toques X")
```

Reglas:
- `vN` = el `canal-version` que dejás tras subirlo.
- Newest on top. No borres entradas viejas, solo agregá.
- Si dos sesiones editan a la vez y hay conflicto de merge en este archivo,
  quien mergea deja las **dos** entradas, ambas con su `vN` original.
- Los docs de diseño (`PRENDAS.md`, `MAQUINAS.md`, `SUBLIMADORA.md`,
  `FEMCLOTHES.md`) son la fuente de verdad del *qué*. Este archivo es el
  *estado*: qué se hizo, qué sigue, qué está trabado.

---

## Bitácora

## v10 — 2026-09-07 — cuenta de H0p3san · Claude Code (extensión de VSCode)

**Hecho:**

1. **El layering de v9 se probó EN PERSONA y anda.** El dueño equipó medias +
   shorts en el mundo guardado, confirmó que la media se sigue viendo por
   debajo — la sospecha que quedaba anotada en v9 está resuelta. (Yo intenté
   automatizar la prueba a ciegas con PowerShell/Win32 antes de esto —
   `SetForegroundWindow` no le puede robar el foco a un proceso que no es el
   foreground, hace falta el truco del tap de ALT antes— y terminó siendo más
   lento que dejar que el dueño juegue directo. Anotado por si a alguien se
   le ocurre retomarlo: no vale la pena, no hay Playwright para un juego 3D.)

2. **`shorts` se reemplazó por `pantalon`**, a pedido del dueño: la prenda
   larga de pierna es la base (igual que la remera con el torso), y se
   recorta después con molde — de pantalón completo a tanga, 7 valores.
   - `PantalonLargo` (enum, clave+filas, en `com.femclothes.item` — NO en
     `sublimadora`, porque no tiene nada que ver con estampar) +
     `PantalonItem` (`ClothingTrinketItem`, nombre por valor) +
     `MoldePantalonItem` (cicla el eje, no consume, mismo mecanismo que los
     moldes de remera pero clase propia — el `MoldeItem.Eje` de remera está
     tipado a `Variante`, que es de otra prenda).
   - Componente nuevo `femclothes:pantalon_largo`. Ausente = pantalón
     completo (default del crafteo, igual que remera con `Variante.BASE`).
   - **Rama propia en el telar** (`reformarPantalon`), pero MÁS CHICA que la
     de remera: el pantalón es bilateral para el tinte (cada pierna su
     color, lo que ya tenía shorts) así que el tinte sigue el camino
     genérico — solo el LARGO necesita rama especial, porque el molde no es
     un `ClothingPatternItem` que el camino genérico reconozca.
   - **7 texturas de cuerpo generadas** (Python otra vez, mismo generador de
     v9 parametrizado por filas: 12/9/7/4/3/2/1). La de `shorts` (4 filas)
     NO se regeneró — se copió el archivo ya probado en el juego, para no
     arriesgar el único pixel ya validado.
   - **Ícono: UNO SOLO para los 7 valores**, a propósito — la silueta del
     ítem es simbólica, no un desdoblado proporcional como la textura de
     cuerpo, así que no hay forma principista de recortarla en 7. El nombre
     (que sí cambia por valor) es la diferenciación en el inventario.
     Documentado como gap explícito, mismo criterio que el ícono de patrón
     de remera en v8.
   - `molde_pantalon` reusa el ícono de `molde_largo` (de remera) — dos
     herramientas distintas, mismo sprite, hasta que alguien dibuje el
     propio. Casi se me pasa: el primer build con el cliente arriba tiró un
     `FileNotFoundException` por el modelo de ítem faltante — quedó
     detectado y arreglado ANTES de devolverle el cliente al dueño.
   - Renombrados: `shorts.json`→`pantalon.json` (receta), item/slot icons,
     tag de `piernas/exterior`. `PrendasDelMod`/`PiezasDelMod` actualizados.

**Cliente probado dos veces tras el cambio** (arranca limpio, sin warnings de
femclothes) pero **el pantalón en sí — puesto, con el molde cicleando los 7
largos — todavía NO se probó en persona.** Solo shorts (el valor que ya
tenía, filas=4) se vio andando.

**Próximo:** que el dueño pruebe el molde ciclando los 7 largos (¿el
dobladillo se ve bien en las siete? ¿"pantalón" completo tapa bien la media
entera o se ve una franja?). Después: patrón para pantalón (mismo mecanismo
que remera en v8), o seguir con fishnet/socks_34 que ya no tienen nada
bloqueando.

**Para el otro Claude:**
- `PantalonLargo`/`PantalonItem`/`MoldePantalonItem` viven en
  `com.femclothes.item`, NO en `sublimadora` — a diferencia de
  `Variante`/`MoldeItem` de remera. Si generalizás el `Map<Eje,Valor>` de
  PRENDAS.md §6 algún día, este es el segundo caso concreto (después de
  remera) para chequear que el diseño generalice de verdad.
- El generador Python de texturas TAMPOCO quedó guardado esta vez (de nuevo
  ad-hoc). Si esto se repite una tercera vez para otra prenda, es señal de
  que vale la pena guardarlo como script real en el repo en vez de
  reescribirlo cada sesión — está documentado igual en los comentarios de
  `PantalonLargo`/`PiezasDelMod.pantalon` con la matemática exacta.
- Automatizar la interacción con el cliente de Minecraft (mouse/teclado) no
  vale la pena con las herramientas de este entorno — dejar que el dueño
  pruebe es más rápido y más confiable.

## v9 — 2026-09-07 — cuenta de H0p3san · Claude Code (extensión de VSCode)

**Hecho:** shorts migrados al sistema de capas, a pedido del dueño — **la
primera prueba real del layering** (shorts dibujados ENCIMA de las medias sin
borrarlas, que es exactamente lo que el sistema viejo no podía hacer). El
cliente arranca limpio con el nuevo slot cargado, sin probar puesto en el
juego todavía.

- `FemclothesItems.SHORTS`: de `ClothingArmorItem` (geometría de armadura,
  **sin textura de armadura siquiera** — `cloth_layer_1.png` nunca existió,
  así que puesto no se veía nada) a `ClothingTrinketItem`. No había
  comportamiento previo que preservar.
- **Slot nuevo** `piernas/exterior` (Trinkets), separado de `socks/pair` a
  propósito: las dos prendas coexisten, y quién va arriba lo decide
  `Capa.PIERNA_EXTERIOR` (20) contra `Capa.MEDIA` (10) — no el `order` de
  Trinkets. Nombre tomado directo de `PRENDAS.md` §1 (la región `piernas` ya
  estaba diseñada con este slot).
- **Sin patrón ni corte todavía** (`Garment.regionesDe`: solo `TENIR` →
  `BILATERAL`). A propósito, para no mezclar "probar que el layering anda"
  con features nuevas — el mecanismo para sumarlo después es el mismo que ya
  tienen medias y remera.
- **Textura base generada por script** (Python + Pillow, no había generador
  en el repo así que escribí uno ad-hoc para esto), reusando la MISMA
  matemática de `CajaSkin`/`LayoutSkin` que el renderer para no adivinar
  coordenadas: cubre las 4 filas de arriba de las 12 del cuboide de pierna
  (waist → medio muslo) más la tapa de arriba, con una fila de dobladillo un
  poco más oscura en el borde — mismo principio que pide FEMCLOTHES.md para
  las mangas ("no cortado con tijera"). 512×512, blanco puro + gris de
  dobladillo, para que el tinte multiplicativo de siempre funcione.
- **Ícono**: recoloreado a blanco/gris tintables (mismo patrón que
  `socks_solid.png`) — antes era un color fijo sin tinte real, porque
  `ClothingArmorItem` con material `dyeable=false` nunca lo tiñó. Comparte
  el `ColorProviderRegistry` de las medias (mismo callback, dos ítems).
- Slot icon (`gui/slot/shorts.png`) generado con la misma convención: gris
  al 45% de alpha.
- FEMCLOTHES.md y PRENDAS.md actualizados: tabla de prendas, la mención
  vieja a `BodyPartTrinketRenderer` en la sección de layouts (que ya no
  existe desde la fase 1 y quedó sin corregir hasta ahora), roadmap.

**Próximo:** fishnet y socks_34 son los siguientes candidatos obvios —
mismo mecanismo, sin slot nuevo que inventar (fishnet podría compartir
`piernas/exterior` con shorts, o necesitar el suyo si conviven). Patrón para
shorts, si hace falta, es directamente reusar `ClothingTextureCache` con una
`patternMaskFor("shorts", ...)`.

**Para el otro Claude:**
- **Nada de esto se probó puesto en el juego** — ver medias/short simultáneos
  en persona es lo primero que hay que hacer. La sospecha concreta a
  verificar: que el short tape las primeras 4 filas de la media sin agujeros
  ni un borde raro en el dobladillo.
- El generador Python que usé para la textura y el ícono no quedó guardado en
  el repo (lo corrí ad-hoc, one-off) — si hace falta regenerar o hacer una
  variante, la matemática está documentada en el comentario de
  `PiezasDelMod.shorts`/`texturaShorts` y en este mismo mensaje, no hay
  script para reejecutar.
- El slot `piernas/exterior` es nuevo pero el NOMBRE ya estaba en
  `PRENDAS.md` §1 desde v3 — no lo inventé, lo implementé.

## v8 — 2026-09-07 — cuenta de H0p3san · Claude Code (extensión de VSCode)

**Hecho:** patrones de tela (rayas, etc) para la remera, a pedido del dueño.
Antes solo existían para medias. El dato ya era genérico
(`RegionResolver`/`PATTERN_ID` no son de ninguna prenda en particular); lo
que faltaba era quien lo aplicara y quien lo pintara.

- **Telar** (`ClothingLoomScreenHandler.reformarRemera`): ahora acepta un
  `ClothingPatternItem` en el slot de patrón. Remera + tinte + patrón aplica
  el patrón con ese color sin tocar la base (misma regla que el resto de las
  prendas); remera sola le saca el patrón si tenía. Es **interino a
  propósito**: el dueño aclaró que corte y patrón se van a mudar a las
  máquinas nuevas de `MAQUINAS.md` (mesa de sastrería / Estación de tintes —
  sus nombres para lo que el doc llama mesa de sastrería / mesa de
  tinturas), hoy sin implementar. Cuando eso exista, se borra
  `reformarRemera` y no se toca ni el dato ni el render.
- **Render** (`EstampaTextures.aplicarPatron`, nuevo): pinta la máscara
  DESPUÉS del tenido y ANTES de la estampa. Reusa **una sola máscara para
  los 36 cortes** (tamaño del corte más grande) en vez de generar 36 — se
  recorta sola contra el alfa de cada corte, así un croptop nunca ve patrón
  más allá de su propio ruedo. `cuerpoEstampado` ganó dos parámetros
  (`patronId`, `patronColor`) y la clave de cache los incluye.
- **Tooltip**: `ClothingTrinketItem.patternLine` pasó a público y
  `RemeraItem` lo reusa (`Lado.AMBAS`, sin side-key) en vez de reimplementar
  el mismo texto.
- `ClothingTextureCache.tintPixel` pasó a público: es la misma cuenta ABGR
  que ya usaba `composeGarment`, sin motivo para duplicarla.
- **Queda afuera a propósito**: el ÍCONO de remera no muestra el patrón —
  necesita una segunda capa tintada en cada uno de los 36 modelos de corte,
  trabajo de arte. Y falta el PNG de la máscara en sí
  (`textures/models/armor/patterns/remera/*.png`); sin él cae a la prenda
  tenida lisa, como cualquier máscara faltante del mod.
- FEMCLOTHES.md: tabla del telar con las filas nuevas, sección de la
  máscara compartida, y tres ítems nuevos en "Lo que falta".

**Próximo:** sin cambios de fondo — migrar shorts sigue primero. Nada de
esto se probó puesto en el juego (sigue siendo cierto para TODA la fase 1,
no es nuevo de esta entrada).

**Para el otro Claude:**
- Si tocás `EstampaTextures`, la resolución de tela (2×/4×/8× de v7) sigue
  SIN aplicarse ahí a propósito — quedó explícitamente afuera por lo frágil
  del compositor, y esta entrada tampoco la tocó.
- El "puño que no se pinta" que el dueño señaló en v7 sigue sin
  investigarse. No lo toqué en esta entrada tampoco; sigue pendiente.
- Si armás la Estación de tintes de verdad: `reformarRemera` es exactamente
  la lógica a migrar (molde/patrón/tinte), y `RegionResolver`/
  `EstampaTextures.aplicarPatron` NO deberían necesitar tocarse — el dato y
  el render ya son genéricos, es la UI la que hoy vive en el lugar
  equivocado.

## v7 — 2026-09-06 — cuenta de H0p3san · Claude Code (extensión de VSCode)

**Hecho:** tres resoluciones de tela en vez de una, a pedido del dueño ("para
minimizar el impacto en servers"). Aclarado además un malentendido propio en
v6: `CuerpoGeometria.ESCALA_*` **no controla el detalle real de nada** — la
fracción de UV de un cuboide se cancela sola respecto de `S` (verificado
línea por línea contra `ModelPart$Cuboid` de vanilla). El ahorro de VRAM no
sale de re-declarar geometría a otra escala — eso es cosméticamente inerte —
sino de encoger la imagen YA COMPUESTA antes de subirla a la GPU. Por eso
`CuerpoGeometria.Superficie`/`Pieza`/`GarmentFeatureRenderer` **no se
tocaron**: el cambio quedó contenido en `ClothingTextureCache`.

- `CuerpoGeometria`: `ESCALA_TELA_LISA = 2`, `ESCALA_TELA_PATRON = 4`, junto a
  `ESCALA_TELA = 8` (sin cambios). Son constantes de COMPOSICIÓN, no de
  geometría — las usa `ClothingTextureCache`, no `raiz()`.
- `ClothingTextureCache.reducirSiHaceFalta`: compone SIEMPRE a resolución
  nativa (8×) — el sombreado por cara y el recorte de estampa no se tocan —
  y recién al final encoge con `NativeImage.resizeSubRectTo` (STB, no
  vecino-más-cercano) si el resultado no tiene ni patrón ni foto. El nivel
  sale gratis de datos que la función YA recibía (`maskImg != null` →
  patrón, `encima != null` → estampa), cero parámetros nuevos.
- **Remera (`EstampaTextures`) queda AFUERA a propósito.** Es un compositor
  separado con su propia matemática (lienzo virtual, tablas de caras, full
  print) que `SUBLIMADORA.md` marca como frágil, y el dueño señaló que
  todavía pinta el puño cuando la doc dice que no debería — señal de que ese
  pipeline tiene deuda propia sin resolver antes de tocarlo.
- FEMCLOTHES.md: nueva sección "Tres resoluciones de tela, no una" +
  corregida la afirmación de v6 sobre qué controla `ESCALA_*`.

**Próximo:** sin cambios de fondo respecto de v6 — migrar shorts sigue
primero, y nada de esto se probó puesto en el juego. Si alguien retoma la
resolución de tela para remera: primero investigar el puño (por qué se
pinta si la doc dice que no), después decidir si `EstampaTextures` necesita
su propio `reducirSiHaceFalta` o si conviene esperar a fusionar los dos
compositores.

**Para el otro Claude:** si vas a tocar resolución de texturas, la lección
de esta entrada es la que importa: **medí qué controla cada número antes de
asumirlo**. `CuerpoGeometria.ESCALA_*` se lee como si fuera "la resolución",
pero la resolución real la decide quien arma el `NativeImage` de salida —
`CuerpoBaseTextures`/`ClothingTextureCache` — no la geometría. Si algo
similar te hace ruido, verificalo contra el `.java` de vanilla antes de
construir encima, como acá.

## v6 — 2026-09-06 — cuenta de H0p3san · Claude Code (extensión de VSCode)

**Hecho:** corregido un desperdicio que el dueño detectó al leer v5: el
cuerpo base compartía la escala 8× de la tela (`CuerpoGeometria.ESCALA`) sin
motivo. La tela va a 8× porque se sublima —una foto necesita resolución—,
pero el cuerpo nunca recibe foto ni patrón, nada más fino que un pixel de
skin, así que 8× ahí eran 64 veces los texels para pintar lo mismo (512×512
en vez de 64×64 por cuerpo).

- `CuerpoGeometria.ESCALA` se partió en `ESCALA_TELA` (8, sin cambios) y
  `ESCALA_CUERPO` (1, nuevo) dentro de un enum `Superficie` que junta escala +
  dilatación por superficie (evita pedir una combinación que no exista).
- Verificado por qué las dos escalas pueden diferir sin desalinear nada: en
  `ModelPart.Cuboid` la fracción de UV de una cara es `(tamaño×S)/(64×S)`, la
  misma para cualquier `S` — confirmado leyendo el .java de vanilla
  (`ModelPart$Cuboid`, líneas ~252-257). La escala solo decide cuántos texels
  caen adentro, nunca dónde cae el borde.
- `CuerpoBaseTextures` ahora compone a 64×64. `GarmentFeatureRenderer` pasa
  `Superficie.CUERPO`/`Superficie.TELA` en vez de una dilatación suelta.
  `ClothingTextureCache` y `EstampaTextures` (que trabajan con TELA) solo
  cambiaron el nombre de la constante, ningún número.
- De yapa: el arte del cuerpo (`textures/entity/cuerpo/*.png`) pasa a ser una
  skin común de 64×64 pintable en cualquier editor de skins, y de 1 MB a
  16 KB por cuerpo.
- FEMCLOTHES.md y PRENDAS.md §7 actualizados (la v5 decía "8×" para el
  cuerpo, que ya no es cierto).

**Próximo:** sin cambios respecto de v5 — migrar shorts sigue siendo lo
primero, y sigue sin probarse puesto en el juego.

**Para el otro Claude:** si tocás `CuerpoGeometria`, las dos escalas viven en
el enum `Superficie` (`CUERPO` y `TELA`) junto con su dilatación — no vuelvas
a separarlas en constantes sueltas, es lo que evita pedir una combinación que
no tiene sentido (p. ej. tela a la dilatación del cuerpo).

## v5 — 2026-09-06 — cuenta de H0p3san · Claude Code (extensión de VSCode)

**Hecho:** **FASE 1 IMPLEMENTADA** — primer código del track de prendas. Compila
y el cliente arranca limpio (63 mods, sin excepciones nuestras).

- **Sistema de capas (§1)**: `GarmentFeatureRenderer` es AHORA EL ÚNICO punto
  de dibujo de toda la ropa del mod, enganchado con
  `LivingEntityFeatureRendererRegistrationCallback` (hook público de Fabric,
  sin Mixins). Por parte del cuerpo: cuerpo base a 0.30, después las piezas
  ordenadas por `Capa`, todas a 0.32. Piezas nuevas: `Parte`, `Capa`,
  `Garment`/`Garments`/`PrendasDelMod` (servidor), `Pieza`/`PiezasDePrenda`/
  `PiezasDelMod` (cliente).
- **Cuerpo base (§7)**: `PerfilCuerpo` (cuerpo · tono · ropa interior) como
  attachment de Fabric, persistente, `copyOnDeath`, sincronizado a TODOS (los
  demás también te dibujan). `CuerpoBaseTextures` lo compone en layout de skin
  a 8×. Default = `SKIN_REAL` con tono derivado de tu propia skin: ponerse una
  prenda NO te cambia el cuerpo.
- **RegionResolver (§3)**: `Lado`/`Cara`/`Region`/`Operacion`/`Orientacion`.
  Absorbió `ClothingStyle`, que ya no existe. El telar dejó su enum `Target` y
  usa `Lado`. Componente nuevo `femclothes:orientacion`.
- **Se fueron**: `ClothingStyle`, `BodyPartTrinketRenderer`,
  `RemeraTrinketRenderer`, `TrinketsClientCompat`. La fusión de los dos
  renderers (punto 3 del roadmap) se adelantó porque el ordinal la exige.
- **Se simplificó**: `SkinRegions` pasó de tabla de rectángulos a mano +
  reconstrucción de piel a solo "qué overlay borrar", `f(partes)` y ya no
  `f(prenda, corte)`. `ComposedSkin` ya no repinta piel. `SkinToneSampler`
  perdió `Tones`/`Paleta`. `composeGarment` perdió el parámetro de piel: lo
  que la prenda no cubre queda TRANSPARENTE, que es el contrato del sistema.
  `LayoutSkin`/`CajaSkin` calculan el desdoblado en vez de tenerlo escrito.
- Docs: `FEMCLOTHES.md` y `PRENDAS.md` actualizados con lo hecho y lo que no.

**Próximo:** lo que quedó explícitamente afuera de la fase 1, en este orden:
1. **Migrar shorts** — es la primera prueba REAL del layering (short sobre
   media) y ya no está bloqueado. Hasta que eso ande, el sistema de capas
   está implementado pero no ejercitado.
2. **GUI de primera interacción** del cuerpo base. Hoy se cambia solo con
   `/femclothes cuerpo|tono|interior|reset|ver`.
3. **Arte del cuerpo base**: `textures/entity/cuerpo/<id>.png` (mapa de
   sombras, se multiplica) e `interior_<ropa>.png` (encima). Los dos son
   OPCIONALES: sin ellos sale el cuerpo liso sombreado por cara, o sea lo
   mismo que antes — pero los cinco cuerpos del set se ven idénticos.
4. `RegionPickerWidget` y colapsar `Estampa.Cara` en `region.Cara`.

**Para el otro Claude:**
- **NADA de esto se probó puesto en el juego.** Compila y el cliente llega al
  menú sin excepciones; equipar una prenda y mirarla no lo pude hacer desde
  acá. Lo primero que hay que hacer es entrar con medias y con remera y mirar.
  Sospechas concretas a verificar: que el cuerpo base no asome por el borde de
  la media, que la musculosa muestre brazo y no la manga pintada de la skin, y
  que el torso no pierda la cintura del pantalón.
- ⚠️ **`Garment.partes()` es un arma de doble filo**: declarar una parte hace
  que se le dibuje cuerpo base Y se le borre la capa externa de la skin. Si la
  prenda no tiene tela ahí, le estás borrando el pantalón pintado al jugador a
  cambio de nada. Por eso el remerón NO declara las piernas todavía, aunque el
  diseño diga que las va a usar. Declarar la parte recién cuando haya tela.
- ⚠️ **Toda la tela va a la MISMA dilatación (0.32)**. Poner una por capa deja
  un "anillo de árbol" en la silueta. Solo el cuerpo va adentro (0.30).
- El bug histórico de "teñir una pierna tiñe las dos" ya no se puede pisar:
  `fijarDerecha()` lo llaman los propios setters de `RegionResolver`. No
  vuelvas a exponerlo como algo que el que llama tiene que acordarse.
- Del canal v1 sigue todo vigente: nada de re-litigar las decisiones cerradas.

## v4 — 2026-09-06 — cuenta de H0p3san · Claude Code (app de escritorio, sesión patchouli)

**Hecho:** `PRENDAS.md` §7 y §8 nuevas, roadmap renumerado a §9:
- **§7 Cuerpo base**: reemplaza `SkinToneSampler`/`ComposedSkin`/`SkinRegions`
  reconstruyendo piel. Set curado de cuerpos (plano/abs/curvy/binder), tono
  paramétrico por rampa, ropa interior baked o via slots `*/interior`, GUI
  de primera interacción, componente persistente. "Usar mi skin real" queda
  como opción. **Es parte de fase 1** — el sustrato ES el cuerpo base.
- **§8 3D Skin Layers**: las prendas ajustadas se **componen en la capa
  externa de la skin** y 3DSL las extruye (mismo estilo voxel). Las flojas
  ya son geometría, solo estilarlas blocky. 3DSL pasa de compat a camino de
  render primario; `SkinLayersCompat`/`HttpTextureAccessor` se vuelve
  crítico. Costo: recompón + remesh async/debounce. Fase 1 tiene que
  anticiparlo.

**Próximo:** **fase 1 = sistema de capas + cuerpo base**, diseñado
anticipando la ruta 3DSL. El diseño de fase 1 ya está cerrado en el doc.

**Para el otro Claude:** fase 1 ahora incluye el cuerpo base (§7), no solo
el ordinal. No implementes el sustrato como "reconstrucción de piel" — es
directamente el cuerpo base elegido. Y dejá la puerta abierta a componer la
tela en la capa externa (§8) sin tener que rediseñarlo.

## v3 — 2026-09-06 — cuenta de H0p3san · Claude Code (app de escritorio, sesión patchouli)

**Hecho:** ampliado `PRENDAS.md`:
- §1: el **torso también es multi-capa** (`torso/interior` binder/corpiño +
  `torso/exterior`) — se revierte la decisión vieja de slot único, el
  sistema de capas la hace innecesaria. La **zona de piel reconstruida
  sigue al corte** (`SkinRegions` = `f(prenda, variante)`, no fija). Mapa de
  regiones y slots (cabeza/torso/brazos/piernas/pies).
- §3: **rotar la prenda entera** — `orientacion {girado, espejado}` en
  `variante`, no destructivo, aplicado por el `RegionResolver` al resolver.
  Espejar por capa vs rotar todo.
- §4: cuello ahora incluye **escote** (V/scoop/plunge, misma máscara más
  profunda). **Mangas/armwarmers = un garment con eje de banda** (armwarmers
  = preset, no prenda). **Botamanga = eje del pantalón**, no slot.
  **Calzado = slot nuevo `pies`**.
- §6: `femclothes:variante` **agnóstico de prenda** (`Map<Eje,Valor>`,
  `ejes()` por prenda, moldes de eje universales, molde de corte = spec
  parcial, migración del formato viejo). Patrones de tinte: **agrupar por
  topología de región** + largo plazo **proyector body-space**. **Combinar
  patrones** (lista de 3-4 capas). Rotar máscaras: no.

**Próximo:** sin cambios — **fase 1 (sistema de capas)** sigue siendo el
arranque. El diseño de fase 1 ya está bastante cerrado: sustrato de piel +
ordinal, torso y piernas multi-capa, `SkinRegions` paramétrico por corte.

**Para el otro Claude:** el diseño está grande pero **fase 1 es acotada** —
no te dejes abrumar por §4-§6, eso es después. Fase 1 = §1 + el
`RegionResolver` de §3. Nada más para empezar.

## v2 — 2026-09-06 — cuenta de H0p3san · Claude Code (app de escritorio, sesión patchouli)

**Hecho:**
- `PRENDAS.md` §3 nuevo: **Selector de región** — normaliza el
  izq/der/frente/espalda que hoy está disperso (telar cicla, sublimadora
  selector físico, componentes `right_*` vs `estampa_frente`). Enums `Lado`
  y `Cara`, `regionesDe(op)` por prenda, un `RegionResolver` que absorbe
  `ClothingStyle`, un `RegionPickerWidget`, la trampa de que `AMBAS` no es
  uniforme (teñir borra override / estampar cuesta por cara). Secciones 3-7
  renumeradas.

**Próximo:** sin cambios respecto de v1 — fase 1 (sistema de capas) sigue
siendo el arranque del track de prendas.

**Para el otro Claude:** el `RegionResolver` y el `RegionPickerWidget` son
transversales a las dos mesas nuevas y a la sublimadora. Si tocás cualquier
UI de estación o el per-lado de medias, pasá por ahí, no dupliques.

## v1 — 2026-09-06 — cuenta de H0p3san · Claude Code (app de escritorio, sesión patchouli)

**Hecho:**
- Escritos `docs/PRENDAS.md` y `docs/MAQUINAS.md` con todo el diseño charlado:
  sistema de capas (sustrato de piel + ordinal), modelo prenda =
  `(región, fuente de geometría, capa)`, cómo construir los 36 cortes,
  polleras/pantalones acampanados/babuchas via `FLARE_MESH` paramétrico
  anclado a un hueso de pelvis, mesa de tinturas + mesa de sastrería,
  auto-io modular, convención de caras de máquina.
- `FEMCLOTHES.md` ahora apunta a los dos docs nuevos desde el encabezado.
- Creado este canal.
- **Nada de código tocado.** Todo es diseño.

**Próximo:**
- **Fase 1 de `PRENDAS.md`**: sistema de capas (sustrato de piel separado +
  ordinal `layer`). Es el desbloqueo de todo el track de prendas.
- El track de máquinas (`MAQUINAS.md`) es independiente y se puede arrancar
  en paralelo por la mesa de tinturas.

**Para el otro Claude:**
- Antes de implementar nada, leé `FEMCLOTHES.md` + `PRENDAS.md` +
  `MAQUINAS.md` enteros. Las trampas de render están marcadas ahí y en
  `SUBLIMADORA.md`.
- Decisiones ya cerradas con el dueño, no las re-litigues: 2 mesas nuevas
  (tinturas + sastrería) modulares, no una fusionada; tinte de 16 colores
  discretos, **no** mezclador CMYK; GUI de config rara + aplicación por
  click derecho; se saca el hook del telar; `largo` de remera = B1 (ceñido),
  el oversize que flota es prenda aparte.
- Pendientes viejos del mod (sección "Lo que falta" de `FEMCLOTHES.md`)
  siguen vigentes: arte de verdad, ícono de medias estampadas, la prenda
  sobre la plancha de la sublimadora no muestra estampa/tinte.
