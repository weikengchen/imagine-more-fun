#!/bin/bash
# Build the macOS WebView helper binary.
# Requires Xcode command line tools.
#
# Usage: ./build.sh
# Output: webview-helper (universal binary, arm64 + x86_64)

set -euo pipefail
cd "$(dirname "$0")"

echo "Building webview-helper for macOS..."

# Build universal binary (Apple Silicon + Intel)
swiftc -O \
  -target arm64-apple-macosx11.0 \
  -o webview-helper-arm64 \
  WebViewHelper.swift \
  -framework WebKit \
  -framework AppKit

swiftc -O \
  -target x86_64-apple-macosx11.0 \
  -o webview-helper-x86_64 \
  WebViewHelper.swift \
  -framework WebKit \
  -framework AppKit

lipo -create -output webview-helper webview-helper-arm64 webview-helper-x86_64
rm webview-helper-arm64 webview-helper-x86_64

echo "Built: webview-helper (universal binary)"
echo ""
echo "To install, copy to your Minecraft directory:"
echo "  mkdir -p config/not-riding-alert/native"
echo "  cp webview-helper <minecraft-dir>/config/not-riding-alert/native/"
