package utils;

public record ValidationResult(boolean valid, String message) {
    public static ValidationResult success() {
        return new ValidationResult(true, "");
    }

    public static ValidationResult failure(String message) {
        return new ValidationResult(false, message);
    }
}
