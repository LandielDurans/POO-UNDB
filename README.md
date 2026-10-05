O **FuelFinder** é uma plataforma desenvolvida para a disciplina de Programação Orientada a Objetos (POO) no curso de Engenharia de Software. O projeto funciona como um "Waze para postos de gasolina", permitindo a busca por estabelecimentos próximos, consulta de preços de combustíveis atualizados e gerenciamento de valores por parte dos administradores dos postos.

---

## 🏛️ Arquitetura do Sistema

O projeto adota a **Arquitetura Hexagonal (Ports and Adapters)**. Essa abordagem isola as regras de negócio centrais de dependências externas, garantindo um código desacoplado, altamente testável e de fácil manutenção.

---

## 🛠️ Tecnologias Utilizadas

| Camada                           | Tecnologia                              |
| :------------------------------- | :-------------------------------------- |
| **Front-End**                    | Next.js com Tailwind CSS                |
| **Back-End**                     | Java 21 com Spring Boot                 |
| **Banco de Dados**               | PostgreSQL                              |
| **DevOps**                       | Docker e Esteiras de CI/CD              |
| **Garantia de Qualidade (Q.A.)** | Playwright / Selenium, Insomnia e Mocha |

---

## 👥 Equipe e Responsabilidades

| Integrante         | Papel         | Responsabilidades Principais                                                                                  |
| :----------------- | :------------ | :------------------------------------------------------------------------------------------------------------ |
| **José Dominick**  | **Tech Lead** | Arquitetura do sistema, modelagem das entidades base de POO, contratos da API e revisão de código.            |
| **Landiel Durans** | **Back-End**  | Desenvolvimento da API em Java 21, implementação das regras de negócio, cálculos polimórficos e persistência. |
| **João Marcelo**   | **Front-End** | Criação da interface gráfica reativa em Next.js e Tailwind CSS, e integração com a API.                       |
| **Luiz Gabriel**   | **Q.A.**      | Automação de testes, validação de regras de negócio, testes de API e garantia da qualidade do código.         |
| **Higor Gabriel**  | **DevOps**    | Conteinerização com Docker, configuração do banco PostgreSQL e estruturação da esteira de CI/CD.              |
