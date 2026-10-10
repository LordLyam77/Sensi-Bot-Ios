import UIKit

public final class FloatingHudView: UIView {
    private let titleLabel = UILabel()
    private let statusBadge = UILabel()
    private let statsLabel = UILabel()
    private let hzBadge = UILabel()
    private let crosshairCenterDot = UIView()
    private let crosshairRing = UIView()
    private let lineTop = UIView()
    private let lineBottom = UIView()
    private let lineLeft = UIView()
    private let lineRight = UIView()

    public override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }

    private func setupView() {
        backgroundColor = UIColor(red: 0.043, green: 0.043, blue: 0.055, alpha: 0.94) // Obsidian dark
        layer.cornerRadius = 14
        layer.masksToBounds = true
        layer.borderColor = UIColor(red: 1.0, green: 0.165, blue: 0.302, alpha: 0.75).cgColor // Ruby Red
        layer.borderWidth = 1.5

        // 1. Top Header Row
        let headerStack = UIStackView()
        headerStack.axis = .horizontal
        headerStack.distribution = .equalSpacing
        headerStack.alignment = .center
        headerStack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(headerStack)

        titleLabel.text = "⚡ SENSI BOT PRO"
        titleLabel.font = UIFont.monospacedSystemFont(ofSize: 11, weight: .black)
        titleLabel.textColor = .white

        statusBadge.text = "● AIM LOCK"
        statusBadge.font = UIFont.monospacedSystemFont(ofSize: 10, weight: .bold)
        statusBadge.textColor = UIColor(red: 0.0, green: 0.9, blue: 0.46, alpha: 1.0) // Fair play green

        headerStack.addArrangedSubview(titleLabel)
        headerStack.addArrangedSubview(statusBadge)

        // 2. Center Tactical Crosshair
        // Center Ring
        crosshairRing.translatesAutoresizingMaskIntoConstraints = false
        crosshairRing.layer.cornerRadius = 18
        crosshairRing.layer.borderColor = UIColor(red: 0.0, green: 0.898, blue: 1.0, alpha: 0.35).cgColor
        crosshairRing.layer.borderWidth = 1.0
        addSubview(crosshairRing)

        // Center Cyan Dot
        crosshairCenterDot.translatesAutoresizingMaskIntoConstraints = false
        crosshairCenterDot.backgroundColor = UIColor(red: 0.0, green: 0.898, blue: 1.0, alpha: 1.0) // Glowing Cyan
        crosshairCenterDot.layer.cornerRadius = 3.5
        crosshairCenterDot.layer.shadowColor = UIColor(red: 0.0, green: 0.898, blue: 1.0, alpha: 1.0).cgColor
        crosshairCenterDot.layer.shadowRadius = 4
        crosshairCenterDot.layer.shadowOpacity = 0.9
        crosshairCenterDot.layer.shadowOffset = .zero
        addSubview(crosshairCenterDot)

        // Crosshair Reticle lines
        let ruby = UIColor(red: 1.0, green: 0.165, blue: 0.302, alpha: 0.9)
        for line in [lineTop, lineBottom, lineLeft, lineRight] {
            line.backgroundColor = ruby
            line.translatesAutoresizingMaskIntoConstraints = false
            line.layer.cornerRadius = 1
            addSubview(line)
        }

        // 3. Bottom Aim Stats Bar
        let bottomStack = UIStackView()
        bottomStack.axis = .horizontal
        bottomStack.distribution = .equalSpacing
        bottomStack.alignment = .center
        bottomStack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(bottomStack)

        statsLabel.text = "GEN:188  RED:172  2X:160  4X:145"
        statsLabel.font = UIFont.monospacedSystemFont(ofSize: 10, weight: .heavy)
        statsLabel.textColor = UIColor(red: 0.9, green: 0.9, blue: 0.95, alpha: 1.0)

        hzBadge.text = "120Hz"
        hzBadge.font = UIFont.monospacedSystemFont(ofSize: 9, weight: .black)
        hzBadge.textColor = UIColor(red: 0.0, green: 0.898, blue: 1.0, alpha: 1.0)
        hzBadge.backgroundColor = UIColor(red: 0.0, green: 0.898, blue: 1.0, alpha: 0.15)
        hzBadge.layer.cornerRadius = 4
        hzBadge.layer.masksToBounds = true

        bottomStack.addArrangedSubview(statsLabel)
        bottomStack.addArrangedSubview(hzBadge)

        // Layout Constraints
        NSLayoutConstraint.activate([
            // Header
            headerStack.topAnchor.constraint(equalTo: topAnchor, constant: 8),
            headerStack.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10),
            headerStack.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10),

            // Center Crosshair Dot
            crosshairCenterDot.centerXAnchor.constraint(equalTo: centerXAnchor),
            crosshairCenterDot.centerYAnchor.constraint(equalTo: centerYAnchor),
            crosshairCenterDot.widthAnchor.constraint(equalToConstant: 7),
            crosshairCenterDot.heightAnchor.constraint(equalToConstant: 7),

            // Crosshair Ring
            crosshairRing.centerXAnchor.constraint(equalTo: centerXAnchor),
            crosshairRing.centerYAnchor.constraint(equalTo: centerYAnchor),
            crosshairRing.widthAnchor.constraint(equalToConstant: 36),
            crosshairRing.heightAnchor.constraint(equalToConstant: 36),

            // Reticle Lines (with 6pt center clearance)
            lineTop.centerXAnchor.constraint(equalTo: centerXAnchor),
            lineTop.bottomAnchor.constraint(equalTo: crosshairCenterDot.topAnchor, constant: -6),
            lineTop.widthAnchor.constraint(equalToConstant: 2),
            lineTop.heightAnchor.constraint(equalToConstant: 12),

            lineBottom.centerXAnchor.constraint(equalTo: centerXAnchor),
            lineBottom.topAnchor.constraint(equalTo: crosshairCenterDot.bottomAnchor, constant: 6),
            lineBottom.widthAnchor.constraint(equalToConstant: 2),
            lineBottom.heightAnchor.constraint(equalToConstant: 12),

            lineLeft.centerYAnchor.constraint(equalTo: centerYAnchor),
            lineLeft.trailingAnchor.constraint(equalTo: crosshairCenterDot.leadingAnchor, constant: -6),
            lineLeft.widthAnchor.constraint(equalToConstant: 12),
            lineLeft.heightAnchor.constraint(equalToConstant: 2),

            lineRight.centerYAnchor.constraint(equalTo: centerYAnchor),
            lineRight.leadingAnchor.constraint(equalTo: crosshairCenterDot.trailingAnchor, constant: 6),
            lineRight.widthAnchor.constraint(equalToConstant: 12),
            lineRight.heightAnchor.constraint(equalToConstant: 2),

            // Bottom Stack
            bottomStack.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -8),
            bottomStack.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10),
            bottomStack.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10)
        ])

        startPulseAnimation()
    }

    private func startPulseAnimation() {
        UIView.animate(withDuration: 1.0, delay: 0, options: [.autoreverse, .repeat, .allowUserInteraction], animations: {
            self.crosshairCenterDot.transform = CGAffineTransform(scaleX: 1.3, y: 1.3)
            self.crosshairRing.layer.borderColor = UIColor(red: 1.0, green: 0.165, blue: 0.302, alpha: 0.6).cgColor
        }, completion: nil)
    }

    public func updateSensitivity(general: Int, redDot: Int, scope2x: Int, scope4x: Int, hz: Int) {
        statsLabel.text = "GEN:\(general)  RED:\(redDot)  2X:\(scope2x)  4X:\(scope4x)"
        hzBadge.text = "\(hz)Hz"
    }
}
