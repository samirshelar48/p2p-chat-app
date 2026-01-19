# Add project specific ProGuard rules here.
# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
