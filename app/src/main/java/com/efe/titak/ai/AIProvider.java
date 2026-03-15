package com.efe.titak.ai;

/**
 * Otomatik tespit edilen yapay zeka sağlayıcı türleri.
 */
public enum AIProvider {
    OPENAI,           // api.openai.com — OpenAI resmi
    OPENAI_COMPATIBLE, // Özel / v1/chat/completions uyumlu (LocalAI, Groq, vb.)
    ANTHROPIC,        // api.anthropic.com — Claude
    GOOGLE            // generativelanguage.googleapis.com — Gemini
}
