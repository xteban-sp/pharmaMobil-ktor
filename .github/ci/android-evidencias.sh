#!/usr/bin/env bash
# Actividad autonoma 9: corre dentro de android-emulator-runner, con el emulador
# ya encendido. Instala la app, recorre los flujos de Maestro y deja las
# capturas en $EVID. Ningun fallo corta el script: se publica lo que haya.
set -u
export PATH="$PATH:$HOME/.maestro/bin"
export MAESTRO_CLI_NO_ANALYTICS=1
FLUJOS="$GITHUB_WORKSPACE/.github/ci/maestro"
APP_ID=pe.edu.upeu.pharmamobil
cd "$EVID"

adb shell getprop ro.build.version.release > android-version.txt
adb shell getprop ro.product.model >> android-version.txt
adb install -r "$APK" > adb-install.log 2>&1 || echo "::warning title=adb install::$(tail -3 adb-install.log | tr '\n' ' ')"
adb logcat -c || true

if maestro test -e APP_ID="$APP_ID" "$FLUJOS/compartir-android.yaml" > maestro-compartir.log 2>&1; then
  echo "::notice title=Maestro Android::listado, detalle y selector de compartir"
else
  echo "::warning title=Maestro Android (compartir)::$(tail -5 maestro-compartir.log | tr '\n' ' ' | cut -c1-800)"
  adb exec-out screencap -p > maestro-fallo-compartir.png || true
fi
adb shell input keyevent KEYCODE_BACK || true

if maestro test -e APP_ID="$APP_ID" "$FLUJOS/acerca-de-android.yaml" > maestro-acerca.log 2>&1; then
  echo "::notice title=Maestro Android::pantalla Acerca de"
else
  echo "::warning title=Maestro Android (acerca de)::$(tail -5 maestro-acerca.log | tr '\n' ' ' | cut -c1-800)"
  adb exec-out screencap -p > maestro-fallo-acerca.png || true
fi

adb exec-out screencap -p > 09-android-estado-final.png || true
adb logcat -d > logcat-completo.txt 2>/dev/null || true
grep -E "KtorHttp" logcat-completo.txt > android-ktor.log || true
exit 0
