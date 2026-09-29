# Implementation Plan: CampusLab — Sistema de Gerenciamento de Laboratórios do Campus

## Overview

Implementação completa do CampusLab em duas frentes independentes: **backend** (API REST em Java 21 + Spring Boot 3) e **frontend** (SPA em React + TypeScript + Tailwind CSS). O backend expõe endpoints REST protegidos por JWT, com persistência em PostgreSQL via Spring Data JPA. O frontend consome essa API com Axios e gerencia autenticação com estado global (Zustand/Context API). Testes baseados em propriedades (jqwik no backend, fast-check no frontend) validam as 12 propriedades de correção definidas no design.

---

## Tasks

### BACKEND

- [x] 1. Configurar projeto backend (Spring Boot)
  - Gerar projeto via Spring Initializr com dependências: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
  - Adicionar dependências de teste: `jqwik`, `jqwik-spring`, `testcontainers`, `postgresql` (Testcontainers), `spring-boot-starter-test`
  - Configurar `application.properties` / `application.yml` com datasource, JPA (`ddl-auto=validate`), JWT secret e `expiresIn`
  - Criar estrutura de pacotes conforme design: `com.campuslab.{auth, user, laboratory, reservation, shared}`
  - _Requirements: 1.1, 2.1_

- [x] 2. Implementar modelo de dados e entidades JPA
  - [x] 2.1 Criar entidade `User` e enum `UserRole`
    - Implementar `User` com `@Entity`, campos `id (UUID)`, `name`, `email (unique)`, `password`, `role`, `createdAt`, `updatedAt` com `@CreationTimestamp` / `@UpdateTimestamp`
    - Implementar `UserRole` enum com valores `ALUNO` e `PROFESSOR`
    - Criar `UserRepository` com `findByEmail(String email): Optional<User>`
    - _Requirements: 1.1, 1.4_

  - [x] 2.2 Criar entidade `Laboratory`
    - Implementar `Laboratory` com `@Entity`, campos `id (UUID)`, `name (unique)`, `block`, `reservations (OneToMany)`, `createdAt`, `updatedAt`
    - Criar `LaboratoryRepository` com `findByName(String name): Optional<Laboratory>` e `existsByNameAndIdNot(String name, UUID id): boolean`
    - _Requirements: 3.1, 4.1_

  - [x] 2.3 Criar entidade `Reservation`
    - Implementar `Reservation` com `@Entity`, campos `id (UUID)`, `laboratory (ManyToOne)`, `user (ManyToOne)`, `startAt`, `endAt`, `createdAt`, `updatedAt`
    - Adicionar `CONSTRAINT chk_reservation_order CHECK (end_at > start_at)` no schema
    - _Requirements: 6.1, 7.1_

  - [x] 2.4 Criar schema SQL e migration
    - Criar arquivo de migration (Flyway ou script SQL) com as tabelas `users`, `laboratories`, `reservations` conforme o schema do design
    - Criar índice `idx_reservations_lab_time` em `(laboratory_id, start_at, end_at)`
    - _Requirements: 3.1, 5.1_

