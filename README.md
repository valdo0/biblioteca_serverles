# Sistema Biblioteca Serverless - Sumativa 1

Sistema de gestión de biblioteca basado en microservicios con arquitectura serverless usando Azure Functions y Spring Boot.

## Arquitectura

```
CLIENTE
   ↓
BFF (localhost:8080) [API Gateway]
   ├─→ /api/auth/*           → ms-auth:8082 (Autenticación)
   ├─→ /api/roles/*          → ms-roles:8081 (Gestión de Roles)
   ├─→ /api/libros/*         → ms-biblioteca:8083 (CRUD Libros)
   ├─→ /api/prestamos/*      → ms-biblioteca:8083 (CRUD Préstamos)
   └─→ /api/graphql/*        → Azure Functions:7071 (GraphQL)
       ├─→ /catalog  (Catálogo de libros)
       └─→ /profile  (Perfil de usuario)
```

## Componentes

### Microservicios Spring Boot
- **BFF** (puerto 8080) - Backend For Frontend / API Gateway
- **ms-auth** (puerto 8082) - Autenticación y registro de usuarios
- **ms-roles** (puerto 8081) - Gestión de roles y permisos
- **ms-biblioteca** (puerto 8083) - CRUD de libros y préstamos

### Azure Functions (puerto 7071)
- **RoleNotificationFunction** - Notifica cambios de rol
- **SuspiciousActivityFunction** - Alerta de actividad sospechosa (3 intentos fallidos)
- **LibraryCatalogGraphqlFunction** - Consultas GraphQL de catálogo
- **UserProfileGraphqlFunction** - Consultas GraphQL de perfil

## Requisitos Previos

- Java 17+
- Maven 3.6+
- Docker y Docker Compose
- Azure Functions Core Tools 4.x
- Oracle Database (configurado en application.properties)

## Instrucciones de Uso

### 1. Iniciar Microservicios con Docker

```bash
# Construir e iniciar todos los microservicios
docker-compose up --build

# Ver logs
docker-compose logs -f

# Detener servicios
docker-compose down
```

Esto iniciará:
- BFF en `http://localhost:8080`
- ms-roles en `http://localhost:8081`
- ms-auth en `http://localhost:8082`
- ms-biblioteca en `http://localhost:8083`

### 2. Iniciar Azure Functions

En una terminal separada:

```bash
cd function
mvn clean package
mvn azure-functions:run
```

Esto iniciará Azure Functions en `http://localhost:7071`

### 3. Probar los Endpoints

**IMPORTANTE:** Todos los endpoints deben llamarse a través del BFF (`localhost:8080`) para mantener la arquitectura de API Gateway.

#### Autenticación

**Registrar Usuario:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Ana García",
    "email": "ana@biblioteca.cl",
    "password": "pass123",
    "rol": "LECTOR"
  }'
```

**Iniciar Sesión:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ana@biblioteca.cl",
    "password": "pass123"
  }'
```

#### Roles

**Asignar Rol (activa notificación serverless):**
```bash
curl -X POST http://localhost:8080/api/roles/assign \
  -H "Content-Type: application/json" \
  -d '{
    "usuarioId": "1",
    "rol": "ADMINISTRADOR",
    "asignadoPor": "superadmin"
  }'
```

#### Libros

**Listar Libros:**
```bash
curl http://localhost:8080/api/libros
```

**Crear Libro:**
```bash
curl -X POST http://localhost:8080/api/libros \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Cien Años de Soledad",
    "autor": "Gabriel García Márquez",
    "isbn": "978-0000000001",
    "cantidad": 3
  }'
```

**Obtener Libro por ID:**
```bash
curl http://localhost:8080/api/libros/1
```

**Actualizar Libro:**
```bash
curl -X PUT http://localhost:8080/api/libros/1 \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Cien Años de Soledad (Ed. 2024)",
    "autor": "Gabriel García Márquez",
    "cantidad": 5,
    "disponible": true
  }'
```

**Eliminar Libro:**
```bash
curl -X DELETE http://localhost:8080/api/libros/1
```

#### Préstamos

**Listar Préstamos:**
```bash
curl http://localhost:8080/api/prestamos
```

**Crear Préstamo:**
```bash
curl -X POST http://localhost:8080/api/prestamos \
  -H "Content-Type: application/json" \
  -d '{
    "usuarioId": 1,
    "libroId": 2,
    "libroTitulo": "El Principito",
    "fechaPrestamo": "2026-03-28",
    "fechaDevolucion": "2026-04-11"
  }'
```

