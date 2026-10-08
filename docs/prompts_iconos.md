# Prompts de íconos para generar con GPT (ChatGPT Plus) — TODOS los ítems

Todos los íconos de ítem van a **64×64 px**, PNG con fondo propio (el papel), sin transparencia salvo donde se aclare.
Generalos grandes (1024×1024) y bajalos a 64×64 con LANCZOS (`tools/armar_iconos_assets.py` y
`tools/ajustar_iconos_medias_pantalon.py` ya hacen ese tipo de recorte). **Probá primero con 2 o 3 y ajustá el estilo
antes de pedir el resto.** Como referencia de estilo, mirá `docs/referencias_iconos.png` (8 que ya están bonitos).

## Estilo base (pegalo al principio de cada pedido)

> Ícono de videojuego estilo "taller de sastrería", vista frontal, centrado, margen del 10 % alrededor. Fondo: una
> hoja de papel kraft anaranjado plegada en cruz, con cuatro zonas de luz planas (como papel doblado en cuatro) y
> pliegues finos. Sobre el papel, el dibujo del tema en trazo oscuro marrón de contorno grueso y colores planos
> cálidos, sombreado simple de 2 tonos, sin degradés finos ni texto. Marco redondeado fino marrón oscuro. Cuadrado,
> legible a 64×64 píxeles. Estilo cohesivo con una serie de ~150 íconos: mismo papel, mismo grosor de línea, misma paleta.

Paleta base: papel `#D9A066`, luz `#E8B982`, sombra de pliegue `#B9824A`, contorno `#4A2E1B`, acentos: crema `#F2E2C4`,
rojo cuero `#A3263A`, dorado `#D6B05C`.

## Color de destaque por máquina

Cada ítem se usa en una máquina del mod, y esa máquina tiene su color. **El ícono lleva ese color como destaque**: el filo
del marco redondeado y UN detalle chico del dibujo (una chincheta, la puntada, la costura o la flecha), nunca el fondo
entero. Pasalo en el pedido como "color de destaque: …".

| Máquina | Color de destaque | Claro / oscuro |
|---|---|---|
| Mesa de Modelado (cortes, moldes de forma) | **cobre** | `#F2AA78` / `#602C16` |
| Estación de Tintes (patrones, muestras) | **verdín** | `#96D4AA` / `#1C4A38` |
| Sublimadora (máscaras de estampa) | **oro** | `#FFE476` / `#7A5608` |
| Estilista / Mesa de estilado (apliques, correas, texturas) | **lila** | `#D6AAE8` / `#462260` |
| Telar automático (prendas básicas) | **granate** | `#C8485E` / `#4A0E1A` |
| Guardarropas | **plata** | `#E6EAF0` / `#424854` |
| Maniquí | **azul esmaltado** | `#96B8E6` / `#1E345A` |

Estado: **OK** = ya tiene arte bueno (se puede rehacer para unificar el destaque), **PROVISORIO** = hoy es una copia o un
dibujo de código y hay que hacerlo, **NUEVO** = ítem reciente.
Ruta de cada archivo: `src/main/resources/assets/modamod/textures/item/<archivo>.png`.

---

## A. Mesa de Modelado — destaque cobre

### A1. Cuello (6) — torso con el cuello cortado
`molde_cuello_redondo` (escote redondo, el de fábrica; OK), `molde_cuello_v` (escote en V; OK), `molde_cuello_polera`
(cuello alto; OK), `molde_cuello_cuadrado` (escote cuadrado; OK), `molde_cuello_corazon` (escote en corazón; OK),
`molde_cuello_camisa` (cuello de camisa con dos puntas y V chico; PROVISORIO).

### A2. Rango (7) — pierna/media o manga con una línea de corte a distinta altura
`molde_rango_cero` (línea arriba de todo), `molde_rango_minimo`, `molde_rango_corto`, `molde_rango_medio`,
`molde_rango_mediolargo`, `molde_rango_largo`, `molde_rango_maximo` (línea abajo de todo). OK los siete.

