# Guía Práctica: Configuración de LiteLLM, Entorno y Credenciales

> **Cuadrante Diátaxis: Guías Prácticas / How-To (Problem-Oriented)**  
> En esta guía aprenderás cómo configurar el gateway de inferencia LiteLLM, las credenciales de
> Azure DevOps y los tokens de entorno para operar el Harness.

---

## 🎯 Objetivo

Conectar `azure-devops-agent` con un proveedor de LLM (OpenAI, Azure OpenAI, AWS Bedrock, Google
Cloud Vertex AI) de forma transparente mediante **LiteLLM**, evitando modificaciones de código en el
agente.

---

## 1. Configuración de LiteLLM como Gateway Local

LiteLLM actúa como un proxy unificado que expone la API estándar de OpenAI (`/v1/chat/completions`)
hacia más de 100 proveedores de IA.

### Levantar LiteLLM con Docker:

```bash
docker run -d \
  --name litellm-gateway \
  -p 4141:4000 \
  -e OPENAI_API_KEY="tu-clave-openai-o-azure" \
  -e AZURE_API_KEY="tu-clave-azure" \
  -e AZURE_API_BASE="https://tu-recurso.openai.azure.com/" \
  ghcr.io/berriai/litellm:main-latest \
  --model azure/gpt-4o-mini
```

---

## 2. Configurar el Agente para Usar LiteLLM

En `azure-devops-agent`, edita tu archivo `.env` o variables de entorno:

```env
# Apunta al puerto de LiteLLM (4141)
AI_BASE_URL=http://localhost:4141
AI_API_KEY=sk-dummy-local-key
AI_MODEL=gpt-4o-mini
AI_EMBEDDING_MODEL=text-embedding-ada-002
```

---

## 3. Generación y Configuración del PAT de Azure DevOps

Para que `azure-devops-mcp` pueda leer y crear Work Items o inspeccionar Pull Requests:

1. Ve a **Azure DevOps Portal** $\rightarrow$ Configuración de usuario $\rightarrow$ **Personal
   Access Tokens**.
2. Pulsa **"New Token"**.
3. Asigna los siguientes alcances mínimos (*Scopes*):
    - **Work Items:** `Read & Write`
    - **Code:** `Read & Write`
    - **Build / Pipelines:** `Read & Execute`
    - **Wiki:** `Read & Write`
    - **Project and Team:** `Read`
4. Copia el token generado y asígnalo en `.env`:
   ```env
   AZURE_DEVOPS_PAT=tu_token_aqui
   AZURE_DEVOPS_DEFAULT_ORGANIZATION=grupobancolombia
   AZURE_DEVOPS_DEFAULT_PROJECT=Vicepresidencia Servicios de Tecnología
   ```

---

## 4. Verificación de Conectividad

Ejecuta el script de validación rápida en PowerShell:

```powershell
# Probar inferencia en el agente
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/" -ContentType "application/json" -Body (@{
  jsonrpc = "2.0"
  id = "test-health"
  method = "message/send"
  params = @{
    message = "Hola agente, responde con tu rol en una frase"
  }
} | ConvertTo-Json)
```

Si la configuración es correcta, recibirás una respuesta JSON con el saludo del agente en menos de 2
segundos.
