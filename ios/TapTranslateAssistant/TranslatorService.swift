import Foundation

struct TranslatorService {
    struct TranslationRequest: Encodable {
        let text: String
        let sourceLanguage: String
        let targetLanguage: String
    }

    struct TranslationResponse: Decodable {
        let text: String?
        let source: String?
        let error: String?
    }

    func translate(
        text: String,
        source: TranslationLanguage,
        target: TranslationLanguage,
        backendURL: String
    ) async throws -> String {
        if source == target {
            return text
        }

        if isChinesePair(source: source, target: target) {
            return ChineseTextConverter.convert(text, from: source, to: target)
        }

        guard let url = URL(string: backendURL), !backendURL.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw TranslationError.missingBackend
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(
            TranslationRequest(
                text: text,
                sourceLanguage: source.rawValue,
                targetLanguage: target.rawValue
            )
        )

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, 200..<300 ~= httpResponse.statusCode else {
            throw TranslationError.requestFailed
        }

        let decoded = try JSONDecoder().decode(TranslationResponse.self, from: data)
        if let translatedText = decoded.text?.trimmingCharacters(in: .whitespacesAndNewlines), !translatedText.isEmpty {
            return translatedText
        }

        throw TranslationError.emptyResult(decoded.error)
    }

    private func isChinesePair(source: TranslationLanguage, target: TranslationLanguage) -> Bool {
        (source == .traditionalChinese && target == .simplifiedChinese) ||
            (source == .simplifiedChinese && target == .traditionalChinese)
    }
}

enum TranslationError: LocalizedError {
    case missingBackend
    case requestFailed
    case emptyResult(String?)

    var errorDescription: String? {
        switch self {
        case .missingBackend:
            return "這組語言需要先設定翻譯後端。"
        case .requestFailed:
            return "翻譯後端暫時無法使用。"
        case .emptyResult(let message):
            return message ?? "翻譯結果為空。"
        }
    }
}
