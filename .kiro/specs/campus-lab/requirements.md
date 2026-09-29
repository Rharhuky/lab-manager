# Requirements Document

## Introduction

O CampusLab é um sistema web para gerenciamento dos laboratórios de um campus universitário. O sistema permite que usuários autenticados consultem laboratórios disponíveis e suas reservas. Professores possuem acesso completo ao gerenciamento, podendo criar, editar e excluir laboratórios e reservas, enquanto alunos têm acesso somente leitura. O sistema é composto por um backend em Java 21 com Spring Boot e um frontend em React com TypeScript e Tailwind CSS, com autenticação via JWT.

## Glossary

- **System**: O CampusLab como um todo (backend + frontend).
- **API**: O serviço backend REST exposto via Spring Boot.
- **Frontend**: A aplicação React/TypeScript/Tailwind CSS.
- **Aluno**: Usuário autenticado com role `ALUNO`, com acesso somente leitura.
- **Professor**: Usuário autenticado com role `PROFESSOR`, com acesso completo de gerenciamento.
- **JWT_Token**: Token de autenticação JSON Web Token retornado após login bem-sucedido.
- **Laboratory**: Entidade que representa um laboratório do campus, com atributos `id` (UUID), `name` (String, máximo 100 caracteres), `block` (String, máximo 20 caracteres), `createdAt` e `updatedAt`.
- **Reservation**: Entidade que representa uma reserva de laboratório, com atributos `id` (UUID), `laboratoryId`, `userId`, `startAt`, `endAt`, `createdAt` e `updatedAt`.
- **User**: Entidade que representa um usuário do sistema, com atributos `id`, `name`, `email`, `password` (BCrypt), `role` (ALUNO ou PROFESSOR), `createdAt` e `updatedAt`.
- **AuthService**: Componente responsável pela autenticação e geração de JWT_Token.
- **LaboratoryService**: Componente responsável pelas regras de negócio de laboratórios.
- **ReservationService**: Componente responsável pelas regras de negócio de reservas.
- **LaboratoryController**: Componente responsável por expor os endpoints REST de laboratórios.
- **ReservationController**: Componente responsável por expor os endpoints REST de reservas.
- **SecurityFilter**: Componente responsável por interceptar requisições e validar o JWT_Token.
- **ErrorHandler**: Componente responsável por formatar e retornar respostas de erro padronizadas.
- **ReservationConflict**: Condição em que dois intervalos de tempo `[startAt, endAt]` de reservas no mesmo Laboratory se sobrepõem, ou seja, quando não é verdade que `A.endAt <= B.startAt` ou `B.endAt <= A.startAt`.
- **FutureReservation**: Reservation cujo `startAt` é estritamente posterior ao instante atual do sistema no momento da verificação.

---

## Requirements

### Requirement 1: Autenticação de Usuários

**User Story:** Como usuário (Aluno ou Professor), quero fazer login com e-mail e senha, para que eu possa acessar o sistema de forma segura e receber um token de acesso.

#### Acceptance Criteria

1. WHEN o usuário envia `POST /api/auth/login` com `email` e `password` válidos, THE AuthService SHALL retornar um JWT_Token com os campos `token`, `type`, `expiresIn` e os dados do usuário autenticado com status HTTP 200.
2. IF o `email` informado não corresponder a nenhum usuário cadastrado ou o `password` informado não corresponder ao hash armazenado para o usuário, THEN THE AuthService SHALL retornar uma resposta de erro com status HTTP 401 no formato padronizado `{ timestamp, status, code, message, path }`, sem indicar qual campo está incorreto.
3. IF o corpo da requisição `POST /api/auth/login` estiver ausente, ou os campos `email` ou `password` estiverem ausentes, ou o campo `email` não estiver no formato de endereço de e-mail válido (contendo `@` e domínio), ou qualquer campo exceder 255 caracteres, THEN THE API SHALL retornar uma resposta de erro com status HTTP 400 no formato padronizado `{ timestamp, status, code, message, path }`.
4. THE AuthService SHALL armazenar senhas de usuários usando o algoritmo BCrypt antes de persistir no banco de dados.
5. THE JWT_Token SHALL conter o identificador do usuário e sua role (`ALUNO` ou `PROFESSOR`), e SHALL ter tempo de expiração em segundos definido pela configuração `expiresIn`, com valor mínimo de 60 segundos e máximo de 86400 segundos.
6. WHEN o Frontend recebe o JWT_Token após login bem-sucedido, THE Frontend SHALL armazenar o JWT_Token e incluí-lo no header `Authorization: Bearer <token>` em todas as requisições subsequentes autenticadas.
7. IF o AuthService não conseguir se comunicar com o banco de dados durante o processamento do login, THEN THE AuthService SHALL retornar uma resposta de erro com status HTTP 503 no formato padronizado `{ timestamp, status, code, message, path }`.

