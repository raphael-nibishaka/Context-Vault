package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestoreResult {
    private final List<String> warnings = new ArrayList<>();

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
}
