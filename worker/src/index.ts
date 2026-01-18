import { OpenAI } from "openai";

type Env = {
  OPENAI_API_KEY: string;
  OPENAI_MODEL?: string;
};

const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "access-control-allow-headers": "content-type,authorization",
  "access-control-allow-methods": "GET,POST,OPTIONS",
};

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...corsHeaders },
  });
}

function badRequest(message: string, details?: unknown) {
  return json({ ok: false, error: message, details }, 400);
}

function serverError(message: string, details?: unknown) {
  return json({ ok: false, error: message, details }, 500);
}

function pickPath(url: string) {
  const u = new URL(url);
  return u.pathname.replace(/\/+$/, "") || "/";
}

type Equipment = "fogao" | "forno" | "air_fryer" | "qualquer";

function normalizeEquipment(equipmentRaw?: string, cuisineRaw?: string): Equipment {
  const a = (equipmentRaw ?? "").toLowerCase().trim();
  const c = (cuisineRaw ?? "").toLowerCase().trim();

  const text = `${a} ${c}`;

  if (/(air\s*fryer|airfryer)/i.test(text)) return "air_fryer";
  if (/(forno|assad)/i.test(text)) return "forno";
  if (/(fog[aã]o|panela|frigideira|boca)/i.test(text)) return "fogao";
  if (/(qualquer|tanto faz|tanto\s*faz)/i.test(text)) return "qualquer";

  return "qualquer";
}

type RestrictionHints = {
  tags: string[];
  guidance: string[];
  hasHealth: boolean;
};

function analyzeRestrictions(raw: string): RestrictionHints {
  const t = (raw ?? "").toLowerCase();

  const tags: string[] = [];
  const guidance: string[] = [];
  let hasHealth = false;

  const add = (tag: string, tip: string, health = false) => {
    if (!tags.includes(tag)) tags.push(tag);
    guidance.push(tip);
    if (health) hasHealth = true;
  };

  if (/(sem gl[uú]ten|gluten|cel[ií]ac)/i.test(t)) {
    add("sem_gluten", "Evitar farinha de trigo e derivados. Preferir arroz, milho, mandioca e tapioca.", false);
  }
  if (/(lactose|sem lactose|intoler[aâ]ncia)/i.test(t)) {
    add("sem_lactose", "Evitar leite/queijos comuns. Preferir versões sem lactose ou alternativas vegetais.", false);
  }
  if (/(vegano|vegan)/i.test(t)) {
    add("vegano", "Não usar ingredientes de origem animal. Trocar ovos por substitutos quando fizer sentido.", false);
  }
  if (/(vegetar)/i.test(t)) {
    add("vegetariano", "Evitar carne. Priorizar proteína vegetal e ovos/laticínios se permitido.", false);
  }

  if (/(diabet|glicem|a[cç]ucar)/i.test(t)) {
    add("diabetes", "Sem açúcar adicionado; mais fibras e proteína; evitar excesso de carbo simples.", true);
  }
  if (/(press[aã]o alta|hipertens|s[oó]dio)/i.test(t)) {
    add("pressao_alta", "Reduzir sal/embutidos/caldo pronto. Usar ervas e limão com moderação.", true);
  }
  if (/(gastrite|reflux|azia)/i.test(t)) {
    add("gastrite_refluxo", "Preferir preparo leve. Evitar fritura pesada e pimenta forte.", true);
  }
  if (/(colesterol|triglicer)/i.test(t)) {
    add("colesterol", "Evitar fritura pesada. Preferir assado/grelhado e azeite com moderação.", true);
  }

  return { tags, guidance, hasHealth };
}

function ensureArrayStrings(v: unknown): string[] {
  if (!Array.isArray(v)) return [];
  return v
    .map((x) => (typeof x === "string" ? x.trim() : ""))
    .filter((s) => s.length > 0);
}

