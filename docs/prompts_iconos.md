# Prompts de íconos para generar con GPT (ChatGPT Plus)

Todos los íconos de ítem van a **64×64 px**, PNG con fondo propio (el papel), sin transparencia salvo donde se aclare.
Generalos grandes (1024×1024) y bajalos a 64×64 con LANCZOS (`tools/armar_iconos_assets.py` y
`tools/ajustar_iconos_medias_pantalon.py` ya hacen ese tipo de recorte). **Probá primero con 2 o 3 y ajustá el estilo
antes de pedir los 40.**

## Estilo base (pegalo al principio de cada pedido)

> Ícono de videojuego estilo "taller de sastrería", vista frontal, centrado, margen del 10 % alrededor. Fondo: una
> hoja de papel kraft anaranjado plegada en cruz, con cuatro zonas de luz planas (como papel doblado en cuatro) y
> pliegues finos. Sobre el papel, el dibujo del tema en trazo oscuro marrón de contorno grueso y colores planos
> cálidos, sombreado simple de 2 tonos, sin degradés finos ni texto. Marco redondeado fino marrón oscuro. Cuadrado,
> legible a 64×64 píxeles. Estilo cohesivo con una serie de 40 íconos: mismo papel, mismo grosor de línea, misma paleta.

Paleta: papel `#D9A066`, luz `#E8B982`, sombra de pliegue `#B9824A`, contorno `#4A2E1B`, acentos: crema `#F2E2C4`,
rojo cuero `#A3263A`, dorado `#D6B05C`.

Cada ícono de abajo es "estilo base + este dibujo". Nombre de archivo y ruta al final de cada línea
(`src/main/resources/assets/femclothes/textures/item/<archivo>.png`).

## 1. Chaqueta (10) — nuevos, hoy copias provisorias

Una campera de frente (siluetas de prenda superior con mangas largas):

| Archivo | Dibujo |
|---|---|
| `molde_chaqueta_frente_cerrada` | campera con el frente cerrado, un cierre vertical completo en el medio |
| `molde_chaqueta_frente_abierta` | la misma campera con el frente abierto: una franja vertical del medio vacía (se ve el papel) con las dos orillas marcadas |
| `molde_chaqueta_capucha_con` | campera con una capucha caída detrás del cuello |
| `molde_chaqueta_capucha_sin` | campera con cuello simple, sin capucha, y una cruz roja chica en la esquina |
| `molde_chaqueta_remate_elastico` | puño y ruedo de una campera con tejido acanalado (canales verticales) |
| `molde_chaqueta_remate_recto` | puño y ruedo de una campera con borde liso y una costura simple |
| `molde_chaqueta_solapa_ninguna` | cuello de saco liso, sin solapas, y una cruz roja chica en la esquina |
| `molde_chaqueta_solapa_pico` | saco con solapas en pico (la punta de cada solapa sale hacia afuera) |
| `molde_chaqueta_solapa_redonda` | saco con solapas redondeadas y anchas |
| `molde_chaqueta_solapa_chal` | saco tipo esmoquin con una solapa chal larga y curva hasta la cintura |

## 2. Cuello (1) — nuevo

| Archivo | Dibujo |
|---|---|
| `molde_cuello_camisa` | torso de camisa con cuello de camisa: dos puntas triangulares y escote en V chico |

Debe parecerse en estilo a los otros moldes de cuello (`molde_cuello_redondo`, `_v`, `_polera`, `_cuadrado`, `_corazon`).

## 3. Capa (3) — pendientes de sprite propio

| Archivo | Dibujo |
|---|---|
| `molde_capa_con_capucha` | capa corta con capucha puesta |
| `molde_capa_sin_capucha` | capa sin capucha, cuello liso |
| `molde_capa_sin_cuello` | capa con borde de cuello recto y bajo |

## 4. Pollera (3 bordes de ruedo)

| Archivo | Dibujo |
|---|---|
| `molde_borde_ondulado` | ruedo de pollera con borde ondulado suave |
| `molde_borde_festoneado` | ruedo con festones (semicírculos repetidos) |
| `molde_borde_pico` | ruedo con picos triangulares (dientes de sierra) |

## 5. Máscaras de sublimado (5)

Una forma recortada con un pedacito de foto adentro (un cuadradito de paisaje):
`molde_mascara_cuadrado`, `molde_mascara_franja` (tira horizontal), `molde_mascara_circulo`, `molde_mascara_estrella`,
`molde_mascara_triangulo`.

## 6. Patrones (2)

Motivo repetido adentro de un aro de bordado de madera, **sin papel de fondo**: `pattern_lunares` (puntos), `pattern_vichy`
(cuadros chicos tipo mantel).

## 7. Sombrero de bruja (5)

`sombrero_bruja` (sombrero de bruja entero, puntiagudo, con cinta), `molde_sombrero_ala_ancha`, `molde_sombrero_ala_corta`
(sombrero visto de lado con el ala marcada), `molde_sombrero_punta_recta` (cono derecho), `molde_sombrero_punta_doblada`
(cono con la punta caída).

## 8. Banda: cintos y chokers (9)

`banda` (un cinto de cuero con hebilla), `molde_banda_zona_cintura` (torso con un cinto), `molde_banda_zona_cuello`
(cuello con un choker), `molde_banda_ancho_fino` / `_medio` / `_ancho` (tres tiras de cuero de distinto grosor, una al lado de
otra), `molde_banda_herraje_ninguno` (tira lisa con cruz roja), `molde_banda_herraje_placa` (tira con placa dorada),
`molde_banda_herraje_aro` (tira con aro dorado).

## 9. Correas (5)

`molde_correa_lisa` (tira de cuero), `molde_correa_cadena` (cadena gruesa dorada), `molde_correa_cadena_fina` (cadena fina),
`molde_correa_ojalillos` (tira con agujeros y ojalillos metálicos), `molde_correa_cordon` (cordón trenzado con puntas).

## 10. Otros

`molde_volado_recto` / `molde_volado_circular` ya tienen sprite; el **primer volante** (falda lisa con puntada) sigue con el
anterior. `molde_textura_acolchado` y `molde_textura_fruncido` están bien por ahora.

## Cómo seguir

1. Pedí un ícono por mensaje y pegá el estilo base entero cada vez (GPT se olvida entre pedidos).
2. Cuando uno te guste, subilo de referencia ("mismo estilo que esta imagen") para los demás.
3. Recortá a cuadrado, bajá a 64×64 y copialo a la ruta de la tabla (pisa la copia provisoria).
