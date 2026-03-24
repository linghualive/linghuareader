# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard configuration.

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.linghualive.flamekit.**$$serializer { *; }
-keepclassmembers class com.linghualive.flamekit.** {
    *** Companion;
}
-keepclasseswithmembers class com.linghualive.flamekit.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Navigation type-safe route classes (used by Compose Navigation at runtime)
-keep class com.linghualive.flamekit.core.navigation.Screen { *; }
-keep class com.linghualive.flamekit.core.navigation.Screen$* { *; }

# Kotlin metadata needed for Compose Navigation type-safe routes
-keep class kotlin.Metadata { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Kotlinx serialization internals (R8 strips these causing runtime crashes)
-keep class kotlinx.serialization.internal.** { *; }
-keep class kotlinx.serialization.descriptors.** { *; }

# Keep @Serializable data classes used in DataStore
-keep class com.linghualive.flamekit.core.datastore.ReadingPreferences { *; }
-keep class com.linghualive.flamekit.core.datastore.CustomReaderTheme { *; }
-keep class com.linghualive.flamekit.core.datastore.PageMode { *; }
-keep class com.linghualive.flamekit.core.datastore.ReaderThemeType { *; }
-keep class com.linghualive.flamekit.core.datastore.ScreenOrientation { *; }
-keep class com.linghualive.flamekit.feature.sync.domain.model.SyncConfig { *; }

# Keep serializable enum values
-keepclassmembers enum com.linghualive.flamekit.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Hilt ViewModel factories and generated code
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_HiltComponents* { *; }
-keep class * { @dagger.* *; }
-keep class * { @javax.inject.* *; }
-keep class dagger.hilt.internal.** { *; }

# Keep AppRelease (used in UpdateDialog, not @Serializable)
-keep class com.linghualive.flamekit.feature.update.domain.model.AppRelease { *; }

# Keep Backup/Sync serializable data classes
-keep class com.linghualive.flamekit.feature.sync.domain.model.BackupData { *; }
-keep class com.linghualive.flamekit.feature.sync.domain.model.BookDto { *; }
-keep class com.linghualive.flamekit.feature.sync.domain.model.BookmarkDto { *; }
-keep class com.linghualive.flamekit.feature.sync.domain.model.ReadingProgressDto { *; }
-keep class com.linghualive.flamekit.feature.sync.domain.model.BookSourceDto { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }
-keeppackagenames org.jsoup.nodes
-dontwarn org.jspecify.annotations.NullMarked

# juniversalchardet
-keep class org.mozilla.universalchardet.** { *; }
