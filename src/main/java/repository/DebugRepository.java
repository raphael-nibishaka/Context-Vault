package repository;

import models.DebugEntry;

import java.util.List;
import java.util.Optional;

public interface DebugRepository {
    List<DebugEntry> findAll();

    List<DebugEntry> search(String query);

    Optional<DebugEntry> findById(long id);

    DebugEntry save(DebugEntry entry);

    DebugEntry update(DebugEntry entry);

    void delete(long id);
}
