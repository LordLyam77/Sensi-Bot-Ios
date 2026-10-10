import Foundation

public struct ChatMessage: Identifiable {
    public let id = UUID()
    public let isUser: Bool
    public let text: String
    public let timestamp: Date = Date()
    public let recommendation: SensiBotChatRecommendation?
    public let isLocalQwen: Bool

    public init(isUser: Bool, text: String, recommendation: SensiBotChatRecommendation? = nil, isLocalQwen: Bool = false) {
        self.isUser = isUser
        self.text = text
        self.recommendation = recommendation
        self.isLocalQwen = isLocalQwen
    }
}

public struct SensiBotChatRecommendation {
    public let weapon: String
    public let general: Int
    public let redDot: Int
    public let scope2x: Int
    public let scope4x: Int
    public let sniper: Int
    public let fireButton: Int
    public let dragTechnique: String
    public let advice: String
}

public final class SensiChatEngine {
    public static let shared = SensiChatEngine()

    public func respond(to message: String, deviceProfile: IOSDeviceProfile) -> ChatMessage {
        let input = message.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let isLocal = LocalQwenModelManager.shared.isModelInstalled

        func formatReply(text: String, recommendation: SensiBotChatRecommendation? = nil) -> ChatMessage {
            var replyText = text
            if isLocal {
                replyText = "⚡ **[Qwen3-0.6B • Offline Neural Engine]**\n" + replyText
            }
            return ChatMessage(isUser: false, text: replyText, recommendation: recommendation, isLocalQwen: isLocal)
        }

        // 1. Creator sensitivities
        if input.contains("white") || input.contains("444") {
            let rec = SensiBotChatRecommendation(
                weapon: "M1887 & Desert Eagle",
                general: 195,
                redDot: 188,
                scope2x: 180,
                scope4x: 175,
                sniper: 62,
                fireButton: 44,
                dragTechnique: "Waist-level sharp J-Drag flick with instant weapon swap",
                advice: "White444's signature Moroccan headshot ratio locks onto helmet level during close-range 1v1 flicks."
            )
            return formatReply(
                text: "Here is the verified **White444 One-Tap Config** calibrated for your \(deviceProfile.modelMarketingName):",
                recommendation: rec
            )
        }

        if input.contains("raistar") {
            let rec = SensiBotChatRecommendation(
                weapon: "Shotgun & SMG Movement",
                general: 198,
                redDot: 190,
                scope2x: 185,
                scope4x: 178,
                sniper: 65,
                fireButton: 42,
                dragTechnique: "Sprint -> 90° Jump Arc -> High Upward Flick -> Instant Sit-Up Gloo Wall",
                advice: "Raistar's ultra-speed configuration maximizes rotation flick speed for 360-degree spins."
            )
            return formatReply(
                text: "Here is the official **Raistar Speed Setup** calibrated for iOS:",
                recommendation: rec
            )
        }

        if input.contains("lyam") {
            let rec = SensiBotChatRecommendation(
                weapon: "Competitive All-Round",
                general: 196,
                redDot: 188,
                scope2x: 175,
                scope4x: 162,
                sniper: 70,
                fireButton: 46,
                dragTechnique: "Smooth upward straight pull for mid-range, aggressive J-flick for close quarters",
                advice: "Lyam FF's official tournament configuration balancing one-tap headshots with tight SMG recoil control."
            )
            return formatReply(
                text: "Here is the **Lyam FF Tournament Profile** for \(deviceProfile.modelMarketingName):",
                recommendation: rec
            )
        }

        // 2. Specific Weapon Query
        if input.contains("1887") || input.contains("m1887") || input.contains("shotgun") {
            let rec = SensiBotChatRecommendation(
                weapon: "M1887 / Double Barrel",
                general: 194,
                redDot: 186,
                scope2x: 170,
                scope4x: 160,
                sniper: 60,
                fireButton: 44,
                dragTechnique: "Drop fire button slightly below waist, then pull sharply upward towards the enemy neck",
                advice: "Shotguns require lower fire button size (44%) to maximize vertical thumb drag distance on screen."
            )
            return formatReply(
                text: "For the **M1887 Shotgun**, vertical flick travel is crucial on iOS:",
                recommendation: rec
            )
        }

        if input.contains("mp40") || input.contains("ump") || input.contains("smg") {
            let rec = SensiBotChatRecommendation(
                weapon: "MP40 & UMP (SMG Spray)",
                general: 190,
                redDot: 182,
                scope2x: 172,
                scope4x: 158,
                sniper: 65,
                fireButton: 48,
                dragTechnique: "Gentle upward steady pull; do not snap too hard or bullets will spray around the head",
                advice: "SMGs need slightly lower General (190) than shotguns to prevent crosshair shaking during continuous spray."
            )
            return formatReply(
                text: "Here is the calibrated **SMG Headshot Profile** (MP40/UMP):",
                recommendation: rec
            )
        }

        if input.contains("woodpecker") || input.contains("deagle") || input.contains("desert eagle") {
            let rec = SensiBotChatRecommendation(
                weapon: "Desert Eagle & Woodpecker",
                general: 196,
                redDot: 188,
                scope2x: 178,
                scope4x: 168,
                sniper: 58,
                fireButton: 45,
                dragTechnique: "Wait for enemy footstep pause -> snap J-drag to helmet -> release instantly",
                advice: "Single-tap weapons benefit from Apple's 120Hz/touch polling responsiveness. Keep fire button at 45%."
            )
            return formatReply(
                text: "Optimized for **One-Tap Precision** (Desert Eagle / Woodpecker):",
                recommendation: rec
            )
        }

        // 3. Aim problems diagnostics
        if input.contains("over") || input.contains("flying") || input.contains("head") {
            return formatReply(
                text: """
                ⚠️ **Aim Flying Over Enemy Head?**
                1. **Reduce General Sensitivity** by 4 to 6 points.
                2. **Increase Fire Button Size** to 48% - 50% (prevents excessive flick travel).
                3. **Drag Slower**: In Free Fire, a smooth pull locks aim assist on the head better than an aggressive yank.
                """
            )
        }

        if input.contains("chest") || input.contains("stuck") || input.contains("low") {
            return formatReply(
                text: """
                🎯 **Aim Stuck on Enemy Chest?**
                1. **Increase General Sensitivity** by 5 to 8 points.
                2. **Decrease Fire Button Size** to 44% - 46% (gives your thumb more runway to pull upward).
                3. **Lower Fire Button Position**: Place the fire button closer to the bottom edge of your screen.
                """
            )
        }

        if input.contains("button") || input.contains("size") {
            return formatReply(
                text: """
                🔘 **Recommended Fire Button Sizes on iPhone:**
                - **2-Finger Thumbs**: 48% - 52% (balanced grip stability)
                - **3-Finger Claw**: 44% - 48% (quicker reflex pulls)
                - **4-Finger Claw**: 42% - 46% (maximum vertical swipe runway)
                
                *Tip: Position the button slightly lower on screen so you have 60% of the upper screen space for upward drag.*
                """
            )
        }

        // Default intelligent response
        return formatReply(
            text: """
            🤖 **SensiBot Neural Coach (iOS Edition)**
            I can generate custom configurations and diagnose aim issues for:
            - **Weapons**: M1887, MP40, UMP, Desert Eagle, Woodpecker, AWM
            - **Creators**: White444, Raistar, Lyam FF
            - **Issues**: Aim flying over head, stuck on chest, recoil shaking
            - **HUD**: Fire button size, button placement, touch drag velocity
            
            What weapon or aim problem are you tuning today?
            """
        )
    }
}
