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
