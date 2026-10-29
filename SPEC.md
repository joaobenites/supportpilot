# SupportPilot — Especificação do projeto

## Visão geral

**SupportPilot** é uma central de atendimento ao cliente com triagem por IA. Clientes abrem chamados; um serviço de IA classifica cada chamado (categoria, prioridade, sentimento) e sugere uma resposta com base na base de conhecimento da empresa. Atendentes trabalham num painel que atualiza em tempo real.

**Por que esse projeto chama atenção de recrutadores:**

- Usa a stack mais pedida em vagas Java: **Spring Boot, Angular, PostgreSQL**.
- É **orientado a eventos com Kafka**, mostrando que você entende sistemas distribuídos (e conecta com sua experiência na Sicredi).
- Tem **IA aplicada a um problema real** (classificação e respostas com RAG), não um chatbot genérico.
- Segue **práticas profissionais**: autenticação JWT, testes automatizados, Docker, CI no GitHub Actions e documentação clara.

**Regra de ouro:** cada etapa é feita com o Claude Code, mas você revisa e entende cada decisão antes de seguir. Numa entrevista, você precisa saber explicar o porquê de tudo.

## Funcionalidades

### MVP (o que precisa estar pronto)

- **Login e papéis:** dois tipos de usuário, cliente e atendente, com autenticação JWT.
- **Chamados:** o cliente abre, acompanha e responde chamados; o atendente vê todos, filtra e muda o status (aberto, em andamento, resolvido).
- **Triagem por IA:** ao criar um chamado, a IA define categoria, prioridade e sentimento em segundos, de forma assíncrona.
- **Resposta sugerida:** a IA sugere uma resposta usando artigos da base de conhecimento (RAG). O atendente pode aceitar, editar ou descartar.
- **Painel em tempo real:** novos chamados e resultados da triagem aparecem na tela do atendente sem recarregar a página.
- **Base de conhecimento:** o atendente cadastra artigos que alimentam as respostas sugeridas.

### Extras (se sobrar tempo)

- Métricas no painel: tempo médio de resposta, chamados por categoria, taxa de aceitação das sugestões da IA.
- Detecção de chamados urgentes com destaque visual.
- Histórico de auditoria de cada chamado.
- Modo escuro na interface.

## Arquitetura

&#91;embedded content: arquitetura do SupportPilot · 2 serviços, Kafka no meio\]

O **ticket-service** cuida de usuários, chamados e tempo real; o **ai-triage-service** só cuida da IA. Eles nunca se chamam diretamente: conversam por eventos no Kafka. Se a IA estiver lenta ou fora do ar, os chamados continuam sendo criados normalmente e a triagem acontece quando o serviço voltar.

**Fluxo de um chamado:**

1. O cliente cria o chamado no Angular; o ticket-service salva no PostgreSQL e publica o evento `ticket.created`.
2. O ai-triage-service consome o evento, busca artigos parecidos na base de conhecimento (pgvector) e pede ao modelo local, via Ollama, a categoria, a prioridade, o sentimento e uma resposta sugerida.
3. O ai-triage-service publica o resultado em `ticket.triaged`.
4. O ticket-service consome o resultado, atualiza o chamado e avisa o painel do atendente por WebSocket.

**Decisão importante para a demo online:** a IA fica atrás de uma interface (`TriageProvider`) com duas implementações: Ollama, para rodar local, e uma versão simples por regras, para a demo hospedada, onde não dá para rodar um modelo de IA de graça. Isso também é um ótimo ponto para explicar em entrevista.

## Stack técnica