### A3. Calce (5) — pantalón con flechas hacia adentro o afuera
`molde_calce_pegado` (flechas hacia adentro, juntas), `molde_calce_ajustado`, `molde_calce_normal`, `molde_calce_suelto`,
`molde_calce_oversize` (flechas bien hacia afuera). OK.

### A4. Trama / red (11) — un trozo de tela con el calado
`molde_red_lisa`, `molde_red_fina`, `molde_red_gruesa`, `molde_red_hexagonal` (panal), `molde_red_perforada`
(agujeritos), `molde_red_encaje`, `molde_red_escocesa` (tartán), `molde_red_rayas`, `molde_red_arnes_bandas` (arnés de
bandas), `molde_red_arnes_tirantes` (arnés de tirantes), `molde_red_arnes_x` (arnés cruzado en X). OK.

### A4b. Materiales (2) — NUEVOS, PROVISORIOS (hoy copia de la trama escocesa)
`molde_material_denim` (un retazo de tela vaquera azul con la sarga diagonal visible, costura de contraste naranja y un borde deshilachado) y `molde_material_cuero` (un retazo de cuero marrón con grano de poros, un brillo suave en el medio y una costura crema). Mismo papel y destaque cobre que las tramas.

### A5. Pollera (5 formas) + volados (2) + ruedo (6)
Falda de frente, cada una con su silueta: `molde_pollera_campana`, `molde_pollera_tableada` (con tablas),
`molde_pollera_tubo` (recta y ajustada), `molde_pollera_globo` (ancha al medio, cerrada abajo), `molde_pollera_circular`
(muy abierta, vuelo grande). OK.
Volados: `molde_volado_recto` (volante recto; OK), `molde_volado_circular` (volante en ondas; OK).
**Ruedo (NUEVO, reemplaza a los viejos bordes de pollera y remates de chaqueta):** borde de abajo de una prenda genérica en
primer plano:

| Archivo | Dibujo |
|---|---|
| `molde_ruedo_recto` | borde liso y recto con una costura simple |
| `molde_ruedo_ajustado` | banda elástica acanalada (canales verticales) que aprieta la tela |
| `molde_ruedo_campana` | borde que se abre hacia afuera en campana |
| `molde_ruedo_ondulado` | borde ondulado suave |
| `molde_ruedo_festoneado` | festones (semicírculos repetidos) |
| `molde_ruedo_pico` | picos triangulares (dientes de sierra) |

### A6. Capa (7)
`molde_capa_ruedo_recto`, `molde_capa_ruedo_redondeado`, `molde_capa_ruedo_cola` (con cola larga arrastrando),
`molde_capa_cuello_alto` (OK los cuatro); `molde_capa_con_capucha` (capa con capucha puesta), `molde_capa_sin_capucha`
(cuello liso), `molde_capa_sin_cuello` (borde de cuello recto y bajo) — estos tres PROVISORIOS.

### A7. Chaqueta / Top (8) — PROVISORIOS (hoy copias del ancho de banda)
Campera de frente: `molde_chaqueta_frente_cerrada` (cierre vertical completo), `molde_chaqueta_frente_abierta` (franja
del medio vacía, con las dos orillas marcadas), `molde_chaqueta_capucha_con` (capucha caída detrás del cuello),
`molde_chaqueta_capucha_sin` (cuello simple y una cruz roja chica), `molde_chaqueta_solapa_ninguna` (cuello de saco liso y
cruz roja chica), `molde_chaqueta_solapa_pico` (solapas en pico), `molde_chaqueta_solapa_redonda` (solapas redondeadas
anchas), `molde_chaqueta_solapa_chal` (esmoquin con solapa chal larga y curva).

### A8. Sombrero (4) — PROVISORIOS o por rehacer
`molde_sombrero_ala_ancha`, `molde_sombrero_ala_corta` (sombrero de lado con el ala marcada), `molde_sombrero_punta_recta`
(cono derecho), `molde_sombrero_punta_doblada` (cono con la punta caída). Habrá **sombreros paramétricos**: que admitan serie.

