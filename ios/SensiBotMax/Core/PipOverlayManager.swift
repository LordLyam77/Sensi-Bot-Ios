import UIKit
import AVKit
import Combine

public final class PipOverlayManager: NSObject, ObservableObject {
    public static let shared = PipOverlayManager()

    @Published public var isPipActive: Bool = false
    @Published public var isPipSupported: Bool = false
    @Published public var statusMessage: String = "Ready"

    public var playerLayer: AVPlayerLayer?
    private var pipController: AVPictureInPictureController?
    private var player: AVQueuePlayer?
    private var looper: AVPlayerLooper?

    public override init() {
        super.init()
        self.isPipSupported = AVPictureInPictureController.isPictureInPictureSupported()
        setupAudioSession()
        setupLoopingPipLayer()
    }

    private func setupAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .moviePlayback, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("AudioSession setup error: \(error.localizedDescription)")
        }
    }

    public func setupLoopingPipLayer() {
        guard let videoURL = PipVideoGenerator.getOrCreatePipVideoURL() else {
            self.statusMessage = "Video asset generation failed"
            return
        }

        let item = AVPlayerItem(url: videoURL)
        let queuePlayer = AVQueuePlayer(playerItem: item)
        queuePlayer.isMuted = true
        self.looper = AVPlayerLooper(player: queuePlayer, templateItem: item)
        self.player = queuePlayer

        let layer = AVPlayerLayer(player: queuePlayer)
        layer.videoGravity = .resizeAspectFill
        layer.frame = CGRect(x: 0, y: 0, width: 320, height: 180)
        self.playerLayer = layer

        if AVPictureInPictureController.isPictureInPictureSupported() {
            self.pipController = AVPictureInPictureController(playerLayer: layer)
            self.pipController?.delegate = self
            if #available(iOS 14.2, *) {
                self.pipController?.canStartPictureInPictureAutomaticallyFromInline = true
            }
            queuePlayer.play()
        }
    }

    public func startPip() {
        guard let controller = pipController else {
            self.statusMessage = "PiP Controller not ready"
            return
        }
        player?.play()
        controller.startPictureInPicture()
    }

    public func stopPip() {
        guard let controller = pipController else { return }
        controller.stopPictureInPicture()
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
            self.statusMessage = "HUD Closed"
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
