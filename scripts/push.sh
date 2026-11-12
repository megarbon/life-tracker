#!/usr/bin/env bash
# Compila la APK debug, la instala en el móvil conectado por adb y abre la app.
# Uso: scripts/push.sh [--logs]   (--logs deja el logcat de la app abierto al terminar)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"
ADB="${ADB:-/c/Users/MELAKUGARCIABONACHO/AppData/Local/Android/Sdk/platform-tools/adb.exe}"
APP_ID="com.example.lifet"

# Primer dispositivo en estado "device" (ignora emuladores offline).
SERIAL="$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
if [ -z "$SERIAL" ]; then
  echo "No hay ningún dispositivo conectado. Activa la depuración inalámbrica y ejecuta 'adb pair/connect'." >&2
  exit 1
fi
echo "Dispositivo: $SERIAL"

./gradlew :app:assembleDebug -q
APK="app/build/outputs/apk/debug/app-debug.apk"

"$ADB" -s "$SERIAL" install -r "$APK" | tail -1
"$ADB" -s "$SERIAL" shell am force-stop "$APP_ID"
"$ADB" -s "$SERIAL" logcat -c
"$ADB" -s "$SERIAL" shell am start -n "$APP_ID/.MainActivity" >/dev/null
sleep 2

PID="$("$ADB" -s "$SERIAL" shell pidof "$APP_ID" || true)"
if [ -z "$PID" ]; then
  echo "La app no está en ejecución: probable crash." >&2
  "$ADB" -s "$SERIAL" logcat -d -s AndroidRuntime:E | tail -20 >&2
  exit 1
fi
echo "App en marcha (pid $PID)."

if [ "${1:-}" = "--logs" ]; then
  "$ADB" -s "$SERIAL" logcat --pid="$PID"
fi
