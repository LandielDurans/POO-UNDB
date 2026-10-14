# ⛽ FuelFinder — Sistema Inteligente de Localização e Economia de Combustível

> **Projeto Prático Baseado em Problemas (PJBL) — Programação Orientada a Objetos (POO) / UNDB**

O **FuelFinder** é uma plataforma desenvolvida para auxiliar motoristas a encontrarem os postos de combustível mais vantajosos com base em localização em tempo real, preços atualizados e um algoritmo de cálculo de custo-benefício real (considerando distância, consumo do veículo e preço final do combustível).

---

## 👥 Equipe e Papéis

| Integrante | Papel no Projeto | Responsabilidades Principais |
| :--- | :--- | :--- |
| **José Dominick** | **Tech Lead** | Arquitetura do sistema, design do Modelo de Domínio, governança do Git e Code Review. |
| **Landiel Durans** | **Back-End Developer** | Implementação dos serviços Spring Boot, persistência JPA, regras do Core Domínio e Flyway. |
| **João Marcelo** | **Front-End Developer** | Interface em Next.js 14, componentes Tailwind, consumo de APIs e experiência do usuário (UX). |
| **Luiz Gabriel** | **QA / Testes** | Suíte de testes unitários no domínio (JUnit 5) e testes end-to-end com Playwright. |
| **Higor Gabriel** | **DevOps** | Containerização em Docker, orquestração com Docker Compose e pipeline de CI/CD. |

---

## 🏗️ Arquitetura do Sistema

O projeto utiliza **Arquitetura Hexagonal (Ports & Adapters)** combinada com **Bounded Contexts (Package-by-Feature)** para garantir o desacoplamento total da inteligência de negócio em relação aos frameworks e ao banco de dados.

### Princípios da Arquitetura:
1. **Core de Domínio Puro:** O pacote `domain` contém apenas código Java 21 puro (zero dependências de Spring, JPA ou Jackson).
2. **Ports (Interfaces):** Interfaces de entrada (`in/`) definem os Casos de Uso; interfaces de saída (`out/`) definem contratos de persistência e serviços externos.
3. **Adapters (Infraestrutura):** As implementações do Spring MVC (`@RestController`), Spring Data JPA (`@Entity`) e de segurança ficam estritamente isoladas na camada `adapter`.

---

## 🌳 Estrutura Completa do Repositório

