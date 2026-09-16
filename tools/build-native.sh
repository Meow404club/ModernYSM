#!/usr/bin/env bash
# tools/build-native.sh — rebuild the ysm-core native libs and backfill them
# into src/main/resources/natives/.
#
# Deliberately a standalone script (zero new gradle mechanics): the native lib
# lives in the native/openysm-cpp submodule and is built with zig; gradle never
# compiles it. Run from the repo root (or anywhere; paths are resolved).
#
# Usage:
#   tools/build-native.sh                 # build all platforms + backfill
#   tools/build-native.sh linux-x64       # build one platform, no backfill
#   BACKFILL=0 tools/build-native.sh      # build all, no backfill
#
# Toolchain policy (no system installs):
#   - zig: found on PATH, else tmp/zig/zig, else a tarball is downloaded into
#     tmp/zig/ (MIN_ZIG_VERSION below).
#   - Android NDK (android-arm64 only): $ANDROID_NDK_ROOT, else the newest
#     ~/Android/Sdk/ndk/<version>. Missing NDK -> android-arm64 is skipped and
#     reported as failed (never faked with stale artifacts).
#
# Acceptance wiring (native-dll-round1): after backfilling linux-x64, run
#   native/openysm-cpp/harness/run.sh <built .so>
# differential drift vs the previously shipped libysm-core.so must stay
# byte-identical (10/10) and the semantic cases must pass (7/7).
set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
SUB="$REPO/native/openysm-cpp"
OUT="$SUB/zig-out"
RES="$REPO/src/main/resources/natives"
MIN_ZIG_VERSION="0.14.0"

PLATFORMS=(linux-x64 windows-x64 windows-x86 android-arm64 macos-x64 macos-arm64)
# NOTE: macos dylibs ship in resources too; rebuilding them keeps the whole
# family on one source revision. They are untestable here (no darwin host) —
# final on-device verification for windows/macos belongs to the user.

log() { printf '[build-native] %s\n' "$*"; }
fail() { printf '[build-native] FAIL %s\n' "$*" >&2; }

find_zig() {
  if command -v zig >/dev/null 2>&1; then
    ZIG="$(command -v zig)"
  elif [ -x "$REPO/tmp/zig/zig" ]; then
    ZIG="$REPO/tmp/zig/zig"
  else
    log "zig not found; downloading tarball into tmp/zig/ (no system install)"
    case "$(uname -m)-$(uname -s)" in
      x86_64-Linux) ZC="zig-linux-x86_64"; ZT="tar.xz" ;;
      aarch64-Linux) ZC="zig-linux-aarch64"; ZT="tar.xz" ;;
      x86_64-Darwin) ZC="zig-macos-x86_64"; ZT="tar.xz" ;;
      arm64-Darwin) ZC="zig-macos-aarch64"; ZT="tar.xz" ;;
      *) fail "unsupported host for zig download: $(uname -m)-$(uname -s)"; return 1 ;;
    esac
    mkdir -p "$REPO/tmp/zig"
    url="https://ziglang.org/download/${MIN_ZIG_VERSION}/${ZC}-${MIN_ZIG_VERSION}.${ZT}"
    curl -fsSL "$url" -o "$REPO/tmp/zig/zig.tar.xz" || { fail "download $url"; return 1; }
    tar -xJf "$REPO/tmp/zig/zig.tar.xz" -C "$REPO/tmp/zig"
    mv "$REPO/tmp/zig/${ZC}-${MIN_ZIG_VERSION}"/* "$REPO/tmp/zig/" || true
    ZIG="$REPO/tmp/zig/zig"
  fi
  log "zig = $($ZIG version) ($ZIG)"
}

find_ndk() {
  if [ -n "${ANDROID_NDK_ROOT:-}" ] && [ -d "$ANDROID_NDK_ROOT" ]; then
    NDK="$ANDROID_NDK_ROOT"
  else
    local newest
    newest="$(ls -1d "$HOME"/Android/Sdk/ndk/* 2>/dev/null | sort -V | tail -1 || true)"
    if [ -n "$newest" ]; then NDK="$newest"; else NDK=""; fi
  fi
  if [ -n "$NDK" ]; then
    export ANDROID_NDK_ROOT="$NDK"
    log "android ndk = $NDK"
  else
    log "android ndk NOT found (android-arm64 will be skipped)"
  fi
}

build_platform() {
  local p="$1"
  log "building $p ..."
  if [ "$p" = "android-arm64" ] && [ -z "${NDK:-}" ]; then
    fail "$p (no NDK)"; return 1
  fi
  # zig build must run from the submodule (build.zig lives there)
  if ! (cd "$SUB" && "$ZIG" build -Doptimize=ReleaseFast -Dplatform="$p"); then
    fail "$p (zig build)"; return 1
  fi
  case "$p" in
    windows-*) [ -f "$OUT/$p/ysm-core.dll" ] || { fail "$p (artifact missing)"; return 1; } ;;
    macos-*)   [ -f "$OUT/$p/libysm-core.dylib" ] || { fail "$p (artifact missing)"; return 1; } ;;
    *)         [ -f "$OUT/$p/libysm-core.so" ] || { fail "$p (artifact missing)"; return 1; } ;;
  esac
  log "ok $p"
}

backfill() {
  # ONLY platforms built successfully in this invocation are copied; a stale
  # zig-out from an older source revision must never masquerade as fresh.
  local p f ok=0
  for p in "$@"; do
    case "$p" in
      windows-*) f="ysm-core.dll" ;;
      macos-*)   f="libysm-core.dylib" ;;
      *)         f="libysm-core.so" ;;
    esac
    mkdir -p "$RES/$p"
    cp -f "$OUT/$p/$f" "$RES/$p/$f"
    log "backfilled $p/$f  sha256=$(sha256sum "$RES/$p/$f" | cut -d' ' -f1)"
    ok=$((ok+1))
  done
  log "backfilled $ok/$# platforms (this run)"
}

main() {
  [ -d "$SUB" ] || { fail "submodule $SUB missing (git submodule update --init)"; exit 1; }
  find_zig || exit 1
  find_ndk

  local targets=("$@")
  [ ${#targets[@]} -eq 0 ] && targets=("${PLATFORMS[@]}")

  local failed=() built=() p
  for p in "${targets[@]}"; do
    if build_platform "$p"; then
      built+=("$p")
    else
      failed+=("$p")
    fi
  done

  if [ ${#failed[@]} -gt 0 ]; then
    fail "platforms failed: ${failed[*]} (stale artifacts NOT replaced)"
  fi

  if [ ${#built[@]} -gt 0 ] && [ "${BACKFILL:-1}" = "1" ]; then
    backfill "${built[@]}"
  fi

  [ ${#failed[@]} -eq 0 ] || exit 1
  log "done"
}

main "$@"
