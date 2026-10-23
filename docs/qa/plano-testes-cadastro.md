# Plano de Testes — Cadastro de Usuários

## Objetivo

Verificar se o cadastro de usuários do FuelFinder aceita dados válidos e rejeita informações incorretas.

## Cenários de Teste
| ID | Cenário | Resultado esperado |
|---|---|---|
| CT-01 | Cadastro com e-mail válido e senha válida | Cadastro aceito |
| CT-02 | Cadastro com e-mail e sem @ | Cadastro rejeitado |
| CT-03 | Cadastro com senha de 14 caracteres | Cadastro rejeitado |
| CT-04 | Cadastro com e-mail já cadastrado | Cadastro rejeitado |

**Status:** Cenários documentados, ainda não executados.