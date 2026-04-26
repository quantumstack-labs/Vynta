# Groq API Models
-keep class com.first_project.chronoai.ai.** { *; }

# Google API Client
-keep class com.google.api.services.calendar.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.client.json.gson.** { *; }

# Keep GSON annotations and sensitive names
-keepattributes Signature, *Annotation*, EnclosingMethod
-keep class com.google.gson.annotations.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class com.first_project.chronoai.data.local.entity.** { *; }
-keep class com.first_project.chronoai.data.local.dao.** { *; }

# Keep all SerializedName fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
