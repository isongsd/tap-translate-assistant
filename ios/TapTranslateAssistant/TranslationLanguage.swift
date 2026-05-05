import Foundation

enum TranslationLanguage: String, CaseIterable, Identifiable {
    case traditionalChinese = "zh-Hant"
    case simplifiedChinese = "zh-Hans"
    case english = "en"
    case japanese = "ja"
    case korean = "ko"
    case thai = "th"
    case vietnamese = "vi"
    case indonesian = "id"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .traditionalChinese:
            return "繁體中文"
        case .simplifiedChinese:
            return "簡體中文"
        case .english:
            return "英文"
        case .japanese:
            return "日文"
        case .korean:
            return "韓文"
        case .thai:
            return "泰文"
        case .vietnamese:
            return "越南文"
        case .indonesian:
            return "印尼文"
        }
    }

    var shortTitle: String {
        switch self {
        case .traditionalChinese:
            return "繁"
        case .simplifiedChinese:
            return "簡"
        case .english:
            return "英"
        case .japanese:
            return "日"
        case .korean:
            return "韓"
        case .thai:
            return "泰"
        case .vietnamese:
            return "越"
        case .indonesian:
            return "印"
        }
    }
}