---

### Requirement 2: Proteção de Endpoints via JWT

**User Story:** Como desenvolvedor, quero que todos os endpoints protegidos exijam um JWT_Token válido, para que apenas usuários autenticados possam acessar os recursos do sistema.

#### Acceptance Criteria

1. WHEN uma requisição é recebida em um endpoint protegido sem o header `Authorization`, THE SecurityFilter SHALL retornar uma resposta de erro com status HTTP 401 no formato padronizado contendo um campo de mensagem indicando ausência de credenciais.
2. WHEN uma requisição é recebida com um JWT_Token expirado, THE SecurityFilter SHALL retornar uma resposta de erro com status HTTP 401 no formato padronizado contendo um campo de mensagem indicando que o token está expirado.
3. WHEN uma requisição é recebida com um JWT_Token com assinatura inválida ou estrutura malformada, THE SecurityFilter SHALL retornar uma resposta de erro com status HTTP 401 no formato padronizado contendo um campo de mensagem indicando que o token é inválido.
4. WHEN uma requisição autenticada como `ALUNO` tenta acessar um endpoint restrito a `PROFESSOR`, THE SecurityFilter SHALL retornar uma resposta de erro com status HTTP 403 no formato padronizado contendo um campo de mensagem indicando permissão insuficiente.
5. WHEN uma requisição é recebida com um JWT_Token válido e não expirado no header `Authorization`, THE SecurityFilter SHALL permitir o acesso ao endpoint solicitado se a role do token corresponder às roles permitidas para aquele endpoint.
6. IF o header `Authorization` está presente mas não segue o formato `Bearer <token>`, THEN THE SecurityFilter SHALL retornar uma resposta de erro com status HTTP 401 no formato padronizado contendo um campo de mensagem indicando formato de credencial inválido.

---

### Requirement 3: Consulta de Laboratórios

**User Story:** Como Aluno ou Professor autenticado, quero visualizar a lista de laboratórios e os detalhes de cada um, para que eu possa saber quais laboratórios existem e seu status de disponibilidade.

#### Acceptance Criteria

1. WHEN um usuário autenticado envia `GET /api/laboratories`, THE LaboratoryController SHALL retornar a lista de todos os Laboratory cadastrados com status HTTP 200, incluindo os campos `id`, `name`, `block`, `reserved`, `createdAt` e `updatedAt`.
2. WHEN um usuário autenticado envia `GET /api/laboratories/{id}` com um `id` existente, THE LaboratoryController SHALL retornar os dados completos do Laboratory com a lista de Reservation associadas e status HTTP 200.
3. IF um usuário autenticado envia `GET /api/laboratories/{id}` com um `id` inexistente, THEN THE LaboratoryController SHALL retornar uma resposta de erro com status HTTP 404 no formato padronizado.
4. THE LaboratoryService SHALL calcular o campo `reserved` como `true` quando o Laboratory possuir ao menos uma FutureReservation, e como `false` caso contrário.
5. IF um usuário não autenticado envia `GET /api/laboratories` ou `GET /api/laboratories/{id}`, THEN THE LaboratoryController SHALL retornar uma resposta de erro com status HTTP 401.
6. WHEN um usuário autenticado envia `GET /api/laboratories` e nenhum Laboratory estiver cadastrado, THE LaboratoryController SHALL retornar uma lista vazia com status HTTP 200.

