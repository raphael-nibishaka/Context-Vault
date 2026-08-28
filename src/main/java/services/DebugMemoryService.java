package services;

import models.DebugEntry;
import models.SimilarDebugMatch;
import repository.DebugRepository;
import utils.ValidationResult;
import utils.ValidationUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DebugMemoryService {
    private static final Pattern ERROR_TYPE_PATTERN = Pattern.compile(
            "\\b([A-Z][A-Za-z0-9]*(?:Error|Exception|Failure|Fault|Timeout|Refused|Denied))\\b"
    );

    private final DebugRepository debugRepository;

    public DebugMemoryService(DebugRepository debugRepository) {
        this.debugRepository = debugRepository;
    }

    public List<DebugEntry> findAll() {
        return debugRepository.findAll();
    }

    public List<DebugEntry> search(String query) {
        return debugRepository.search(query);
    }

    public Optional<DebugEntry> findById(long id) {
        return debugRepository.findById(id);
    }

    public DebugEntry save(DebugEntry entry) {
        validateOrThrow(entry);
        entry.setErrorType(extractErrorType(entry.getErrorMessage(), entry.getStackTrace()));
        return debugRepository.save(entry);
    }

    public DebugEntry update(DebugEntry entry) {
        validateOrThrow(entry);
        entry.setErrorType(extractErrorType(entry.getErrorMessage(), entry.getStackTrace()));
        return debugRepository.update(entry);
    }

    public void delete(long id) {
        debugRepository.delete(id);
    }

    public Optional<SimilarDebugMatch> findBestSimilarMatch(String rawErrorText) {
        if (rawErrorText == null || rawErrorText.isBlank()) {
            return Optional.empty();
        }

        String normalizedInput = rawErrorText.trim();
        String inputErrorType = extractErrorType(normalizedInput, normalizedInput);
        Set<String> inputTokens = tokenize(normalizedInput);

        List<SimilarDebugMatch> matches = new ArrayList<>();
        for (DebugEntry entry : debugRepository.findAll()) {
            int score = scoreSimilarity(entry, inputErrorType, inputTokens, normalizedInput);
            if (score >= 45) {
                matches.add(new SimilarDebugMatch(entry, score));
            }
        }

        return matches.stream()
                .max(Comparator.comparingInt(SimilarDebugMatch::score));
    }

    public List<SimilarDebugMatch> findSimilarMatches(String rawErrorText, int limit) {
        if (rawErrorText == null || rawErrorText.isBlank()) {
            return List.of();
        }

        String normalizedInput = rawErrorText.trim();
        String inputErrorType = extractErrorType(normalizedInput, normalizedInput);
        Set<String> inputTokens = tokenize(normalizedInput);

        return debugRepository.findAll().stream()
                .map(entry -> new SimilarDebugMatch(entry, scoreSimilarity(entry, inputErrorType, inputTokens, normalizedInput)))
                .filter(match -> match.score() >= 30)
                .sorted(Comparator.comparingInt(SimilarDebugMatch::score).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    public String extractErrorType(String errorMessage, String stackTrace) {
        String combined = ((errorMessage == null ? "" : errorMessage) + "\n" + (stackTrace == null ? "" : stackTrace)).trim();
        if (combined.isBlank()) {
            return "";
        }

        Matcher matcher = ERROR_TYPE_PATTERN.matcher(combined);
        if (matcher.find()) {
            return matcher.group(1);
        }

        String firstLine = combined.lines().findFirst().orElse(combined).trim();
        if (firstLine.length() > 80) {
            return firstLine.substring(0, 77) + "...";
        }
        return firstLine;
    }

    public ValidationResult validate(DebugEntry entry) {
        return ValidationUtils.validateRequired(entry.getErrorMessage(), "Error message");
    }

    private int scoreSimilarity(DebugEntry entry,
                                String inputErrorType,
                                Set<String> inputTokens,
                                String normalizedInput) {
        int score = 0;

        if (!inputErrorType.isBlank() && inputErrorType.equalsIgnoreCase(blank(entry.getErrorType()))) {
            score += 60;
        }

        String entryText = String.join("\n",
                blank(entry.getErrorMessage()),
                blank(entry.getStackTrace()),
                blank(entry.getErrorType()),
                blank(entry.getTags())
        ).toLowerCase(Locale.ROOT);

        String inputLower = normalizedInput.toLowerCase(Locale.ROOT);
        if (!inputLower.isBlank() && entryText.contains(inputLower)) {
            score += 35;
        } else if (!inputErrorType.isBlank() && entryText.contains(inputErrorType.toLowerCase(Locale.ROOT))) {
            score += 25;
        }

        Set<String> entryTokens = tokenize(entryText);
        int overlap = 0;
        for (String token : inputTokens) {
            if (entryTokens.contains(token)) {
                overlap++;
            }
        }
        score += Math.min(overlap * 8, 32);

        return score;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }

        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() >= 3) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private void validateOrThrow(DebugEntry entry) {
        ValidationResult result = validate(entry);
        if (!result.valid()) {
            throw new IllegalArgumentException(result.message());
        }
    }

    private String blank(String value) {
        return value == null ? "" : value;
    }
}
