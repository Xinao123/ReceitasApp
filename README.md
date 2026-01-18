# Receitas (Android) + AI Worker 🤖🍲

> **PT-BR / EN** (bilingual README)

---

## 🇧🇷 Português (PT-BR)

App de receitas feito em **Kotlin + Jetpack Compose**, com **Firebase (Auth + Firestore)** e uma IA que gera **3 sugestões completas** usando um **Cloudflare Worker** (OpenAI).

O foco é: UX simples, rápido, bonito e pronto pra escalar.

---

### ✨ Funcionalidades

#### App Android
- ✅ Login com **Firebase Auth**
- ✅ Receitas públicas e privadas
- ✅ Criar, editar e excluir receitas (somente dono)
- ✅ Favoritar receitas (persistido no Firestore)
- ✅ Busca por título/categoria/ingredientes
- ✅ Tela de detalhes com ingredientes e modo de preparo
- ✅ Integração com IA:
  - usuário manda ingredientes + porções + restrições + equipamento
  - recebe **3 sugestões completas**
  - pode “Usar” e transformar em receita editável

#### AI (Cloudflare Worker)
- ✅ Endpoint `/health`
- ✅ Endpoint `/generate`
- ✅ Integração com OpenAI (Responses API)
- ✅ Retorno validado com JSON Schema (sempre 3 sugestões)
- ✅ Opção **Allow Extras**:
  - desmarcado: IA tenta usar **somente os ingredientes informados**
  - marcado: IA pode sugerir ingredientes extras (ex: alho, limão, etc)

---

### 🧱 Stack

**Android**
- Kotlin
- Jetpack Compose (Material 3)
- Coroutines + Flow
- Firebase Auth
- Cloud Firestore

**Backend (Worker)**
- Cloudflare Workers
- OpenAI SDK (`responses.create`)
- JSON Schema strict output


## 🇺🇸 English (EN)
Recipe app built with Kotlin + Jetpack Compose, featuring Firebase (Auth + Firestore) and an AI that generates 3 complete suggestions using a Cloudflare Worker (OpenAI).

Focus: Simple UX, fast, beautiful, and ready to scale.

✨ Features
Android App
✅ Login with Firebase Auth

✅ Public and private recipes

✅ Create, edit, and delete recipes (owner only)

✅ Favorite recipes (persisted in Firestore)

✅ Search by title/category/ingredients

✅ Details screen with ingredients and preparation instructions

✅ AI Integration:

User sends ingredients + servings + restrictions + equipment

Receives 3 complete suggestions

Can "Use" a suggestion to transform it into an editable recipe

AI (Cloudflare Worker)
✅ /health Endpoint

✅ /generate Endpoint

✅ OpenAI Integration (Responses API)

✅ Response validated with JSON Schema (always 3 suggestions)

✅ Allow Extras Option:

Unchecked: AI tries to use only the provided ingredients

Checked: AI can suggest extra ingredients (e.g., garlic, lemon, etc.)

🧱 Stack
Android

Kotlin

Jetpack Compose (Material 3)

Coroutines + Flow

Firebase Auth

Cloud Firestore

Backend (Worker)

Cloudflare Workers

OpenAI SDK (responses.create)

JSON Schema strict output



