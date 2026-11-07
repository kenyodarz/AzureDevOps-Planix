# Tutorial: Despliegue del Entorno Local en Menos de 5 Minutos

> **Cuadrante Diátaxis: Tutoriales (Learning-Oriented)**  
> En este tutorial aprenderás paso a paso cómo clonar, configurar y levantar los cuatro componentes
> del monorepo en tu estación de trabajo local.

---

## 📋 Prerrequisitos

Asegúrate de contar con las siguientes herramientas instaladas en tu equipo:

* **Java Development Kit (JDK):** Versión 21 LTS (ej. Eclipse Temurin, Amazon Corretto).
* **Node.js:** Versión 22 LTS o superior (incluye `npm`).
* **Git:** 2.40+
* **Docker:** *(Opcional)* Para levantar PostgreSQL con pgvector localmente.

Verifica tus versiones ejecutando:

```powershell
java -version
node -v
npm -v
```

---

## Paso 1: Configurar Variables de Entorno Locales

Copia la plantilla de variables de entorno en la raíz del monorepo:

```powershell
# En la raíz del proyecto AzureDevOps
Copy-Item .env.example .env
```

Edita `.env` con tus credenciales de Azure DevOps o valores de prueba:

```env
AI_API_KEY=dummy-local-key
AI_BASE_URL=http://localhost:4141
AZURE_DEVOPS_PAT=tu-personal-access-token
```

---

## Paso 2: Compilar y Levantar el Motor del Agente (`azure-devops-agent`)

Abre una terminal y ejecuta el build de Gradle:

```powershell
cd azure-devops-agent
.\gradlew.bat build -x test
.\gradlew.bat :app-service:bootRun
```

> El servicio quedará escuchando en `http://localhost:8080` (o el puerto configurado en
> `application.yaml`).

Verifica que el agente esté saludable consultando su Agent Card en otra terminal o navegador:

```powershell
curl http://localhost:8080/.well-known/agent-card.json
```

---

## Paso 3: Levantar el Servidor MCP (`azure-devops-mcp`)

En una segunda terminal, levanta el servidor de herramientas de Azure DevOps:

```powershell
cd azure-devops-mcp
.\gradlew.bat :app-service:bootRun
```

> El servidor MCP quedará disponible en `http://localhost:8082`.

---

## Paso 4: Levantar el Backend Orquestador (`azure-devops-backend`)

En una tercera terminal, inicia el BFF reactivo:

```powershell
cd azure-devops-backend
.\gradlew.bat :app-service:bootRun
```

> El BFF quedará activo en `http://localhost:8081`.

---

## Paso 5: Levantar la Interfaz Web en Angular (`azure-devops-frontend`)

En una cuarta terminal, instala las dependencias de npm y arranca el servidor de desarrollo:

```powershell
cd azure-devops-frontend
npm install
npm start
```

> La aplicación web abrirá en `http://localhost:4200`.

---

## ✅ Verificación del Ecosistema

1. Abre tu navegador en **`http://localhost:4200`**.
2. Verás la pantalla principal del **Azure DevOps Agent**.
3. Haz clic en **"Program Planning Wizard"** para explorar el flujo guiado de planeación o escribe
   en el chat:
   ```text
   /plan Q3-2026 6 sprints 34 sp [Canales]
   ```
4. Observa cómo el mensaje viaja por el BFF, activa `ProgramPlanningUseCase` en el agente y genera
   el roadmap en tiempo real.

¡Felicitaciones! Has completado el despliegue del entorno local del Harness de Agentes.
