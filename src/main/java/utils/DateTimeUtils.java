package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private DateTimeUtils() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? "-" : DISPLAY_FORMATTER.format(value);
    }
}
