# ⛽ FuelFinder

> Projeto acadêmico desenvolvido no contexto de Projeto Baseado em Problemas (PJBL) da disciplina de Programação Orientada a Objetos (POO), na UNDB.

O **FuelFinder** é um projeto em desenvolvimento que busca apoiar motoristas na tomada de decisões relacionadas ao abastecimento. A proposta é reunir informações sobre combustíveis e postos e, em etapas futuras, permitir comparar alternativas considerando fatores como preço, distância e custo estimado por abastecimento.

> **Estado atual do repositório:** a implementação disponível concentra-se na base da aplicação, especialmente no cadastro de usuários e na verificação de e-mail, com back-end Java/Spring Boot e interface web Next.js. O domínio de postos, a comparação de combustíveis e o cálculo de economia descritos na visão do produto ainda não estão presentes no código desta versão. Este README distingue a visão planejada do que está efetivamente implementado.

---

## 📌 Índice

- [Objetivos](#-objetivos)
- [Estado atual e escopo](#-estado-atual-e-escopo)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Conceitos de POO](#-conceitos-de-poo)
- [Estrutura do repositório](#-estrutura-do-repositório)
- [Pré-requisitos](#-pré-requisitos)
- [Configuração e execução local](#-configuração-e-execução-local)
- [API disponível](#-api-disponível)
- [Testes e integração contínua](#-testes-e-integração-contínua)
- [Documentação complementar](#-documentação-complementar)
- [Equipe](#-equipe)
- [Contribuição](#-contribuição)
- [Próximas etapas](#-próximas-etapas)

---

## 🎯 Objetivos

O FuelFinder tem como objetivos de projeto:

- desenvolver uma aplicação web organizada e extensível;
- aplicar os fundamentos de Programação Orientada a Objetos em um problema prático;
- separar regras de negócio dos detalhes de infraestrutura;
- construir uma base de cadastro e identidade de usuários;
- evoluir para uma solução de consulta e comparação de opções de abastecimento;
- praticar testes automatizados, controle de versão, documentação e integração contínua.

A proposta de produto é comparar opções de abastecimento de maneira útil para o motorista. Os cálculos deverão considerar os dados efetivamente disponíveis e deixar explícitas as premissas usadas; não se deve apresentar preço ou localização como informação em tempo real sem uma fonte de dados integrada e atualizada.

## 📍 Estado atual e escopo

### Implementado nesta versão

- Estrutura inicial do back-end em Java e Spring Boot.
- Contexto de identidade (`identity`) com modelo de usuário e objetos de valor.
- Cadastro local de usuário com validação de dados.
- Política de senha e armazenamento de hash usando PBKDF2 por meio do Spring Security Crypto.
- Fluxo de verificação de e-mail baseado em token.
- Persistência com Spring Data JPA e adaptadores JDBC.
- Migração de banco de dados com Flyway para o esquema de identidade.
- Interface web inicial com página principal, cadastro e verificação de e-mail.
- Testes automatizados de unidade e integração no back-end.
- Pipeline de CI para executar a verificação do projeto Java em pushes e pull requests para `main`.

### Planejado para evolução

- Modelagem do domínio de postos e combustíveis.
- Cadastro, consulta e atualização de preços de postos.
- Integração com fonte de dados geográficos ou de localização, se aplicável.
- Cálculo comparativo de custo-benefício entre opções de abastecimento.
- Integração das funcionalidades de negócio com a interface web.
- Expansão dos testes de integração e da documentação de API.

**Importante:** os itens planejados não devem ser considerados funcionalidades disponíveis até que sejam implementados, testados e documentados no repositório.

---

## ✨ Funcionalidades

| Área | Situação | Descrição |
|---|---|---|
| Cadastro local | Implementado no back-end e conectado à interface | Recebe nome, e-mail e senha; cria uma conta pendente de verificação. |
| Verificação de e-mail | Endpoint implementado | Valida um token e ativa uma conta pendente quando o token é válido. |
| Persistência de identidade | Implementada | Armazena dados de usuário, credenciais e informações necessárias ao fluxo de verificação. |
| Interface web | Inicial | Páginas para entrada, cadastro e verificação de e-mail. |
| Login e sessão de usuário | Não confirmado como implementado nesta versão | Não considerar disponível sem um fluxo de autenticação completo. |
| Catálogo de postos e combustíveis | Planejado | Não há contexto `station` implementado no código desta versão. |
| Comparação de preços e cálculo de economia | Planejado | Depende da implementação do domínio de negócio e de dados de postos. |
| Geolocalização e preços em tempo real | Planejado / depende de integração | Requer fonte de dados apropriada e atualizada. |

---

## 🧰 Tecnologias

### Back-end

- **Java 21** — linguagem principal.
- **Spring Boot** — configuração e execução da aplicação.
- **Spring Web MVC** — endpoints HTTP.
- **Spring Data JPA / Hibernate** — persistência orientada a objetos.
- **JDBC** — operações de persistência específicas do fluxo de verificação.
- **PostgreSQL** — banco de dados relacional.
- **PostGIS** — extensão geoespacial disponibilizada pela imagem Docker de desenvolvimento; sua presença no contêiner não significa que as funcionalidades geográficas do produto já estejam implementadas.
- **Flyway** — versionamento do esquema do banco.
- **Spring Security Crypto** — hash de senhas com PBKDF2.
- **Springdoc OpenAPI** — documentação dos endpoints quando habilitada.
- **Maven Wrapper** — execução padronizada do build.
- **JUnit 5 e Spring Boot Test** — testes automatizados.

### Front-end

- **Next.js 14** — aplicação web com App Router.
- **React 18** — construção da interface.
- **TypeScript** — tipagem estática.
- **Tailwind CSS** — estilos e composição visual.
- **ESLint** — análise estática do front-end.

### Infraestrutura e qualidade

- **Docker Compose** — execução local do PostgreSQL.
- **GitHub Actions** — integração contínua do back-end.

---

## 🏗️ Arquitetura

O back-end organiza o contexto de identidade seguindo ideias da **Arquitetura Hexagonal (Ports and Adapters)**. O objetivo é manter as regras do domínio e os casos de uso desacoplados dos detalhes de comunicação HTTP e persistência.

O contexto implementado está organizado em três áreas principais:

```text
identity/
├── domain/
│   ├── enums/
│   ├── model/
│   └── valueobject/
├── application/
│   ├── exception/
│   ├── model/
│   ├── port/
│   │   ├── in/
│   │   └── out/
│   └── service/
└── adapter/
    ├── config/
    ├── in/
    │   └── web/
    ├── out/
    │   └── persistence/
    └── security/
```

### Responsabilidades das camadas

- **`domain`**: representa conceitos e regras centrais, como usuário, identificador, e-mail, papéis e estados.
- **`application`**: coordena os casos de uso e define portas de entrada e saída.
- **`adapter`**: conecta os casos de uso a tecnologias concretas, como HTTP, JPA, JDBC e mecanismos criptográficos.
- **`shared`**: contém componentes de infraestrutura compartilhados, como a configuração de CORS.

As portas permitem que os serviços de aplicação dependam de contratos em vez de acoplarem suas regras diretamente a uma implementação específica. Isso também facilita testes com dependências simuladas.

> A arquitetura acima descreve o que está presente no código enviado. O contexto de postos e combustíveis deverá ser documentado nesta seção quando for implementado.

---

## 🧠 Conceitos de POO

O código atual demonstra conceitos de orientação a objetos principalmente no contexto de identidade.

| Conceito | Exemplo no código atual | Aplicação |
|---|---|---|
| Encapsulamento | `identity.domain.model.User` | O estado do usuário é privado e suas transições são controladas por métodos de domínio. |
| Abstração | Interfaces nas portas de aplicação | Os casos de uso descrevem contratos sem depender diretamente de todos os detalhes técnicos. |
| Polimorfismo | Implementações que atendem às interfaces de saída | Permite substituir implementações concretas por outras compatíveis com o mesmo contrato. |
| Composição | Serviços de aplicação recebem dependências pelo construtor | O comportamento é organizado pela colaboração entre objetos. |
| Objetos de valor | `Email` e `UserId` | Encapsulam representação e validações específicas dos valores. |
| Enumerações | `UserRole`, `UserStatus` e `AuthenticationProvider` | Representam conjuntos definidos de opções do domínio. |
| Tratamento de exceções | Exceções específicas de aplicação e validações | Permite representar falhas de negócio de forma explícita. |
| Injeção de dependências | Configuração de serviços de identidade | Separa a criação dos objetos de sua utilização e melhora a testabilidade. |

A hierarquia de combustíveis, a herança entre tipos de combustível e o polimorfismo dos cálculos de preço pertencem à proposta futura do domínio de postos. Eles não são apresentados como implementados nesta versão.

---

## 📂 Estrutura do repositório

```text
POO-UNDB/
├── .github/
│   ├── workflows/
│   │   └── ci.yml                    # Pipeline de CI do back-end
│   └── PULL_REQUEST_TEMPLATE.md
├── backend/
│   ├── .mvn/                          # Configuração do Maven Wrapper
│   ├── infra/
│   │   └── compose.dev.yml             # PostgreSQL para desenvolvimento
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/br/com/fuelfinder/
│   │   │   │   ├── identity/           # Domínio, casos de uso e adaptadores
│   │   │   │   ├── shared/             # Infraestrutura compartilhada
│   │   │   │   └── FuelfinderApplication.java
│   │   │   └── resources/
│   │   │       ├── db/migration/       # Migrações Flyway
│   │   │       └── application.properties
│   │   └── test/                       # Testes do back-end
│   ├── infra/                          # Configurações locais de infraestrutura
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
├── docs/
│   ├── backend/                       # Guia técnico do back-end
│   ├── frontend/                      # Guia de estudo do front-end
│   ├── openapi/                       # Contrato OpenAPI
│   └── qa/                            # Plano de testes
├── frontend/
│   ├── infra/                         # Arquivos de infraestrutura do front-end
│   ├── src/
│   │   ├── app/                       # Páginas e rotas Next.js
│   │   ├── components/                # Componentes reutilizáveis
│   │   ├── lib/                       # Cliente HTTP e validações
│   │   └── types/                     # Tipos TypeScript
│   ├── .env.example
│   └── package.json
├── .env.example
├── CONTRIBUTING.md
└── README.md
```

---

## 💻 Pré-requisitos

Antes de iniciar, instale:

- Git;
- JDK 21;
- Node.js compatível com Next.js 14 (recomenda-se Node.js 18 ou superior);
- npm;
- Docker com Docker Compose.

Os comandos abaixo consideram um terminal na raiz do repositório.

## 🚀 Configuração e execução local

### 1. Clonar o projeto

```bash
git clone https://github.com/LandielDurans/POO-UNDB.git
cd POO-UNDB
```

### 2. Configurar as variáveis de ambiente

Crie o arquivo `.env` a partir do exemplo:

**Linux/macOS/Git Bash:**

```bash
cp .env.example .env
```

**PowerShell:**

```powershell
Copy-Item .env.example .env
```

Revise o arquivo `.env` e altere as credenciais conforme o ambiente. O valor incluído no exemplo é apenas para desenvolvimento local; não reutilize essa senha em produção nem publique credenciais reais no Git.

Variáveis principais:

| Variável | Finalidade | Valor de desenvolvimento no exemplo |
|---|---|---|
| `DB_NAME` | Nome do banco | `fuelfinder` |
| `DB_USER` | Usuário do banco | `fuelfinder` |
| `DB_PASSWORD` | Senha do banco | Valor de exemplo; altere conforme necessário |
| `DB_PORT` | Porta local publicada pelo contêiner | `5432` |
| `DB_URL` | URL JDBC usada pelo back-end executado no host | `jdbc:postgresql://localhost:5432/fuelfinder` |

### 3. Iniciar o PostgreSQL

O arquivo Compose de desenvolvimento está em `backend/infra/compose.dev.yml`. Não há um `docker-compose.yml` na raiz nesta versão.

```bash
docker compose --env-file .env -f backend/infra/compose.dev.yml up -d
```

Verifique o estado do contêiner:

```bash
docker compose --env-file .env -f backend/infra/compose.dev.yml ps
```

Para acompanhar os logs:

```bash
docker compose --env-file .env -f backend/infra/compose.dev.yml logs -f postgres
```

O contêiner publica o banco em `127.0.0.1`, limitando a exposição à máquina local. O volume `fuelfinder_postgres_data` mantém os dados entre reinicializações.

Para parar o serviço sem remover os dados:

```bash
docker compose --env-file .env -f backend/infra/compose.dev.yml down
```

> O comando `down -v` remove também o volume de dados. Use-o somente se realmente quiser apagar o banco local.

### 4. Executar o back-end

Com o banco em execução, abra outro terminal:

```bash
cd backend
```

**Linux/macOS/Git Bash:**

```bash
./mvnw spring-boot:run
```

**Windows:**

```powershell
.\mvnw.cmd spring-boot:run
```

Por padrão, o back-end usa a porta `8080` e tenta conectar-se ao banco definido em `DB_URL`, `DB_USER` e `DB_PASSWORD`. As migrações Flyway são configuradas para executar na inicialização, e o Hibernate valida o esquema existente.

Se a aplicação não iniciar, confira se o PostgreSQL está saudável, se as credenciais do `.env` correspondem às usadas pelo contêiner e se a porta `5432` está livre.

### 5. Configurar e executar o front-end

Abra outro terminal na raiz do repositório e entre no diretório `frontend`:

```bash
cd frontend
```

Instale as dependências:

```bash
npm install
```

Crie o arquivo de ambiente local:

**Linux/macOS/Git Bash:**

```bash
cp .env.example .env.local
```

**PowerShell:**

```powershell
Copy-Item .env.example .env.local
```

O exemplo configura `BACKEND_URL=http://localhost:8080`. Essa variável é usada pelo proxy do Next.js para encaminhar as chamadas `/api/...` ao back-end.

Inicie a aplicação:

```bash
npm run dev
```

A interface ficará disponível em `http://localhost:3000`.

### 6. Verificar o fluxo de cadastro

Com o back-end, o banco e o front-end em execução:

1. Abra `http://localhost:3000`.
2. Acesse a página de cadastro em `/cadastro`.
3. Envie nome, e-mail e senha válidos.
4. Verifique a resposta do cadastro e o estado pendente de verificação.
5. Complete a verificação utilizando um token válido, conforme o fluxo configurado no ambiente.

**Observação:** o código possui portas e serviços relacionados ao processamento de e-mails de verificação. A entrega efetiva de mensagens depende de uma implementação/configuração de envio apropriada. Não presuma que um e-mail real será entregue apenas porque o cadastro foi aceito. Para detalhes do fluxo disponível, consulte `docs/backend/identity-guide.md` e o plano de testes.

---

## 🔌 API disponível

A API implementada nesta versão está concentrada no contexto de identidade e utiliza o prefixo `/api/v1/auth`.

| Método | Endpoint | Sucesso | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | `202 Accepted` | Aceita o cadastro e cria uma conta com estado inicial `PENDING_VERIFICATION`. |
| `POST` | `/api/v1/auth/verify-email` | `204 No Content` | Verifica o endereço de e-mail usando um token válido. |

### Exemplo de cadastro

Requisição:

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "name": "Maria da Silva",
  "email": "maria@example.com",
  "password": "UmaSenhaSegura123"
}
```

Resposta esperada de sucesso (`202 Accepted`):

```json
{
  "userId": "<identificador-gerado>",
  "status": "PENDING_VERIFICATION"
}
```

Os valores acima são ilustrativos. A resposta real utiliza o identificador gerado pelo sistema.

### Exemplo de verificação de e-mail

Requisição:

```http
POST /api/v1/auth/verify-email
Content-Type: application/json
```

```json
{
  "token": "<token-de-verificacao>"
}
```

Resposta de sucesso: `204 No Content`. Um token inválido ou expirado resulta em erro de requisição conforme o tratamento implementado.

### Documentação OpenAPI / Swagger

A dependência de OpenAPI está incluída, mas a interface e os documentos gerados ficam desabilitados por padrão. Para habilitá-los localmente, inicie o back-end com:

```bash
OPENAPI_ENABLED=true SWAGGER_UI_ENABLED=true ./mvnw spring-boot:run
```

No Windows PowerShell, configure as variáveis de ambiente antes de executar o Maven:

```powershell
$env:OPENAPI_ENABLED = "true"
$env:SWAGGER_UI_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

Com a aplicação iniciada, os caminhos padrão do Springdoc são:

- Interface Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Documento OpenAPI: `http://localhost:8080/v3/api-docs`

O contrato versionado do contexto de identidade também está em `docs/openapi/identity-v1.yaml`.

---

## 🧪 Testes e integração contínua

### Testes do back-end

Na raiz do repositório, execute:

```bash
cd backend
./mvnw clean verify
```

No Windows:

```powershell
cd backend
.\mvnw.cmd clean verify
```

O comando compila o projeto e executa as verificações configuradas no ciclo Maven, incluindo os testes automatizados. Para que testes que dependem de contêineres funcionem, o ambiente pode precisar do Docker em execução.

A existência de testes no repositório não garante, por si só, que todos estejam passando em toda máquina; consulte a saída do comando para confirmar o resultado.

### Front-end

Para gerar a versão de produção:

```bash
cd frontend
npm run build
```

Para executar o verificador de estilo configurado no projeto:

```bash
npm run lint
```

### GitHub Actions

O workflow `.github/workflows/ci.yml` configura a integração contínua do back-end para pushes e pull requests direcionados à branch `main`. Ele prepara o Java 21 e executa `./mvnw -B -ntp clean verify`.

O workflow presente nesta versão não deve ser interpretado como uma pipeline completa de deploy ou como validação automática do front-end.

---

## 📚 Documentação complementar

- `docs/backend/identity-guide.md` — decisões e fluxo do contexto de identidade.
- `docs/frontend/guia-de-estudo.md` — guia de estudo da implementação do front-end.
- `docs/openapi/identity-v1.yaml` — contrato OpenAPI do contexto de identidade.
- `docs/qa/plano-testes-cadastro.md` — plano de testes do cadastro.
- `CONTRIBUTING.md` — orientações para contribuição e convenções do repositório.

---

## 👥 Equipe

Os integrantes e as responsabilidades abaixo refletem a divisão de trabalho registrada na documentação do projeto. As contribuições específicas devem ser verificadas pelo histórico de commits e pelas entregas realizadas.

| Integrante | Responsabilidade registrada |
|---|---|
| José Dominick | Liderança técnica, arquitetura, organização do domínio e revisão de código. |
| Landiel Durans | Desenvolvimento do back-end e persistência. |
| João Marcelo | Desenvolvimento do front-end e experiência de usuário. |
| Luiz Gabriel | Qualidade e testes. |
| Higor Gabriel | Infraestrutura, Docker e automação. |

---

## 🤝 Contribuição

Contribuições devem seguir as orientações de `CONTRIBUTING.md`. Antes de abrir um pull request:

1. mantenha o escopo da alteração claro;
2. atualize ou crie testes para as regras afetadas;
3. execute as verificações pertinentes;
4. atualize a documentação quando o comportamento ou a configuração mudar;
5. descreva no pull request o problema, a solução e como validar a mudança.

Não inclua senhas reais, tokens, arquivos `.env` locais ou outras credenciais no repositório.

---

## 🗺️ Próximas etapas

A evolução sugerida do FuelFinder é:

1. concluir e documentar o fluxo de identidade, incluindo o mecanismo de envio de e-mail do ambiente;
2. implementar o domínio de postos e combustíveis com regras e invariantes explícitas;
3. modelar preços, localização e dados necessários ao cálculo comparativo;
4. definir casos de uso e portas para consulta de postos e cálculo de custo-benefício;
5. implementar persistência, migrações e endpoints para o novo contexto;
6. conectar a interface aos casos de uso efetivamente disponíveis;
7. ampliar os testes unitários, de integração e de ponta a ponta;
8. atualizar os diagramas e este README conforme o produto evoluir.

---

## 📄 Licença

Nenhuma licença de código aberto foi identificada na raiz do material analisado. Antes de reutilizar ou redistribuir o projeto, defina e inclua a licença apropriada, conforme as orientações da equipe e da instituição.

---

<p align="center">
  Desenvolvido como projeto acadêmico de Programação Orientada a Objetos — UNDB.
</p>
