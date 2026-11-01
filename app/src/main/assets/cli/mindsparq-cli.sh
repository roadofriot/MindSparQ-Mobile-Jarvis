#!/usr/bin/env bash
# MindSparQ CLI — headless multi-API launcher
# Runs an AI prompt through the configured provider without the full UI.
# Usage: mindsparq-cli "your prompt here"
#        mindsparq-cli --provider openrouter "your prompt"
#        mindsparq-cli --model qwen/qwen-2.5-coder-32b-instruct:free "your prompt"

set -euo pipefail

PROMPT="${1:-}"
PROVIDER="${PROVIDER:-}"
MODEL="${MODEL:-}"

# Shift if provider/model flags are given
while [[ $# -gt 0 ]]; do
    case "$1" in
        --provider)
            PROVIDER="$2"
            shift 2
            ;;
        --model)
            MODEL="$2"
            shift 2
            ;;
        *)
            PROMPT="$1"
            shift
            ;;
    esac
done

if [[ -z "$PROMPT" ]]; then
    echo "Usage: mindsparq-cli [--provider PROVIDER] [--model MODEL] \"prompt\""
    echo ""
    echo "Supported providers: gemini, openrouter, ollama, nvidia, deepseek, opencode-zen, custom"
    echo "Default: uses the provider configured in MindSparQ settings."
    exit 1
fi

# Resolve provider from app preferences if not specified
APP_PREFS="/data/data/com.jarves.mh/shared_prefs/pocket_preferences.xml"
if [[ -z "$PROVIDER" && -f "$APP_PREFS" ]]; then
    PROVIDER=$(grep -o 'provider_kind"[^>]*>[^<]*' "$APP_PREFS" | sed 's/.*>//' | head -1 || echo "openrouter")
fi
PROVIDER="${PROVIDER:-openrouter}"

# Default models per provider
case "$PROVIDER" in
    gemini) MODEL="${MODEL:-gemini-2.5-flash}" ;;
    openrouter) MODEL="${MODEL:-qwen/qwen-2.5-coder-32b-instruct:free}" ;;
    ollama) MODEL="${MODEL:-qwen2.5-coder:1.5b}" ;;
    nvidia) MODEL="${MODEL:-meta/llama-3.3-70b-instruct}" ;;
    deepseek) MODEL="${MODEL:-deepseek-chat}" ;;
    opencode-zen) MODEL="${MODEL:-deepseek-v4-flash}" ;;
    *) MODEL="${MODEL:-qwen/qwen-2.5-coder-32b-instruct:free}" ;;
esac

echo "MindSparQ CLI — provider: $PROVIDER, model: $MODEL"
echo "Prompt: $PROMPT"
echo "---"

# Check if OpenCode CLI is available (preferred for multi-model)
if command -v opencode &>/dev/null; then
    export OPENCODE_PROVIDER="$PROVIDER"
    export OPENCODE_MODEL="$MODEL"
    opencode run "$PROMPT" 2>&1
    exit $?
fi

# Fallback: try DeepSeek Harness
if command -v dsh &>/dev/null; then
    dsh --provider "$PROVIDER" --model "$MODEL" "$PROMPT" 2>&1
    exit $?
fi

# Fallback: try Claude Code
if command -v claude &>/dev/null; then
    claude --model "$MODEL" -p "$PROMPT" 2>&1
    exit $?
fi

echo "Error: No CLI agent found. Install OpenCode, DeepSeek Harness, or Claude Code from MindSparQ settings."
exit 1
