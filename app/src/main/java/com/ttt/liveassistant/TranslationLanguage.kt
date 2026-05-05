package com.ttt.liveassistant

enum class TranslationLanguage(val code: String, val displayName: String, val shortName: String) {
    TraditionalChinese("zh-Hant", "繁體中文", "繁"),
    SimplifiedChinese("zh-Hans", "簡體中文", "簡"),
    English("en", "英文", "英"),
    Japanese("ja", "日文", "日"),
    Korean("ko", "韓文", "韓"),
    Thai("th", "泰文", "泰"),
    Vietnamese("vi", "越南文", "越"),
    Indonesian("id", "印尼文", "印");

    companion object {
        fun fromCode(code: String): TranslationLanguage =
            entries.firstOrNull { it.code == code } ?: TraditionalChinese
    }
}