| Camada | Tecnologia | Para quê |
| --- | --- | --- |
| Back end | Java 21, Spring Boot 3 | Os dois serviços |
| Segurança | Spring Security com JWT | Login e papéis (cliente e atendente) |
| Dados | PostgreSQL com pgvector, Spring Data JPA, Flyway | Chamados, usuários, artigos e busca por similaridade |
| Mensageria | Apache Kafka (modo KRaft), Spring Kafka | Eventos entre os serviços |
| IA | Ollama com um modelo local (ex.: Gemma 3) e um modelo de embeddings | Triagem e respostas sugeridas |
| Tempo real | WebSocket com STOMP | Atualizar o painel do atendente |
| Front end | Angular, Angular Material | Telas do cliente e do atendente |
| Testes | JUnit 5, Mockito, Testcontainers | Testes de unidade e de integração com Postgres e Kafka reais |
| Infra | Docker, Docker Compose | Subir tudo com um comando |
| CI | GitHub Actions | Rodar build e testes a cada push |

As versões exatas, você confirma com o Claude Code na Etapa 0, usando sempre versões estáveis.

## Modelo de dados

| Tabela | Campos principais |
| --- | --- |
| `users` | id, nome, email, senha (hash), papel (CUSTOMER ou AGENT), criado\_em |
| `tickets` | id, cliente\_id, atendente\_id, título, descrição, status, categoria, prioridade, sentimento, triagem\_status (PENDING, DONE, FAILED), criado\_em, atualizado\_em |
| `ticket_messages` | id, ticket\_id, autor\_id, conteúdo, criado\_em |
| `ai_suggestions` | id, ticket\_id, resposta\_sugerida, artigos\_usados, status (PENDING, ACCEPTED, EDITED, DISCARDED), criado\_em |
| `kb_articles` | id, título, conteúdo, embedding (vetor pgvector), criado\_em |

**Eventos no Kafka:**

- `ticket.created`: ticketId, título, descrição, criado\_em.
- `ticket.triaged`: ticketId, categoria, prioridade, sentimento, resposta sugerida, artigos usados.

O campo `triagem_status` deixa claro na tela quando a IA ainda está trabalhando ou falhou, em vez de o chamado ficar sem informação.

## Etapas de desenvolvimento

Cada etapa termina com algo funcionando, testes passando e um commit. Não avance sem entender o que foi feito.

### Etapa 0 · Fundação

Repositório com a estrutura de pastas (`ticket-service`, `ai-triage-service`, `frontend`), Docker Compose com PostgreSQL (pgvector) e Kafka, e o GitHub Actions rodando o build.

**Pronto quando:** `docker compose up` sobe o banco e o Kafka, e o CI fica verde no GitHub.

### Etapa 1 · Chamados e login

No ticket-service: cadastro e login com JWT, papéis de cliente e atendente, CRUD de chamados e mensagens, migrações com Flyway.

**Pronto quando:** dá para criar usuário, logar e abrir chamado pela API, com testes de integração usando Testcontainers.

### Etapa 2 · Front end básico

Angular com login, lista de chamados, criação, detalhe com mensagens e o painel do atendente com filtros.

**Pronto quando:** cliente e atendente conseguem usar o fluxo completo pela tela.

### Etapa 3 · Eventos e tempo real

O ticket-service publica `ticket.created` no Kafka e consome `ticket.triaged`; o painel do atendente recebe atualizações por WebSocket.

**Pronto quando:** um chamado novo aparece no painel do atendente sem recarregar a página.

### Etapa 4 · Triagem por IA

O ai-triage-service consome `ticket.created`, chama o Ollama através da interface `TriageProvider`, valida a resposta e publica `ticket.triaged`. Inclui a implementação por regras para a demo e tratamento de falhas (tentativas e status FAILED).

**Pronto quando:** categoria, prioridade e sentimento aparecem no chamado segundos depois de criado, e os testes cobrem o caso de falha da IA.

### Etapa 5 · Base de conhecimento e respostas sugeridas

Cadastro de artigos, geração de embeddings, busca por similaridade com pgvector e resposta sugerida com os artigos usados. O atendente aceita, edita ou descarta.

**Pronto quando:** um chamado parecido com um artigo recebe uma sugestão que cita esse artigo.

### Etapa 6 · Acabamento e apresentação

