import SwiftUI

public struct FloatingTargetingSilhouetteView: View {
    let targetZone: TargetZoneType
    let isCalculating: Bool
    let calculationProgress: Double

    @State private var scanLineY: CGFloat = 0.0
    @State private var pulseScale: CGFloat = 1.0

    public init(targetZone: TargetZoneType, isCalculating: Bool, calculationProgress: Double) {
        self.targetZone = targetZone
        self.isCalculating = isCalculating
        self.calculationProgress = calculationProgress
    }

    public var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height

            ZStack {
                // 1. Tactical Grid Background
                Canvas { context, size in
                    let step: CGFloat = 14
                    var x: CGFloat = 0
                    while x <= size.width {
                        var path = Path()
                        path.move(to: CGPoint(x: x, y: 0))
                        path.addLine(to: CGPoint(x: x, y: size.height))
                        context.stroke(path, with: .color(Color(red: 0.08, green: 0.09, blue: 0.14)), lineWidth: 0.8)
                        x += step
                    }
                    var y: CGFloat = 0
                    while y <= size.height {
                        var path = Path()
                        path.move(to: CGPoint(x: 0, y: y))
                        path.addLine(to: CGPoint(x: size.width, y: y))
                        context.stroke(path, with: .color(Color(red: 0.08, green: 0.09, blue: 0.14)), lineWidth: 0.8)
                        y += step
                    }
                }
                .background(Color(red: 0.04, green: 0.04, blue: 0.07))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                // 2. Character Image (Red Criminal) or Holographic Silhouette
                if let uiImage = loadCharacterImage() {
                    Image(uiImage: uiImage)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: w * 0.82, height: h * 0.90)
                        .opacity(isCalculating ? 0.6 : 0.92)
                } else {
                    // Vector Holographic Character Fallback
                    VStack(spacing: 3) {
                        Circle()
                            .fill(LinearGradient(colors: [targetZone.color.opacity(0.8), targetZone.color.opacity(0.3)], startPoint: .top, endPoint: .bottom))
                            .frame(width: 32, height: 32)
                        RoundedRectangle(cornerRadius: 6)
                            .fill(LinearGradient(colors: [targetZone.color.opacity(0.6), targetZone.color.opacity(0.2)], startPoint: .top, endPoint: .bottom))
                            .frame(width: 48, height: 56)
                        HStack(spacing: 6) {
                            RoundedRectangle(cornerRadius: 4)
                                .fill(LinearGradient(colors: [targetZone.color.opacity(0.5), targetZone.color.opacity(0.15)], startPoint: .top, endPoint: .bottom))
                                .frame(width: 18, height: 52)
                            RoundedRectangle(cornerRadius: 4)
                                .fill(LinearGradient(colors: [targetZone.color.opacity(0.5), targetZone.color.opacity(0.15)], startPoint: .top, endPoint: .bottom))
                                .frame(width: 18, height: 52)
                        }
                    }
                    .frame(height: h * 0.85)
                }

                // 3. Aim Lock Reticles (Head / Body / Legs)
                if !isCalculating {
                    switch targetZone {
                    case .head:
                        // Headshot Lock Ring
                        Circle()
                            .stroke(SensiTheme.rubyRed, lineWidth: 2)
                            .frame(width: 38, height: 38)
                            .scaleEffect(pulseScale)
                            .position(x: w / 2, y: h * 0.22)
                            .overlay(
                                Image(systemName: "scope")
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundColor(SensiTheme.rubyRed)
                                    .position(x: w / 2, y: h * 0.22)
                            )
                    case .body:
                        // Body DPS Chest Lock Box
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(SensiTheme.cyanAccent, lineWidth: 2)
                            .frame(width: 54, height: 44)
                            .position(x: w / 2, y: h * 0.44)
                            .overlay(
                                Image(systemName: "shield.fill")
                                    .font(.system(size: 16))
                                    .foregroundColor(SensiTheme.cyanAccent)
                                    .position(x: w / 2, y: h * 0.44)
                            )
                    case .legs:
                        // Leg Sweep Target Area
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(SensiTheme.goldAccent, lineWidth: 2)
                            .frame(width: 60, height: 42)
                            .position(x: w / 2, y: h * 0.72)
                            .overlay(
                                Image(systemName: "bolt.fill")
                                    .font(.system(size: 16))
                                    .foregroundColor(SensiTheme.goldAccent)
                                    .position(x: w / 2, y: h * 0.72)
                            )
                    }
                } else {
                    // 4. Calculating Phase: Cyber Laser Scanning Line
                    VStack {
                        Rectangle()
                            .fill(
                                LinearGradient(
                                    colors: [Color.clear, targetZone.color, Color.clear],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(height: 2.5)
                            .shadow(color: targetZone.color, radius: 6)
                    }
                    .offset(y: (h * CGFloat(calculationProgress)) - (h / 2))

                    // Calculating Progress Banner
                    VStack {
                        Spacer()
                        Text("ANALYZING \(Int(calculationProgress * 100))%")
                            .font(.system(size: 9, weight: .black, design: .monospaced))
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 3)
                            .background(targetZone.color)
                            .clipShape(RoundedRectangle(cornerRadius: 4))
                            .padding(.bottom, 6)
                    }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(targetZone.color.opacity(0.5), lineWidth: 1.2)
            )
            .onAppear {
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                    pulseScale = 1.15
                }
            }
        }
    }

    private func loadCharacterImage() -> UIImage? {
        if let path = Bundle.main.path(forResource: "character_criminal", ofType: "png") {
            return UIImage(contentsOfFile: path)
        }
        return UIImage(named: "character_criminal")
    }
}
