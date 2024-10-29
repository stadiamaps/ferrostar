# Ferrostar & JNA
# TODO: This needs validation from a publishing app. Ticket https://github.com/stadiamaps/ferrostar/issues/185
-keep class com.sun.jna.** { *; }
-keep class uniffi.ferrostar.** { *; }
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window