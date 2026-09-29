# AuraSpend R8 rules (full mode is the AGP 9 default)

# ─── Native (JNI) runtime ────────────────────────────────────────────────────
# There is deliberately NO local rule for the on-device runtime. ONNX Runtime's
# AAR ships no consumer rules of its own, and its native methods are registered
# dynamically at JNI_OnLoad with `RegisterNatives` rather than by the
# `Java_com_microsoft_onnxruntime_*` naming convention — which reads like a
# guaranteed rename-breaking crash. It is not one: AGP's default
# proguard-android-optimize.txt already carries
#
#   -keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
#
# which keeps any class declaring a native method, names included, so
# FindClass() at JNI_OnLoad still resolves. Verified in the merged
# configuration.txt and by R8's keep-radius analysis: that rule pins 40 items
# (DONT_OBFUSCATE + DONT_OPTIMIZE).
#
# So do NOT add `-keep class com.microsoft.onnxruntime.** { *; }`. It is
# unnecessary, and it would suppress the obfuscation score for nothing. The
# thing to protect instead is AGP's default file: swapping it for
# proguard-android.txt is the change that would actually break inference, and
# AGP 9 rejects that file outright.
#
# This block previously held `-keep class com.arm.aichat.** { *; }` for the
# vendored llama.cpp library, deleted in handoff 0013. It matched nothing and
# was the only native-runtime rule in the file, which made the JNI story look
# handled locally when it is handled by AGP.

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
