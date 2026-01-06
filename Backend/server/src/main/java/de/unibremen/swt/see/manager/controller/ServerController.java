package de.unibremen.swt.see.manager.controller;

import de.unibremen.swt.see.manager.model.*;
import de.unibremen.swt.see.manager.security.UserDetailsImpl;
import de.unibremen.swt.see.manager.service.AccessControlService;
import de.unibremen.swt.see.manager.service.ServerService;
import de.unibremen.swt.see.manager.service.ServerSnapshotService;
import de.unibremen.swt.see.manager.service.UserService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles HTTP requests for the /server endpoint.
 * <p>
 * This REST controller exposes various methods to control SEE game server
 * instances.
 */
@RestController
@RequestMapping("/api/v1/server")
@RequiredArgsConstructor
@Slf4j
public class ServerController {

    /**
     * Handle server-related operations and business logic.
     */
    private final ServerService serverService;

    /**
     * Used to check if a user has access to a server.
     * <p>
     * This service is used in {@code @PreAuthorize} process and is likely not
     * unused even if your IDE tells you so.
     */
    private final AccessControlService accessControlService;

    /**
     * Used to access user data.
     */
    private final UserService userService;

    /**
     * Used to access server snapshots.
     */
    private final ServerSnapshotService serverSnapshotService;

