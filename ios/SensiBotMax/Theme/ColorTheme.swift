import SwiftUI

public enum SensiTheme {
    // Background Colors
    public static let voidBlack = Color(red: 7/255, green: 7/255, blue: 10/255)
    public static let obsidianBlack = Color(red: 7/255, green: 7/255, blue: 10/255)
    public static let surface = Color(red: 14/255, green: 14/255, blue: 22/255)
    public static let surfaceElevated = Color(red: 22/255, green: 22/255, blue: 34/255)
    public static let glassBorder = Color.white.opacity(0.10)
    public static let cardBorder = Color.white.opacity(0.10)
    
    // Brand Primary Accents
    public static let rubyRed = Color(red: 255/255, green: 42/255, blue: 77/255)
    public static let rubyDark = Color(red: 204/255, green: 16/255, blue: 48/255)
    public static let rubyRedDark = Color(red: 204/255, green: 16/255, blue: 48/255)
    public static let rubyGlow = Color(red: 255/255, green: 42/255, blue: 77/255).opacity(0.35)
    
    // Tactical Accents
    public static let cyanAccent = Color(red: 0/255, green: 229/255, blue: 255/255)
    public static let goldAccent = Color(red: 255/255, green: 179/255, blue: 0/255)
    public static let fairPlayGreen = Color(red: 0/255, green: 200/255, blue: 83/255)
    public static let greenFairPlay = Color(red: 0/255, green: 200/255, blue: 83/255)
    
    // Text Colors
    public static let pureWhite = Color.white
    public static let textPrimary = Color.white
    public static let textSecondary = Color(red: 160/255, green: 160/255, blue: 184/255)
    public static let textMuted = Color(red: 101/255, green: 101/255, blue: 122/255)

    public struct CardModifier: ViewModifier {
        public var borderColor: Color
        public init(borderColor: Color = SensiTheme.cardBorder) {
            self.borderColor = borderColor
        }
        public func body(content: Content) -> some View {
            content
                .padding(16)
                .background(SensiTheme.surface)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(borderColor, lineWidth: 1)
                )
        }
    }
}

public typealias ColorTheme = SensiTheme

public struct GamingCardModifier: ViewModifier {
    var borderColor: Color = SensiTheme.glassBorder
    
    public func body(content: Content) -> some View {
        content
            .padding(16)
            .background(SensiTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(borderColor, lineWidth: 1)
            )
    }
}

public extension View {
    func gamingCard(borderColor: Color = SensiTheme.glassBorder) -> some View {
        self.modifier(GamingCardModifier(borderColor: borderColor))
    }
}

public extension Color {
    init(hex: String) {
        let cleanHex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: cleanHex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch cleanHex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 255, 42, 77)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
