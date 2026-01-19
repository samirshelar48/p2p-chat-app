#!/data/data/com.termux/files/usr/bin/bash
export TMPDIR=/data/data/com.termux/files/usr/tmp
export GRADLE_USER_HOME=/data/data/com.termux/files/home/.gradle
export HOME=/data/data/com.termux/files/home
cd /data/data/com.termux/files/home/p2p-chat-app
./gradlew assembleDebug --no-daemon --console=plain
