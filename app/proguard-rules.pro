# SpendLog Production ProGuard / R8 Configuration

# 1. KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.internal.GeneratedSerializer {
    java.lang.Object[] getChildSerializers();
}
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class * { *; }

# 2. Room Database & SQLite
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 3. SpendLog Network & Data Models
-keep class com.example.pulseledger.data.remote.** { *; }
-keep class com.example.pulseledger.data.local.entity.** { *; }
-keep class com.example.pulseledger.data.local.model.** { *; }

# 4. Compose Navigation & Serialization
-keep class com.example.pulseledger.** implements java.io.Serializable { *; }

# 5. WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
