# CampusLab Backend

Backend REST API para o sistema de gerenciamento de laboratórios universitários.

## Tecnologias

- **Java 21**
- **Spring Boot 3.3.2**
- **Spring Security** (JWT stateless)
- **Spring Data JPA** + **PostgreSQL**
- **Flyway** (database migrations)
- **JJWT 0.12.6** (JWT generation/validation)
- **jqwik 1.8.5** (property-based testing)
- **Testcontainers 1.20.1** (integration testing)

## Estrutura de Pacotes

```
com.campuslab
├── auth/               # Autenticação e JWT
│   └── dto/            # DTOs de login/auth
├── user/               # Usuários e roles
├── laboratory/         # Gerenciamento de laboratórios
│   └── dto/            # DTOs de laboratórios
├── reservation/        # Gerenciamento de reservas
│   └── dto/            # DTOs de reservas
└── shared/             # Componentes compartilhados
    ├── config/         # Configurações (Security, JWT)
    ├── security/       # SecurityFilter, JwtService
    └── exception/      # GlobalExceptionHandler, custom exceptions
```

## Configuração

### Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/campuslab` | URL do PostgreSQL |
| `DATABASE_USERNAME` | `postgres` | Usuário do banco |
| `DATABASE_PASSWORD` | `postgres` | Senha do banco |
| `JWT_SECRET` | (base64 default) | Chave secreta JWT (≥256 bits) |
| `JWT_EXPIRES_IN` | `86400` | Tempo de expiração do token em segundos |
| `SERVER_PORT` | `8080` | Porta do servidor |
| `LOG_LEVEL` | `INFO` | Nível de log |

### Executar Localmente

```bash
# Instalar dependências
mvn clean install

# Executar aplicação
mvn spring-boot:run

# Executar testes
mvn test

# Executar testes de propriedade (jqwik)
mvn test -Dtest="**/*PropertyTest"
```

## Database Migrations

As migrations são gerenciadas pelo Flyway e estão em `src/main/resources/db/migration/`.

## Testes

O projeto utiliza três abordagens de teste:
- **Unit tests** (JUnit 5 + Mockito)
- **Property-based tests** (jqwik)
- **Integration tests** (Spring Boot Test + Testcontainers)

## Endpoints da API

Ver [design.md](../.kiro/specs/campus-lab/design.md) para documentação completa dos endpoints.

### Autenticação
- `POST /api/auth/login` - Login

### Laboratórios
- `GET /api/laboratories` - Listar laboratórios
- `GET /api/laboratories/{id}` - Detalhes do laboratório
- `POST /api/laboratories` - Criar laboratório (PROFESSOR)
- `PUT /api/laboratories/{id}` - Atualizar laboratório (PROFESSOR)
- `DELETE /api/laboratories/{id}` - Excluir laboratório (PROFESSOR)

### Reservas
- `GET /api/laboratories/{labId}/reservations` - Listar reservas
- `GET /api/reservations/{id}` - Detalhes da reserva
- `POST /api/laboratories/{labId}/reservations` - Criar reserva (PROFESSOR)
- `PUT /api/reservations/{id}` - Atualizar reserva (PROFESSOR)
- `DELETE /api/reservations/{id}` - Excluir reserva (PROFESSOR)
