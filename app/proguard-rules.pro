# AuraSpend R8 rules (full mode is the AGP 9 default)

# ─── llama.cpp runtime (JNI) ─────────────────────────────────────────────────
# The vendored :llama native library binds to Kotlin classes/methods through JNI by name;
# renaming or stripping them breaks inference at runtime.
-keep class com.arm.aichat.** { *; }

# ─── Enums persisted as strings ──────────────────────────────────────────────
# TransactionType / SmsMessageStatus / BudgetPeriod values round-trip through Room as names,
# and are restored with Enum.valueOf().
-keepclassmembers enum com.awbuilds.auraspend.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Crash diagnostics ───────────────────────────────────────────────────────
# Keep source file and line numbers so stack traces from release builds remain actionable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Release noise reduction ─────────────────────────────────────────────────
# Verbose/debug logging is dead weight in release builds; R8 removes the calls entirely.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
