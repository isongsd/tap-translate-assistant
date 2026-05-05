import Foundation

enum ChineseTextConverter {
    static func convert(_ text: String, from source: TranslationLanguage, to target: TranslationLanguage) -> String {
        guard source != target else { return text }

        if source == .traditionalChinese, target == .simplifiedChinese {
            return transform(text, using: kCFStringTransformTraditionalChineseToSimplified)
        }

        if source == .simplifiedChinese, target == .traditionalChinese {
            return transform(text, using: kCFStringTransformSimplifiedChineseToTraditional)
        }

        return text
    }

    private static func transform(_ text: String, using transform: CFString) -> String {
        let mutableText = NSMutableString(string: text)
        CFStringTransform(mutableText as CFMutableString, nil, transform, false)
        return mutableText as String
    }
}
