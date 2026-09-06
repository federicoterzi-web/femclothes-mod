<!-- canal-version: 5 -->
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
