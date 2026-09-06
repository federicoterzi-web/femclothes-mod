<!-- canal-version: 4 -->
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