    /**
     * Retrieves metadata of the server identified by the specified ID.
     *
     * @param id the ID of the server to retrieve
     * @return {@code 200 OK} with the server metadata as payload, or
     * {@code 401 Unauthorized} if access cannot be granted.
     */
    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @accessControlService.canAccessServer(principal.id, #id)")
    public ResponseEntity<?> get(@RequestParam("id") UUID id) {
        return ResponseEntity.ok().body(serverService.get(id));
    }

    /**
     * Retrieves the metadata of all accessible server resources.
     * <p>
     * If the client is authenticated as {@code ADMIN}, all available servers
     * are returned.
     *
     * @return {@code 200 OK} with the server metadata as payload, or
     * {@code 401 Unauthorized} if access cannot be granted.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> getAll() {
        final UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final User user = userService.get(userDetails.getId());

        if (userService.hasRole(user, RoleType.ROLE_ADMIN)) {
            return ResponseEntity.ok().body(serverService.getAll());
        }

        return ResponseEntity.ok().body(user.getServers());
    }

    /**
     * Creates a new server.
     *
     * @param server metadata object to create new server instance
     * @return {@code 200 OK} with the server metadata object as payload, or
     * {@code 500 Internal Server Error} if the server could not be persisted,
     * or {@code 401 Unauthorized} if access cannot be granted.
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Server server) {
        server = serverService.create(server);
        if (server == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().body(server);
    }

    /**
     * Adds a file to an existing server.
     *
     * @param serverId the ID of the server
     * @param projectType {@code String} representation of a {@code ProjectType}
     * value
     * @param file file content
     * @return {@code 200 OK} with the file metadata object as payload, or
     * {@code 500 Internal Server Error} if the file could not be persisted, or
     * {@code 401 Unauthorized} if access cannot be granted.
     */
    @PostMapping("/addFile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addFile(
            @RequestParam("id") UUID serverId,
            @RequestParam("projectType") String projectType,
            @RequestParam("file") MultipartFile file) {
        File responseFile = serverService.addFile(serverId, projectType, file);
        if (responseFile == null) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().body(responseFile);
    }

    /**
     * Deletes the server with the specified ID.
     * <p>
     * Deletes the server along with its files.
     *
     * @param id the ID of the server to delete
     * @return {@code 200 OK}, or {@code 404 Not Found} if the server does not
     * exist, or {@code 500 Internal Server Error} if the server is busy or
     * could not be deleted, or {@code 401 Unauthorized} access cannot be
     * granted.
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@RequestParam("id") UUID id) {
        try {
            serverService.delete(id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().body(ControllerUtils.wrapMessage(e.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(ControllerUtils.wrapMessage("Error during file deletion!"));
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Start the server with the specified ID.
     *
     * @param id the ID of the server to start
     * @return {@code 200 OK}, or {@code 404 Not Found} if the server does not
     * exist, or {@code 500 Internal Server Error} if the server is busy or
     * already online, or {@code 401 Unauthorized} if access cannot be granted.
     */
    @PostMapping("/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> start(@RequestParam("id") UUID id) {
        try {
            serverService.start(id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().body(ControllerUtils.wrapMessage(e.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(ControllerUtils.wrapMessage("Error accessing server files!"));
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Stop the server with the specified ID.
     *
     * @param id the ID of the server to stop
     * @return {@code 200 OK}, or {@code 404 Not Found} if the server does not
     * exist, or {@code 500 Internal Server Error} if the server is busy or
     * already stopped, or {@code 401 Unauthorized} if access cannot be granted.
     */
    @PostMapping("/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> stop(@RequestParam("id") UUID id) {
        try {
            serverService.stop(id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().body(ControllerUtils.wrapMessage(e.getMessage()));
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the file list of the server with the specified ID.
     *
     * @param id the ID of the server
     * @return {@code 200 OK} with the file metadata as payload, or
     * {@code 401 Unauthorized} if access cannot be granted.
     */
    @GetMapping("/files")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @accessControlService.canAccessServer(principal.id, #id)")
    public ResponseEntity<?> getFiles(@RequestParam("id") UUID id) {
        return ResponseEntity.ok().body(serverService.getFilesForServer(id));
    }


    /**
     * Retrieves all snapshots of a server.
     *
     * @param serverId the ID of the server
     * @return {@code 200 Ok} if successful, or {@code 404 Not Found} if
     * the server does not exist, or {@code 500 Internal Server Error} if an
     * error occurs.
     */
    @GetMapping("/snapshots")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @accessControlService.canAccessFile(principal.id, #serverId)")
    public ResponseEntity<List<ServerSnapshot>> getAllSnapshotsOfServer(@RequestParam("serverId") UUID serverId) {
        Optional<List<ServerSnapshot>> server = serverSnapshotService.getServerSnapshots(serverId);

        if (server.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(server.get());
    }

    /**
     * Retrieves the latest snapshot of a specified server.
     *
     * @param serverId the unique identifier of the server whose latest snapshot is being requested
     * @return a ResponseEntity containing the latest ServerSnapshot if found;
     * otherwise, a ResponseEntity with a not found (404) status
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest snapshot retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ServerSnapshot.class))),
            @ApiResponse(responseCode = "404", description = "Server or snapshot not found",
                    content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @GetMapping("/snapshots:latest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ServerSnapshot> getLatestSnapshotsOfServer(@RequestParam("serverId") UUID serverId) {
        Optional<ServerSnapshot> server = serverSnapshotService.getLatestServerSnapshot(serverId);

        if (server.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(server.get());
    }

    /**
     * Creates a snapshot for the specified server using the provided file.
     *
     * @param serverId the unique identifier of the server for which the snapshot is to be created
     * @param file the multipart file containing the snapshot data
     * @return a {@link ResponseEntity} containing the created {@link ServerSnapshot} when successful,
     * a 404 Not Found response if the server is not found,
     * or a 400 Bad Request response in case of invalid input or errors during processing
     */
    @PostMapping(path = "/snapshots", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ServerSnapshot> createSnapshot(
            @RequestParam("serverId") UUID serverId,
            HttpServletRequest httpServletRequest) {
        try {
            ServletInputStream inputStream = httpServletRequest.getInputStream();
            Server server = serverService.get(serverId);
            if (server == null) {
                return ResponseEntity.notFound().build();
            }

            ServerSnapshot snapshot = serverSnapshotService.createServerSnapshotFromFile(serverId, inputStream);
            return ResponseEntity.ok(           ).build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