---

### Requirement 4: Gerenciamento de Laboratórios (Professor)

**User Story:** Como Professor, quero criar, editar e excluir laboratórios, para que eu possa manter o cadastro de laboratórios do campus atualizado.

#### Acceptance Criteria

1. WHEN um Professor envia `POST /api/laboratories` com `name` não vazio (máximo 100 caracteres) e `block` não vazio (máximo 20 caracteres), THE LaboratoryController SHALL persistir o novo Laboratory e retornar os dados do Laboratory criado (incluindo o `id` gerado) com status HTTP 201.
2. WHEN um Professor envia `PUT /api/laboratories/{id}` com `name` não vazio (máximo 100 caracteres) e `block` não vazio (máximo 20 caracteres) e um `id` existente, THE LaboratoryController SHALL atualizar o Laboratory e retornar os dados atualizados do Laboratory com status HTTP 200.
3. WHEN um Professor envia `DELETE /api/laboratories/{id}` para um Laboratory sem reservas com `startAt` posterior ao momento atual da requisição, THE LaboratoryController SHALL excluir o Laboratory e retornar status HTTP 204.
4. WHEN um Professor envia `DELETE /api/laboratories/{id}` para um Laboratory com ao menos uma reserva com `startAt` posterior ao momento atual da requisição, THE LaboratoryService SHALL rejeitar a exclusão e THE LaboratoryController SHALL retornar uma resposta de erro com status HTTP 409 no formato padronizado com código `LABORATORY_HAS_FUTURE_RESERVATIONS`.
5. WHEN um Professor envia `POST /api/laboratories` ou `PUT /api/laboratories/{id}` com `name` ou `block` ausentes, vazios ou excedendo os limites de caracteres definidos, THE API SHALL retornar uma resposta de erro com status HTTP 400 no formato padronizado.
6. WHEN um Professor envia `PUT /api/laboratories/{id}` ou `DELETE /api/laboratories/{id}` com um `id` inexistente, THE LaboratoryController SHALL retornar uma resposta de erro com status HTTP 404 no formato padronizado.
7. IF um Professor envia `POST /api/laboratories` ou `PUT /api/laboratories/{id}` com um `name` já cadastrado para outro Laboratory existente, THEN THE LaboratoryController SHALL retornar uma resposta de erro com status HTTP 409 no formato padronizado indicando conflito de nome.

---

### Requirement 5: Consulta de Reservas

**User Story:** Como Aluno ou Professor autenticado, quero visualizar as reservas de um laboratório e os detalhes de uma reserva específica, para que eu possa acompanhar o uso dos laboratórios.

#### Acceptance Criteria

1. WHEN um usuário autenticado envia `GET /api/laboratories/{laboratoryId}/reservations` com um `laboratoryId` existente, THE ReservationController SHALL retornar a lista de Reservation do Laboratory ordenada por `startAt` de forma crescente com status HTTP 200.
2. WHEN um usuário autenticado envia `GET /api/reservations/{id}` com um `id` existente, THE ReservationController SHALL retornar os dados completos da Reservation com os campos `id`, `laboratoryId`, `userId`, `startAt`, `endAt` e `createdAt` com status HTTP 200.
3. IF um usuário autenticado envia `GET /api/laboratories/{laboratoryId}/reservations` com um `laboratoryId` inexistente, THEN THE ReservationController SHALL retornar uma resposta de erro com status HTTP 404 contendo mensagem indicando que o recurso não foi encontrado.
4. IF um usuário autenticado envia `GET /api/reservations/{id}` com um `id` inexistente, THEN THE ReservationController SHALL retornar uma resposta de erro com status HTTP 404 contendo mensagem indicando que o recurso não foi encontrado.
5. IF um usuário não autenticado envia `GET /api/laboratories/{laboratoryId}/reservations` ou `GET /api/reservations/{id}`, THEN THE ReservationController SHALL retornar uma resposta de erro com status HTTP 401 no formato padronizado.

