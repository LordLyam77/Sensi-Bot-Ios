import UIKit
import AVKit
import AVFoundation
import Combine

public final class PipOverlayManager: NSObject, ObservableObject {
    public static let shared = PipOverlayManager()

    @Published public var isPipActive: Bool = false
    @Published public var isPipSupported: Bool = false
    @Published public var isPipPossible: Bool = false
    @Published public var statusMessage: String = "Ready - Floats Over Games"

    private var pipController: AVPictureInPictureController?
    private var pipCallVC: AVPictureInPictureVideoCallViewController?
    private var pipObservation: NSKeyValueObservation?
    private weak var activeSourceView: UIView?
    private weak var activeHud: FloatingHudView?
    private var audioPlayer: AVAudioPlayer?

    public override init() {
        super.init()
        self.isPipSupported = AVPictureInPictureController.isPictureInPictureSupported()
        initSilentAudioPlayer()
        setupAudioSession()
    }

    private func initSilentAudioPlayer() {
        let silentData = createSilentAudioData()
        do {
            let player = try AVAudioPlayer(data: silentData)
            player.numberOfLoops = -1 // loop infinitely
            player.volume = 0.0 // 100% silent
            player.prepareToPlay()
            self.audioPlayer = player
        } catch {
            print("AudioPlayer error: \(error.localizedDescription)")
        }
    }

    private func createSilentAudioData() -> Data {
        let sampleRate: Int32 = 44100
        let channels: Int16 = 1
        let bitsPerSample: Int16 = 16
        let durationSeconds: Double = 1.0
        let totalSamples = Int(Double(sampleRate) * durationSeconds)
        let dataSize = Int32(totalSamples * Int(channels) * Int(bitsPerSample / 8))
        let fileSize = 36 + dataSize

        var data = Data()
        data.append(contentsOf: [0x52, 0x49, 0x46, 0x46]) // "RIFF"
        var chunkSize = fileSize.littleEndian
        data.append(Data(bytes: &chunkSize, count: 4))
        data.append(contentsOf: [0x57, 0x41, 0x56, 0x45]) // "WAVE"

        data.append(contentsOf: [0x66, 0x6D, 0x74, 0x20]) // "fmt "
        var subchunk1Size: Int32 = 16.littleEndian
        data.append(Data(bytes: &subchunk1Size, count: 4))
        var audioFormat: Int16 = 1.littleEndian
        data.append(Data(bytes: &audioFormat, count: 2))
        var numChannels = channels.littleEndian
        data.append(Data(bytes: &numChannels, count: 2))
        var sr = sampleRate.littleEndian
        data.append(Data(bytes: &sr, count: 4))
        var byteRate = (sampleRate * Int32(channels * (bitsPerSample / 8))).littleEndian
        data.append(Data(bytes: &byteRate, count: 4))
        var blockAlign = (channels * (bitsPerSample / 8)).littleEndian
        data.append(Data(bytes: &blockAlign, count: 2))
        var bps = bitsPerSample.littleEndian
        data.append(Data(bytes: &bps, count: 2))

        data.append(contentsOf: [0x64, 0x61, 0x74, 0x61]) // "data"
        var subchunk2Size = dataSize.littleEndian
        data.append(Data(bytes: &subchunk2Size, count: 4))
        data.append(Data(count: Int(dataSize)))
        return data
    }

    public func setupAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .moviePlayback, options: [.mixWithOthers])
            try session.setActive(true)
            audioPlayer?.play()
        } catch {
            print("AudioSession setup error: \(error.localizedDescription)")
        }
    }

    public func attachSourceView(_ sourceView: UIView) {
        self.activeSourceView = sourceView

        guard AVPictureInPictureController.isPictureInPictureSupported() else {
            self.statusMessage = "In-game floating not supported"
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
            self.activeHud = floatingHud

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
                        self?.statusMessage = "Ready - Floats When Outside App"
                    }
                }
            }
        }
    }

    public func updateHudMetrics(general: Int, redDot: Int, scope2x: Int, scope4x: Int, hz: Int) {
        activeHud?.updateSensitivity(general: general, redDot: redDot, scope2x: scope2x, scope4x: scope4x, hz: hz)
    }

    public func updateHudZone(name: String, color: UIColor = UIColor(red: 0.0, green: 0.9, blue: 0.46, alpha: 1.0)) {
        activeHud?.updateZone(name: name, color: color)
    }

    public func startPip() {
        setupAudioSession()
        guard let controller = pipController else {
            self.statusMessage = "Floating Assistant Initializing..."
            return
        }

        if controller.isPictureInPicturePossible {
            controller.startPictureInPicture()
        } else {
            self.statusMessage = "Activating In-Game Overlay..."
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
                self?.pipController?.startPictureInPicture()
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
            self.statusMessage = "Floating Assistant Active Over Game"
        }
    }

    public func pictureInPictureControllerDidStopPictureInPicture(_ pictureInPictureController: AVPictureInPictureController) {
        DispatchQueue.main.async {
            self.isPipActive = false
            self.statusMessage = "Ready - Floats When Outside App"
        }
    }

    public func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, failedToStartPictureInPictureWithError error: Error) {
        DispatchQueue.main.async {
            self.isPipActive = false
            self.statusMessage = "Overlay Notice: \(error.localizedDescription)"
        }
    }
}
