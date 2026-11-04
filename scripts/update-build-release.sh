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
ERROR_MARKER="/home/mini/.hermes/futo-last-error.txt"
AAPT2="$(ls /opt/android-sdk/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1 || true)"

export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export JAVA_HOME="${JAVA_HOME:-/opt/jdk-21.0.6+7}"
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx4096m"

log() { echo "$*" >&2; }

# Fehlerbehandlung: Fehlermeldung in Marker-Datei schreiben (Repair-Job liest sie 15 Min später).
# Haengt das Repo nach einem MERGE-KONFLIKT fest, wird der Merge zusaetzlich abgebrochen und die
# Konfliktdateien landen in der Marker-Datei — sonst scheitert jeder weitere Lauf sofort wieder
# am offenen Merge und der Repair-Job findet keinen sauberen Ausgangszustand vor.
fail() {
  echo "!!! $* !!!"
  if [ -f "$REPO_DIR/.git/MERGE_HEAD" ]; then
    CONFLICTS="$(git -C "$REPO_DIR" diff --name-only --diff-filter=U | tr '\n' ' ')"
    {
      echo "$(date '+%Y-%m-%d %H:%M:%S') — $*"
      echo "Merge-Konflikt in: ${CONFLICTS:-<unbekannt>}"
      echo "Der Merge wurde abgebrochen (Repo wieder auf dem letzten Commit)."
    } > "$ERROR_MARKER"
    git -C "$REPO_DIR" merge --abort >/dev/null 2>&1 || true
  else
    echo "$(date '+%Y-%m-%d %H:%M:%S') — $*" > "$ERROR_MARKER"
  fi
  exit 1
}

cd "$REPO_DIR"

# Token aus der Umgebung, sonst aus ~/.hermes/.env. Bewusst NICHT als Zuweisung mit
# fehlschlagendem grep: unter "set -euo pipefail" beendet das Skript sonst VOR fail(),
# also ohne Marker-Datei — der Repair-Job wuerde den Fehler nicht sehen.
if [ -z "${GITHUB_TOKEN:-}" ]; then
  GITHUB_TOKEN="$(grep '^GITHUB_TOKEN=' "$HOME/.hermes/.env" 2>/dev/null | cut -d= -f2- || true)"
fi
[ -n "${GITHUB_TOKEN:-}" ] || fail "GITHUB_TOKEN fehlt (weder in der Umgebung noch in $HOME/.hermes/.env)"
export GITHUB_TOKEN

log "=== [1/5] Upstream holen ==="
if ! git fetch upstream master >/dev/null 2>&1; then
  log "Fetch fehlgeschlagen (Netzwerk/Repo?) — überspringe diese Woche (still)"
  exit 0
fi

if git merge-base --is-ancestor upstream/master HEAD 2>/dev/null; then
  if [ "${FORCE:-0}" = "1" ]; then
    log "Kein neues Upstream-Commit, aber FORCE=1 -> baue und veroeffentliche trotzdem"
  else
    log "Kein neues Update (upstream ist in HEAD enthalten) -> still"
    exit 0
  fi
fi

log "=== [2/5] Upstream mergen ==="
if ! git merge upstream/master --no-edit >&2; then
  fail "MERGE-KONFLIKT in $REPO_DIR — Reparatur-Agent übernimmt"
fi

# Submodule mitziehen: upstream aendert gelegentlich Gitlinks (z.B. java/assets/futo-swipe =
# Swipe-Sprachmodell). Ohne Update baut Gradle still mit dem ALTEN Asset und das Release
# enthaelt veraltete Modelle — der Merge selbst laesst den Worktree auf dem alten Commit stehen.
log "=== [2/5] Submodule aktualisieren ==="
if ! git submodule update --init --recursive --depth 1 >&2; then
  fail "SUBMODULE-UPDATE fehlgeschlagen — Reparatur-Agent uebernimmt"
fi

