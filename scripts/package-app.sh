#!/usr/bin/env bash
#
# Builds an installer for whichever system this is running on.
#
# jpackage cannot cross-compile, so this produces one platform's installer and the release
# workflow runs it on three machines. What comes out needs no Java installed and no terminal:
# a .dmg, an .msi or a .deb with a trimmed runtime inside it.
set -euo pipefail

cd "$(dirname "$0")/.."

NAME="Leading Tone"

# The version comes from the build unless the release workflow passes the tag. Reading it
# rather than repeating it: the two drifted once already, and a release whose jar calls
# itself a snapshot makes a reader wonder what else is stale. The suffix is stripped because
# jpackage will not take one.
POM_VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' backend/pom.xml | head -1)
VERSION="${APP_VERSION:-${POM_VERSION%-SNAPSHOT}}"
[ -n "$VERSION" ] || { echo "could not read a version from backend/pom.xml"; exit 1; }
JAR=backend/target/leading-tone-runner.jar
OUT=dist
WORK=backend/target/packaging

[ -f "$JAR" ] || { echo "no jar: run 'make package' first"; exit 1; }

case "$(uname -s)" in
    Darwin)  TYPE=dmg;  ICON=packaging/icons/leading-tone.icns; EXE= ;;
    Linux)   TYPE=deb;  ICON=packaging/icons/leading-tone.png;  EXE= ;;
    MINGW*|MSYS*|CYGWIN*) TYPE=msi; ICON=packaging/icons/leading-tone.ico; EXE=.exe ;;
    *) echo "unknown system: $(uname -s)"; exit 1 ;;
esac

JAVA_HOME_DIR="${JAVA_HOME:-$(dirname "$(dirname "$(command -v java)")")}"
JLINK="$JAVA_HOME_DIR/bin/jlink$EXE"
JPACKAGE="$JAVA_HOME_DIR/bin/jpackage$EXE"
for tool in "$JLINK" "$JPACKAGE"; do
    [ -x "$tool" ] || { echo "missing $tool — a full JDK is needed, not a JRE"; exit 1; }
done

# The modules the application actually reaches for. Not a guess: the list was cut down until
# it broke and then the breakages were added back. java.rmi is the one nobody expects -- H2
# needs it, and without it the application dies at startup with a NoClassDefFoundError that
# says nothing about databases.
MODULES=java.base,java.logging,java.naming,java.sql,java.sql.rowset,java.rmi,java.desktop
MODULES=$MODULES,java.management,java.instrument,java.xml,java.compiler,java.scripting
MODULES=$MODULES,java.security.jgss,java.net.http,java.transaction.xa
MODULES=$MODULES,jdk.unsupported,jdk.zipfs,jdk.crypto.ec,jdk.management,jdk.httpserver

rm -rf "$WORK" "$OUT"
mkdir -p "$WORK/input" "$OUT"
cp "$JAR" "$WORK/input/"

echo "==> trimming a runtime"
"$JLINK" --add-modules "$MODULES" \
    --strip-debug --no-header-files --no-man-pages --compress=zip-6 \
    --output "$WORK/runtime"
du -sh "$WORK/runtime" | awk '{print "    " $1}'

# --java-options is how the packaged application says it is packaged. It is the only
# difference from running the jar by hand, and it turns on exactly two things: a data
# directory the person is allowed to write to, and opening a browser onto the interface.
EXTRA=()
case "$TYPE" in
    dmg) EXTRA=(--mac-package-name "$NAME") ;;
    deb) EXTRA=(--linux-shortcut --linux-menu-group "Education" \
                --linux-app-category "education" \
                --linux-deb-maintainer "noreply@lapetina.fr") ;;
    msi) EXTRA=(--win-shortcut --win-menu --win-menu-group "$NAME" --win-dir-chooser) ;;
esac

echo "==> building the $TYPE"
"$JPACKAGE" \
    --type "$TYPE" \
    --name "$NAME" \
    --app-version "$VERSION" \
    --vendor "David Lapetina" \
    --description "A music theory tutor that teaches you, rather than testing you." \
    --icon "$ICON" \
    --input "$WORK/input" \
    --main-jar "$(basename "$JAR")" \
    --runtime-image "$WORK/runtime" \
    --java-options "-Dmusic.packaged=true" \
    --java-options "-Xmx1g" \
    --dest "$OUT" \
    "${EXTRA[@]}"

echo
ls -lh "$OUT" | tail -n +2 | awk '{print "    " $9 "  " $5}'
echo "    ready in $OUT/"
