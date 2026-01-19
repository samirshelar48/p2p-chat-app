#!/data/data/com.termux/files/usr/bin/bash

# Direct build using system Gradle
# Run: bash ~/p2p-chat-app/build_direct.sh

export TMPDIR=/data/data/com.termux/files/usr/tmp
export GRADLE_USER_HOME=$HOME/.gradle
export ANDROID_SDK_ROOT=$HOME/android-sdk
export _JAVA_OPTIONS="-Djava.io.tmpdir=$TMPDIR"

mkdir -p $TMPDIR

cd ~/p2p-chat-app

echo "Building P2P Chat APK..."
echo "This may take several minutes on first run."
echo ""

gradle assembleDebug --no-daemon --console=plain

if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
    echo ""
    echo "SUCCESS! APK built at:"
    echo "  ~/p2p-chat-app/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "Install with:"
    echo "  pm install app/build/outputs/apk/debug/app-debug.apk"
else
    echo ""
    echo "Build may have failed. Check output above."
fi
