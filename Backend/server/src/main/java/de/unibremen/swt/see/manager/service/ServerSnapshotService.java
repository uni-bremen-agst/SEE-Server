package de.unibremen.swt.see.manager.service;

import de.unibremen.swt.see.manager.model.Server;
import de.unibremen.swt.see.manager.model.ServerSnapshot;
import de.unibremen.swt.see.manager.repository.ServerSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing server snapshots.
 * <p>
 * This service provides methods to retrieve server snapshots associated with a specific server.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ServerSnapshotService {

    /**
     * Repository for accessing server snapshots.
     */
    private final ServerSnapshotRepository snapshotRepo;

    /**
     * Service for managing server-related operations.
     */
    private final ServerService serverService;

    /**
     * Service for managing file-related operations.
     */
    private final FileService fileService;

    /**
     * Retrieves all server snapshots for a given server ID.
     *
     * @param serverId the ID of the server for which to retrieve snapshots
     * @return a list of server snapshots associated with the specified server
     * @throws IllegalArgumentException if the server with the given ID does not exist
     */
    public Optional<List<ServerSnapshot>> getServerSnapshots(UUID serverId) throws IllegalArgumentException {
        Server server = serverService.get(serverId);
        if (server == null) {
            log.warn("Server with ID {} not found", serverId);
            return Optional.empty();
        }
        return snapshotRepo.findAllByServer(server);
    }

    /**
     * Retrieves the latest server snapshot for a given server ID.
     * <p>
     * This method fetches the server associated with the provided server ID and retrieves
     * all snapshots for that server. It then determines the most recent snapshot based on
     * its creation time.
     *
     * @param serverId the ID of the server whose latest snapshot is to be retrieved
     * @return an Optional containing the latest server snapshot if it exists, or an empty Optional
     *         if the server does not exist or has no snapshots
     */
    public Optional<ServerSnapshot> getLatestServerSnapshot(UUID serverId) {
        Server server = serverService.get(serverId);
        if (server == null) {
            log.warn("Server with ID {} not found", serverId);
            return Optional.empty();
        }
        return snapshotRepo.findAllByServer(server)
                .flatMap(list -> list.stream()
                        .max((s1, s2) -> s1.getCreationTime().compareTo(s2.getCreationTime())));
    }

    /**
     * Deletes a server snapshot by its ID.
     *
     * @param id the ID of the server snapshot to delete
     * @throws IllegalArgumentException if no server snapshot with the given ID exists
     */
    public void deleteSnapshotsById(UUID id) throws IllegalArgumentException {
        Optional<ServerSnapshot> snapshot = snapshotRepo
                .findById(id);
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("No server snapshot found with ID " + id);
        }
        snapshotRepo.deleteById(id);
    }

    /**
     * Retrieves a server snapshot by its ID.
     *
     * @param snapshotId the ID of the server snapshot to retrieve
     * @return an Optional containing the server snapshot if found, or empty if not found
     */
    public Optional<ServerSnapshot> getServerSnapshotById(UUID snapshotId) {
        return snapshotRepo.findById(snapshotId);
    }

    /**
     * Creates a new snapshot for the specified server from the provided file input stream.
     * @param serverId The id of the server.
     * @param fileIs The snapshot's file input stream.
     * @param cityName The name of the city the snapshot is for.
     * @return The created snapshot.
     * @throws IOException Is thrown if the snapshot file cannot be written to disk.
     * @throws PersistenceException If the snapshot cannot be saved to the database.
     */
    public ServerSnapshot createServerSnapshotFromFile(UUID serverId, InputStream fileIs, String cityName) throws IOException, PersistenceException {
        Server server = serverService.get(serverId);
        if (server == null) {
            log.warn("Server with ID {} not found", serverId);
            throw new EntityNotFoundException("No server found with ID " + serverId);
        }

        Path basePath = fileService.getServerUploadPath(server);
        ServerSnapshot snapshot = new ServerSnapshot();
        snapshot.setServer(server);
        snapshot.setCreationTime(ZonedDateTime.now());
        snapshot.setCityName(cityName);

        Path filePath = basePath.resolve("snapshot-" + snapshot.getCreationTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".zip");

        if (Files.exists(filePath) && !Files.isRegularFile(filePath)) {
            throw new IOException("File not deleted. Not a regular file: " + filePath);
        }

        try (fileIs) {
            Files.copy(fileIs, filePath);
        } catch (IOException e) {
            throw new IOException("Unable to save file: " + filePath, e);
        }

        snapshot.setSize(Files.size(filePath));
        snapshot.setSnapshotDataPath(filePath.toString());

        try {
            ServerSnapshot snapshotResult = snapshotRepo.save(snapshot);
            log.info("Created server snapshot for server ID {}", serverId);
            return snapshotResult;
        } catch (PersistenceException e) {
            // Cleanup file if the database operation fails
            Files.deleteIfExists(filePath);
            log.error("Failed to create server snapshot for server ID {}: {}", serverId, e.getMessage());
            throw e;
        }
    }
}
