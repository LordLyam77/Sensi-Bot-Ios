import SwiftUI

public struct SensiLogoView: View {
    public let size: CGFloat
    public let withGlow: Bool

    public init(size: CGFloat = 32, withGlow: Bool = true) {
        self.size = size
        self.withGlow = withGlow
    }

    public var body: some View {
        Group {
            if let path = Bundle.main.path(forResource: "sensi_logo", ofType: "png"),
               let uiImg = UIImage(contentsOfFile: path) {
                Image(uiImage: uiImg)
                    .resizable()
                    .scaledToFit()
            } else if let uiImg = UIImage(named: "sensi_logo") {
                Image(uiImage: uiImg)
                    .resizable()
                    .scaledToFit()
            } else {
                // Procedural high-tech vector fallback
                ZStack {
                    // Outer ring
                    Circle()
                        .stroke(SensiTheme.rubyRed, lineWidth: max(2, size * 0.08))
                        .frame(width: size * 0.72, height: size * 0.72)
                    
                    // Crosshair ticks
                    Capsule()
                        .fill(Color.white)
                        .frame(width: max(2, size * 0.06), height: size * 0.22)
                        .offset(y: -size * 0.36)
                    Capsule()
                        .fill(Color.white)
                        .frame(width: max(2, size * 0.06), height: size * 0.22)
                        .offset(y: size * 0.36)
                    Capsule()
                        .fill(Color.white)
                        .frame(width: size * 0.22, height: max(2, size * 0.06))
                        .offset(x: -size * 0.36)
                    Capsule()
                        .fill(Color.white)
                        .frame(width: size * 0.22, height: max(2, size * 0.06))
                        .offset(x: size * 0.36)

                    // Center dot
                    Circle()
                        .fill(SensiTheme.rubyRed)
                        .frame(width: size * 0.18, height: size * 0.18)
                    Circle()
                        .fill(Color.white)
                        .frame(width: size * 0.06, height: size * 0.06)
                }
            }
        }
        .frame(width: size, height: size)
        .shadow(color: withGlow ? SensiTheme.rubyRed.opacity(0.8) : .clear, radius: withGlow ? size * 0.25 : 0)
    }
}
