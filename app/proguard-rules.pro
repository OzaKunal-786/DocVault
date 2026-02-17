# DocVault ProGuard Rules

# Keep Room entities
-keep class com.docvault.data.database.** { *; }

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }

# Keep Compose
-dontwarn androidx.compose.**

# Keep Kotlin metadata
-keepattributes Annotation
-keepattributes Signature
-keepattributes InnerClasses
