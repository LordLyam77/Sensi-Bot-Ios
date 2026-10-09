import UIKit
import AVKit
import Combine

public final class PipOverlayManager: NSObject, ObservableObject {
    public static let shared = PipOverlayManager()

    @Published public var isPipActive: Bool = false
    @Published public var isPipSupported: Bool = false

    private var pipController: AVPictureInPictureController?
    private var playerLayer: AVPlayerLayer?
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

    private func setupLoopingPipLayer() {
        // Generate a minimal silent looping video asset or dynamic frame asset
        // for AVPictureInPictureController to hold the floating window over other apps
        guard let sampleURL = Bundle.main.url(forResource: "pip_dummy", withExtension: "mp4") else {
            return
        }

        let item = AVPlayerItem(url: sampleURL)
        let queuePlayer = AVQueuePlayer(playerItem: item)
        self.looper = AVPlayerLooper(player: queuePlayer, templateItem: item)
        self.player = queuePlayer

        let layer = AVPlayerLayer(player: queuePlayer)
        layer.frame = CGRect(x: 0, y: 0, width: 320, height: 180)
        self.playerLayer = layer

        if AVPictureInPictureController.isPictureInPictureSupported() {
            self.pipController = AVPictureInPictureController(playerLayer: layer)
            self.pipController?.delegate = self
        }
    }

    public func togglePip() {
        guard let controller = pipController else { return }
        if controller.isPictureInPictureActive {
            controller.stopPictureInPicture()
        } else {
            player?.play()
            controller.startPictureInPicture()
        }
    }
}

extension PipOverlayManager: AVPictureInPictureControllerDelegate {
    public func pictureInPictureControllerWillStartPictureInPicture(_ pictureInPictureController: AVPictureInPictureController) {
        DispatchQueue.main.async { self.isPipActive = true }
    }

    public func pictureInPictureControllerDidStopPictureInPicture(_ pictureInPictureController: AVPictureInPictureController) {
        DispatchQueue.main.async { self.isPipActive = false }
    }

    public func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, failedToStartPictureInPictureWithError error: Error) {
        DispatchQueue.main.async { self.isPipActive = false }
        print("PiP failed: \(error.localizedDescription)")
    }
}