```text
POO-UNDB/
├── .github/
│   ├── workflows/
│   │   └── ci.yml                            # Pipeline de Integração Contínua (GitHub Actions)
│   └── PULL_REQUEST_TEMPLATE.md              # Padronização de Pull Requests
│
├── docs/                                     # Documentação Técnica
│   └── UML_DOMINIO.md                        # Diagrama de Classes do Domínio (Mermaid)
│
├── backend/                                  # API Java 21 + Spring Boot 3
│   ├── .mvn/
│   ├── infra/
│   │   └── compose.dev.yml                   # Container isolado para ambiente de Dev
│   │
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/br/com/fuelfinder/
│   │   │   │   │
│   │   │   │   ├── identity/                 # 👤 CONTEXTO: Autenticação, Usuários e Segurança
│   │   │   │   │   ├── adapter/              # Web Controllers, Security Tokens e Persistência JPA
│   │   │   │   │   │   ├── persistence/
│   │   │   │   │   │   ├── security/
│   │   │   │   │   │   └── web/
│   │   │   │   │   ├── application/          # Use Cases, Exception, Models e Ports
│   │   │   │   │   │   ├── exception/
│   │   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── port/
│   │   │   │   │   │   │   ├── in/
│   │   │   │   │   │   │   └── out/
│   │   │   │   │   │   └── service/
│   │   │   │   │   └── domain/               # Core de Usuário (Java Puro)
│   │   │   │   │       ├── enums/            # UserRole, UserStatus, AuthenticationProvider
│   │   │   │   │       ├── model/            # User.java
│   │   │   │   │       └── valueobject/      # Email.java, UserId.java
│   │   │   │   │
│   │   │   │   └── station/                  # ⛽ CONTEXTO: Postos, Combustíveis e Algoritmo de Economia
│   │   │   │       ├── domain/               # 🟢 CORE DE POO (JAVA PURO - ZERO FRAMEWORK)
│   │   │   │       │   ├── exception/        # PrecoInvalidoException, PostoNaoEncontradoException
│   │   │   │       │   ├── model/            # Combustivel (Abstrata), GasolinaComum, GasolinaAditivada, Etanol, Posto
│   │   │   │       │   ├── port/             # Calculavel (Interface)
│   │   │   │       │   └── valueobject/      # Coordenada (Record X,Y)
│   │   │   │       ├── application/          # Orquestração de Regras e Portas
│   │   │   │       │   ├── port/
│   │   │   │       │   │   ├── in/           # CalcularEconomiaUseCase, AtualizarPrecoUseCase
│   │   │   │       │   │   └── out/          # PostoRepositoryPort
│   │   │   │       │   └── service/          # CalculoEconomiaService
│   │   │   │       └── adapter/              # 🔵 ADAPTERS (Spring, JPA, Web Controllers)
│   │   │   │           ├── persistence/      # PostoEntity (@Entity JPA) e SpringDataRepository
│   │   │   │           └── web/              # PostoController (@RestController) e DTOs
│   │   │   │
│   │   │   │   └── FuelfinderApplication.java# Classe Principal da Aplicação Spring Boot
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── db/migration/             # Migrações Versionadas com Flyway
│   │   │       │   ├── V1__create_identity_schema.sql
│   │   │       │   └── V2__create_station_schema.sql
│   │   │       └── application.properties    # Configurações do Spring Boot e Banco de Dados
│   │   │
│   │   └── test/                             # 🧪 SUÍTE DE TESTES
│   │       └── java/br/com/fuelfinder/
│   │           ├── identity/                 # Testes de Unidade do Contexto Identity
│   │           └── station/                  # Testes do Contexto Station (JUnit 5)
│   │               ├── domain/               # Testes de Unidade Puramente de Negócio
│   │               └── adapter/              # Testes de Integração de API/Banco
│   │
│   ├── pom.xml                               # Gerenciador de Dependências Maven
│   └── Dockerfile                            # Build em múltiplos estágios para o Back-End
│
├── frontend/                                 # Aplicação Web (Next.js 14 + Tailwind CSS)
│   ├── src/
│   │   ├── app/                              # Rotas e Páginas (App Router)
│   │   ├── components/                       # Componentes Reutilizáveis (Cards de Posto, Filtros)
│   │   └── services/                         # Cliente HTTP (Axios/Fetch)
│   ├── package.json
│   └── Dockerfile
│
├── docker-compose.yml                        # Orquestração do Banco PostgreSQL + Backend + Frontend
├── .env.example                              # Variáveis de ambiente de exemplo
├── .gitignore
├── CONTRIBUTING.md                           # Guia de contribuição e convenção de commits
└── README.md                                 # Documentação principal do projeto
```

---

## 📐 Demonstração dos Conceitos de POO Aplicados

| Conceito | Aplicação Prática no Projeto |
| :--- | :--- |
| **Abstração** | Classe abstrata `Combustivel` encapsulando atributos comuns e métodos de validação. |
| **Herança** | `GasolinaComum`, `GasolinaAditivada` e `Etanol` herdam de `Combustivel`. |
| **Polimorfismo** | O método `calcularPrecoFinal(quantidadeLitros)` possui regras próprias para cada tipo de combustível (ex: taxa de aditivo na gasolina aditivada). |
| **Interfaces** | Interface `Calculavel` define o contrato do cálculo de preços. Interfaces `Port` definem os contratos do padrão Hexagonal. |
| **Value Objects / Records** | `Coordenada` (Java 21 Record) encapsula a posição geográfica $(X,Y)$ e calcula distâncias de forma imutável. `Email` e `UserId` encapsulam validações de identidade. |
| **Tratamento de Exceções** | `PrecoInvalidoException` garante que nenhum combustível seja instanciado com preço $\le 0$. |

---

## 🚀 Como Executar o Projeto Localmente

### Pré-requisitos
* Java 21 JDK
* Node.js 18+
* Docker e Docker Compose

### 1. Clonar o repositório
```bash
git clone https://github.com/LandielDurans/POO-UNDB.git
cd POO-UNDB
```

### 2. Subir o ambiente com Docker Compose
```bash
docker-compose up -d
```

### 3. Executar o Back-End isoladamente (via Maven Wrapper)
```bash
cd backend
./mvnw spring-boot:run
```

### 4. Executar o Front-End
```bash
cd frontend
npm install
npm run dev
```
