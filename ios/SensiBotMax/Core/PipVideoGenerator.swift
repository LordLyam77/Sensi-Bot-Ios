import Foundation
import AVFoundation
import CoreGraphics
import UIKit

public enum PipVideoGenerator {
    public static func getOrCreatePipVideoURL() -> URL? {
        let fileManager = FileManager.default
        let caches = fileManager.urls(for: .cachesDirectory, in: .userDomainMask).first!
        let videoURL = caches.appendingPathComponent("pip_hud_stream.mp4")

        if fileManager.fileExists(atPath: videoURL.path) {
            return videoURL
        }

        // Synthesize a lightweight 30-frame 1-second video
        return createVideoFile(at: videoURL)
    }

    private static func createVideoFile(at url: URL) -> URL? {
        let width = 320
        let height = 180
        let fps: Int32 = 30
        let durationSeconds = 1

        guard let writer = try? AVAssetWriter(outputURL: url, fileType: .mp4) else {
            return nil
        }

        let settings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: width,
            AVVideoHeightKey: height
        ]

        let writerInput = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
        writerInput.expectsMediaDataInRealTime = false

        let sourcePixelBufferAttributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32ARGB),
            kCVPixelBufferWidthKey as String: width,
            kCVPixelBufferHeightKey as String: height
        ]

        let adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: writerInput,
            sourcePixelBufferAttributes: sourcePixelBufferAttributes
        )

        guard writer.canAdd(writerInput) else { return nil }
        writer.add(writerInput)

        guard writer.startWriting() else { return nil }
        writer.startSession(atSourceTime: .zero)

        let frameDuration = CMTime(value: 1, timescale: fps)

        for frameIndex in 0..<(fps * Int32(durationSeconds)) {
            while !writerInput.isReadyForMoreMediaData {
                Thread.sleep(forTimeInterval: 0.005)
            }

            if let buffer = newPixelBuffer(width: width, height: height, frame: Int(frameIndex)) {
                let frameTime = CMTimeMultiply(frameDuration, multiplier: frameIndex)
                adaptor.append(buffer, withPresentationTime: frameTime)
            }
        }

        writerInput.markAsFinished()

        let semaphore = DispatchSemaphore(value: 0)
        writer.finishWriting {
            semaphore.signal()
        }
        semaphore.wait()

        return url
    }

    private static func newPixelBuffer(width: Int, height: Int, frame: Int) -> CVPixelBuffer? {
        var pixelBuffer: CVPixelBuffer?
        let status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            width,
            height,
            kCVPixelFormatType_32ARGB,
            nil,
            &pixelBuffer
        )

        guard status == kCVReturnSuccess, let buffer = pixelBuffer else {
            return nil
        }

        CVPixelBufferLockBaseAddress(buffer, [])
        defer { CVPixelBufferUnlockBaseAddress(buffer, []) }

        let context = CGContext(
            data: CVPixelBufferGetBaseAddress(buffer),
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
        )

        // Background: Obsidian Void Black (#07070A)
        context?.setFillColor(red: 7.0/255.0, green: 7.0/255.0, blue: 10.0/255.0, alpha: 1.0)
        context?.fill(CGRect(x: 0, y: 0, width: width, height: height))

        // Draw Cyberpunk Red Tactical HUD Frame & Crosshair
        context?.setStrokeColor(red: 255.0/255.0, green: 42.0/255.0, blue: 77.0/255.0, alpha: 0.9)
        context?.setLineWidth(2.0)
        context?.stroke(CGRect(x: 10, y: 10, width: width - 20, height: height - 20))

        // Center crosshair dot
        let cx = CGFloat(width) / 2.0
        let cy = CGFloat(height) / 2.0
        context?.setFillColor(red: 0.0, green: 229.0/255.0, blue: 255.0/255.0, alpha: 1.0)
        context?.fillEllipse(in: CGRect(x: cx - 4, y: cy - 4, width: 8, height: 8))

        return buffer
    }
}
