// FocusShield AI Coach — Supabase Edge Function
// Proxies requests to Gemini 2.0 Flash. API key stored in Supabase Vault.
//
// Deploy: supabase functions deploy ai-coach --no-verify-jwt

import { serve } from "https://deno.land/std@0.177.0/http/server.ts"

declare const Deno: {
  env: {
    get(key: string): string | undefined
  }
}

const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY") ?? ""
const GEMINI_MODEL = "gemini-2.0-flash"
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`

const MAX_PROMPT_LENGTH = 6000
const RATE_LIMIT_WINDOW_MS = 60_000
const MAX_REQUESTS_PER_WINDOW = 20

const requestLog: { timestamp: number; userId: string }[] = []

function isRateLimited(userId: string): boolean {
  const now = Date.now()
  // Remove entries older than the window
  while (requestLog.length > 0 && now - requestLog[0].timestamp > RATE_LIMIT_WINDOW_MS) {
    requestLog.shift()
  }
  const userRequests = requestLog.filter((r) => r.userId === userId).length
  return userRequests >= MAX_REQUESTS_PER_WINDOW
}

function sanitizePrompt(prompt: string): string {
  let sanitized = prompt
  // Strip attempts to override the system prompt
  sanitized = sanitized.replace(/ignore (all |)previous instructions/gi, "[removed]")
  sanitized = sanitized.replace(/forget (all |)your (system |)prompt/gi, "[removed]")
  sanitized = sanitized.replace(/you are now .+/gi, "[removed]")
  sanitized = sanitized.replace(/pretend to be .+/gi, "[removed]")
  sanitized = sanitized.replace(/act as .+/gi, "[removed]")
  sanitized = sanitized.replace(/system:\s*.+/gi, "[removed]")
  return sanitized
}

interface SafetyFlag {
  type: string
  reason: string
}

function checkContentSafety(reply: string): SafetyFlag[] {
  const flags: SafetyFlag[] = []
  const lowerReply = reply.toLowerCase()

  const dangerousPatterns = [
    { pattern: /self[\s-]*harm/i, type: "SELF_HARM" },
    { pattern: /suicide/i, type: "SUICIDE" },
    { pattern: /kill (yourself|myself)/i, type: "VIOLENCE" },
    { pattern: /how to (make|build|create) .*(bomb|weapon|drug)/i, type: "DANGEROUS_CONTENT" },
  ]

  for (const { pattern, type } of dangerousPatterns) {
    if (pattern.test(lowerReply)) {
      flags.push({ type, reason: `Response contained flagged pattern: ${type}` })
    }
  }

  return flags
}

serve(async (req: Request) => {
  // CORS preflight
  if (req.method === "OPTIONS") {
    return new Response(null, {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
        "Access-Control-Allow-Headers": "Authorization, Content-Type",
      },
    })
  }

  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    })
  }

  // Extract user identity from Supabase auth header
  const authHeader = req.headers.get("Authorization") ?? ""
  const userId = authHeader.replace("Bearer ", "").substring(0, 20) || "anonymous"

  // Rate limit check
  if (isRateLimited(userId)) {
    return new Response(JSON.stringify({ error: "Rate limit exceeded. Please wait before sending more messages.", retryAfter: RATE_LIMIT_WINDOW_MS / 1000 }), {
      status: 429,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    })
  }

  let body: { prompt?: string }
  try {
    body = await req.json()
  } catch {
    return new Response(JSON.stringify({ error: "Invalid JSON body" }), {
      status: 400,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    })
  }

  const rawPrompt = body.prompt ?? ""
  if (!rawPrompt || rawPrompt.trim().length === 0) {
    return new Response(JSON.stringify({ reply: "I didn't catch that. Could you say it again?", safetyFlags: [] }), {
      status: 200,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    })
  }

  if (rawPrompt.length > MAX_PROMPT_LENGTH) {
    return new Response(JSON.stringify({ error: "Prompt too long. Maximum is " + MAX_PROMPT_LENGTH + " characters." }), {
      status: 400,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    })
  }

  const prompt = sanitizePrompt(rawPrompt)
  requestLog.push({ timestamp: Date.now(), userId })

  if (!GEMINI_API_KEY) {
    console.error("GEMINI_API_KEY not configured in Supabase Vault")
    return new Response(JSON.stringify({ reply: "I'm having a temporary configuration issue. Please try again shortly.", safetyFlags: [], error: "KEY_NOT_CONFIGURED" }), {
      status: 200,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    })
  }

  try {
    const geminiResponse = await fetch(`${GEMINI_URL}?key=${GEMINI_API_KEY}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        safetySettings: [
          { category: "HARM_CATEGORY_HARASSMENT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
          { category: "HARM_CATEGORY_HATE_SPEECH", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
          { category: "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
          { category: "HARM_CATEGORY_DANGEROUS_CONTENT", threshold: "BLOCK_MEDIUM_AND_ABOVE" },
        ],
      }),
    })

    if (!geminiResponse.ok) {
      const errText = await geminiResponse.text()
      console.error(`Gemini API error ${geminiResponse.status}: ${errText}`)
      return new Response(JSON.stringify({ reply: "I'm having trouble thinking right now. Can you try again?", safetyFlags: [] }), {
        status: 200,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      })
    }

    const data = await geminiResponse.json()
    const reply = data?.candidates?.[0]?.content?.parts?.[0]?.text ?? "I'm here for you! Keep pushing forward."

    // Safety check on response
    const safetyFlags = checkContentSafety(reply)

    return new Response(JSON.stringify({ reply, safetyFlags }), {
      status: 200,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    })
  } catch (err) {
    console.error("Gemini API exception:", err)
    return new Response(JSON.stringify({ reply: "I'm having a connection issue right now. But remember — every moment of resistance builds your strength.", safetyFlags: [] }), {
      status: 200,
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
    })
  }
})
