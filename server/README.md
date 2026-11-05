# Servidor de Comandos ATS

Este servidor recibe comandos desde la aplicación Android y simula pulsaciones de teclas en tu PC.

## Tecnologías del proyecto

El proyecto está compuesto por dos partes principales:

- Backend del servidor:
  - Node.js
  - Express.js para crear la API HTTP
  - RobotJS para simular pulsaciones de teclado en la PC
  - JavaScript como lenguaje principal del servidor

- Aplicación Android:
  - Kotlin
  - Jetpack Compose para la interfaz de usuario
  - OkHttp para consumir el servidor
  - Gradle como sistema de construcción

## Requisitos

1. **Node.js**: Instalado en tu sistema.
2. **Herramientas de compilación**: `robotjs` es una librería nativa y requiere:
   - En Windows: `npm install --global windows-build-tools` (o tener Visual Studio instalado con C++).
   - En Linux/Mac: Herramientas de desarrollo estándar (gcc, make, etc.).

## Instalación

1. Abre una terminal en esta carpeta (`server`).
2. Instala las dependencias:
   ```bash
   npm install
   ```

## Uso

1. Inicia el servidor:
   ```bash
   npm start
   ```
2. Asegúrate de que la IP en tu aplicación Android (`MainActivity.kt`) coincida con la IP local de tu PC.
3. El servidor escuchará en el puerto 3000 y ejecutará la tecla recibida cada vez que presiones un botón en la app.
