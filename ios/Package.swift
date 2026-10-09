// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SensiBotMax",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "SensiBotMax",
            targets: ["SensiBotMax"]
        ),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "SensiBotMax",
            dependencies: [],
            path: "SensiBotMax"
        )
    ]
)