### A9. Banda: cintos y chokers (8) — PROVISORIOS
`molde_banda_zona_cintura` (torso con un cinto), `molde_banda_zona_cuello` (cuello con un choker),
`molde_banda_ancho_fino` / `_medio` / `_ancho` (tres tiras de cuero de distinto grosor), `molde_banda_herraje_ninguno`
(tira lisa con cruz roja), `molde_banda_herraje_placa` (placa dorada), `molde_banda_herraje_aro` (aro dorado).

### A10. Carpeta de corte
`molde_de_corte` (carpeta de papel kraft con tijera; OK). `molde_largo` y `molde_manga` están jubilados (no hace falta).

---

## B. Estación de Tintes — destaque verdín

`pattern_corazones` y `pattern_estrellas` (OK; motivo repetido adentro de un aro de bordado de madera, **sin papel de fondo**),
`pattern_lunares` (puntos) y `pattern_vichy` (cuadros chicos tipo mantel) — PROVISORIOS, mismo aro de bordado.
`pattern_stripe_alt`, `pattern_stripe_top`, `pattern_triple_stripe` (rayas: alternadas, arriba, triple; en el aro).
`tinte_mezcla` (frasco de vidrio vacío con corcho; OK) y `tinte_mezcla_liquido` (el líquido, en **escala de grises** porque
el juego lo tiñe con la mezcla; OK).

## C. Sublimadora — destaque oro

Máscaras de estampa, una forma recortada con un pedacito de foto adentro (un cuadradito de paisaje), PROVISORIOS:
`molde_mascara_cuadrado`, `molde_mascara_franja` (tira horizontal), `molde_mascara_circulo`, `molde_mascara_estrella`,
`molde_mascara_triangulo`.

## D. Estilista / Mesa de estilado — destaque lila

**Apliques (5):** `molde_aplique_mono` (moño rojo, OK), `molde_aplique_mariposa` (mariposa violeta, OK), `molde_aplique_flor`
(flor rosa, OK), `molde_aplique_canguro` (bolsillo canguro: parche de tela con las dos bocas en diagonal y costura abajo; PROVISORIO),
`molde_aplique_corbata` (corbata con nudo y hoja ancha hacia la punta; PROVISORIO), sobre el papel.
**Correas (5, PROVISORIOS):** `molde_correa_lisa` (tira de cuero), `molde_correa_cadena` (cadena gruesa dorada),
`molde_correa_cadena_fina`, `molde_correa_ojalillos` (tira con agujeros y ojalillos metálicos), `molde_correa_cordon`
(cordón trenzado con puntas).
**Texturas de tela (2, OK por ahora):** `molde_textura_fruncido`, `molde_textura_acolchado`.
**Retazo de aplique (1, PROVISORIO):** `retazo_aplique`, un retazo de tela chico, cuadrado, con el borde deshilachado y una
puntada, doblado una vez, **casi en escala de grises / crema** porque el juego lo tiñe con el color de cada zona; sin papel
de fondo.

## E. Prendas (se ven en el inventario; el juego dibuja encima la tela real) — destaque según su máquina

Estos son de respaldo: el ícono de la prenda real lo arma el código con la textura. Solo hace falta si querés renovarlos.
`remera`, `pantalon`, `calientabrazos`, `socks_solid`, `socks_solid_pattern`, `socks_34`, `fishnet_socks`, `banda`
(cinto de cuero con hebilla), `sombrero_bruja` (sombrero de bruja entero, puntiagudo, con cinta; hoy lo dibuja un script
por código: **hay que rehacerlo**), `oversized_hoodie`, `maid_outfit`, `estrogenos` (frasquito con hormona).

## Cómo seguir

1. Probá el estilo base con 3 íconos de tipos distintos (un cuello, una pollera, una trama) y comparalos con
   `docs/referencias_iconos.png`.
2. Ajustá el "color de destaque" hasta que se note sin pisar el papel.
3. Pedí por tandas de una máquina a la vez, para que el destaque quede parejo.
4. Bajá a 64×64, pegá en la ruta de arriba y avisame: los íconos de molde de aplique y los de prenda no necesitan más trabajo.