---

### Requirement 6: Gerenciamento de Reservas (Professor)

**User Story:** Como Professor, quero criar, editar e excluir reservas de laboratório, para que eu possa controlar a ocupação dos laboratórios do campus.

#### Acceptance Criteria

1. WHEN um Professor envia `POST /api/laboratories/{laboratoryId}/reservations` com `startAt` e `endAt` válidos (ambos no futuro, `startAt` anterior a `endAt` e intervalo mínimo de 1 minuto), THE ReservationService SHALL verificar conflitos de horário e THE ReservationController SHALL persistir a Reservation e retornar os dados criados com status HTTP 201.
2. WHEN um Professor envia `PUT /api/reservations/{id}` com novos valores de `startAt` e `endAt` válidos, THE ReservationService SHALL reverificar conflitos de horário desconsiderando a Reservation sendo editada e THE ReservationController SHALL retornar os dados atualizados com status HTTP 200.
3. WHEN um Professor envia `DELETE /api/reservations/{id}` com um `id` existente, THE ReservationController SHALL excluir a Reservation e retornar status HTTP 204.
4. IF um Professor envia `POST /api/laboratories/{laboratoryId}/reservations` com um `laboratoryId` inexistente, THEN THE ReservationController SHALL retornar uma resposta de erro com status HTTP 404 no formato padronizado.
5. IF um Professor envia `PUT /api/reservations/{id}` ou `DELETE /api/reservations/{id}` com um `id` inexistente, THEN THE ReservationController SHALL retornar uma resposta de erro com status HTTP 404 no formato padronizado.
6. WHEN um Professor envia `POST` ou `PUT` em reservas com campos ausentes, com formato de data/hora inválido, ou com `startAt` maior ou igual a `endAt`, THE API SHALL retornar uma resposta de erro com status HTTP 400 no formato padronizado.
7. WHEN um Professor envia `POST` ou `PUT` em reservas e existe ReservationConflict com outra Reservation ativa no mesmo Laboratory, THE ReservationService SHALL rejeitar a operação e THE ReservationController SHALL retornar uma resposta de erro com status HTTP 409 no formato padronizado.

---

### Requirement 7: Regras de Negócio das Reservas

**User Story:** Como Professor, quero que o sistema impeça reservas inconsistentes ou conflitantes, para que o uso dos laboratórios seja organizado e sem sobreposição de horários.

#### Acceptance Criteria

1. WHEN o ReservationService processa uma criação ou atualização de Reservation, THE ReservationService SHALL rejeitar a operação com status HTTP 400 e mensagem de erro indicando intervalo inválido se `startAt` for maior ou igual a `endAt`.
2. WHEN o ReservationService processa uma criação ou atualização de Reservation, THE ReservationService SHALL rejeitar a operação com status HTTP 409 e mensagem de erro indicando conflito de horário se existir ReservationConflict com qualquer outra Reservation ativa no mesmo Laboratory, preservando os dados da Reservation existente sem modificação.
3. WHEN o ReservationService verifica conflitos durante a atualização de uma Reservation existente, THE ReservationService SHALL desconsiderar a própria Reservation sendo editada na verificação de ReservationConflict, comparando exclusivamente com as demais Reservations ativas do mesmo Laboratory.
4. WHEN o ReservationService verifica sobreposição de horários, THE ReservationService SHALL considerar apenas Reservations cujo `endAt` seja estritamente posterior ao instante atual no momento da verificação, excluindo da comparação quaisquer Reservations cujo `endAt` seja igual ou anterior ao instante atual.
5. FOR ALL pares de Reservation `A` e `B` no mesmo Laboratory onde `A.id != B.id` e ambas possuem `endAt` estritamente posterior ao instante atual, THE ReservationService SHALL garantir que `A.endAt <= B.startAt` ou `B.endAt <= A.startAt`, rejeitando com status HTTP 409 qualquer operação que viole essa condição.
6. WHEN o ReservationService processa uma criação ou atualização de Reservation, THE ReservationService SHALL rejeitar a operação com status HTTP 400 e mensagem de erro indicando duração mínima inválida se o intervalo entre `startAt` e `endAt` for inferior a 1 minuto.
7. IF o ReservationService não conseguir acessar o repositório de Reservations durante a verificação de conflitos, THEN THE ReservationService SHALL rejeitar a operação com status HTTP 503 e mensagem de erro indicando indisponibilidade temporária, sem persistir a Reservation.

