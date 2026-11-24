# Reglas de ProGuard/R8 para DCM MUSIC (Fase 1)

# NewPipeExtractor
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
