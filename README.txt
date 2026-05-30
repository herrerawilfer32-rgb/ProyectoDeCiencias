╔══════════════════════════════════════════════════════════════╗
║          MOTOR BD  v1.0  —  Arbol AVL Autobalanceado         ║
║     Ciencias de la Computacion I  —  Grupo 020-83  2026-I   ║
╚══════════════════════════════════════════════════════════════╝

INTEGRANTES
-----------
  Anyelo Esteban Casas Zapata   20251020106
  Diego Alejandro Yanez Zabala  20210103
  Wilfer Arbey Herrera Garzon   20251020071
  Docente: Ing. Simar Enrique Herrera Jimenez

DESCRIPCION
-----------
Motor de base de datos no relacional con:
  - Arbol AVL autobalanceado como indice (O(log n))
  - REPL interactivo (SQL-like)
  - Persistencia en JSON Lines
  - Soporte para esquemas fijos y libres
  - Busqueda por rango con el arbol AVL

ESTRUCTURA
----------
  MotorBD/
  ├── src/
  │   ├── motor/
  │   │   ├── arbol/         NodoAVL.java, ArbolAVL.java
  │   │   ├── catalogo/      Campo.java, TipoDato.java, Esquema.java, Catalogo.java
  │   │   ├── almacenamiento/ Registro.java, Espacio.java, GestorEspacios.java
  │   │   ├── persistencia/  GestorPersistencia.java
  │   │   └── parser/        Motor.java, ResultadoComando.java
  │   ├── cli/               REPL.java
  │   └── test/              TestMotor.java
  ├── data/
  │   ├── esquemas/          *.schema (definiciones de espacios)
  │   ├── espacios/          *.json   (registros en JSON Lines)
  │   ├── script_pequeno.sql
  │   └── script_mediano.sql
  └── nbproject/             Configuracion NetBeans

COMPILAR Y EJECUTAR (NetBeans)
-------------------------------
  1. File → Open Project → seleccionar carpeta MotorBD
  2. Click derecho → Run       → inicia el REPL interactivo
  3. Click derecho → Run Test  → ejecuta la suite de pruebas

COMPILAR Y EJECUTAR (linea de comandos)
-----------------------------------------
  Compilar:
    Windows: javac -d build\classes -sourcepath src src\cli\REPL.java src\motor\**\*.java src\test\*.java
    Linux:   javac -d build/classes -sourcepath src $(find src -name "*.java")

  Modo interactivo:
    java -cp build/classes cli.REPL

  Ejecutar script:
    java -cp build/classes cli.REPL --script data/script_pequeno.sql
    java -cp build/classes cli.REPL --script data/script_mediano.sql

  Pruebas:
    java -cp build/classes test.TestMotor

  Directorio de datos alternativo:
    java -cp build/classes cli.REPL --data /ruta/mis/datos

COMANDOS DISPONIBLES
---------------------

  -- Gestion de espacios --
  CREATE SPACE nombre (campo tipo [PK], ...)   Crea espacio con esquema fijo
  CREATE SPACE nombre                          Crea espacio sin esquema (libre)
  DROP SPACE nombre                            Elimina el espacio
  SHOW SPACES                                  Lista todos los espacios
  DESC nombre                                  Describe un espacio

  -- CRUD --
  INSERT INTO nombre VALUES (v1, v2, ...)
  INSERT INTO nombre (c1, c2, ...) VALUES (v1, v2, ...)
  SELECT * FROM nombre
  SELECT * FROM nombre WHERE campo op valor
  SELECT * FROM nombre WHERE campo BETWEEN v1 AND v2
  UPDATE nombre SET c1=v1, c2=v2 WHERE campo op valor
  DELETE FROM nombre WHERE campo op valor
  DELETE FROM nombre ALL

  -- Herramientas --
  TREE nombre         Muestra el arbol AVL en ASCII
  SAVE nombre         Persiste un espacio en disco
  SAVE ALL            Persiste todos los espacios
  EXIT / QUIT         Salir

  Tipos soportados: ENTERO, TEXTO, REAL, BOOLEAN
  Operadores WHERE:  =  !=  <  <=  >  >=

EJEMPLOS
---------
  motorbd> CREATE SPACE alumnos (id ENTERO PK, nombre TEXTO, nota REAL);
  motorbd> INSERT INTO alumnos VALUES (1, 'Juan', 4.5);
  motorbd> SELECT * FROM alumnos WHERE nota >= 4.0;
  motorbd> UPDATE alumnos SET nota=4.8 WHERE id = 1;
  motorbd> DELETE FROM alumnos WHERE id = 1;
  motorbd> TREE alumnos;
  motorbd> SAVE ALL;
  motorbd> EXIT;

COMPLEJIDAD
-----------
  Insercion por clave    O(log n)  — AVL
  Busqueda por clave     O(log n)  — AVL
  Busqueda por rango     O(log n + k) — AVL
  Actualizacion/Delete   O(log n)  — AVL (clave) / O(n) (condicion)
  Listar todos           O(n)      — inorden
  Carga inicial          O(n log n)— n inserciones AVL