log "=== [3/5] Release-Tag bestimmen (VOR dem Build) ==="
# Der Tag muss VOR dem Build feststehen: getVersionName() faellt sonst auf git describe zurueck
# (Version des VORHERIGEN Tags), der ins APK eingebrannte versionName passt dann nicht mehr zum
# Release-Tag -> Obtainium bietet das Update endlos an oder gar nicht.
# Ist der heutige Tag schon vergeben, wird der naechste freie Tag genommen. KEIN "-2"-Suffix:
# das ist laut SemVer eine Pre-Release und damit NIEDRIGER als der vorhandene Tag -> das Handy
# ignoriert das Update komplett (live erlebt 30.08.2026).
REMOTE_TAGS="$(git ls-remote --tags origin 2>/dev/null)" || fail "Tag-Liste (git ls-remote --tags origin) nicht abrufbar"
EXISTING_DATES="$(printf '%s\n' "$REMOTE_TAGS" \
  | sed -n 's#.*refs/tags/v\([0-9][0-9][0-9][0-9]\)\.\([0-9][0-9]\)\.\([0-9][0-9]\)$#\1-\2-\3#p' \
  | sort -u)"
NEWEST_DATE="$(printf '%s\n' "$EXISTING_DATES" | tail -1)"
# Startpunkt: heute — liegt der neueste vorhandene Tag in der Zukunft (z.B. weil ein
# Same-Day-Fix auf den Folgetag gelegt wurde), startet die Suche einen Tag danach.
START_ISO="$(date +%Y-%m-%d)"
if [ -n "$NEWEST_DATE" ] && [[ "$NEWEST_DATE" > "$START_ISO" ]]; then
  START_ISO="$(date -d "$NEWEST_DATE +1 day" +%Y-%m-%d)"
fi
TAG=""
for offset in 0 1 2 3 4 5 6 7; do
  iso="$(date -d "$START_ISO +${offset} day" +%Y-%m-%d)"
  candidate="v${iso//-/.}"
  if ! printf '%s\n' "$REMOTE_TAGS" | grep -q "refs/tags/${candidate}$"; then
    TAG="$candidate"
    break
  fi
done
[ -n "$TAG" ] || fail "kein freier Release-Tag in den naechsten 8 Tagen gefunden"
export VERSION_NAME="$TAG"
export VERSION_CODE="$(git rev-list --first-parent --count master 2>/dev/null || echo 0)"
log "Release-Tag: $TAG (VERSION_NAME=$VERSION_NAME, VERSION_CODE=$VERSION_CODE)"

log "=== [3/5] Build ($GRADLE_TASK) ==="
if ! ./gradlew "$GRADLE_TASK" 2>&1 | tail -25 >&2; then
  fail "BUILD FEHLGESCHLAGEN ($GRADLE_TASK) — Reparatur-Agent übernimmt"
fi
APK="$(ls -t build/outputs/apk/unstable/release/*.apk 2>/dev/null | head -1)"
[ -n "$APK" ] && [ -f "$APK" ] || fail "APK nicht gefunden"

# Kontrolle: der eingebrannte versionName muss den Release-Tag enthalten, sonst laeuft die
# Obtainium-Aktualisierung ins Leere. Kein aapt2 gefunden -> nur Hinweis, kein Abbruch.
if [ -n "$AAPT2" ]; then
  APK_VERSION="$("$AAPT2" dump badging "$APK" 2>/dev/null | sed -n "s/^package:.*versionName='\([^']*\)'.*/\1/p")" || true
  case "$APK_VERSION" in
    "$TAG"*) log "versionName-Kontrolle OK: $APK_VERSION" ;;
    *) fail "APK-versionName '${APK_VERSION:-<leer>}' passt nicht zum Tag '$TAG' — Release abgebrochen" ;;
  esac
else
  log "WARNUNG: kein aapt2 gefunden, versionName-Kontrolle uebersprungen"
fi

