package utils;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ValidationUtils {
    private ValidationUtils() {
    }

    public static ValidationResult validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return ValidationResult.failure(fieldName + " is required.");
        }
        return ValidationResult.success();
    }

    public static ValidationResult validateDirectory(String value) {
        ValidationResult required = validateRequired(value, "Project folder");
        if (!required.valid()) {
            return required;
        }

        try {
            Path path = Path.of(value);
            if (!Files.exists(path)) {
                return ValidationResult.failure("Project folder does not exist.");
            }
            if (!Files.isDirectory(path)) {
                return ValidationResult.failure("Project folder must be a directory.");
            }
            return ValidationResult.success();
        } catch (Exception exception) {
            return ValidationResult.failure("Project folder path is invalid.");
        }
    }
}
