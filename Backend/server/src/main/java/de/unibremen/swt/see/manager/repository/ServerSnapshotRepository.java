package de.unibremen.swt.see.manager.repository;

import de.unibremen.swt.see.manager.model.Server;
import de.unibremen.swt.see.manager.model.ServerSnapshot;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link ServerSnapshot} entities.
 */
public interface ServerSnapshotRepository extends JpaRepository<ServerSnapshot, UUID> {

    /**
     * Finds all snapshots of a given server.
     * @param server the server for which to find snapshots.
     * @return a list of found snapshots.
     */
    Optional<List<ServerSnapshot>> findAllByServer(@NotNull Server server);

}
