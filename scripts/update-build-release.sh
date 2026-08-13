#!/usr/bin/env bash
# Futo Keyboard M — automatischer Update-Build-Release-Lauf
# Holt die neuesten FUTO-Änderungen, merged sie in den Custom-Branch,
# baut das Release-APK und veröffentlicht es als GitHub-Release.
#
# Exit-Codes: 0 = ok (leerer stdout bei "kein Update" = still), 1 = Fehler
set -euo pipefail

REPO_DIR="/mnt/wd_nvme/futo-keyboard-m"
OWNER="pikatchu2k3"
REPO="futo-keyboard-m"
GRADLE_TASK="assembleUnstableRelease"

GITHUB_TOKEN="${GITHUB_TOKEN:-$(grep '^GITHUB_TOKEN=' ~/.hermes/.env | cut -d= -f2)}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export JAVA_HOME="${JAVA_HOME:-/opt/jdk-21.0.6+7}"
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx4096m"

cd "$REPO_DIR"

echo "=== [1/5] Upstream holen ==="
git fetch upstream master 2>&1 | tail -2

UPSTREAM_SHA=$(git rev-parse upstream/master)
LOCAL_SHA=$(git rev-parse HEAD)
if [ "$UPSTREAM_SHA" = "$LOCAL_SHA" ]; then
  exit 0   # kein neues Update -> still
fi

echo "=== [2/5] Upstream mergen ==="
if ! git merge upstream/master --no-edit 2>&1; then
  echo "!!! MERGE-KONFLIKT in $REPO_DIR — manuell lösen: git status !!!"
  exit 1
fi

echo "=== [3/5] Build ($GRADLE_TASK) ==="
if ! ./gradlew "$GRADLE_TASK" 2>&1 | tail -25; then
  echo "!!! BUILD FEHLGESCHLAGEN !!!"
  exit 1
fi
APK="$(ls -t build/outputs/apk/unstable/release/*.apk 2>/dev/null | head -1)"
[ -n "$APK" ] && [ -f "$APK" ] || { echo "!!! APK nicht gefunden !!!"; exit 1; }

echo "=== [4/5] Push zum Fork ==="
git push origin master 2>&1 | tail -2

echo "=== [5/5] Release erstellen ==="
UPSTREAM_HASH=$(git log -1 --format=%h upstream/master)
UPSTREAM_DATE=$(git log -1 --format=%cs upstream/master)
TAG="v$(date +%Y.%m.%d)"
BODY="Automatischer Build von **FUTO Keyboard** (Upstream \`${UPSTREAM_HASH}\` vom ${UPSTREAM_DATE}) + B1-Patch: externe Spracherkennung über RECOGNIZE_SPEECH-Popup.

Installation: Obtainium → Add App → GitHub Releases → \`https://github.com/${OWNER}/${REPO}\`"
APK_SIZE=$(stat -c%s "$APK")
MD5=$(md5sum "$APK" | cut -d' ' -f1)

# Release anlegen (bei Tag-Kollision: Suffix anhängen)
release_json=""
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
[ -n "$release_json" ] && [ "$code" = "201" ] || { echo "!!! Release-Erstellung fehlgeschlagen !!!"; exit 1; }

RELEASE_ID=$(echo "$release_json" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")

echo "--- APK hochladen ---"
curl -s -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Content-Type: application/octet-stream" \
  --data-binary @"$APK" \
  "https://uploads.github.com/repos/$OWNER/$REPO/releases/$RELEASE_ID/assets?name=$(basename "$APK")" \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print('Asset:', d.get('name'), d.get('size'), 'Bytes')"

echo ""
echo "✅ Fertig: https://github.com/$OWNER/$REPO/releases/tag/$TAG"
echo "APK: $(basename "$APK") | ${APK_SIZE} Bytes | MD5 ${MD5}"
