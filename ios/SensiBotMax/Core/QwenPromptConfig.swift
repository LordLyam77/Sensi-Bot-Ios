import Foundation

public enum QwenPromptConfig {
    public static let modelName = "Qwen3-0.6B-Instruct"
    public static let modelFilename = "qwen3-0.6b-instruct.gguf"
    public static let modelQuantization = "Q4_K_M (4-bit Mobile Quantized)"
    public static let estimatedModelSizeMb = 392

    // Official Hugging Face Direct Download URL for on-demand installation
    public static let modelDownloadUrl = URL(
        string: "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
    )!

    public static let systemPrompt: String = """
    You are SENSI BOT Pro, an offline gaming sensitivity coach.
    You do not control or modify the game.
    You provide manual sensitivity recommendations and aim-training advice.
    Never claim guaranteed headshots.
    When structured recommendations are provided by the recommendation engine, treat them as authoritative.
    Do not invent sensitivity values that contradict the provided recommendation.
    Keep responses concise and useful.
    If 600+ DPI is recommended or discussed for high-flick devices, advise turning it back down after playing.
    Never provide cheats, automation, injection, memory modification, anti-ban methods, or game modification instructions.
    """

    public static func buildChatMLPrompt(
        systemInstruction: String = systemPrompt,
        context: String? = nil,
        history: [(user: String, assistant: String)] = [],
        userMessage: String
    ) -> String {
        var prompt = "<|im_start|>system\n\(systemInstruction.trimmingCharacters(in: .whitespacesAndNewlines))"
        if let ctx = context, !ctx.isEmpty {
            prompt += "\n\n[AUTHORITATIVE RECOMMENDATION CONTEXT]\n\(ctx)\nStrictly use the sensitivity numbers and advice provided above. Convert them into natural, motivating coach feedback."
        }
        prompt += "\n<|im_end|>\n"

        for turn in history.suffix(3) {
            prompt += "<|im_start|>user\n\(turn.user.trimmingCharacters(in: .whitespacesAndNewlines))\n<|im_end|>\n"
            prompt += "<|im_start|>assistant\n\(turn.assistant.trimmingCharacters(in: .whitespacesAndNewlines))\n<|im_end|>\n"
        }

        prompt += "<|im_start|>user\n\(userMessage.trimmingCharacters(in: .whitespacesAndNewlines))\n<|im_end|>\n"
        prompt += "<|im_start|>assistant\n"
        return prompt
    }
}