log "=== [4/5] Push zum Fork ==="
# Fork-Divergenz abfangen: falls origin/master Commits hat, die lokal fehlen (z.B. Fork-Level-Commits,
# wie rename/README vom 17.08.), schlaegt 'push' sonst mit 'fetch first' fehl und killt den Release-Lauf.
git fetch origin master >/dev/null 2>&1 || true
if ! git rebase --rebase-merges origin/master >&2; then
  fail "REBASE auf origin/master fehlgeschlagen — Reparatur-Agent uebernimmt"
fi
if ! git push origin master >&2; then
  fail "PUSH FEHLGESCHLAGEN zum Fork — Reparatur-Agent uebernimmt"
fi

log "=== [5/5] Release erstellen ==="
UPSTREAM_HASH=$(git log -1 --format=%h upstream/master)
UPSTREAM_DATE=$(git log -1 --format=%cs upstream/master)
BODY="Automatischer Build von **FUTO Keyboard** (Upstream \`${UPSTREAM_HASH}\` vom ${UPSTREAM_DATE}) + B1-Patch: externe Spracherkennung über RECOGNIZE_SPEECH-Popup.

Installation: Obtainium → Add App → GitHub Releases → \`https://github.com/${OWNER}/${REPO}\`"
APK_SIZE=$(stat -c%s "$APK")
MD5=$(md5sum "$APK" | cut -d' ' -f1)

# Release anlegen. Der Tag steht schon fest (== eingebrannter versionName), eine Kollision ist vor
# dem Build ausgeschlossen — deshalb KEIN Suffix-Fallback mehr: ein 422 hier ist ein echter Fehler
# und soll fail() ausloesen, statt still einen Tag zu veroeffentlichen, der nicht im APK steht.
log "--- Release $TAG anlegen ---"
resp=$(curl -s -w '\n%{http_code}' -X POST \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/json" \
    -d "{\"tag_name\":\"$TAG\",\"name\":\"Futo Keyboard M $TAG\",\"body\":$(python3 -c "import json,sys; print(json.dumps(sys.argv[1]))" "$BODY")}" \
    "https://api.github.com/repos/$OWNER/$REPO/releases")
code=$(printf '%s\n' "$resp" | tail -1)
release_json=$(printf '%s\n' "$resp" | head -n -1)
[ "$code" = "201" ] || fail "Release-Erstellung fehlgeschlagen (HTTP $code): $(printf '%s' "$release_json" | head -c 300)"

RELEASE_ID=$(printf '%s' "$release_json" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")

log "--- APK hochladen ---"
upload_resp=$(curl -s -w '\n%{http_code}' -X POST \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @"$APK" \
  "https://uploads.github.com/repos/$OWNER/$REPO/releases/$RELEASE_ID/assets?name=$(basename "$APK")")
upload_code=$(printf '%s\n' "$upload_resp" | tail -1)
upload_json=$(printf '%s\n' "$upload_resp" | head -n -1)
# Frueher stand hier "curl -s ... | python3 -c ...": bei HTTP 4xx/5xx liefert curl Exit 0, das
# Release blieb ohne APK und das Skript meldete trotzdem Erfolg (still, exit 0).
[ "$upload_code" = "201" ] || fail "APK-Upload fehlgeschlagen (HTTP $upload_code): $(printf '%s' "$upload_json" | head -c 300)"
log "Asset: $(printf '%s' "$upload_json" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('name'), d.get('size'), 'Bytes')" 2>/dev/null || echo 'hochgeladen')"

log "Release fertig: https://github.com/$OWNER/$REPO/releases/tag/$TAG"
log "APK: $(basename "$APK") | ${APK_SIZE} Bytes | MD5 ${MD5}"
# Erfolg: Fehler-Marker entfernen (falls vorhanden) + stiller Erfolg (kein stdout -> keine Signal-Meldung)
rm -f "$ERROR_MARKER"
exit 0
