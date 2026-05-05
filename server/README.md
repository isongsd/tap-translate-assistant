# 翻譯後端範例

`llm_proxy.py` 是無第三方依賴的 OpenAI-compatible 翻譯代理。它提供 `/v1/translate`。

手機端主畫面「翻譯後端」填入：

```text
https://你的後端/v1/translate
```

本機測試可填：

```text
http://電腦IP:8000/v1/translate
```

環境變數：

```powershell
$env:LLM_API_KEY="你的 key"
$env:LLM_BASE_URL="https://api.openai.com/v1"
$env:LLM_MODEL="你的模型名稱"
python server\llm_proxy.py
```

不要把 API key 寫進 Android App。請只放在雲端後端的環境變數。