- [x] 3. Implementar infraestrutura de segurança e JWT
  - [x] 3.1 Implementar `JwtService`
    - Criar `JwtService` com métodos `generateToken(User)`, `validateToken(String): Claims`, `extractUserId(String)`, `extractRole(String)` usando JJWT
    - O payload do JWT deve conter `sub (userId)`, `role`, `iat` e `exp = iat + expiresIn`
    - Validar que `expiresIn` está entre 60 e 86400 segundos na configuração
    - _Requirements: 1.5, 2.3_

  - [x]* 3.2 Escrever property test para `JwtService` (Property 2)
    - **Property 2: JWT gerado contém userId e role corretos**
    - **Validates: Requirements 1.5**

  - [x] 3.3 Implementar `SecurityFilter` (OncePerRequestFilter)
    - Criar `SecurityFilter` que extrai o header `Authorization: Bearer <token>`, chama `JwtService.validateToken`, cria `UsernamePasswordAuthenticationToken` com authorities e chama `SecurityContextHolder.getContext().setAuthentication(...)`
    - Em caso de falha na validação, não lançar exceção — deixar a requisição prosseguir sem autenticação
    - _Requirements: 2.1, 2.2, 2.3, 2.5, 2.6_

  - [x]* 3.4 Escrever property test para `SecurityFilter` (Property 3)
    - **Property 3: Tokens inválidos sempre retornam 401**
    - **Validates: Requirements 2.3**

  - [x] 3.5 Implementar `SecurityConfig`
    - Configurar `SecurityFilterChain` com CSRF desabilitado, sessão `STATELESS`, regras `authorizeHttpRequests` conforme design (POST `/api/auth/login` público, GETs com `ALUNO` ou `PROFESSOR`, demais endpoints com `PROFESSOR`), e registrar `SecurityFilter` antes de `UsernamePasswordAuthenticationFilter`
    - Configurar `AuthenticationEntryPoint` e `AccessDeniedHandler` para retornar `ErrorResponse` em vez de respostas padrão do Spring Security
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [x] 4. Implementar autenticação (`AuthController` e `AuthService`)
  - [x] 4.1 Criar DTOs de autenticação
    - Criar `LoginRequest` com campos `email` e `password`, com anotações `@NotBlank`, `@Email`, `@Size(max=255)`
    - Criar `LoginResponse` com campos `token`, `type`, `expiresIn` e `user (UserSummary)`
    - _Requirements: 1.1, 1.3_

  - [x] 4.2 Implementar `AuthService`
    - Implementar `login(LoginRequest): LoginResponse` que busca usuário por email com `UserRepository.findByEmail`, verifica senha com `BCryptPasswordEncoder.matches`, gera JWT com `JwtService.generateToken` e retorna `LoginResponse`
    - Lançar exceção mapeada para HTTP 401 quando credenciais forem inválidas (sem indicar qual campo está incorreto)
    - Expor método `encodePassword(String): String` usando `BCryptPasswordEncoder.encode`
    - _Requirements: 1.1, 1.2, 1.4_

  - [x]* 4.3 Escrever property test para `AuthService` (Property 1)
    - **Property 1: BCrypt hash é verificável para qualquer senha**
    - **Validates: Requirements 1.4**

  - [x] 4.4 Implementar `AuthController`
    - Criar `POST /api/auth/login` que delega para `AuthService.login` e retorna HTTP 200 com `LoginResponse`
    - _Requirements: 1.1, 1.3_