Dados de exemplo, README completo em inglês, prints e um GIF do fluxo, diagrama da arquitetura, badges do CI e, se possível, a demo online usando o modo por regras.

**Pronto quando:** um recrutador entende o projeto em 30 segundos lendo o README.

## Prompts para o Claude Code

Antes da primeira etapa, cole esta especificação inteira no Claude Code ou salve como `SPEC.md` na raiz do projeto. Depois, use um prompt por etapa.

**Etapa 0**

> Leia o SPEC.md. Crie a Etapa 0: estrutura do monorepo, Docker Compose com PostgreSQL (imagem com pgvector) e Kafka em modo KRaft, e um workflow do GitHub Actions que faz o build. Use versões estáveis. Antes de escrever código, me explique o plano. No final, me diga como testar.

**Etapa 1**

> Implemente a Etapa 1 do SPEC.md no ticket-service: Spring Security com JWT, papéis CUSTOMER e AGENT, CRUD de chamados e mensagens, migrações com Flyway e testes de integração com Testcontainers. Explique cada decisão de segurança enquanto implementa.

**Etapa 2**

> Implemente a Etapa 2 do SPEC.md: app Angular com Angular Material, telas de login, lista, criação e detalhe de chamado, e o painel do atendente com filtros. Guarde o token com segurança e trate erros da API na tela.

**Etapa 3**

> Implemente a Etapa 3 do SPEC.md: publicar ticket.created no Kafka, consumir ticket.triaged e enviar atualizações ao painel por WebSocket com STOMP. Me explique como fica a garantia de entrega e o que acontece se o Kafka cair.

**Etapa 4**

> Implemente a Etapa 4 do SPEC.md no ai-triage-service: interface TriageProvider com uma implementação Ollama e outra por regras, validação da resposta do modelo, tentativas em caso de falha e status FAILED. Teste o caso em que a IA responde algo inválido.

**Etapa 5**

> Implemente a Etapa 5 do SPEC.md: cadastro de artigos, embeddings via Ollama, busca por similaridade com pgvector e resposta sugerida citando os artigos usados. O atendente pode aceitar, editar ou descartar a sugestão.

**Etapa 6**

> Faça a Etapa 6 do SPEC.md: dados de exemplo, README em inglês com descrição, diagrama da arquitetura, como rodar, decisões técnicas e badges do CI. Me diga quais prints e qual GIF eu devo gravar.

**Depois de cada etapa, peça:** "Me explique o que você fez como se eu fosse defender isso numa entrevista técnica."

## Apresentação

### README que vende o projeto

- **No topo:** uma frase sobre o que é, um GIF de 15 a 20 segundos mostrando um chamado sendo criado e triado ao vivo, e os badges do CI.
- **Logo depois:** o diagrama da arquitetura e a lista da stack.
- **Como rodar:** um único `docker compose up`, com dados de exemplo e usuários de teste já prontos.
- **Decisões técnicas:** por que Kafka, por que a interface `TriageProvider`, como a IA falha sem derrubar o sistema.
- **Próximos passos:** o que você faria com mais tempo. Isso mostra visão de produto.

### No currículo e no LinkedIn

> **SupportPilot** · Event-driven customer support platform with AI triage. Spring Boot, Kafka, PostgreSQL/pgvector, Angular, Ollama (RAG), Docker, GitHub Actions, Testcontainers.

### Perguntas que você deve saber responder

- Por que usar Kafka em vez de o ticket-service chamar a IA direto?
- O que acontece se o ai-triage-service cair no meio de uma triagem?
- Como você evita processar o mesmo evento duas vezes?
- Como funciona a autenticação JWT e onde o token fica guardado no front end?
- O que é RAG e por que ele melhora a resposta sugerida?
- Como você testa algo que depende de um modelo de IA, que nem sempre responde igual?
- O que você mudaria para esse sistema aguentar 100 vezes mais chamados?

Se você souber responder essas perguntas com as suas palavras, o projeto vai cumprir o papel dele.
