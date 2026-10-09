import SwiftUI

public enum SensiTheme {
    // Background Colors
    public static let voidBlack = Color(red: 7/255, green: 7/255, blue: 10/255)
    public static let surface = Color(red: 14/255, green: 14/255, blue: 22/255)
    public static let surfaceElevated = Color(red: 22/255, green: 22/255, blue: 34/255)
    public static let glassBorder = Color.white.opacity(0.10)
    
    // Brand Primary Accents
    public static let rubyRed = Color(red: 255/255, green: 42/255, blue: 77/255)
    public static let rubyRedDark = Color(red: 204/255, green: 16/255, blue: 48/255)
    public static let rubyGlow = Color(red: 255/255, green: 42/255, blue: 77/255).opacity(0.35)
    
    // Tactical Accents
    public static let cyanAccent = Color(red: 0/255, green: 229/255, blue: 255/255)
    public static let goldAccent = Color(red: 255/255, green: 179/255, blue: 0/255)
    public static let greenFairPlay = Color(red: 0/255, green: 200/255, blue: 83/255)
    
    // Text Colors
    public static let textPrimary = Color.white
    public static let textSecondary = Color(red: 160/255, green: 160/255, blue: 184/255)
    public static let textMuted = Color(red: 101/255, green: 101/255, blue: 122/255)
}

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
