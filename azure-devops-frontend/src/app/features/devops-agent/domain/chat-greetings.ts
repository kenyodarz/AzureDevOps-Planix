import { Message } from '../models/devops-agent.model';

export const GENERAL_GREETING: Message = {
  role: 'agent',
  parts: [
    {
      text: `### ¡Hola! Soy tu asistente y Scrum Master virtual de Bancolombia. 🤖

Estoy aquí para ayudarte en el **Chat General**. Aquí puedes hacer consultas libres, pedir reportes, listados de DevOps, soporte general y más.`,
    },
  ],
};

export const REFINEMENT_GREETING: Message = {
  role: 'agent',
  parts: [
    {
      text: `### ¡Hola! Soy tu asistente y Scrum Master virtual de Bancolombia. 🤖

Estoy aquí para ayudarte a redactar e instanciar tus **Historias de Usuario (HU)** o **Historias Habilitadoras (HA)** en Azure DevOps, siguiendo estrictamente la plantilla corporativa.

---

### 📝 ¿Cómo enviarme tu idea?
Puedes escribir una descripción breve, pero obtendrás un resultado ideal si me proporcionas una estructura clara.

**Ejemplo de mensaje perfecto:**
* **Título**: Carga masiva de aprobadores
* **Equipo**: Canales Digitales - Célula Core
* **Descripción**: Crear la función de carga en batch (.csv) para poblar la tabla de aprobadores del sistema de control de accesos (MCP). Esto incluye el CRUD completo para gestionar los registros individuales desde el panel de administración, asegurando que solo usuarios con rol de SuperAdmin puedan operarlo.
* **Criterios de Aceptación**: Debe validar que los campos requeridos no estén vacíos, que el formato de correo sea válido y que el proceso se ejecute de forma asíncrona informando el resultado al finalizar.

---

### 📘 ¿Cómo funciona la sección "Cargar Planeación"?
En el panel lateral izquierdo tienes la opción de **Cargar Planeación**. Aquí puedes subir archivos de planeación en formato Markdown (\`.md\`).

**¿Cómo ayuda esto al proceso?**
1. **Contexto Semántico**: Al subir un documento (como la planeación de un Q o los lineamientos de arquitectura), el contenido se procesa y se almacena en nuestra **base de datos vectorial**.
2. **Generación Alineada**: Cuando me pidas redactar una HU o HA, realizaré una **búsqueda semántica** automática en ese archivo cargado. De este modo, la historia generada adoptará automáticamente los detalles de negocio, restricciones técnicas, criterios técnicos u objetivos previamente acordados en tu planeación.
3. **Menos esfuerzo**: No necesitas redactar todo desde cero ni copiar y pegar extensos documentos en el chat; el agente recuperará la información relevante por ti.
`,
    },
  ],
};

export const GENERAL_RESET_GREETING: Message = {
  role: 'agent',
  parts: [
    {
      text: 'Chat general limpio. Entrégame una consulta libre, listado de DevOps o reporte para comenzar.',
    },
  ],
};

export const REFINEMENT_RESET_GREETING: Message = {
  role: 'agent',
  parts: [
    {
      text: 'Chat limpio (Asistente de Refinamiento). Entrégame una nueva idea de Historia de Usuario o Historia Habilitadora para comenzar.',
    },
  ],
};
