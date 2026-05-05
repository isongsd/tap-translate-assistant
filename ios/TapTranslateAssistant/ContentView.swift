import SwiftUI
import UIKit

struct ContentView: View {
    @AppStorage("sourceLanguage") private var sourceLanguageRaw = TranslationLanguage.traditionalChinese.rawValue
    @AppStorage("targetLanguage") private var targetLanguageRaw = TranslationLanguage.simplifiedChinese.rawValue
    @AppStorage("translationSuffix") private var translationSuffix = ""
    @AppStorage("translationBackendURL") private var translationBackendURL = ""

    @State private var inputText = ""
    @State private var translatedText = ""
    @State private var statusText = ""
    @State private var isTranslating = false
    @FocusState private var inputFocused: Bool

    private let translator = TranslatorService()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    languageDirectionSection
                    inputSection
                    resultSection
                    settingsSection
                    bannerSection
                }
                .padding(20)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("點按翻譯助手")
            .toolbar {
                ToolbarItem(placement: .keyboard) {
                    Button("完成") {
                        inputFocused = false
                    }
                }
            }
        }
    }

    private var languageDirectionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle("翻譯方向")

            HStack(spacing: 12) {
                languagePicker("來源", selection: sourceLanguageBinding)
                Image(systemName: "arrow.right")
                    .font(.headline)
                    .foregroundStyle(.secondary)
                languagePicker("目標", selection: targetLanguageBinding)
            }

            Text("\(sourceLanguage.shortTitle) → \(targetLanguage.shortTitle)")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.teal)
        }
        .panelStyle()
    }

    private var inputSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle("輸入文字")

            TextEditor(text: $inputText)
                .focused($inputFocused)
                .frame(minHeight: 132)
                .padding(10)
                .background(Color(.secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(alignment: .topLeading) {
                    if inputText.isEmpty {
                        Text("輸入要翻譯的文字")
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 18)
                            .allowsHitTesting(false)
                    }
                }

            Button {
                translateAndCopy()
            } label: {
                HStack {
                    if isTranslating {
                        ProgressView()
                    }
                    Text(isTranslating ? "翻譯中" : "翻譯並複製")
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(isTranslating || inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

            if !statusText.isEmpty {
                Text(statusText)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .panelStyle()
    }

    private var resultSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle("最近結果")

            Text(translatedText.isEmpty ? "尚未翻譯" : translatedText)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(Color(.secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .textSelection(.enabled)
        }
        .panelStyle()
    }

    private var settingsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionTitle("設定")

            TextField("翻譯後尾碼，例如  (13", text: $translationSuffix)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)

            TextField("翻譯後端 URL", text: $translationBackendURL)
                .keyboardType(.URL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)
        }
        .panelStyle()
    }

    private var bannerSection: some View {
        GeometryReader { proxy in
            let width = max(proxy.size.width, 320)
            BannerAdView(width: width)
                .frame(width: width, height: 60)
                .frame(maxWidth: .infinity)
        }
        .frame(height: 60)
        .padding(.vertical, 4)
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title)
            .font(.headline)
    }

    private func languagePicker(
        _ title: String,
        selection: Binding<TranslationLanguage>
    ) -> some View {
        Picker(title, selection: selection) {
            ForEach(TranslationLanguage.allCases) { language in
                Text(language.title).tag(language)
            }
        }
        .pickerStyle(.menu)
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private var sourceLanguage: TranslationLanguage {
        TranslationLanguage(rawValue: sourceLanguageRaw) ?? .traditionalChinese
    }

    private var targetLanguage: TranslationLanguage {
        TranslationLanguage(rawValue: targetLanguageRaw) ?? .simplifiedChinese
    }

    private var sourceLanguageBinding: Binding<TranslationLanguage> {
        Binding(
            get: { sourceLanguage },
            set: { sourceLanguageRaw = $0.rawValue }
        )
    }

    private var targetLanguageBinding: Binding<TranslationLanguage> {
        Binding(
            get: { targetLanguage },
            set: { targetLanguageRaw = $0.rawValue }
        )
    }

    private func translateAndCopy() {
        let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }

        isTranslating = true
        statusText = ""
        inputFocused = false

        Task {
            do {
                let translated = try await translator.translate(
                    text: text,
                    source: sourceLanguage,
                    target: targetLanguage,
                    backendURL: translationBackendURL
                )
                let finalText = translated + translationSuffix

                await MainActor.run {
                    UIPasteboard.general.string = finalText
                    translatedText = finalText
                    inputText = ""
                    statusText = "已複製到剪貼簿。"
                    isTranslating = false
                }
            } catch {
                await MainActor.run {
                    statusText = error.localizedDescription
                    isTranslating = false
                }
            }
        }
    }
}

private extension View {
    func panelStyle() -> some View {
        padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

#Preview {
    ContentView()
}
