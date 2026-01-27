import Foundation
import UIKit

/// Simple haptics helper used across the app.
/// Works on iOS devices; safely no-ops in Simulator where haptics are not present.
enum Haptics {
    enum ImpactStyle {
        case light, medium, heavy, soft, rigid
    }

    /// Triggers an impact haptic.
    static func impact(_ style: ImpactStyle) {
        #if os(iOS)
        let generator: UIImpactFeedbackGenerator
        switch style {
        case .light: generator = UIImpactFeedbackGenerator(style: .light)
        case .medium: generator = UIImpactFeedbackGenerator(style: .medium)
        case .heavy: generator = UIImpactFeedbackGenerator(style: .heavy)
        case .soft: generator = UIImpactFeedbackGenerator(style: .soft)
        case .rigid: generator = UIImpactFeedbackGenerator(style: .rigid)
        }
        generator.prepare()
        generator.impactOccurred()
        #endif
    }

    /// Triggers a success notification haptic.
    static func success() {
        #if os(iOS)
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.success)
        #endif
    }

    /// Triggers a warning notification haptic.
    static func warning() {
        #if os(iOS)
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.warning)
        #endif
    }

    /// Triggers an error notification haptic.
    static func error() {
        #if os(iOS)
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.error)
        #endif
    }
}
