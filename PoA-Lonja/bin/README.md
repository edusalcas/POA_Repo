# Poniendo en marcha el proyecto en Eclipse
## Peparando el contenido
1. ``` mkdir proyectoPoA && cd proyectoPoA```
2. ``` git clone https://gitlab.atica.um.es/dan.garcia/PoA-plantilla-practica ```
2. ```mkdir workspace```

## Añadiendo el proyecto a Eclipse
1. Abrimos un proyecto nuevo en Eclipse, seleccionamos la carpeta workspace
2. Seleccionamos Importar Proyectos > Maven > Proyectos Existentes
3. Buscamos la carpeta PoA-plantilla-practica
4. Hacemos click en Finish

## Ejecutando el proyecto
1. Vamos a src/main/java
2. Seleccionamos el paquete es.um.poa.scenarios
3. Abrimos ScenarioLauncher.java
4. Boton derecho > Run As > Run Configuration
5. Seleccionamos en la columna de la Izquierda > Java Application
6. Hacemos click en el icono del documento con un signo +
7. En argumentos colocamos la ruta relativa del fichero configuración del escneario "configs/scenario.yaml"

## Configurando el proyecto maven desde linea de comando
```
cd PoA-plantilla-practica
mvn eclipse:eclipse
```
Cargar el proyecto en Eclipse como un General > Proyecto existente en workspace

