# Sistema Multi-Agente con JADE | Guía de Requerimientos e Instalación

## 1. Requerimientos del sistema
Antes de ejecutar el proyecto, asegúrese de tener instalado lo siguiente:

### Sotware necesario
- Java JDK 11 o superior
- Maven 3.8 o superior
- Sistema operativo Windows / Linux / macOS
- Terminal o PowerShell
- Editor de código (recomendado: VS Code)

### Verificar instalación de Java y Maven
- Ejecutar: java -version
- Ejecutar: mvn -version

## 2. Compilación inicial
- Desde la carpeta raíz del proyecto ejecutar: mvn clean compile
Se espera que salga "BUILD SUCCESS" para que genere los archivos en la carpeta target/classes

## 3. Ejecución del sistema multi-agente
Para iniciar la plataforma JADE y los agentes, ejecutar en la terminal:
java -cp "lib/jade.jar;target/classes" jade.Boot -gui "Cliente:agents.ClienteAgent;Gestor:agents.GestorAgent;Restaurante:agents.RestauranteAgent;Repartidor:agents.RepartidorAgent"