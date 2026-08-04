package services;

import models.ContextEntry;
import repository.ContextRepository;
import utils.ValidationResult;
import utils.ValidationUtils;

import java.util.List;

public class ContextService {
    private final ContextRepository contextRepository;

    public ContextService(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    public List<ContextEntry> findAll() {
        return contextRepository.findAll();
    }

    public List<ContextEntry> search(String query) {
        return contextRepository.search(query);
    }

    public ContextEntry save(ContextEntry contextEntry) {
        validateOrThrow(contextEntry);
        return contextRepository.save(contextEntry);
    }

    public ContextEntry update(ContextEntry contextEntry) {
        validateOrThrow(contextEntry);
        return contextRepository.update(contextEntry);
    }

    public void delete(long id) {
        contextRepository.delete(id);
    }

    public ValidationResult validate(ContextEntry contextEntry) {
        ValidationResult nameValidation = ValidationUtils.validateRequired(contextEntry.getName(), "Context name");
        if (!nameValidation.valid()) {
            return nameValidation;
        }
        return ValidationUtils.validateDirectory(contextEntry.getProjectPath());
    }

    private void validateOrThrow(ContextEntry contextEntry) {
        ValidationResult result = validate(contextEntry);
        if (!result.valid()) {
            throw new IllegalArgumentException(result.message());
        }
    }
}