**Actualizar Préstamo:**
```bash
curl -X PUT http://localhost:8080/api/prestamos/1 \
  -H "Content-Type: application/json" \
  -d '{
    "usuarioId": 1,
    "libroId": 1,
    "estado": "DEVUELTO",
    "fechaDevolucion": "2026-03-28"
  }'
```

**Eliminar Préstamo:**
```bash
curl -X DELETE http://localhost:8080/api/prestamos/1
```

#### GraphQL (Nuevos Endpoints)

**Consultar Catálogo de Libros (todos):**
```bash
curl -X POST http://localhost:8080/api/graphql/catalog \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { books { id titulo autor isbn cantidad disponible } }"
  }'
```

**Consultar solo libros disponibles:**
```bash
curl -X POST http://localhost:8080/api/graphql/catalog \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { books(disponible: true) { id titulo autor cantidad } }"
  }'
```

**Buscar libros por autor:**
```bash
curl -X POST http://localhost:8080/api/graphql/catalog \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { books(autor: \"García\") { id titulo autor isbn } }"
  }'
```

**Consultar un libro específico por ID:**
```bash
curl -X POST http://localhost:8080/api/graphql/catalog \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { book(id: \"1\") { id titulo autor isbn cantidad disponible } }"
  }'
```

**Consultar Perfil de Usuario:**
```bash
curl -X POST http://localhost:8080/api/graphql/profile \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { user(id: \"1\") { id nombre email rol { nombre permisos } librosPrestados { titulo fechaDevolucion } } }"
  }'
```

## Schemas GraphQL

### LibraryCatalogGraphql

```graphql
type Query {
  books(disponible: Boolean, autor: String): [Book]
  book(id: ID!): Book
}

type Book {
  id: ID!
  titulo: String
  autor: String
  isbn: String
  cantidad: Int
  disponible: Boolean
}
```

**Nota:** Esta función consulta datos reales de **ms-biblioteca** (`GET /libros`)

### UserProfileGraphql

```graphql
type Query {
  user(id: ID!): User
}

type User {
  id: ID!
  nombre: String
  email: String
  rol: Role
  librosPrestados: [BorrowedBook]
}

type Role {
  nombre: String
  permisos: [String]
}

type BorrowedBook {
  titulo: String
  fechaDevolucion: String
}
```

**Nota:** Esta función consulta datos reales de:
- **ms-auth** para obtener información del usuario (nombre, email, rol)
- **ms-biblioteca** para obtener los préstamos activos del usuario

## Flujos Serverless

### 1. Notificación de Cambio de Rol
```
Cliente → BFF → ms-roles → Azure Function (RoleNotification)
```
Cuando se asigna un rol, se dispara automáticamente una notificación.

### 2. Alerta de Actividad Sospechosa
```
Cliente → BFF → ms-auth (3 intentos fallidos) → Azure Function (SuspiciousActivity)
```
Después de 3 intentos fallidos de login, se dispara una alerta.

### 3. Consulta GraphQL de Catálogo
```
Cliente → BFF → Azure Function (LibraryCatalog) → ms-biblioteca
```
La función GraphQL consulta el microservicio de biblioteca y filtra resultados.

## Documentación API

Importa el archivo `apidog-collection.json` en Postman/Apidog para ver la documentación completa de la API con ejemplos.

## Configuración para Producción

Para desplegar en producción, actualiza las variables de entorno en `docker-compose.yml`:

```yaml
# Cambiar de:
- AZURE_FUNCTION_BASE_URL=http://host.docker.internal:7071/api

# A:
- AZURE_FUNCTION_BASE_URL=https://sumativa1.azurewebsites.net/api
```

## Troubleshooting

### Azure Functions no se conectan a microservicios
- Verifica que `local.settings.json` tenga `MS_BIBLIOTECA_URL=http://localhost:8083`
- Asegúrate de que los microservicios estén corriendo en Docker

### BFF no alcanza Azure Functions
- Verifica que Azure Functions estén corriendo en el puerto 7071
- Revisa que `docker-compose.yml` tenga `extra_hosts: host.docker.internal:host-gateway`

### Error de base de datos
- Verifica que Oracle esté corriendo
- Revisa las credenciales en `application.properties` de cada microservicio
- Confirma que los wallets estén en las carpetas correctas

## Estructura del Proyecto

```
.
├── biblioteca/              # BFF (Backend For Frontend)
├── ms-auth/                # Microservicio de Autenticación
├── ms-roles/               # Microservicio de Roles
├── ms-biblioteca/          # Microservicio de Libros y Préstamos
├── function/               # Azure Functions (GraphQL + Notificaciones)
├── docker-compose.yml      # Orquestación de microservicios
└── apidog-collection.json  # Documentación API
```

## Licencia

Proyecto académico - Sumativa 1
