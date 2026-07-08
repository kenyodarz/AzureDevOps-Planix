#¿Qué es una Historia de Usuario? ![image.png](/.attachments/image-071d4723-280e-4bfb-beaa-a7722c1ee0ba.png)

#Definición:

| Definición Agilidad en Acción | Definición para Dummies |
|--|--|
| Es la declaración corta para expresar una necesidad deseada por el usuario final y debe ser simple, fácil de implementar y clara para la célula. | Es una forma sencilla de escribir lo que un usuario necesita. Es como una pequeña nota que explica que quiere hacer y por qué.  |

![image.png](img.png)  ![img_1.png](img_1.png)

## **¿Qué finalidad tiene una Historia de Usuario?**
- Capturar una necesidad real del usuario de forma clara y sencilla. 
- Dividir el trabajo en unidades manejables, facilitando su desarrollo e implementación. 
- Garantizar que el desarrollo genere valor, asegurando que cada historia contribuya a los indicadores y épicas de la célula. 

**Regla rápida para decidir entre Historia de Usuario vs Habilitadora:**


| Historia de Usuario | Historia Habilitadora  |
|--|--|
|Se crea cuando hay una necesidad funcional para el usuario final.  | Se crea cuando hay una necesidad técnica o de negocio que debe resolverse antes de desarrollar una HU. |
|Cuando el usuario final necesita una mejora sobre una funcionalidad liberada.  |  Cuando se necesita preparar algo antes de poder desarrollar una HU. |
|Cuando la historia tiene valor directo para el negocio o usuario.|Cuando una funcionalidad depende de una infraestructura o decisiones previas.|
|**Ejemplo**: "Yo como usuario requiero recuperar mi contraseña para poder acceder a mi cuenta si la olvido."|**Ejemplo**: " Yo como usuario necesito evaluar que opción de autenticación cumple con la normativa para poder implementarlo en la recuperación de contraseña."|



## **Formato de una Historia de Usuario:**

| **Concepto** | **Detalle**| 
|--|--|
|Titulo  | Expresión corta, que define el objetivo de la HU|
|Descripción  | Es un texto que explica de manera clara y concisa qué se quiere lograr. Debe seguir una estructura clara y estandarizada para asegurar que el equipo entienda: quién necesita algo, qué necesita, y porqué lo necesita. Siempre **debe ir expresada desde el **usuario** que tiene la necesidad.** <br><br> Formato estandar: "**yo como usuario**" "**requiero**..." (aquí va el detalle de la necesidad o funcionalidad,  "**para**..." (explicación de la finalidad o beneficio)|
| Criterios de aceptación | Son las condiciones que se deben cumplir para considerar la historia de usuario terminada. Ayudan a validar que el trabajo cumple con los requisitos y alcances definidos. Deben ser validados por el Dueño de Producto, quien Acepta/Rechaza la Historia de Usuario. |
| Responsable | El responsable será un integrante de célula definido durante la planeación del sprint, según sus conocimientos.  |
|Estado|Indica en que punto del proceso se encuentra el work item. En azure los estados disponibles son: Active, Closed, New, Impedimento.|
|Story Points|Es una forma de medir que tanto esfuerzo necesita una historia para completarse. Se mide a través de la Escala de Fibonacci: 1, 2, 3, 5, 8, 13. |
|Prioridad|Indica que tan importante es el work item en comparación con otros. Ayuda al equipo a decidir qué debe hacer primero según el valor que aporta y la urgencia de la necesidad.  |
|Parent|El parent de una historia de usuario por taxonomía, es el work item **Épica**.|


## **Datos Importantes:**
| ¿Quién puede crear una HU? | El dueño de producto y/o los integrantes de célula. |
|--|--|
| ¿Quién define el alcance? | El dueño de producto en compañia de los integrantes de célula, durante la planeación del sprint.  |
|¿Cuándo se crea este work item?|Principalmente durante la Planeación del Sprint. Si surge una nueva necesidad durante el transcurso del sprint, se puede crear una historia adicional, bajo la aprobación y conocimiento del dueño de producto. |
|¿Quién es el responsable de actualizar el estado de la HU?|El integrante de célula asignado durante la planeación del sprint|
|Una HU puede ser parent de:|Por taxonomía, los WI que pueden tener como parent una HU, son: tareas, bugs.|
|¿Qué Work Items pueden ir relacionados a una HU?|Los WI que se pueden relacionar a una HU son: habilitadores, test plan, issues, Dod.  |

## **Información Adicional:**
 
**a) Tipos de estado del WI:**
- **New**: la historia ha sido creada, pero aún no ha comenzado su desarrollo. 
- **Active**: indica que la historia esta en progreso, el equipo está trabajando en su implementación.
- **Impedimento**: significa que la historia está bloqueada por algun motivo externo a la célula, que impide su avance (falta de información, dependencias, problemas técnicos, etc.)
- **Closed**: indica que la historia ha sido completada, cumple con los criterios de aceptación y se considera terminada. 

**b) Campo Prioridad:**
En Azure, el campo está enumerado del 1 al 4, donde 4 es lo más urgente e importante, y 1 lo menos prioritario.
- **Nivel 4: (Crítica)** debe resolverse de inmediato, ya que impacta directamente a los usuarios o al negocio. 
- **Nivel 3: (Alta)** muy importante, pero no detiene completamente la operación. Se debe resolver en el corto plazo. 
- **Nivel 2: (Media)** importante, pero no urgente. Puede trabajarse después de las prioridades 4 y 3. 
- **Nivel 1: (Baja)** deseable, pero no imprescindible. Se trabajará a medida que se libere tiempo del sprint. 


**c)  Campo Story Points:**

A continuación te mostramos una guía para realizar una estimación adecuada, basada en la serie de fibonacci.

| ESTIMACIÓN | 1 | 2 | 3 | 5 | 8 | >8 Requiere Dividir |
|--|--|--|--|--|--|--|
| DEFINICIÓN | He realizado esto antes y sé como hacerlo. Ej. Cambiar un estilo | He realizado esto antes, se que me va a tomar tiempo. Ej. Públicar un nuevo endpoint | Apenas se como hacer esto, sé donde comenzar aunque existe incertidumbre. Ej. Implementar un nuevo widget | Es la primera vez que hacemos esto, requerimos apoyo y guía para recibir conexto. Ej. Trabajar con una nueva líbreria. | Es una historia extensa, tiene una dificultad técnica alta. | La historia es extensa, cubre varios flujos y se requiere dividir para entregar valor |
| DIFICULTAD | Bajo | Bajo | Medio - Bajo | Medio - Alto | Alto | Alto |
| DEPENDENCIAS | Bajo / No existe | Bajo / No existe | Medio - Bajo | Medio - Alto | Alto | Alto |
| INCERTIDUMBRE | Bajo | Bajo | Medio | Medio | Alto | Muy Alto |




**d) Ejemplo de una Historia de usuario:**


- Como: usuario que tiene la necesidad. 
- Quiero: descripción de la necesidad.
- Para: beneficio que recibirá el usuario con esta necesidad implementada

![img_2.png](img_2.png)![image.png](/.attachments/image-63baa9bb-8330-449d-a623-ccb58669d8d8.png)

Fuente oficial: Agilidad en Acción - Modelos y Definiciones.  
Link: [Agilidad en Acción - Modelos y Definiciones.](https://bancolombia.sharepoint.com/:u:/r/sites/CO-VGH/SitePages/Modelo-operativo-agil.aspx?csf=1&web=1&e=jeVJq8)