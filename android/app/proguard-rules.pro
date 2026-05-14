# Keep annotations needed by runtime reflection (serialization, Room).
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# kotlinx.serialization — keep generated serializers + companions.
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static kotlinx.serialization.KSerializer serializer(...);
}

# Ktor (uses reflection for the engine factory in JVM mode).
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# Room — generated DAO impls are referenced reflectively.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose tooling (debug-only previews).
-dontwarn androidx.compose.ui.tooling.**
