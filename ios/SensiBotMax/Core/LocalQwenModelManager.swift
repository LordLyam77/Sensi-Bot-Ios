import Foundation
import Combine

public enum QwenModelStatus: Equatable {
    case notInstalled
    case downloading(progress: Double)
    case ready
    case loaded
    case error(String)

    public var displayLabel: String {
        switch self {
        case .notInstalled:
            return "Not Installed (~392MB)"
        case .downloading(let progress):
            return "Downloading \(Int(progress * 100))%"
        case .ready:
            return "Installed (On-Device Offline)"
        case .loaded:
            return "Loaded in Neural Engine"
        case .error(let msg):
            return "Error: \(msg)"
        }
    }
}

public final class LocalQwenModelManager: NSObject, ObservableObject {
    public static let shared = LocalQwenModelManager()

    @Published public var status: QwenModelStatus = .notInstalled
    @Published public var downloadProgress: Double = 0.0
    @Published public var downloadedBytesText: String = ""

    private var downloadTask: URLSessionDownloadTask?
    private var urlSession: URLSession?

    public override init() {
        super.init()
        let config = URLSessionConfiguration.default
        self.urlSession = URLSession(configuration: config, delegate: nil, delegateQueue: .main)
        refreshStatus()
    }

    public var modelFileURL: URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let modelsDir = docs.appendingPathComponent("models", isDirectory: true)
        if !FileManager.default.fileExists(atPath: modelsDir.path) {
            try? FileManager.default.createDirectory(at: modelsDir, withIntermediateDirectories: true)
        }
        return modelsDir.appendingPathComponent(QwenPromptConfig.modelFilename)
    }

    public var isModelInstalled: Bool {
        let path = modelFileURL.path
        if FileManager.default.fileExists(atPath: path) {
            if let attrs = try? FileManager.default.attributesOfItem(atPath: path),
               let size = attrs[.size] as? Int64 {
                return size > 50 * 1024 * 1024 // At least 50MB
            }
        }
        return false
    }

    public func refreshStatus() {
        if isModelInstalled {
            status = .ready
        } else {
            status = .notInstalled
        }
    }

    public func startDownload() {
        guard status != .ready else { return }

        status = .downloading(progress: 0.0)
        downloadProgress = 0.0
        downloadedBytesText = "Connecting..."

        let config = URLSessionConfiguration.default
        let session = URLSession(configuration: config, delegate: self, delegateQueue: OperationQueue.main)
        self.urlSession = session

        let task = session.downloadTask(with: QwenPromptConfig.modelDownloadUrl)
        self.downloadTask = task
        task.resume()
    }

    public func cancelDownload() {
        downloadTask?.cancel()
        downloadTask = nil
        refreshStatus()
    }

    public func deleteModel() {
        if FileManager.default.fileExists(atPath: modelFileURL.path) {
            try? FileManager.default.removeItem(at: modelFileURL)
        }
        refreshStatus()
    }

    public func getInstalledSizeMb() -> Int {
        if let attrs = try? FileManager.default.attributesOfItem(atPath: modelFileURL.path),
           let size = attrs[.size] as? Int64 {
            return Int(size / (1024 * 1024))
        }
        return 0
    }
}

extension LocalQwenModelManager: URLSessionDownloadDelegate {
    public func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didWriteData bytesWritten: Int64,
        totalBytesWritten: Int64,
        totalBytesExpectedToWrite: Int64
    ) {
        if totalBytesExpectedToWrite > 0 {
            let progress = Double(totalBytesWritten) / Double(totalBytesExpectedToWrite)
            let currentMb = Double(totalBytesWritten) / (1024.0 * 1024.0)
            let totalMb = Double(totalBytesExpectedToWrite) / (1024.0 * 1024.0)

            DispatchQueue.main.async {
                self.downloadProgress = progress
                self.downloadedBytesText = String(format: "%.1fMB / %.1fMB", currentMb, totalMb)
                self.status = .downloading(progress: progress)
            }
        }
    }

    public func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didFinishDownloadingTo location: URL
    ) {
        let dest = modelFileURL
        do {
            if FileManager.default.fileExists(atPath: dest.path) {
                try FileManager.default.removeItem(at: dest)
            }
            try FileManager.default.moveItem(at: location, to: dest)
            DispatchQueue.main.async {
                self.status = .ready
                self.downloadProgress = 1.0
                self.downloadedBytesText = "Installed successfully!"
            }
        } catch {
            DispatchQueue.main.async {
                self.status = .error("Failed to save weights: \(error.localizedDescription)")
            }
        }
    }

    public func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didCompleteWithError error: Error?
    ) {
        if let error = error {
            DispatchQueue.main.async {
                if (error as NSError).code != NSURLErrorCancelled {
                    self.status = .error(error.localizedDescription)
                } else {
                    self.refreshStatus()
                }
            }
        }
    }
}
