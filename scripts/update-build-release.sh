#!/usr/bin/env bash
# Futo Keyboard M — automatischer Update-Build-Release-Lauf
# Holt die neuesten FUTO-Änderungen, merged sie in den Custom-Branch,
# baut das Release-APK und veröffentlicht es als GitHub-Release.
#
# Verhalten (für no_agent-Cron):
#  - Erfolg: leerer stdout + exit 0 = still (Obtainium informiert den Nutzer)
#  - Fehler: Fehlermeldung auf stdout + exit 1 UND Repair-Agent wird gefeuert
#  - Fortschritt/Diagnose: stderr (landet im Job-Log)
set -euo pipefail

REPO_DIR="/mnt/wd_nvme/futo-keyboard-m"
OWNER="pikatchu2k3"
REPO="futo-keyboard-m"
GRADLE_TASK="assembleUnstableRelease"
REPAIR_JOB_ID_FILE="/home/mini/.hermes/futo-repair-job.id"

GITHUB_TOKEN="${GITHUB_TOKEN:-$(grep '^GITHUB_TOKEN=' ~/.hermes/.env | cut -d= -f2)}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export JAVA_HOME="${JAVA_HOME:-/opt/jdk-21.0.6+7}"
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx4096m"

log() { echo "$*" >&2; }

# Fehlerbehandlung: Fehlermeldung in Marker-Datei schreiben (Repair-Job liest sie 15 Min später)
fail() {
  echo "!!! $* !!!"
  echo "$(date '+%Y-%m-%d %H:%M:%S') — $*" > /home/mini/.hermes/futo-last-error.txt
  exit 1
}

cd "$REPO_DIR"

log "=== [1/5] Upstream holen ==="
if ! git fetch upstream master >/dev/null 2>&1; then
  log "Fetch fehlgeschlagen (Netzwerk/Repo?) — überspringe diese Woche (still)"
  exit 0
fi

if git merge-base --is-ancestor upstream/master HEAD 2>/dev/null; then
  log "Kein neues Update (upstream ist in HEAD enthalten) -> still"
  exit 0
fi

log "=== [2/5] Upstream mergen ==="
if ! git merge upstream/master --no-edit >&2; then
  fail "MERGE-KONFLIKT in $REPO_DIR — Reparatur-Agent übernimmt"
fi

log "=== [3/5] Build ($GRADLE_TASK) ==="
if ! ./gradlew "$GRADLE_TASK" 2>&1 | tail -25 >&2; then
  fail "BUILD FEHLGESCHLAGEN ($GRADLE_TASK) — Reparatur-Agent übernimmt"
fi
APK="$(ls -t build/outputs/apk/unstable/release/*.apk 2>/dev/null | head -1)"
[ -n "$APK" ] && [ -f "$APK" ] || fail "APK nicht gefunden"

log "=== [4/5] Push zum Fork ==="
if ! git push origin master >&2; then
  fail "PUSH FEHLGESCHLAGEN zum Fork — Reparatur-Agent übernimmt"
fi

log "=== [5/5] Release erstellen ==="
UPSTREAM_HASH=$(git log -1 --format=%h upstream/master)
UPSTREAM_DATE=$(git log -1 --format=%cs upstream/master)
TAG="v$(date +%Y.%m.%d)"
BODY="Automatischer Build von **FUTO Keyboard** (Upstream \`${UPSTREAM_HASH}\` vom ${UPSTREAM_DATE}) + B1-Patch: externe Spracherkennung über RECOGNIZE_SPEECH-Popup.

Installation: Obtainium → Add App → GitHub Releases → \`https://github.com/${OWNER}/${REPO}\`"
APK_SIZE=$(stat -c%s "$APK")
MD5=$(md5sum "$APK" | cut -d' ' -f1)

# Release anlegen (bei Tag-Kollision: Suffix anhängen)
release_json=""
code=""
for suffix in "" "-2" "-3" "-4" "-5"; do
  tag="${TAG}${suffix}"
  resp=$(curl -s -w '\n%{http_code}' -X POST \
    -H "Authorization: Bearer $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/json" \
    -d "{\"tag_name\":\"$tag\",\"name\":\"Futo Keyboard M $tag\",\"body\":$(python3 -c "import json,sys; print(json.dumps(sys.argv[1]))" "$BODY")}" \
    "https://api.github.com/repos/$OWNER/$REPO/releases")
  code=$(echo "$resp" | tail -1)
  release_json=$(echo "$resp" | head -n -1)
  if [ "$code" = "201" ]; then
    TAG="$tag"
    break
  fi
done
[ -n "$release_json" ] && [ "$code" = "201" ] || fail "Release-Erstellung fehlgeschlagen"

RELEASE_ID=$(echo "$release_json" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")

log "--- APK hochladen ---"
curl -s -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @"$APK" \
  "https://uploads.github.com/repos/$OWNER/$REPO/releases/$RELEASE_ID/assets?name=$(basename "$APK")" \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print('Asset:', d.get('name'), d.get('size'), 'Bytes')" >&2

log "Release fertig: https://github.com/$OWNER/$REPO/releases/tag/$TAG"
log "APK: $(basename "$APK") | ${APK_SIZE} Bytes | MD5 ${MD5}"
# Erfolg: Fehler-Marker entfernen (falls vorhanden) + stiller Erfolg (kein stdout -> keine Signal-Meldung)
rm -f /home/mini/.hermes/futo-last-error.txt
exit 0
