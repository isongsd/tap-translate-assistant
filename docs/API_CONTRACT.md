# 翻譯 API 契約

手機端不保存模型 API key。多國語翻譯應由後端保存金鑰並呼叫 LLM 或翻譯服務。

## `POST /v1/translate`

將文字翻譯成指定語言。

### Request

```json
{
  "text": "今天天氣很好",
  "sourceLanguage": "zh-Hant",
  "targetLanguage": "en"
}
```

### Response

```json
{
  "text": "The weather is nice today.",
  "source": "llm"
}
```

## 語言代碼

- `zh-Hant`：繁體中文
- `zh-Hans`：簡體中文
- `en`：英文
- `ja`：日文
- `ko`：韓文
- `th`：泰文
- `vi`：越南文
- `id`：印尼文

## 失敗處理

若後端無法翻譯，建議回傳 HTTP 200 並帶：

```json
{
  "text": "",
  "error": "原因"
}
```

手機端收到空白 `text` 會顯示翻譯失敗並保留原輸入文字。
