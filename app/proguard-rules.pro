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

# R8 missing classes (often from Google API / Jetty dependencies)
-dontwarn com.sun.net.httpserver.Headers
-dontwarn com.sun.net.httpserver.HttpContext
-dontwarn com.sun.net.httpserver.HttpExchange
-dontwarn com.sun.net.httpserver.HttpHandler
-dontwarn com.sun.net.httpserver.HttpServer
-dontwarn java.awt.Desktop$Action
-dontwarn java.awt.Desktop
-dontwarn javax.naming.InvalidNameException
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.ldap.LdapName
-dontwarn javax.naming.ldap.Rdn
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid

