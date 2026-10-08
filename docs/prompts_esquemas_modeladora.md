# Prompts de los esquemas de la Modeladora (para GPT / Claude Design)

Los esquemas son el dibujo de fondo de cada pestaña de la **Mesa de Modelado**: una ilustración de la prenda con **marcos para los casilleros** de los moldes. El código dibuja los ítems y las chinchetas encima, así que el arte tiene que dejar los marcos vacíos **exactamente donde dice la tabla**.

## Reglas comunes (pegalas en todos los pedidos)

- **Tamaño:** 960 × 544 px (proporción 1,76; el juego lo muestra a 240×136). Archivo PNG, ruta `src/main/resources/assets/modamod/textures/gui/container/esquema_<prenda>.png`.
- **Estilo:** igual que los esquemas actuales: pergamino/papel de patronista color crema `#E8D2A4`, dibujo de la prenda en trazo marrón `#4A2E1B` de línea gruesa y colores planos cálidos, vista **de frente** centrada, estilo "taller de sastrería". **Color de destaque cobre** (`#F2AA78` claro / `#602C16` oscuro) en el filo de los marcos y en las líneas guía.
- **Marcos de casillero:** cuadrados redondeados de **104 × 104 px** (26 px en el juego), fondo marrón oscuro `#3A2614` hundido (el ítem de 16 px se dibuja adentro), borde cobre. **Centrados en las coordenadas de la tabla** (las coordenadas están en el espacio del juego 240×136; multiplicalas **×4** para el PNG).
- **Líneas guía:** de cada marco sale una línea fina cobre hasta la parte de la prenda que controla (con un puntito en la prenda), como en un esquema técnico. Sin texto: los rótulos los pone el juego.
- **No dibujes** marcos ni nada en los puntos marcados **CAJÓN**: esos casilleros se despliegan con un botón y el juego les dibuja el marco. Dejá esa zona limpia (papel liso).
- **Nada de chinchetas** dibujadas (el juego las pone).
- Zonas ocupadas por la interfaz del juego que **no** deben tener marcos ni detalles importantes: ninguna dentro del esquema; el esquema es todo el rectángulo.

Cómo leer las tablas: `x, y` = **centro** del marco en el espacio 240×136 (PNG = ×4).

## Remera / Top

`esquema_remera.png`. Una remera de frente con mangas largas, cuello y capucha caída; sirve para remera, hoodie y saco. Los marcos de Cuello, Solapa y Capucha van arriba, las mangas a los costados, el torso abajo.

| Casillero | Centro (juego) | Centro (PNG ×4) |
|---|---|---|
| Cuello | (121, 19) | (484, 76) |
| Trama | (66, 23) | (264, 92) |
| Manga izq. | (50, 56) | (200, 224) |
| Manga der. | (191, 56) | (764, 224) |
| Calce | (65, 99) | (260, 396) |
| Largo del torso | (121, 121) | (484, 484) |
| Ruedo del torso | (175, 118) | (700, 472) |
| Puño izq. (CAJÓN) | (38, 82) | (152, 328) ⬅ **CAJÓN: sin marco** |
| Puño der. (CAJÓN) | (198, 86) | (792, 344) ⬅ **CAJÓN: sin marco** |
| Frente | (215, 32) | (860, 128) |
| Capucha | (25, 32) | (100, 128) |
| Solapa (CAJÓN) | (175, 23) | (700, 92) ⬅ **CAJÓN: sin marco** |

## Pantalón

`esquema_pantalon.png`. Pantalón de frente, piernas separadas, con la cintura marcada.

| Casillero | Centro (juego) | Centro (PNG ×4) |
|---|---|---|
| Tiro | (120, 18) | (480, 72) |
| Trama | (120, 51) | (480, 204) |
| Calce | (55, 67) | (220, 268) |
| Corte botamanga izq. | (57, 115) | (228, 460) |
| Corte botamanga der. | (183, 116) | (732, 464) |
| Ruedo bota izq. (CAJÓN) | (83, 115) | (332, 460) ⬅ **CAJÓN: sin marco** |
| Ruedo bota der. (CAJÓN) | (157, 116) | (628, 464) ⬅ **CAJÓN: sin marco** |

## Medias

`esquema_medias.png`. Dos medias largas de frente, una a cada lado, abiertas en el centro; el pin de la izquierda del dibujo es el lado DERECHO del jugador.

| Casillero | Centro (juego) | Centro (PNG ×4) |
|---|---|---|
| Corte superior izq. | (52, 24) | (208, 96) |
| Corte superior der. | (188, 24) | (752, 96) |
| Corte inferior izq. | (51, 114) | (204, 456) |
| Corte inferior der. | (192, 114) | (768, 456) |
| Calce | (120, 114) | (480, 456) |
| Trama izq. | (36, 48) | (144, 192) |
| Ruedo sup. izq. (CAJÓN) | (78, 24) | (312, 96) ⬅ **CAJÓN: sin marco** |
| Ruedo inf. izq. (CAJÓN) | (77, 114) | (308, 456) ⬅ **CAJÓN: sin marco** |
| Trama der. | (204, 48) | (816, 192) |
| Ruedo sup. der. (CAJÓN) | (162, 24) | (648, 96) ⬅ **CAJÓN: sin marco** |
| Ruedo inf. der. (CAJÓN) | (166, 114) | (664, 456) ⬅ **CAJÓN: sin marco** |

