package com.back.minecraftback.util;

/**
 * Ограничения длины пользовательского текста (защита от чрезмерных полей в JSON при большом теле запроса).
 */
public final class TextValidation {

    public static final int MAX_DETAILED_DESCRIPTION_CHARS = 100_000;

    private TextValidation() {
    }

    public static void requireDetailedDescriptionLength(String detailedDescription) {
        if (detailedDescription == null || detailedDescription.isEmpty()) {
            return;
        }
        if (detailedDescription.length() > MAX_DETAILED_DESCRIPTION_CHARS) {
            throw new IllegalArgumentException(
                    "detailedDescription exceeds max length (" + MAX_DETAILED_DESCRIPTION_CHARS + " characters)");
        }
    }
}