---

### Requirement 8: Tratamento de Erros Padronizado

**User Story:** Como desenvolvedor de frontend, quero que a API retorne erros em um formato consistente, para que eu possa exibir mensagens de erro adequadas ao usuário em qualquer situação.

#### Acceptance Criteria

1. THE ErrorHandler SHALL retornar todas as respostas de erro no formato `{ timestamp, status, code, message, path }` para os status HTTP 400, 401, 403, 404, 409 e 500, onde: `timestamp` é uma string ISO 8601 em UTC, `status` é o código HTTP numérico, `code` é um identificador textual de nível de aplicação distinto do `status` numérico, `message` é uma string de no máximo 512 caracteres, e `path` é o caminho da requisição originadora.
2. WHEN um erro interno inesperado ocorre no servidor, THE ErrorHandler SHALL retornar uma resposta com status HTTP 500 no formato padronizado sem expor stack traces, nomes de classes internas, caminhos de arquivos internos ou detalhes de query/conexão de banco de dados na resposta.
3. THE API SHALL utilizar os códigos de status HTTP de forma consistente: 400 para requisições com dados inválidos ou malformados, 401 para acesso sem autenticação válida, 403 para acesso autenticado sem permissão suficiente, 404 para recursos não encontrados, 409 para conflitos de estado (reservas sobrepostas, laboratório com reservas futuras, nome duplicado) e 500 para erros internos inesperados.

---

### Requirement 9: Interface de Autenticação (Frontend)

**User Story:** Como usuário, quero uma tela de login com e-mail e senha, para que eu possa me autenticar no sistema de forma intuitiva.

#### Acceptance Criteria

1. THE Frontend SHALL exibir uma tela de login com um campo de `email`, um campo de `password` e um botão de submissão habilitado.
2. WHEN o usuário submete o formulário de login com o campo `email` vazio, somente com espaços em branco ou sem formato de e-mail válido, THE Frontend SHALL exibir uma mensagem de validação para o campo `email` sem enviar requisição à API.
3. WHEN o usuário submete o formulário de login com o campo `password` vazio ou somente com espaços em branco, THE Frontend SHALL exibir uma mensagem de validação para o campo `password` sem enviar requisição à API.
4. WHEN a API retorna status HTTP 401 na tentativa de login, THE Frontend SHALL exibir uma mensagem de erro informando que as credenciais são inválidas e SHALL preservar os valores dos campos preenchidos para que o usuário possa corrigi-los.
5. WHEN o login é bem-sucedido, THE Frontend SHALL armazenar o JWT_Token recebido e redirecionar o usuário para a tela de lista de laboratórios.

---

### Requirement 10: Interface de Laboratórios (Frontend)

**User Story:** Como usuário autenticado, quero visualizar a lista de laboratórios com seu status de disponibilidade e acessar os detalhes de cada laboratório, para que eu possa consultar as informações necessárias.

#### Acceptance Criteria