- [x] 5. Implementar tratamento global de erros
  - [x] 5.1 Criar exceções customizadas e `ErrorResponse`
    - Criar classes `ResourceNotFoundException`, `ReservationConflictException`, `LaboratoryHasFutureReservationsException`, `DuplicateNameException`, `InvalidReservationPeriodException`
    - Criar `ErrorResponse` com campos `timestamp (Instant)`, `status (int)`, `code (String)`, `message (String, max 512)`, `path (String)`
    - _Requirements: 8.1, 8.3_

  - [x] 5.2 Implementar `ErrorHandler` (`@RestControllerAdvice`)
    - Criar `@ExceptionHandler` para cada exceção customizada, `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `AccessDeniedException`, `AuthenticationException`, `DataAccessException` e `Exception` genérica
    - Mapear cada exceção ao código HTTP e `code` string conforme tabela do design
    - Nunca expor stack traces, nomes de classes internas ou detalhes de query na resposta
    - _Requirements: 8.1, 8.2, 8.3_

  - [x]* 5.3 Escrever property test para `ErrorHandler` (Property 10)
    - **Property 10: Todas as respostas de erro seguem o formato padronizado**
    - **Validates: Requirements 8.1, 8.3**

- [x] 6. Checkpoint — Backend base
  - Garantir que o contexto Spring sobe sem erros, o `SecurityFilter` está funcionando para endpoints públicos e protegidos, e os testes de propriedade das tarefas anteriores passam.

- [x] 7. Implementar CRUD de laboratórios
  - [x] 7.1 Criar DTOs de laboratório
    - Criar `LaboratoryRequest` com campos `name` (`@NotBlank`, `@Size(max=100)`) e `block` (`@NotBlank`, `@Size(max=20)`)
    - Criar `LaboratoryResponse` com campos `id`, `name`, `block`, `reserved`, `createdAt`, `updatedAt`
    - Criar `LaboratoryDetailResponse` estendendo `LaboratoryResponse` com campo `reservations (List<ReservationResponse>)`
    - _Requirements: 3.1, 3.2, 4.1_

  - [x] 7.2 Implementar consultas customizadas em `ReservationRepository`
    - Adicionar query JPQL `existsConflict(labId, excludeId, startAt, endAt, now): boolean`
    - Adicionar query JPQL `existsFutureReservation(labId, now): boolean`
    - Adicionar `findByLaboratoryIdOrderByStartAtAsc(UUID): List<Reservation>`
    - _Requirements: 3.4, 4.4, 5.1, 7.2, 7.4_

  - [x] 7.3 Implementar `LaboratoryService`
    - Implementar `findAll()`: retorna lista de `LaboratoryResponse` com `reserved` calculado via `existsFutureReservation`
    - Implementar `findById(UUID)`: retorna `LaboratoryDetailResponse` com reservas incluídas, lança `ResourceNotFoundException` se não encontrado
    - Implementar `create(LaboratoryRequest)`: verifica unicidade de `name` (lança `DuplicateNameException` se duplicado), persiste e retorna HTTP 201
    - Implementar `update(UUID, LaboratoryRequest)`: verifica existência (lança `ResourceNotFoundException`) e unicidade de `name`, persiste e retorna HTTP 200
    - Implementar `delete(UUID)`: verifica existência e ausência de `FutureReservation` (lança `LaboratoryHasFutureReservationsException` se houver), exclui e retorna HTTP 204
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.6, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [x]* 7.4 Escrever property test para `LaboratoryService` (Property 4)
    - **Property 4: Campo `reserved` reflete corretamente as FutureReservations**
    - **Validates: Requirements 3.4**

  - [x]* 7.5 Escrever property test para `LaboratoryService` (Property 5)
    - **Property 5: Exclusão de laboratório com reservas futuras sempre é rejeitada**
    - **Validates: Requirements 4.4**

  - [x] 7.6 Implementar `LaboratoryController`
    - Mapear `GET /api/laboratories` → `findAll()` (HTTP 200)
    - Mapear `GET /api/laboratories/{id}` → `findById(UUID)` (HTTP 200)
    - Mapear `POST /api/laboratories` → `create(LaboratoryRequest)` com `@Valid` (HTTP 201)
    - Mapear `PUT /api/laboratories/{id}` → `update(UUID, LaboratoryRequest)` com `@Valid` (HTTP 200)
    - Mapear `DELETE /api/laboratories/{id}` → `delete(UUID)` (HTTP 204)
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 4.1, 4.2, 4.3, 4.5, 4.6, 4.7_

  - [ ]* 7.7 Escrever testes de unidade para `LaboratoryService`
    - Testar `findAll`, `findById` (encontrado e não encontrado), `create` (sucesso e nome duplicado), `update` (sucesso, não encontrado, nome duplicado), `delete` (sucesso e com reservas futuras)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

- [x] 8. Implementar CRUD de reservas
  - [x] 8.1 Criar DTOs de reserva
    - Criar `ReservationRequest` com campos `startAt` e `endAt` (`@NotNull`, anotações de formato `Instant`)
    - Criar `ReservationResponse` com campos `id`, `laboratoryId`, `userId`, `startAt`, `endAt`, `createdAt`
    - _Requirements: 5.2, 6.1_

  - [x] 8.2 Implementar `ReservationService`
    - Implementar `findByLaboratory(UUID)`: verifica existência do laboratório (lança `ResourceNotFoundException`) e retorna lista ordenada por `startAt` crescente
    - Implementar `findById(UUID)`: retorna `ReservationResponse`, lança `ResourceNotFoundException` se não encontrado
    - Implementar `create(UUID labId, ReservationRequest)`: valida `laboratoryId` existe, chama `validatePeriod`, chama `checkConflict` (com `excludeId = nil UUID`), persiste e retorna HTTP 201
    - Implementar `update(UUID id, ReservationRequest)`: verifica existência, chama `validatePeriod`, chama `checkConflict` (com `excludeId = id` da própria reserva), persiste e retorna HTTP 200
    - Implementar `delete(UUID id)`: verifica existência, exclui e retorna HTTP 204
    - Implementar `validatePeriod(Instant, Instant)` privado: rejeita se `startAt >= endAt` ou `duration < 60s`
    - Implementar `checkConflict(UUID labId, UUID excludeId, Instant, Instant)` privado: rejeita se `existsConflict` retornar `true`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [ ]* 8.3 Escrever property test para `ReservationService` (Property 6)
    - **Property 6: Lista de reservas sempre está ordenada por `startAt` crescente**
    - **Validates: Requirements 5.1**

  - [ ]* 8.4 Escrever property test para `ReservationService` (Property 7)
    - **Property 7: Intervalos inválidos (startAt >= endAt ou duração < 1 min) são sempre rejeitados**
    - **Validates: Requirements 7.1, 7.6**

  - [ ]* 8.5 Escrever property test para `ReservationService` (Property 8)
    - **Property 8: Intervalos sobrepostos são sempre rejeitados (invariante de não-conflito)**
    - **Validates: Requirements 7.2, 7.5**

  - [ ]* 8.6 Escrever property test para `ReservationService` (Property 9)
    - **Property 9: Atualização de reserva não conflita consigo mesma**
    - **Validates: Requirements 7.3**

  - [x] 8.7 Implementar `ReservationController`
    - Mapear `GET /api/laboratories/{labId}/reservations` → `findByLaboratory(UUID)` (HTTP 200)
    - Mapear `GET /api/reservations/{id}` → `findById(UUID)` (HTTP 200)
    - Mapear `POST /api/laboratories/{labId}/reservations` → `create(UUID, ReservationRequest)` com `@Valid` (HTTP 201)
    - Mapear `PUT /api/reservations/{id}` → `update(UUID, ReservationRequest)` com `@Valid` (HTTP 200)
    - Mapear `DELETE /api/reservations/{id}` → `delete(UUID)` (HTTP 204)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

  - [ ]* 8.8 Escrever testes de unidade para `ReservationService`
    - Testar `findByLaboratory` (lab existente, inexistente), `findById` (encontrado, não encontrado), `create` (sucesso, lab inexistente, período inválido, conflito), `update` (sucesso, não encontrado, conflito com outra reserva, sem conflito consigo mesma), `delete` (sucesso, não encontrado)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.6_

- [x] 9. Testes de integração do backend
  - [x] 9.1 Implementar testes de integração para `AuthController`
    - Configurar `@SpringBootTest` + Testcontainers PostgreSQL com `@DynamicPropertySource`
    - Testar login com sucesso (HTTP 200 + JWT), credenciais inválidas (HTTP 401), campos ausentes (HTTP 400)
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 9.2 Implementar testes de integração para `LaboratoryController`
    - Testar todos os endpoints com usuários `ALUNO` e `PROFESSOR` (controle de acesso), cenários de sucesso e erro (404, 409, 400)
    - _Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [x] 9.3 Implementar testes de integração para `ReservationController`
    - Testar criação com conflito de horário (HTTP 409), período inválido (HTTP 400), laboratório inexistente (HTTP 404), atualização desconsiderando a própria reserva
    - _Requirements: 5.1, 5.2, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 7.1, 7.2, 7.3_

- [x] 10. Checkpoint — Backend completo
  - Garantir que todos os testes de unidade, propriedade e integração do backend passam. Verificar cobertura mínima esperada por camada conforme design.

---

### FRONTEND

- [x] 11. Configurar projeto frontend (Vite + React + TypeScript)
  - Criar projeto com Vite usando template `react-ts`
  - Instalar dependências: `tailwindcss`, `postcss`, `autoprefixer`, `react-router-dom`, `axios`, `zustand` (ou usar Context API nativo)
  - Instalar dependências de teste: `vitest`, `@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom`, `fast-check`, `@fast-check/vitest`
  - Configurar `tailwind.config.js` e importar diretivas Tailwind no CSS global
  - Configurar `vitest.config.ts` com `jsdom` environment e setup file para `@testing-library/jest-dom`
  - Criar estrutura de diretórios: `src/{api, components, pages, hooks, store, types}`
  - _Requirements: 9.1, 10.1_

- [x] 12. Definir tipos TypeScript
  - [x] 12.1 Criar tipos de domínio
    - Criar `src/types/auth.ts`: interfaces `LoginRequest`, `LoginResponse`, `UserSummary`, `AuthState`
    - Criar `src/types/laboratory.ts`: interfaces `Laboratory`, `LaboratoryDetail`, `LaboratoryRequest`, `LaboratoryResponse`
    - Criar `src/types/reservation.ts`: interfaces `Reservation`, `ReservationRequest`, `ReservationResponse`
    - Criar `src/types/api.ts`: interface `ErrorResponse` com campos `timestamp`, `status`, `code`, `message`, `path`
    - _Requirements: 1.5, 3.1, 5.2_

- [x] 13. Implementar cliente HTTP e store de autenticação
  - [x] 13.1 Implementar `axiosClient`
    - Criar `src/api/axiosClient.ts` com instância Axios configurada com `baseURL` apontando para a API
    - Adicionar interceptor de requisição que injeta `Authorization: Bearer <token>` quando o token estiver disponível no store
    - Adicionar interceptor de resposta que trata HTTP 401 limpando o estado de autenticação e redirecionando para `/login`
    - _Requirements: 1.6, 2.1_

  - [x] 13.2 Implementar `authStore`
    - Criar `src/store/authStore.ts` com estado `{ token, user, isAuthenticated }` e ações `setAuth(LoginResponse)`, `clearAuth()`
    - Persistir o token em `localStorage` para sobreviver a refreshes de página
    - Expor `getUserRole(): UserRole | null` derivado do token armazenado
    - _Requirements: 1.6, 9.5_

  - [x] 13.3 Implementar funções de API
    - Criar `src/api/authApi.ts` com `login(LoginRequest): Promise<LoginResponse>`
    - Criar `src/api/laboratoryApi.ts` com `getAll()`, `getById(id)`, `create(req)`, `update(id, req)`, `remove(id)`
    - Criar `src/api/reservationApi.ts` com `getByLaboratory(labId)`, `getById(id)`, `create(labId, req)`, `update(id, req)`, `remove(id)`
    - _Requirements: 3.1, 4.1, 5.1, 6.1_

- [x] 14. Implementar componentes de infraestrutura
  - [x] 14.1 Implementar `ProtectedRoute`
    - Criar `src/components/ProtectedRoute.tsx` que verifica `isAuthenticated` no `authStore` e redireciona para `/login` se não autenticado
    - _Requirements: 9.5, 10.1_

  - [x] 14.2 Implementar `RoleGuard`
    - Criar `src/components/RoleGuard.tsx` que recebe `requiredRole` prop e renderiza filhos somente se o role do usuário autenticado corresponder
    - _Requirements: 10.2, 10.3, 12.1_

  - [x] 14.3 Implementar componentes utilitários
    - Criar `src/components/LoadingSpinner.tsx`: spinner acessível com `role="status"` e `aria-label`
    - Criar `src/components/ErrorMessage.tsx`: exibe mensagem de erro com botão de retry opcional
    - _Requirements: 10.5, 10.7_

  - [x] 14.4 Configurar roteamento principal
    - Criar estrutura de rotas em `App.tsx` ou `src/router/index.tsx` com: `/login` (público), `/laboratories` (protegido), `/laboratories/:id` (protegido), `*` (NotFoundPage)
    - Envolver rotas protegidas com `ProtectedRoute`
    - _Requirements: 9.5, 10.1_

- [x] 15. Implementar tela de Login
  - [x] 15.1 Implementar `LoginPage`
    - Criar `src/pages/LoginPage.tsx` com formulário contendo campo `email`, campo `password` e botão de submissão
    - Implementar validação frontend: campo `email` vazio/inválido exibe mensagem sem chamar API; campo `password` vazio exibe mensagem sem chamar API
    - Chamar `authApi.login`, armazenar token no `authStore` e redirecionar para `/laboratories` em caso de sucesso
    - Exibir mensagem de erro genérica (sem indicar qual campo) quando API retornar HTTP 401, preservando os valores dos campos
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

  - [ ]* 15.2 Escrever testes de componente para `LoginPage`
    - Testar: exibição do formulário, validação de email vazio, validação de senha vazia, erro HTTP 401 preserva campos, redirecionamento após sucesso
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 16. Implementar listagem de laboratórios
  - [x] 16.1 Implementar `LaboratoryListPage`
    - Criar `src/pages/LaboratoryListPage.tsx` que chama `laboratoryApi.getAll()` ao montar
    - Exibir lista com campos `name`, `block` e status `reserved` como "Reservado" / "Disponível"
    - Exibir `LoadingSpinner` enquanto carrega, desabilitando interações dependentes de dados
    - Exibir mensagem de empty state quando lista retornar vazia
    - Exibir `ErrorMessage` com botão de retry quando API retornar erro
    - _Requirements: 10.1, 10.5, 10.6, 10.7_

  - [x] 16.2 Implementar controle de ações por role na `LaboratoryListPage`
    - Usar `RoleGuard` para exibir botões "Criar Laboratório", "Editar" e "Excluir" somente para `PROFESSOR`
    - Ocultar completamente esses botões do DOM para `ALUNO`
    - _Requirements: 10.2, 10.3_

  - [ ]* 16.3 Escrever property test para controle de visibilidade por role (Property 11)
    - **Property 11: Controle de visibilidade de ações por role (Frontend)**
    - **Validates: Requirements 10.2, 10.3, 12.1**

  - [ ]* 16.4 Escrever testes de componente para `LaboratoryListPage`
    - Testar: renderização da lista, estado de carregamento, empty state, error state com retry, visibilidade de botões por role
    - _Requirements: 10.1, 10.2, 10.3, 10.5, 10.6, 10.7_

- [x] 17. Implementar detalhes e gerenciamento de laboratórios
  - [x] 17.1 Implementar `LaboratoryDetailPage`
    - Criar `src/pages/LaboratoryDetailPage.tsx` que chama `laboratoryApi.getById(id)` com o `id` da rota
    - Exibir dados do laboratório e lista de reservas associadas
    - Exibir `LoadingSpinner` durante carregamento e `ErrorMessage` em caso de erro
    - _Requirements: 10.4_

  - [x] 17.2 Implementar formulário de laboratório (criação e edição)
    - Criar componente `LaboratoryForm` (modal ou página) com campos `name` (`maxLength=100`) e `block` (`maxLength=20`)
    - Implementar validação frontend: campos obrigatórios, limites de caracteres — sem enviar requisição se inválido
    - Chamar `laboratoryApi.create` ou `laboratoryApi.update` conforme o modo
    - Exibir feedback de sucesso visível por pelo menos 3 segundos após operação bem-sucedida
    - Atualizar lista de laboratórios após sucesso
    - Tratar HTTP 400 exibindo mensagens de erro nos respectivos campos
    - _Requirements: 11.1, 11.2, 11.4_

  - [x] 17.3 Implementar exclusão de laboratório
    - Adicionar confirmação antes de chamar `laboratoryApi.remove(id)`
    - Tratar HTTP 409 com código `LABORATORY_HAS_FUTURE_RESERVATIONS` exibindo mensagem específica
    - Atualizar lista após exclusão bem-sucedida
    - _Requirements: 11.3_

  - [x] 17.4 Escrever testes de componente para formulário de laboratório
    - Testar: validação de campos obrigatórios e limites, envio bem-sucedido com feedback de sucesso, tratamento de erro HTTP 400, exibição de mensagem para HTTP 409
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [x] 18. Implementar gerenciamento de reservas
  - [x] 18.1 Implementar formulário de reserva (criação e edição)
    - Criar componente `ReservationForm` (modal ou inline) com campos `startAt` e `endAt` (datetime inputs)
    - Implementar validação frontend: campos obrigatórios, `startAt` >= `endAt` exibe mensagem específica, formato de data inválido exibe mensagem específica — sem enviar requisição se inválido
    - Usar `RoleGuard` para exibir o formulário e botões de ação somente para `PROFESSOR`
    - _Requirements: 12.1, 12.2_

  - [ ]* 18.2 Escrever property test para validação de formulário de reserva (Property 12)
    - **Property 12: Formulários com entrada inválida nunca disparam requisições à API**
    - **Validates: Requirements 11.1, 12.2**

  - [x] 18.3 Implementar criação, edição e exclusão de reservas
    - Chamar `reservationApi.create(labId, req)`, `reservationApi.update(id, req)` ou `reservationApi.remove(id)` conforme a ação
    - Tratar HTTP 409 exibindo mensagem de conflito de horário
    - Exibir feedback de sucesso visível por pelo menos 3 segundos após operação bem-sucedida
    - Atualizar lista de reservas do laboratório após cada operação bem-sucedida
    - _Requirements: 12.3, 12.4_

  - [x] 18.4 Escrever testes de componente para gerenciamento de reservas
    - Testar: validação de campos, exibição de botões somente para `PROFESSOR`, tratamento de conflito HTTP 409, feedback de sucesso
    - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [x] 19. Implementar responsividade
  - [x] 19.1 Aplicar classes responsivas do Tailwind CSS
    - Revisar `LoginPage`, `LaboratoryListPage`, `LaboratoryDetailPage` e todos os formulários para garantir layout responsivo em desktop (≥ 1024px), tablet (≥ 768px) e mobile (< 768px)
    - Garantir que nenhum elemento fique cortado, sobreposto ou inacessível
    - Garantir área de toque mínima de 44x44px para botões e campos em mobile (Tailwind `min-h-[44px] min-w-[44px]`)
    - Criar `NotFoundPage` simples com link de retorno para `/laboratories`
    - _Requirements: 13.1, 13.2_

- [x] 20. Checkpoint — Frontend completo
  - Garantir que todos os testes de componente e property tests do frontend passam. Verificar que `ProtectedRoute` redireciona corretamente e que `RoleGuard` oculta elementos do DOM para `ALUNO`.

---

## Notes

- Tasks marcadas com `*` são opcionais e podem ser puladas para um MVP mais rápido
- Cada task referencia os requisitos correspondentes para rastreabilidade
- Os checkpoints (6, 10, 20) garantem validação incremental antes de avançar para a próxima fase
- As property tests do backend usam **jqwik** (Properties 1-10); as do frontend usam **fast-check** (Properties 11-12)
- O backend e o frontend são projetos independentes e podem ser desenvolvidos em paralelo a partir da task 3 / task 11
- As tags de rastreabilidade nos testes de propriedade seguem o formato do design: `// Feature: campus-lab, Property <N>: <descrição>`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["3.3", "3.5", "4.1", "8.1", "7.1"] },
    { "id": 1, "tasks": ["3.2", "3.4", "4.2", "7.2", "14.1", "14.2", "14.3", "14.4"] },
    { "id": 2, "tasks": ["4.3", "4.4", "7.3", "15.1", "16.1"] },
    { "id": 3, "tasks": ["5.1", "7.4", "7.5", "7.6", "16.2", "16.3", "16.4", "17.1"] },
    { "id": 4, "tasks": ["5.2", "7.7", "8.2", "17.2", "18.1"] },
    { "id": 5, "tasks": ["5.3", "8.3", "8.4", "8.5", "8.6", "8.7", "17.3", "18.2", "18.3"] },
    { "id": 6, "tasks": ["8.8", "9.1", "17.4", "18.4"] },
    { "id": 7, "tasks": ["9.2", "19.1"] },
    { "id": 8, "tasks": ["9.3"] }
  ]
}
```
