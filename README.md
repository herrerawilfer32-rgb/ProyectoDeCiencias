# MotorBD - Gestor de Bases de Datos con Arbol AVL

MotorBD es un gestor de bases de datos educativo escrito en Java. Permite crear espacios de almacenamiento, insertar registros, consultar, actualizar, eliminar y guardar datos en disco usando comandos tipo SQL desde una consola interactiva.

El proyecto usa un arbol AVL propio como indice principal para la clave primaria de cada espacio. Esto permite busqueda, insercion y eliminacion por clave en tiempo `O(log n)`.

## Objetivo

Cumplir el Proyecto Final: Motor/Gestor de Bases de Datos con Arboles Autobalanceados.

El sistema demuestra:

- Gestion de esquemas.
- Operaciones CRUD.
- Indice con arbol AVL implementado desde cero.
- Persistencia en archivos.
- REPL de linea de comandos.
- Pruebas funcionales sin dependencias externas.

## Tecnologias

- Java 11.
- NetBeans/Ant opcional.
- Sin librerias externas.
- Persistencia en archivos de texto: `.schema` y `.json` en formato JSON simple/JSON Lines.

## Estructura

```text
MotorBD/
  src/
    cli/                       REPL.java
    motor/
      arbol/                   ArbolAVL.java, NodoAVL.java
      catalogo/                Campo.java, TipoDato.java, Esquema.java, Catalogo.java
      almacenamiento/          Registro.java, Espacio.java, GestorEspacios.java
      persistencia/            GestorPersistencia.java
      parser/                  Motor.java, ResultadoComando.java
    test/                      TestMotor.java
  data/                        scripts y datos persistidos
  docs/                        guia de defensa
  compilar.bat                 compila con JDK 11
  ejecutar.bat                 ejecuta el REPL
  pruebas.bat                  ejecuta la suite de pruebas
```

## Compilar

Desde PowerShell o CMD:

```powershell
cd C:\Users\wilfer\Downloads\MotorBD
.\compilar.bat
```

## Ejecutar

Modo interactivo:

```powershell
.\ejecutar.bat
```

Modo grafico:

```powershell
.\gui.bat
```

Ejecutar script:

```powershell
.\ejecutar.bat --script data\script_pequeno.sql
```

Pruebas:

```powershell
.\pruebas.bat
```

Nota: los `.bat` usan `C:\Program Files\Java\jdk-11.0.0.1`. Si tu JDK esta en otra ruta, cambia la variable `JDK` dentro de esos archivos.

## Comandos disponibles

```sql
HELP;
CREATE SPACE alumnos (id ENTERO PK, nombre TEXTO, nota REAL);
CREATE SPACE notas;
DROP SPACE alumnos;
SHOW SPACES;
DESC alumnos;
INSERT INTO alumnos VALUES (1, 'Ana', 4.5);
INSERT INTO notas (id, materia, nota) VALUES ('n001', 'Calculo', 4.2);
SELECT * FROM alumnos;
SELECT * FROM alumnos WHERE id = 1;
SELECT * FROM alumnos WHERE nota >= 4;
SELECT * FROM alumnos WHERE id BETWEEN 1 AND 10;
UPDATE alumnos SET nota=4.8 WHERE id = 1;
DELETE FROM alumnos WHERE id = 1;
DELETE FROM alumnos ALL;
TREE alumnos;
SAVE ALL;
EXIT;
```

Tipos soportados:

- `ENTERO`
- `TEXTO`
- `REAL`
- `BOOLEAN`

Operadores:

- `=`
- `!=` o `<>`
- `<`
- `<=`
- `>`
- `>=`
- `BETWEEN`

## Arbol AVL

La implementacion esta en `src/motor/arbol/ArbolAVL.java`.

El AVL guarda pares clave-valor. En MotorBD, la clave es la clave primaria normalizada de cada registro y el valor es un `Registro`.

Operaciones principales:

- `insertar()`: inserta o actualiza una clave y rebalancea.
- `buscar()`: busca por clave.
- `eliminar()`: elimina por clave y rebalancea.
- `inorden()`: recorre los registros ordenados.
- `rango()`: obtiene claves dentro de un rango inclusivo.

El balanceo usa el factor de balance `altura(izq) - altura(der)` y rotaciones:

- Rotacion derecha.
- Rotacion izquierda.
- Caso izquierda-derecha.
- Caso derecha-izquierda.

Complejidad:

| Operacion | Complejidad |
| --------- | ----------- |
| Insercion por clave | `O(log n)` |
| Busqueda por clave | `O(log n)` |
| Eliminacion por clave | `O(log n)` |
| Recorrido inorder | `O(n)` |
| Rango por clave | `O(log n + k)` |
| Busqueda por campo no indexado | `O(n)` |

## Persistencia

La persistencia esta en `src/motor/persistencia/GestorPersistencia.java`.

Archivos generados:

- `data/esquemas/<espacio>.schema`: definicion del espacio.
- `data/espacios/<espacio>.json`: registros en JSON Lines.

Al iniciar, `Motor` llama a `persistencia.cargarTodo(...)`. Ese metodo carga esquemas y registros, y reconstruye automaticamente el AVL insertando cada registro en memoria.

Para evitar corrupcion basica, el guardado escribe primero en un archivo temporal `.tmp` y luego reemplaza el archivo final. Si el reemplazo atomico no esta disponible, usa reemplazo normal.

## Limitaciones conocidas

- Solo existe indice AVL para la clave primaria. Los rangos sobre otros campos funcionan, pero hacen escaneo lineal.
- El parser SQL es simple y no cubre toda la sintaxis SQL real.
- No hay concurrencia ni transacciones completas multi-operacion.
- La interfaz es de consola; no hay GUI.

## Guia de defensa

Ver [docs/GUIA_DEFENSA.md](docs/GUIA_DEFENSA.md).

## Interfaz grafica

La GUI es un complemento del REPL obligatorio. No reemplaza la consola ni cambia el motor interno: llama a `motor.parser.Motor` con los mismos comandos que usa el REPL.

Archivo principal:

- `src/gui/DatabaseManagerGUI.java`

Libreria usada:

- Java Swing, incluida en el JDK.

Pestanas:

- `Espacios`: crear espacios, crear espacios libres, listar y eliminar con confirmacion.
- `Registros`: seleccionar espacio, insertar, visualizar, actualizar y eliminar registros.
- `Busquedas`: buscar por clave primaria, condicion simple o rango.
- `Indice / Arbol`: mostrar arbol AVL, claves en orden y busqueda por clave.
- `Ayuda`: instrucciones simples y ejemplos.

Ejecutar con datos por defecto:

```powershell
.\gui.bat
```

Ejecutar con un directorio de datos separado para demostracion:

```powershell
.\gui.bat --data data_demo_gui
```

Limitaciones de la GUI:

- Esta pensada para esquemas fijos en la pestaña Registros. Los espacios libres siguen disponibles desde consola o desde la pestana Ayuda usando los comandos.
- La visualizacion del arbol es textual para facilitar la defensa sin agregar librerias externas.
