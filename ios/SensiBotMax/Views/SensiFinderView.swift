import SwiftUI

public struct SensiFinderView: View {
    @State private var deviceProfile: IOSDeviceProfile = DeviceProbe.current()
    @State private var overshootOption: Int = 1 // 0: Over Head, 1: Balanced, 2: Stuck on Chest
    @State private var tempoOption: Int = 0     // 0: Rusher, 1: Balanced, 2: Sniper
    @State private var weaponOption: Int = 0    // 0: Shotgun, 1: SMG, 2: AR, 3: Sniper
    @State private var fingerOption: Int = 2    // 2: 2-finger, 3: 3-finger, 4: 4-finger
    @State private var resultRecommendation: IOSRecommendation? = nil

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Image(systemName: "slider.horizontal.3")
                            .foregroundColor(SensiTheme.rubyRed)
                        Text("SENSI FINDER")
                            .font(.system(size: 22, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                    }
                    Text("Interactive 4-step biometric questionnaire calibrated for iOS.")
                        .font(.system(size: 13))
                        .foregroundColor(SensiTheme.textSecondary)
                }

                // Step 1: Aim Problem
                QuestionCard(title: "1. What happens during your upward drag?") {
                    OptionButton(label: "Crosshair Flies Over Head", isSelected: overshootOption == 0) {
                        overshootOption = 0
                    }
                    OptionButton(label: "Aim Drags Cleanly / Red Numbers", isSelected: overshootOption == 1) {
                        overshootOption = 1
                    }
                    OptionButton(label: "Stuck on Chest / Can't Lift Aim", isSelected: overshootOption == 2) {
                        overshootOption = 2
                    }
                }

                // Step 2: Playstyle Tempo
                QuestionCard(title: "2. Your competitive combat role:") {
                    OptionButton(label: "Aggressive Rusher (High Flick Speed)", isSelected: tempoOption == 0) {
                        tempoOption = 0
                    }
                    OptionButton(label: "Balanced Flex (All-Round)", isSelected: tempoOption == 1) {
                        tempoOption = 1
                    }
                    OptionButton(label: "Long-Range / Precision Sniper", isSelected: tempoOption == 2) {
                        tempoOption = 2
                    }
                }

                // Step 3: Primary Weapon Focus
                QuestionCard(title: "3. Primary gun class:") {
                    OptionButton(label: "Shotgun (M1887, Charge Buster, SPAS12)", isSelected: weaponOption == 0) {
                        weaponOption = 0
                    }
                    OptionButton(label: "SMG Spray (MP40, UMP, Thompson)", isSelected: weaponOption == 1) {
                        weaponOption = 1
                    }
                    OptionButton(label: "Assault Rifle (M4A1, Groza, Scar)", isSelected: weaponOption == 2) {
                        weaponOption = 2
                    }
                    OptionButton(label: "Sniper / Marksman (AWM, M82B, AC80)", isSelected: weaponOption == 3) {
                        weaponOption = 3
                    }
                }

                // Step 4: Finger Grip
                QuestionCard(title: "4. Finger HUD layout:") {
                    HStack(spacing: 8) {
                        GripButton(label: "2-Finger Thumb", isSelected: fingerOption == 2) { fingerOption = 2 }
                        GripButton(label: "3-Finger Claw", isSelected: fingerOption == 3) { fingerOption = 3 }
                        GripButton(label: "4-Finger Claw", isSelected: fingerOption == 4) { fingerOption = 4 }
                    }
                }

                // Generate Button
                Button(action: calculateFinderProfile) {
                    HStack {
                        Image(systemName: "wand.and.stars")
                        Text("GENERATE iOS PROFILE")
                    }
                    .font(.system(size: 15, weight: .black, design: .rounded))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 15)
                    .background(
                        LinearGradient(
                            colors: [SensiTheme.rubyRed, SensiTheme.rubyRedDark],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }

                if let rec = resultRecommendation {
                    SensitivityDisplayCard(recommendation: rec, deviceProfile: deviceProfile)
                        .padding(.top, 10)
                }

                Spacer(modifier = Modifier.height(40))
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
        }
        .background(SensiTheme.voidBlack.ignoresSafeArea())
    }

    private func calculateFinderProfile() {
        let overshootVal: Double = overshootOption == 0 ? 1.0 : (overshootOption == 2 ? -1.0 : 0.0)
        let tempoVal: Double = tempoOption == 0 ? 1.0 : (tempoOption == 2 ? -1.0 : 0.0)
        let urgencyVal: Double = weaponOption == 0 ? 1.0 : (weaponOption == 1 ? 0.5 : (weaponOption == 3 ? -1.0 : 0.0))

        let signals = PlayerSignals(
            overshoot: overshootVal,
            tempo: tempoVal,
            consistency: 0.85,
            weaponUrgency: urgencyVal,
            currentVsBaseline: 0.0
        )

        self.resultRecommendation = IOSSensitivityEngine.recommend(
            profile: deviceProfile,
            touchHz: nil,
            signals: signals,
            fingers: fingerOption,
            learnedBias: FeedbackTuner.bias
        )
    }
}

struct QuestionCard<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(SensiTheme.textPrimary)
            content
        }
        .gamingCard()
    }
}

struct OptionButton: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(label)
                    .font(.system(size: 13, weight: isSelected ? .bold : .regular))
                    .foregroundColor(isSelected ? .white : SensiTheme.textSecondary)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(SensiTheme.rubyRed)
                }
            }
            .padding(12)
            .background(isSelected ? SensiTheme.rubyRed.opacity(0.12) : Color.black.opacity(0.25))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(isSelected ? SensiTheme.rubyRed.opacity(0.6) : Color.white.opacity(0.06), lineWidth: 1)
            )
        }
    }
}

struct GripButton: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 11, weight: isSelected ? .bold : .regular))
                .foregroundColor(isSelected ? .white : SensiTheme.textSecondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(isSelected ? SensiTheme.rubyRed.opacity(0.15) : Color.black.opacity(0.25))
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(isSelected ? SensiTheme.rubyRed.opacity(0.6) : Color.white.opacity(0.06), lineWidth: 1)
                )
        }
    }
}
