package com.trazo.android.ui.common.vocabulary

/**
 * The names the API sends as codes (goals, muscles, equipment...) in one language. A code missing here falls back to English
 * and then to a readable form of the code, so a value added by the backend never breaks a screen.
 */
internal class Vocabulary(
    val goal: Map<String, String>,
    val experience: Map<String, String>,
    val limitation: Map<String, String>,
    val reason: Map<String, String>,
    val phase: Map<String, String>,
    val pattern: Map<String, String>,
    val muscle: Map<String, String>,
    val equipment: Map<String, String>,
    val difficulty: Map<String, String>,
    val split: Map<String, String>
)