1. THE Frontend SHALL exibir a lista de laboratórios com os campos `name`, `block` e o status derivado `reserved` apresentado como "Reservado" quando `true` ou "Disponível" quando `false`.
2. WHEN o usuário autenticado possui role `PROFESSOR`, THE Frontend SHALL exibir botões de ação para criar, editar e excluir laboratórios na interface da lista.
3. WHEN o usuário autenticado possui role `ALUNO`, THE Frontend SHALL ocultar os botões de ação de criação, edição e exclusão de laboratórios.
4. WHEN o usuário clica em um laboratório, THE Frontend SHALL exibir a tela de detalhes do laboratório com a lista de Reservation associadas.
5. WHILE a lista de laboratórios está sendo carregada, THE Frontend SHALL exibir um indicador visual de carregamento (spinner ou skeleton screen) e SHALL desabilitar interações que dependam dos dados carregados.
6. WHEN a lista de laboratórios retorna vazia, THE Frontend SHALL exibir uma mensagem de empty state informando que nenhum laboratório foi encontrado.
7. WHEN a API retorna erro ao carregar laboratórios, THE Frontend SHALL exibir uma mensagem de error state com um botão de ação que permite ao usuário tentar novamente a requisição.

---

### Requirement 11: Interface de Gerenciamento de Laboratórios (Frontend — Professor)

**User Story:** Como Professor, quero formulários para cadastrar e editar laboratórios diretamente na interface, para que eu possa manter o cadastro atualizado sem precisar de acesso direto à API.

#### Acceptance Criteria

1. WHEN um Professor acessa o formulário de criação ou edição de laboratório, THE Frontend SHALL exibir campos para `name` e `block` com validação de campos obrigatórios e limites de caracteres antes do envio, sem enviar requisição à API se a validação falhar.
2. WHEN o Professor submete o formulário com sucesso, THE Frontend SHALL exibir um feedback de sucesso visível por pelo menos 3 segundos e SHALL atualizar a lista de laboratórios para refletir o registro criado ou modificado.
3. WHEN o Professor confirma a exclusão de um laboratório e a API retorna status HTTP 409 com código `LABORATORY_HAS_FUTURE_RESERVATIONS`, THE Frontend SHALL exibir uma mensagem informando que o laboratório possui reservas futuras e não pode ser excluído.
4. WHEN a API retorna status HTTP 400 em operações de formulário, THE Frontend SHALL exibir as mensagens de erro retornadas pela API nos respectivos campos ou em uma área de erro geral visível ao usuário.

---

### Requirement 12: Interface de Gerenciamento de Reservas (Frontend — Professor)

**User Story:** Como Professor, quero criar, editar e excluir reservas diretamente na interface, para que eu possa controlar a ocupação dos laboratórios de forma eficiente.

#### Acceptance Criteria

1. WHEN um Professor acessa a tela de detalhes de um laboratório, THE Frontend SHALL exibir a lista de Reservation com opções de criação, edição e exclusão visíveis apenas para usuários com role `PROFESSOR`.
2. WHEN um Professor submete o formulário de criação ou edição de reserva com `startAt` ausente, `endAt` ausente, `startAt` maior ou igual a `endAt`, ou data/hora em formato inválido, THE Frontend SHALL exibir mensagens de validação específicas para cada campo sem enviar requisição à API.
3. WHEN a API retorna status HTTP 409 na criação ou edição de reserva, THE Frontend SHALL exibir uma mensagem informando que existe conflito de horário com outra reserva existente.
4. WHEN uma operação de reserva é concluída com sucesso, THE Frontend SHALL exibir um feedback de sucesso visível por pelo menos 3 segundos e SHALL atualizar a lista de reservas do laboratório para refletir a alteração.

---

### Requirement 13: Responsividade (Frontend)

**User Story:** Como usuário, quero acessar o sistema em dispositivos desktop, tablet e mobile, para que eu possa utilizá-lo em qualquer dispositivo com uma experiência adequada.

#### Acceptance Criteria

1. THE Frontend SHALL renderizar todas as telas de forma responsiva para resoluções de desktop (≥ 1024px), tablet (≥ 768px e < 1024px) e mobile (< 768px), garantindo que nenhum elemento de interface fique cortado, sobreposto ou inacessível em nenhum dos três tamanhos de tela.
2. THE Frontend SHALL utilizar as classes responsivas do Tailwind CSS para adaptar o layout das telas de lista, detalhes e formulários às diferentes resoluções, de forma que os elementos de formulário e botões de ação possuam área de toque mínima de 44x44 pixels em resoluções mobile.
