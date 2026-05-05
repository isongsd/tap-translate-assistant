import Foundation

enum ChineseTextConverter {
    private static let traditionalToSimplified = StringTransform(rawValue: "Traditional-Simplified")
    private static let simplifiedToTraditional = StringTransform(rawValue: "Simplified-Traditional")

    static func convert(_ text: String, from source: TranslationLanguage, to target: TranslationLanguage) -> String {
        guard source != target else { return text }

        if source == .traditionalChinese, target == .simplifiedChinese {
            return transform(text, using: traditionalToSimplified)
        }

        if source == .simplifiedChinese, target == .traditionalChinese {
            return transform(text, using: simplifiedToTraditional)
        }

        return text
    }

    private static func transform(_ text: String, using transform: StringTransform) -> String {
        let mutableText = NSMutableString(string: text)
        let fullRange = NSRange(location: 0, length: mutableText.length)
        let didTransform = mutableText.applyTransform(
            transform,
            reverse: false,
            range: fullRange,
            updatedRange: nil
        )
        return didTransform ? mutableText as String : text
    }
}
