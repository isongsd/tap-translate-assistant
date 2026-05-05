import json
import os
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


LANGUAGE_NAMES = {
    "zh-Hant": "繁體中文",
    "zh-Hans": "簡體中文",
    "en": "英文",
    "ja": "日文",
    "ko": "韓文",
    "th": "泰文",
    "vi": "越南文",
    "id": "印尼文",
}


def build_translate_prompt(payload):
    source = LANGUAGE_NAMES.get(payload.get("sourceLanguage"), payload.get("sourceLanguage", "來源語言"))
    target = LANGUAGE_NAMES.get(payload.get("targetLanguage"), payload.get("targetLanguage", "目標語言"))
    text = payload.get("text", "")
    return (
        f"請將下列文字從{source}翻譯成{target}。"
        "只保留翻譯結果，不要解釋，不要加引號，不要輸出 Markdown。\n\n"
        f"{text}"
    )


def call_openai_compatible(payload):
    api_key = os.environ.get("LLM_API_KEY", "").strip()
    base_url = os.environ.get("LLM_BASE_URL", "").strip().rstrip("/")
    model = os.environ.get("LLM_MODEL", "").strip()

    if not api_key or not base_url or not model:
        return {
            "text": str(payload.get("text", "")).strip(),
            "source": "fallback",
            "warning": "LLM env vars are not configured",
        }

    request_body = json.dumps(
        {
            "model": model,
            "messages": [
                {"role": "system", "content": "你是精準翻譯工具，只輸出翻譯結果。"},
                {"role": "user", "content": build_translate_prompt(payload)},
            ],
            "temperature": 0.2,
        },
        ensure_ascii=False,
    ).encode("utf-8")

    request = urllib.request.Request(
        f"{base_url}/chat/completions",
        data=request_body,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    with urllib.request.urlopen(request, timeout=20) as response:
        data = json.loads(response.read().decode("utf-8"))

    text = data["choices"][0]["message"]["content"].strip().strip('"')
    return {"text": text, "source": "llm"}


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/v1/translate":
            self.send_error(404)
            return

        length = int(self.headers.get("Content-Length", "0"))
        payload = json.loads(self.rfile.read(length).decode("utf-8"))

        try:
            result = call_openai_compatible(payload)
        except Exception as exc:
            result = {
                "text": str(payload.get("text", "")).strip(),
                "source": "fallback",
                "error": str(exc),
            }
        self.send_json(200, result)

    def send_json(self, status, payload):
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "8000"))
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print(f"Translation proxy listening on http://0.0.0.0:{port}/v1/translate")
    server.serve_forever()