## Calientabrazos

`esquema_calientabrazos.png`. Dos calientabrazos (manguitos) de frente, uno a cada lado del centro.

| Casillero | Centro (juego) | Centro (PNG ×4) |
|---|---|---|
| Corte superior izq. | (46, 27) | (184, 108) |
| Corte superior der. | (193, 27) | (772, 108) |
| Corte inferior izq. | (45, 106) | (180, 424) |
| Corte inferior der. | (195, 107) | (780, 428) |
| Calce | (120, 116) | (480, 464) |
| Trama izq. | (36, 49) | (144, 196) |
| Ruedo sup. izq. (CAJÓN) | (72, 27) | (288, 108) ⬅ **CAJÓN: sin marco** |
| Ruedo inf. izq. (CAJÓN) | (71, 106) | (284, 424) ⬅ **CAJÓN: sin marco** |
| Trama der. | (204, 49) | (816, 196) |
| Ruedo sup. der. (CAJÓN) | (167, 27) | (668, 108) ⬅ **CAJÓN: sin marco** |
| Ruedo inf. der. (CAJÓN) | (169, 107) | (676, 428) ⬅ **CAJÓN: sin marco** |

## Pollera

`esquema_pollera.png`. Pollera de frente con cintura y vuelo.

| Casillero | Centro (juego) | Centro (PNG ×4) |
|---|---|---|
| Forma | (61, 27) | (244, 108) |
| Largo | (47, 106) | (188, 424) |
| Calce | (47, 67) | (188, 268) |
| Trama | (189, 38) | (756, 152) |
| Volado inferior | (192, 116) | (768, 464) |
| Volado total | (25, 41) | (100, 164) |
| Ruedo | (82, 116) | (328, 464) |

## Capa

`esquema_capa.png`. Capa de frente con capucha, cuello alto y ruedo.

| Casillero | Centro (juego) | Centro (PNG ×4) |
|---|---|---|
| Largo | (198, 72) | (792, 288) |
| Ruedo | (56, 100) | (224, 400) |
| Capucha | (183, 26) | (732, 104) |
| Cuello | (56, 28) | (224, 112) |
| Trama | (56, 61) | (224, 244) |

## Cinto / choker (Banda) — HAY QUE REHACERLO

`esquema_banda.png`. Hoy es un dibujo provisorio. Pedí algo así: **un torso de maniquí de frente con un cinto de cuero con hebilla en la cintura, y arriba un cuello con un choker (banda ceñida con un colgante)**, más al costado una **tira de cuero desenrollada** que muestre el ancho (fino / medio / ancho) y una **hebilla/placa y un aro metálico** para el herraje. Estilo y marcos como arriba.

| Casillero | Centro (juego) | Centro (PNG ×4) | Qué señala |
|---|---|---|---|
| Zona (cinto o choker) | (52, 82) | (208, 328) | línea al torso, entre la cintura y el cuello |
| Ancho | (190, 30) | (760, 120) | línea a la tira de cuero desenrollada |
| Herraje | (187, 63) | (748, 252) | línea a la hebilla y al aro |

(Si preferís otra distribución, avisame las coordenadas finales y las pego en el código.)

## Sombrero (genérico) — HAY QUE REHACERLO

`esquema_sombrero.png`. No es el sombrero de bruja: dibujá **un sombrero genérico de ala, de frente, flotando sobre una cabeza de maniquí** (copa redondeada o cilíndrica, ala visible y una cinta alrededor de la copa), del tipo base que después se modifica (bruja, copa, vaquero, boina...). Marcá con líneas guía las dos partes que se eligen: el **ala** y la **copa/punta**. Estilo y marcos como arriba.

| Casillero | Centro (juego) | Centro (PNG ×4) | Qué señala |
|---|---|---|---|
| Ala | (51, 82) | (204, 328) | línea al borde del ala |
| Copa / punta | (190, 30) | (760, 120) | línea a lo alto de la copa |

Tené en cuenta que más adelante se van a sumar más casilleros para sombreros paramétricos (altura, forma de copa, cinta): dejá **espacio libre** a los costados y abajo del dibujo.

## Cómo seguir

1. Probá con **un** esquema (la remera, que es la más cargada) y comparalo con los actuales.
2. Pasame los PNG o la ruta; reviso que los marcos caigan en las coordenadas y, si no, ajusto los números del código (`PIN_POS` / `PIN_BTN`).
3. Para Banda y Sombrero, si cambiás la distribución, pasame los centros reales de cada marco.
