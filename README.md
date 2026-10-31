# Supernova (Fabric 1.21.1)

Mod de **Fabric 1.21.1** (Java 21): supernova en dos partes, bunker, nave de hierro con tanques de dilitio, viaje de ida y vuelta
a **Gleba** (terreno distinto en cada mundo), armadura de Gleba y VFX/sonidos propios.

## Compilar
- GitHub lo compila solo: pestana **Actions** -> artifact `supernova-fabric` (usa Java 21 + Gradle 8.10.2).
- En local: copia `gradlew` + `gradle/` de un proyecto Fabric 1.21.1 y `./gradlew build`. Necesita **Fabric API 0.102.0+1.21.1** en `mods/`.

## Comandos (operador)
`/supernova part1` · `part2` · `stop` · `bunker` (`bunker tp`) · `ship` · `gleba`

## La nave (ida y vuelta)
1. Click derecho en la nave -> tanques de dilitio (+25 % cada uno, minimo 50 %) -> boton **ENCENDER MOTORES** (en Gleba: **REGRESAR A LA TIERRA**).
2. Mantener **CTRL** hasta 100 % -> 3-2-1 -> despegue (destruye lo que haya encima) -> 450 m anillo de condensacion y nubes -> 600 m aparece el planeta
   -> 800 m circulo del otro planeta -> **salto temporal a los 1000 m** (carga el terreno del destino con barra de progreso) -> estabilizacion
   -> aproximacion con plasma de reentrada -> aterrizaje a los 1200 m con base + nave.
3. El combustible se gasta de verdad durante el ascenso (50 % por viaje). Los pasajeros van invisibles dentro de la cabina.
4. Si el destino no se pudiera crear, la nave baja a su base (nadie cae al vacio) y el chat muestra el error.

## Gleba
Dimension creada **por codigo** al arrancar el servidor (sin datapack: no toca level.dat, sin avisos "experimentales", funciona en mundos existentes).
Terreno por **semilla** del mundo con 4 zonas: pantano de hongos/chorus, tierras altas de cristal, desierto palido y zona de CARNE. Minerales de Gleba bajo tierra.

## Armadura de Gleba
Netherite + lingote de Gleba (mesa de herreria con la plantilla de mejora de netherite). Negra con cuernos (los cuernos se dibujan con una capa propia).

## Recursos
Texturas de 64 px, planetas 2048x1024, fuente espacial propia (bitmap), 20 sonidos propios, VFX con sprites propios (no particulas de Minecraft).

## Aviso
Escrito sin poder compilar contra Fabric/Minecraft: puede requerir corregir algun nombre de API en la primera compilacion.
