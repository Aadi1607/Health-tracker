# Keep kotlinx.serialization generated serializers for backup models.
-keepclassmembers class io.github.aadi1607.habittracker.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.aadi1607.habittracker.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
