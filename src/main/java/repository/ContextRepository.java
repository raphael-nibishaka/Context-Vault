package repository;

import models.ContextEntry;

import java.util.List;
import java.util.Optional;

public interface ContextRepository {
    List<ContextEntry> findAll();

    List<ContextEntry> search(String query);

    Optional<ContextEntry> findById(long id);

    ContextEntry save(ContextEntry contextEntry);

    ContextEntry update(ContextEntry contextEntry);

    void delete(long id);
}
