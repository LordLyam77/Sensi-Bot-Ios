import UIKit
import AVKit
import Combine

public final class PipOverlayManager: NSObject, ObservableObject {
    public static let shared = PipOverlayManager()

    @Published public var isPipActive: Bool = false
    @Published public var isPipSupported: Bool = false
    @Published public var isPipPossible: Bool = false
    @Published public var statusMessage: String = "Ready"

    private var pipController: AVPictureInPictureController?
    private var pipCallVC: AVPictureInPictureVideoCallViewController?
    private var pipObservation: NSKeyValueObservation?
    private weak var activeSourceView: UIView?

    public override init() {
        super.init()
        self.isPipSupported = AVPictureInPictureController.isPictureInPictureSupported()
        setupAudioSession()
    }

    public func setupAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .moviePlayback, options: [.mixWithOthers])
            try session.setActive(true)
        } catch {
            print("AudioSession setup error: \(error.localizedDescription)")
        }
    }

    public func attachSourceView(_ sourceView: UIView) {
        self.activeSourceView = sourceView

        guard AVPictureInPictureController.isPictureInPictureSupported() else {
            self.statusMessage = "PiP not supported on this device"
            return
        }

        if #available(iOS 15.0, *) {
            let callVC = AVPictureInPictureVideoCallViewController()
            callVC.preferredContentSize = CGSize(width: 280, height: 160)

            let floatingHud = FloatingHudView(frame: CGRect(x: 0, y: 0, width: 280, height: 160))
            callVC.view.addSubview(floatingHud)
            floatingHud.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                floatingHud.topAnchor.constraint(equalTo: callVC.view.topAnchor),
                floatingHud.bottomAnchor.constraint(equalTo: callVC.view.bottomAnchor),
                floatingHud.leadingAnchor.constraint(equalTo: callVC.view.leadingAnchor),
                floatingHud.trailingAnchor.constraint(equalTo: callVC.view.trailingAnchor)
            ])

            let contentSource = AVPictureInPictureController.ContentSource(
                activeVideoCallSourceView: sourceView,
                contentViewController: callVC
            )

            let controller = AVPictureInPictureController(contentSource: contentSource)
            controller.delegate = self
            controller.canStartPictureInPictureAutomaticallyFromInline = true
            self.pipController = controller
            self.pipCallVC = callVC

            self.pipObservation = controller.observe(\.isPictureInPicturePossible, options: [.initial, .new]) { [weak self] c, _ in
                DispatchQueue.main.async {
                    self?.isPipPossible = c.isPictureInPicturePossible
                    if c.isPictureInPicturePossible {
                        self?.statusMessage = "Ready to Launch"
                    }
                }
            }
        }
    }

    public func startPip() {
        setupAudioSession()
        guard let controller = pipController else {
            self.statusMessage = "Initializing HUD..."
            return
        }

        if controller.isPictureInPicturePossible {
            controller.startPictureInPicture()
        } else {
            // Retry after momentary delay if layout is still attaching
            self.statusMessage = "Starting Picture-in-Picture..."
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                controller.startPictureInPicture()
            }
        }
    }

    public func stopPip() {
        pipController?.stopPictureInPicture()
    }

    public func togglePip() {
        if isPipActive {
            stopPip()
        } else {
            startPip()
        }
    }
}

extension PipOverlayManager: AVPictureInPictureControllerDelegate {
    public func pictureInPictureControllerWillStartPictureInPicture(_ pictureInPictureController: AVPictureInPictureController) {
        DispatchQueue.main.async {
            self.isPipActive = true
            self.statusMessage = "HUD Floating Over Game"
        }
    }

    public func pictureInPictureControllerDidStopPictureInPicture(_ pictureInPictureController: AVPictureInPictureController) {
        DispatchQueue.main.async {
            self.isPipActive = false
            self.statusMessage = "Ready to Launch"
        }
    }

    public func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, failedToStartPictureInPictureWithError error: Error) {
        DispatchQueue.main.async {
            self.isPipActive = false
            self.statusMessage = "PiP Failed: \(error.localizedDescription)"
        }
        print("PiP failed: \(error.localizedDescription)")
    }
}