function sanitizeSuggestion(s: any, idx: number) {
  const safe = {
    id: typeof s?.id === "string" && s.id.trim() ? s.id.trim() : `ai_${idx + 1}`,
    title: String(s?.title ?? "").trim() || `Sugestão ${idx + 1}`,
    servings: Number.isFinite(s?.servings) ? Math.max(1, Math.floor(s.servings)) : 2,
    timeMinutes: Number.isFinite(s?.timeMinutes) ? Math.max(1, Math.floor(s.timeMinutes)) : 20,
    category: String(s?.category ?? "").trim() || "Geral",
    ingredients: ensureArrayStrings(s?.ingredients),
    steps: ensureArrayStrings(s?.steps),
    extraNeeded: ensureArrayStrings(s?.extraNeeded),
    notes: typeof s?.notes === "string" ? s.notes : "",
  };

  if (safe.notes.trim().toLowerCase() === "null") safe.notes = "";
  return safe;
}



type ParsedInput = {
  allowedIngredientsRaw: string[]; 
  allowedKeys: string[]; 
  preferences: {
    doneness?: "mal_passada" | "ao_ponto" | "bem_passada";
  };
};

function stripAccents(s: string) {
  return s.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

function normalizeKey(s: string) {
  const t = stripAccents(s.toLowerCase())
    .replace(/[0-9]/g, " ")
    .replace(/[\(\)\[\]\{\}\.\,;:\/\\\-\_]/g, " ")
    .replace(/\b(kg|g|grama|gramas|ml|l|litro|litros|xicara|xicaras|colher|colheres|sopa|cha|teaspoon|tablespoon)\b/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  return t;
}

function parseUserInput(ingredientsRaw: string): ParsedInput {
  const parts = ingredientsRaw
    .split(/[\n,;]+/)
    .map((x) => x.trim())
    .filter(Boolean);

  let doneness: ParsedInput["preferences"]["doneness"] | undefined;

  const allowed: string[] = [];

  for (const p of parts) {
    const n = normalizeKey(p);

    if (/(mal passad|malpassad)/i.test(n)) {
      doneness = "mal_passada";
      continue;
    }
    if (/(ao ponto|aoponto)/i.test(n)) {
      doneness = "ao_ponto";
      continue;
    }
    if (/(bem passad|bempassad)/i.test(n)) {
      doneness = "bem_passada";
      continue;
    }

    allowed.push(p);
  }

  const keys = allowed
    .map((a) => normalizeKey(a))
    .filter((k) => k.length > 0);

  return {
    allowedIngredientsRaw: allowed.length ? allowed : [ingredientsRaw.trim()],
    allowedKeys: keys.length ? keys : [normalizeKey(ingredientsRaw)],
    preferences: { doneness },
  };
}

function validateStrict(suggestions: any[], allowedKeys: string[]) {
  
  const violations: string[] = [];

  const isAllowed = (ingredientLine: string) => {
    const k = normalizeKey(ingredientLine);
    return allowedKeys.some((a) => a && k.includes(a));
  };

  for (let i = 0; i < suggestions.length; i++) {
    const sug = suggestions[i];
    const ing: string[] = Array.isArray(sug?.ingredients) ? sug.ingredients : [];
    const extra: string[] = Array.isArray(sug?.extraNeeded) ? sug.extraNeeded : [];

    if (extra.length > 0) {
      violations.push(`Sugestão ${i + 1}: extraNeeded precisa ser [] no modo estrito.`);
    }

    for (const line of ing) {
      if (!isAllowed(line)) {
        violations.push(`Sugestão ${i + 1}: ingrediente fora da lista -> "${line}"`);
      }
    }
  }

  return violations;
}

function donenessGuide(d?: ParsedInput["preferences"]["doneness"]) {
  if (d === "mal_passada") {
    return "Preferência de ponto: MAL PASSADA. Priorize selagem rápida + descanso. Referência: ~52–54°C (se citar temperatura interna).";
  }
  if (d === "ao_ponto") {
    return "Preferência de ponto: AO PONTO. Selagem + tempo moderado. Referência: ~57–60°C (se citar temperatura interna).";
  }
  if (d === "bem_passada") {
    return "Preferência de ponto: BEM PASSADA. Tempo maior e fogo mais controlado. Referência: ~65°C+ (se citar temperatura interna).";
  }
  return "";
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    const path = pickPath(request.url);

    if (request.method === "GET" && (path === "/" || path === "/health")) {
      return json({
        ok: true,
        name: "Receitas AI Worker",
        endpoints: ["/health (GET)", "/generate (POST)"],
        path,
      });
    }

    if (path !== "/generate") {
      return json({ ok: false, error: "Not found", path }, 404);
    }

    if (request.method !== "POST") {
      return badRequest("Use POST em /generate");
    }

    if (!env.OPENAI_API_KEY) {
      return serverError("OPENAI_API_KEY não configurada no Worker.");
    }

    let body: any;
    try {
      body = await request.json();
    } catch {
      return badRequest("JSON inválido no body.");
    }

    const servings = Number(body?.servings ?? 2);
    const servingsSafe = Number.isFinite(servings) ? Math.min(12, Math.max(1, servings)) : 2;

    const ingredients = String(body?.ingredients ?? "").trim();
    const restrictions = body?.restrictions ? String(body.restrictions).trim() : "";
    const cuisine = body?.cuisine ? String(body.cuisine).trim() : "";
    const allowExtras = Boolean(body?.allowExtras ?? false); // ✅ NOVO

    const equipment = normalizeEquipment(body?.equipment ? String(body.equipment) : "", cuisine);

    if (ingredients.length < 3) {
      return badRequest("ingredients é obrigatório (mínimo 3 caracteres).");
    }

    const hints = analyzeRestrictions(restrictions);
    const parsed = parseUserInput(ingredients);

    const equipmentGuide =
      equipment === "air_fryer"
        ? "Equipamento: AIR FRYER. Inclua tempo e temperatura (ex: 180–200°C). Evite muito molho; se usar, em recipiente."
        : equipment === "forno"
        ? "Equipamento: FORNO. Inclua pré-aquecimento quando fizer sentido e temperatura/tempo (ex: 180°C)."
        : equipment === "fogao"
        ? "Equipamento: FOGÃO. Indique fogo baixo/médio/alto quando ajudar."
        : "Equipamento: LIVRE (qualquer).";

    const healthGuide =
      hints.guidance.length > 0
        ? `Ajustes por restrições: ${hints.guidance.join(" ")}`
        : "";

    const disclaimer =
      hints.hasHealth
        ? "Aviso curto obrigatório em notes: 'Sugestão geral, não substitui orientação profissional.'"
        : "Se não houver observação importante, notes deve ser string vazia.";

    const strictRule = !allowExtras
      ? [
          "MODO ESTRITO (permitir extras = DESLIGADO):",
          `- Você SÓ pode usar os ingredientes citados pelo usuário. Lista permitida: ${parsed.allowedIngredientsRaw.join(" | ")}`,
          "- É PROIBIDO adicionar ingredientes extras comestíveis (ex: alho, cebola, pimenta, chimichurri, vinagre, limão, ervas, manteiga, azeite, óleo, açúcar, etc).",
          "- extraNeeded deve ser SEMPRE um array vazio: [].",
          "- A lista 'ingredients' deve conter APENAS itens compatíveis com a lista permitida (com quantidades).",
          "- As 3 sugestões devem ser diferentes por TÉCNICA (fogo direto/indireto, descanso, selagem, reverse sear, espessura/corte), NÃO por molho/tempero extra.",
          "- Se ficar minimalista demais, escreva em notes: 'Modo estrito: ative Permitir extras para versões mais completas.'",
        ].join("\n")
      : [
          "MODO LIVRE (permitir extras = LIGADO):",
          "- Use os ingredientes do usuário como base.",
          "- Você PODE adicionar ingredientes extras, mas limite a no máximo 6 extras por receita.",
          "- Coloque os extras também em 'extraNeeded' (somente os extras, sem repetir os ingredientes do usuário).",
          "- As 3 sugestões devem ser diferentes (técnica + variação de preparo), não só trocar o nome.",
        ].join("\n");

    const doneness = donenessGuide(parsed.preferences.doneness);

    async function callModel(extraSystemNudge = "") {
      const openai = new OpenAI({ apiKey: env.OPENAI_API_KEY });
      const model = env.OPENAI_MODEL || "gpt-4o-mini";

      const response = await openai.responses.create({
        model,
        input: [
          {
            role: "system",
            content: [
              "Você é um chef prático e cuidadoso. Responda SOMENTE em JSON seguindo o schema.",
              "Gere exatamente 3 sugestões bem diferentes.",
              "REGRAS FIXAS:",
              "1) Ingredientes com quantidades aproximadas (ex: '1 kg de picanha').",
              "2) Passos completos: 6 a 12 passos. Cada passo com ação + tempo e/ou temperatura e/ou nível de fogo.",
              "3) Adapte ao equipamento (forno/air fryer/fogão) com tempo/temperatura/fogo.",
              "4) Respeite restrições (sem prometer cura; orientação geral).",
              "5) notes deve ser string (nunca 'null'). PT-BR.",
              disclaimer,
              equipmentGuide,
              healthGuide,
              doneness,
              strictRule,
              extraSystemNudge,
            ].filter(Boolean).join("\n"),
          },
          {
            role: "user",
            content:
              `Ingredientes (do usuário): ${ingredients}\n` +
              `Porções desejadas: ${servingsSafe}\n` +
              (restrictions ? `Restrições/saúde/dieta: ${restrictions}\n` : "") +
              (cuisine ? `Equipamento/estilo: ${cuisine}\n` : "") +
              `permitir extras: ${allowExtras ? "sim" : "nao"}\n` +
              "Crie 3 receitas com preparo completo.",
          },
        ],
        text: {
          format: {
            type: "json_schema",
            name: "recipe_suggestions",
            strict: true,
            schema: {
              type: "object",
              additionalProperties: false,
              properties: {
                suggestions: {
                  type: "array",
                  minItems: 3,
                  maxItems: 3,
                  items: {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                      id: { type: "string" },
                      title: { type: "string" },
                      servings: { type: "integer", minimum: 1 },
                      timeMinutes: { type: "integer", minimum: 1 },
                      category: { type: "string" },
                      ingredients: { type: "array", minItems: 1, items: { type: "string" } },
                      steps: {
                        type: "array",
                        minItems: 6,
                        maxItems: 12,
                        items: { type: "string" },
                      },
                      extraNeeded: { type: "array", items: { type: "string" } },
                      notes: { type: "string" },
                    },
                    required: ["id", "title", "servings", "timeMinutes", "category", "ingredients", "steps", "extraNeeded", "notes"],
                  },
                },
              },
              required: ["suggestions"],
            },
          },
        },
      });

      if (response.status !== "completed") {
        throw new Error("Resposta incompleta do modelo.");
      }

      const first = response.output?.[0]?.content?.[0];
      if ((first as any)?.type === "refusal") {
        throw new Error("O modelo recusou o pedido.");
      }

      const raw = (response as any).output_text as string | undefined;
      if (!raw) throw new Error("Sem output_text na resposta.");

      const parsedJson = JSON.parse(raw);
      const suggestionsRaw = Array.isArray(parsedJson?.suggestions) ? parsedJson.suggestions : [];
      const suggestions = suggestionsRaw.slice(0, 3).map((s: any, i: number) => sanitizeSuggestion(s, i));
      return suggestions;
    }

    try {
      let suggestions = await callModel();

      if (!allowExtras) {
        const violations = validateStrict(suggestions, parsed.allowedKeys);
        if (violations.length > 0) {
   
          suggestions = await callModel(
            "Você QUEBROU as regras no modo estrito. Refaça. Violações detectadas:\n" +
              violations.slice(0, 8).join("\n")
          );

          const v2 = validateStrict(suggestions, parsed.allowedKeys);
          if (v2.length > 0) {
          
            return badRequest(
              "A IA insistiu em adicionar ingredientes extras mesmo no modo estrito. Tente novamente ou ative 'Permitir extras'.",
              { violations: v2 }
            );
          }
        }
      }

      return json({ ok: true, suggestions });
    } catch (e: any) {
      const msg = e?.message ?? "Internal error";
      const details = e?.error ?? e?.response ?? e;
      return serverError("OpenAI error", { message: msg, details });
    }
  },
};
