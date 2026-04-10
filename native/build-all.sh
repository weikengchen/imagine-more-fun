#!/bin/bash
# Builds native webview-helper binaries for all platforms and copies them
# (along with source files for reviewability) into every mods/*/src/main/resources/native/
# directory found at the repo root, for JAR bundling.
#
# This script is run by CI and can also be run locally before
# `cd mods/{version} && ./gradlew build`.
#
# Prerequisites:
#   macOS: Xcode command line tools (swiftc)
#   Windows cross-compile: .NET 8+ SDK (brew install dotnet)

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Discover every mods/<version>/ directory under the repo root.
MOD_DIRS=()
for mod_dir in "$REPO_ROOT"/mods/*/; do
    [ -d "$mod_dir" ] || continue
    MOD_DIRS+=("$mod_dir")
done

if [ ${#MOD_DIRS[@]} -eq 0 ]; then
    echo "ERROR: No mods/<version>/ directories found under $REPO_ROOT/mods/"
    exit 1
fi

echo "Targeting mod directories:"
for mod_dir in "${MOD_DIRS[@]}"; do
    echo "  - $mod_dir"
done

# --- macOS ---
echo ""
echo "=== Building macOS webview-helper ==="
for mod_dir in "${MOD_DIRS[@]}"; do
    mkdir -p "$mod_dir/src/main/resources/native/macos"
    cp "$SCRIPT_DIR/macos/WebViewHelper.swift" "$mod_dir/src/main/resources/native/macos/WebViewHelper.swift"
done

if command -v swiftc &>/dev/null; then
    cd "$SCRIPT_DIR/macos"
    bash build.sh
    for mod_dir in "${MOD_DIRS[@]}"; do
        cp webview-helper "$mod_dir/src/main/resources/native/macos/webview-helper"
        echo "Copied macOS binary to $mod_dir ($(du -h "$mod_dir/src/main/resources/native/macos/webview-helper" | cut -f1))"
    done
else
    echo "WARNING: swiftc not found. Skipping macOS binary build."
    echo "Source file copied for compile-from-source fallback."
fi

# --- Windows ---
echo ""
echo "=== Building Windows webview-helper ==="
for mod_dir in "${MOD_DIRS[@]}"; do
    mkdir -p "$mod_dir/src/main/resources/native/windows"
    cp "$SCRIPT_DIR/windows/WebViewHelper.cs" "$mod_dir/src/main/resources/native/windows/WebViewHelper.cs"
    cp "$SCRIPT_DIR/windows/WebViewHelper.csproj" "$mod_dir/src/main/resources/native/windows/WebViewHelper.csproj"
done

if command -v dotnet &>/dev/null; then
    cd "$SCRIPT_DIR/windows"
    dotnet publish -c Release -r win-x64 --no-self-contained \
        -p:PublishSingleFile=true -o publish 2>&1 | grep -v "warning MSB3277"
    for mod_dir in "${MOD_DIRS[@]}"; do
        cp publish/webview-helper.exe "$mod_dir/src/main/resources/native/windows/webview-helper.exe"
        cp publish/runtimes/win-x64/native/WebView2Loader.dll "$mod_dir/src/main/resources/native/windows/WebView2Loader.dll"
        echo "Copied Windows binaries to $mod_dir ($(du -h "$mod_dir/src/main/resources/native/windows/webview-helper.exe" | cut -f1) + WebView2Loader.dll)"
    done
else
    echo "WARNING: .NET SDK not found. Skipping Windows binary build."
    echo "Source files copied for compile-from-source fallback."
fi

echo ""
echo "=== Done ==="
for mod_dir in "${MOD_DIRS[@]}"; do
    echo "--- $mod_dir ---"
    ls -lh "$mod_dir/src/main/resources/native/macos/" "$mod_dir/src/main/resources/native/windows/" 2>/dev/null || true
done
