#!/bin/bash
# Builds chip-test.apk with the Android SDK command-line tools (no Gradle):
# aapt2, javac, d8, zipalign and apksigner. Needs build-tools 36.0.0 and platform android-36.
# The APK is signed with a local test key, created in build/ on the first run. Uses
# $JAVA_HOME/bin for javac and keytool when JAVA_HOME is set.
set -euo pipefail
cd "$(dirname "$0")"
JBIN=${JAVA_HOME:+$JAVA_HOME/bin/}
SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}
BT=$SDK/build-tools/36.0.0
AJ=$SDK/platforms/android-36/android.jar
rm -rf build/classes build/dex build/*.apk
mkdir -p build/classes build/dex
"$BT/aapt2" link --manifest AndroidManifest.xml -I "$AJ" \
    --min-sdk-version 34 --target-sdk-version 36 -o build/base.apk
"${JBIN}javac" -source 17 -target 17 -classpath "$AJ" -d build/classes $(find src -name '*.java')
"$BT/d8" --lib "$AJ" --min-api 34 --output build/dex $(find build/classes -name '*.class')
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [ ! -f build/test.keystore ]; then
    "${JBIN}keytool" -genkeypair -keystore build/test.keystore -storepass chiptest -keypass chiptest \
        -alias test -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=chip test" >/dev/null
fi
"$BT/apksigner" sign --ks build/test.keystore --ks-pass pass:chiptest --key-pass pass:chiptest \
    --out chip-test.apk build/aligned.apk
"$BT/apksigner" verify chip-test.apk
echo "built chip-test.apk"
