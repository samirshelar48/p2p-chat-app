#!/data/data/com.termux/files/usr/bin/bash

# P2P Chat App Build Script for Termux
# Run this script in Termux to build the APK

set -e

export TMPDIR=/data/data/com.termux/files/usr/tmp
export GRADLE_USER_HOME=$HOME/.gradle
mkdir -p $TMPDIR
mkdir -p $GRADLE_USER_HOME

cd ~/p2p-chat-app

echo "==> Downloading Gradle Wrapper..."
curl -sL "https://services.gradle.org/distributions/gradle-8.5-all.zip" -o $TMPDIR/gradle.zip

echo "==> Extracting Gradle..."
mkdir -p $GRADLE_USER_HOME/wrapper/dists/gradle-8.5-bin
unzip -q $TMPDIR/gradle.zip -d $GRADLE_USER_HOME/wrapper/dists/gradle-8.5-bin/

echo "==> Downloading wrapper jar..."
curl -sL "https://repo.maven.apache.org/maven2/org/gradle/gradle-tooling-api/8.5/gradle-tooling-api-8.5.jar" -o gradle/wrapper/gradle-wrapper.jar 2>/dev/null || {
    # Fallback: copy from extracted gradle
    cp $GRADLE_USER_HOME/wrapper/dists/gradle-8.5-bin/gradle-8.5/lib/gradle-wrapper-*.jar gradle/wrapper/gradle-wrapper.jar 2>/dev/null || {
        echo "Using system gradle instead..."
        gradle assembleDebug --no-daemon
        exit $?
    }
}

echo "==> Building APK..."
./gradlew assembleDebug --no-daemon

echo ""
echo "==> Build complete!"
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
