package com.jarves.mh.ui.theme

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
) {
    SYSTEM("system", "System Default", "Default"),
    NEPALI("ne", "Nepali", "नेपाली"),
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    SPANISH("es", "Spanish", "Español"),
    CHINESE("zh", "Chinese (Simplified)", "简体中文"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    ARABIC("ar", "Arabic", "العربية"),
    RUSSIAN("ru", "Russian", "Русский");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
    }
}
