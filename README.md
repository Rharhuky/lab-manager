# CampusLab

Sistema de gerenciamento de reservas de laboratórios universitários. Permite que professores criem e administrem laboratórios e reservas, enquanto alunos consultam disponibilidade e horários.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Configuração e Execução](#configuração-e-execução)
  - [Backend](#backend)
  - [Frontend](#frontend)
- [Usuários de Teste](#usuários-de-teste)
- [API Reference](#api-reference)
  - [Autenticação](#autenticação)
  - [Laboratórios](#laboratórios)
  - [Reservas](#reservas)
  - [Formato de Erros](#formato-de-erros)
- [Modelo de Autorização](#modelo-de-autorização)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Testes](#testes)
- [Variáveis de Ambiente](#variáveis-de-ambiente)

---

## Visão Geral

O CampusLab serve dois perfis de usuário:

| Role | Descrição |
|---|---|
| **PROFESSOR** | Acesso completo — cria, edita e exclui laboratórios e reservas |
| **ALUNO** | Somente leitura — consulta laboratórios e reservas existentes |

Funcionalidades principais:

- Autenticação via JWT (stateless)
- Listagem de laboratórios com status de disponibilidade em tempo real
- Criação e edição de laboratórios com validação de nome único
- Proteção contra exclusão de laboratório com reservas futuras
- Criação de reservas com detecção de conflito de horário
- Duração mínima de 1 minuto por reserva
- Respostas de erro padronizadas em todos os endpoints

---

## Tecnologias

### Backend

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.3.2 | Framework web |
| Spring Security | — | Autenticação e autorização |
| Spring Data JPA | — | Persistência |
| PostgreSQL | — | Banco de dados |
| Flyway | — | Migrações de schema |
| JJWT | 0.12.6 | Geração e validação de JWT |
| jqwik | 1.8.5 | Property-based testing |
| Testcontainers | 1.20.1 | Testes de integração com PostgreSQL real |

### Frontend

| Tecnologia | Versão | Uso |
|---|---|---|
| TypeScript | 5.7 | Linguagem principal |
| React | 18.3 | Framework de UI |
| Vite | 6 | Bundler |
| React Router | 6.28 | Roteamento |
| Axios | 1.7.9 | Cliente HTTP |
| Zustand | 5.0.3 | Gerenciamento de estado (auth) |
| Tailwind CSS | 3.4 | Estilização |
| Vitest | 2.1 | Runner de testes |
| fast-check | 3.23 | Property-based testing |

---

## Arquitetura

```
lab-manager/
├── backend/        # API REST — Spring Boot
├── frontend/       # SPA — React + TypeScript
└── .kiro/          # Especificações e documentação do projeto
```

O backend expõe uma API REST em `http://localhost:8080`. O frontend é uma SPA servida em `http://localhost:5173` que consome essa API via Axios.

A autenticação é **stateless**: o cliente armazena o JWT no `localStorage` e o envia em cada requisição via header `Authorization: Bearer <token>`. O backend valida o token a cada requisição sem manter sessão.

---

## Pré-requisitos

- **Java 21+**
- **Maven 3.9+** (ou use o wrapper `./mvnw` incluído)
- **Node.js 20+** e **npm**
- **PostgreSQL 14+** rodando localmente (ou via Docker)

**PostgreSQL via Docker (opcional):**

```bash
docker run -d \
  --name campuslab-db \
  -e POSTGRES_DB=campuslab \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

---

## Configuração e Execução

### Backend

**1. Configure as variáveis de ambiente:**

```bash
cp backend/.env.example backend/.env
# Edite o arquivo .env conforme necessário
```

**2. Execute:**

```bash
cd backend
./mvnw spring-boot:run
```

O Flyway aplicará as migrações automaticamente ao subir. O servidor ficará disponível em `http://localhost:8080`.

**Outros comandos úteis:**

```bash
# Build (sem testes)
./mvnw package -DskipTests

# Executar todos os testes
./mvnw test

# Executar uma classe de teste específica
./mvnw test -Dtest=LaboratoryServiceTest
```

### Frontend

**1. Instale as dependências:**

```bash
cd frontend
npm install
```

**2. Configure a URL da API (opcional):**

Por padrão o frontend aponta para `http://localhost:8080`. Para alterar, crie um `.env.local`:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

**3. Execute em modo de desenvolvimento:**

```bash
npm run dev
```

A aplicação ficará disponível em `http://localhost:5173`.

**Outros comandos:**

```bash
# Build para produção
npm run build

# Executar testes (modo único)
npm run test:run

# Executar testes em modo watch
npm test

# Lint
npm run lint
```

---

## Usuários de Teste

A migration `V2__Insert_seed_users.sql` insere dois usuários prontos para uso:

| Role | Email | Senha |
|---|---|---|
| **PROFESSOR** | `professor@campus.edu.br` | `professor123` |
| **ALUNO** | `aluno@campus.edu.br` | `aluno123` |

---

## API Reference

Todos os endpoints (exceto `POST /api/auth/login`) requerem o header:

```
Authorization: Bearer <token>
```

### Autenticação

#### `POST /api/auth/login`

Autentica um usuário e retorna um JWT.

**Request:**
```json
{
  "email": "professor@campus.edu.br",
  "password": "professor123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "name": "Professor Teste",
    "email": "professor@campus.edu.br",
    "role": "PROFESSOR"
  }
}
```

**Erros:** `400` campos inválidos · `401` credenciais incorretas

---

### Laboratórios

#### `GET /api/laboratories`

Lista todos os laboratórios com status de disponibilidade.

**Response `200 OK`:**
```json
[
  {
    "id": "uuid",
    "name": "Laboratório de Redes",
    "block": "Bloco B",
    "reserved": true,
    "createdAt": "2026-09-29T10:00:00Z",
    "updatedAt": "2026-09-29T10:00:00Z"
  }
]
```

O campo `reserved` é `true` se houver ao menos uma reserva com `startAt` após o instante atual.

---

#### `GET /api/laboratories/{id}`

Retorna os detalhes de um laboratório, incluindo todas as suas reservas.

**Response `200 OK`:**
```json
{
  "id": "uuid",
  "name": "Laboratório de Redes",
  "block": "Bloco B",
  "reserved": true,
  "createdAt": "2026-09-29T10:00:00Z",
  "updatedAt": "2026-09-29T10:00:00Z",
  "reservations": [
    {
      "id": "uuid",
      "laboratoryId": "uuid",
      "userId": "uuid",
      "startAt": "2026-09-30T08:00:00Z",
      "endAt": "2026-09-30T10:00:00Z",
      "createdAt": "2026-09-29T10:00:00Z"
    }
  ]
}
```

**Erros:** `404` laboratório não encontrado

---

#### `POST /api/laboratories` — PROFESSOR

Cria um novo laboratório.

**Request:**
```json
{
  "name": "Laboratório de Redes",
  "block": "Bloco B"
}
```

**Response `201 Created`**

**Erros:** `400` dados inválidos · `409` nome já existe

---

#### `PUT /api/laboratories/{id}` — PROFESSOR

Atualiza um laboratório existente.

**Request:** mesmo formato do `POST`

**Response `200 OK`**

**Erros:** `400` · `404` · `409` nome duplicado

---

#### `DELETE /api/laboratories/{id}` — PROFESSOR

Exclui um laboratório.

**Response `204 No Content`**

**Erros:** `404` não encontrado · `409` laboratório possui reservas futuras

---

### Reservas

#### `GET /api/laboratories/{labId}/reservations`

Lista todas as reservas de um laboratório, ordenadas por `startAt` crescente.

**Response `200 OK`:** lista de `ReservationResponse`

**Erros:** `404` laboratório não encontrado

---

#### `GET /api/reservations/{id}`

Retorna os detalhes de uma reserva.

**Response `200 OK`:** `ReservationResponse`

**Erros:** `404` reserva não encontrada

---

#### `POST /api/laboratories/{labId}/reservations` — PROFESSOR

Cria uma nova reserva.

**Request:**
```json
{
  "startAt": "2026-09-30T08:00:00Z",
  "endAt": "2026-09-30T10:00:00Z"
}
```

Os campos são em formato ISO 8601 UTC (`Instant`).

**Response `201 Created`**

**Erros:** `400` período inválido (duração < 1 min ou `startAt >= endAt`) · `404` laboratório não encontrado · `409` conflito de horário

---

#### `PUT /api/reservations/{id}` — PROFESSOR

Atualiza uma reserva existente. A verificação de conflito exclui a própria reserva, permitindo alterar horário sem falso conflito.

**Request:** mesmo formato do `POST`

**Response `200 OK`**

**Erros:** `400` · `404` · `409` conflito com outra reserva

---

#### `DELETE /api/reservations/{id}` — PROFESSOR

Exclui uma reserva.

**Response `204 No Content`**

**Erros:** `404` reserva não encontrada

---

### Formato de Erros

Todos os erros retornam o mesmo formato:

```json
{
  "timestamp": "2026-09-29T10:00:00Z",
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Laboratório não encontrado",
  "path": "/api/laboratories/uuid"
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `timestamp` | ISO 8601 UTC | Momento do erro |
| `status` | `int` | Código HTTP |
| `code` | `string` | Código semântico do erro |
| `message` | `string` | Mensagem legível (máx. 512 caracteres) |
| `path` | `string` | Endpoint chamado |

**Códigos de erro disponíveis:**

| Code | HTTP | Situação |
|---|---|---|
| `RESOURCE_NOT_FOUND` | 404 | Recurso inexistente |
| `AUTHENTICATION_REQUIRED` | 401 | Token ausente, inválido ou expirado |
| `INSUFFICIENT_PERMISSIONS` | 403 | Role sem acesso ao endpoint |
| `VALIDATION_ERROR` | 400 | Campos com anotações `@Valid` violadas |
| `MALFORMED_REQUEST` | 400 | Corpo da requisição ausente ou malformado |
| `INVALID_RESERVATION_PERIOD` | 400 | `startAt >= endAt` ou duração < 1 min |
| `DUPLICATE_LABORATORY_NAME` | 409 | Nome de laboratório já existe |
| `RESERVATION_CONFLICT` | 409 | Sobreposição de horário no laboratório |
| `LABORATORY_HAS_FUTURE_RESERVATIONS` | 409 | Tentativa de excluir lab com reservas futuras |
| `INTERNAL_ERROR` | 500 | Erro interno inesperado |

---

## Modelo de Autorização

| Endpoint | ALUNO | PROFESSOR |
|---|---|---|
| `POST /api/auth/login` | ✅ público | ✅ público |
| `GET /api/laboratories/**` | ✅ | ✅ |
| `GET /api/reservations/**` | ✅ | ✅ |
| `POST /api/laboratories/**` | ❌ 403 | ✅ |
| `PUT /api/laboratories/**` | ❌ 403 | ✅ |
| `DELETE /api/laboratories/**` | ❌ 403 | ✅ |
| `POST /api/reservations/**` | ❌ 403 | ✅ |
| `PUT /api/reservations/**` | ❌ 403 | ✅ |
| `DELETE /api/reservations/**` | ❌ 403 | ✅ |

---

## Estrutura do Projeto

### Backend — `backend/src/main/java/com/campuslab/`

```
├── CampusLabApplication.java       # Entry point
├── auth/                           # Domínio de autenticação
│   ├── AuthController.java         # POST /api/auth/login
│   ├── AuthService.java            # Lógica de login e encoding
│   └── dto/
│       ├── LoginRequest.java
│       ├── LoginResponse.java
│       └── UserSummary.java
├── user/                           # Domínio de usuários (uso interno)
│   ├── User.java
│   ├── UserRole.java               # Enum: ALUNO, PROFESSOR
│   └── UserRepository.java
├── laboratory/                     # Domínio de laboratórios
│   ├── Laboratory.java
│   ├── LaboratoryController.java
│   ├── LaboratoryRepository.java
│   ├── LaboratoryService.java
│   └── dto/
│       ├── LaboratoryRequest.java
│       ├── LaboratoryResponse.java
│       └── LaboratoryDetailResponse.java
├── reservation/                    # Domínio de reservas
│   ├── Reservation.java
│   ├── ReservationController.java
│   ├── ReservationRepository.java
│   ├── ReservationService.java
│   └── dto/
│       ├── ReservationRequest.java
│       └── ReservationResponse.java
└── shared/                         # Infraestrutura transversal
    ├── config/
    │   └── SecurityConfig.java     # Regras de autorização e filtros
    ├── dto/
    │   └── ErrorResponse.java      # Formato padrão de erro
    ├── exception/
    │   ├── ErrorHandler.java       # @RestControllerAdvice
    │   └── exceptions/             # Exceções de domínio
    └── security/
        ├── JwtService.java         # Geração e validação de JWT
        └── SecurityFilter.java     # OncePerRequestFilter
```

### Frontend — `frontend/src/`

```
├── App.tsx                         # Configuração de rotas
├── api/
│   ├── axiosClient.ts              # Instância Axios + interceptors
│   ├── authApi.ts
│   ├── laboratoryApi.ts
│   └── reservationApi.ts
├── components/
│   ├── ProtectedRoute.tsx          # Redireciona para /login se não autenticado
│   ├── RoleGuard.tsx               # Renderiza filhos somente para o role correto
│   ├── LaboratoryForm.tsx
│   ├── ReservationForm.tsx
│   ├── LoadingSpinner.tsx
│   └── ErrorMessage.tsx
├── pages/
│   ├── LoginPage.tsx
│   ├── LaboratoryListPage.tsx
│   ├── LaboratoryDetailPage.tsx
│   └── NotFoundPage.tsx
├── store/
│   └── authStore.ts                # Zustand: token, user, isAuthenticated
└── types/
    ├── auth.ts
    ├── laboratory.ts
    ├── reservation.ts
    └── api.ts
```

### Migrações — `backend/src/main/resources/db/migration/`

| Arquivo | Descrição |
|---|---|
| `V1__Create_initial_schema.sql` | Cria as tabelas `users`, `laboratories`, `reservations` e índices |
| `V2__Insert_seed_users.sql` | Insere usuários de teste (professor e aluno) |

---

## Testes

### Backend

O projeto usa três camadas de teste:

**Testes de propriedade (jqwik)** — verificam invariantes com centenas de entradas geradas automaticamente:

| Propriedade | Classe | Descrição |
|---|---|---|
| Property 1 | `AuthServicePropertyTest` | BCrypt hash é verificável para qualquer senha |
| Property 2 | `JwtServicePropertyTest` | JWT gerado contém userId e role corretos |
| Property 3 | `SecurityFilterPropertyTest` | Tokens inválidos sempre retornam 401 |
| Property 4 | `LaboratoryServicePropertyTest` | Campo `reserved` reflete futuras reservas |
| Property 5 | `LaboratoryServiceDeletePropertyTest` | Exclusão com reservas futuras sempre rejeitada |
| Property 10 | `ErrorHandlerPropertyTest` | Todas as respostas de erro seguem o formato padrão |

**Testes de integração (Testcontainers)** — sobem PostgreSQL real via Docker:

- `AuthControllerIntegrationTest`
- `LaboratoryControllerIntegrationTest`
- `ReservationControllerIntegrationTest`

> Os testes de integração requerem Docker em execução.

```bash
# Rodar todos os testes
cd backend && ./mvnw test

# Somente property tests
./mvnw test -Dtest="**/*PropertyTest"

# Somente integration tests
./mvnw test -Dtest="**/*IntegrationTest"
```

### Frontend

```bash
# Rodar todos os testes (modo único)
cd frontend && npm run test:run

# Modo watch
npm test
```

---

## Variáveis de Ambiente

### Backend

| Variável | Padrão | Descrição |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/campuslab` | URL de conexão PostgreSQL |
| `DATABASE_USERNAME` | `postgres` | Usuário do banco |
| `DATABASE_PASSWORD` | `postgres` | Senha do banco |
| `JWT_SECRET` | (chave base64 padrão) | Segredo HMAC para assinar JWTs — troque em produção |
| `JWT_EXPIRES_IN` | `86400` | Tempo de vida do token em segundos (padrão: 24h) |
| `SERVER_PORT` | `8080` | Porta HTTP do servidor |
| `LOG_LEVEL` | `INFO` | Nível de log da aplicação |
| `SHOW_SQL` | `false` | Exibe queries SQL no log (use somente em desenvolvimento) |

### Frontend

| Variável | Padrão | Descrição |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | URL base da API backend |
