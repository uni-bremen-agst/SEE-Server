package de.unibremen.swt.see.manager.controller;

import de.unibremen.swt.see.manager.model.ServerSnapshot;
import de.unibremen.swt.see.manager.service.ServerSnapshotService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/serversnapshot")
@RequiredArgsConstructor
@Slf4j
public class ServerSnapshotController {

    /**
     * Used to access server snapshots.
     */
    private final ServerSnapshotService serverSnapshotService;

    /**
     * Deletes a server snapshot by its ID.
     *
     * @param snapshotId the ID of the snapshot to delete
     * @return {@code 204 No Content} if successful, or {@code 404 Not Found}
     * if the snapshot does not exist.
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Snapshot deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Snapshot not found")
    })
    @DeleteMapping("/{snapshotId}")
    public ResponseEntity<Void> deleteSnapshot(
            @PathVariable("snapshotId") UUID snapshotId) {
        try {
            serverSnapshotService.deleteSnapshotsById(snapshotId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{snapshotId}/download")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Resource> downloadFile(@PathVariable("snapshotId") UUID snapshotId) {
        Optional<ServerSnapshot> file = serverSnapshotService.getServerSnapshotById(snapshotId);
        if (file.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file.get().getSnapshotDataPath());
        if (!resource.exists()) {
            log.warn("Snapshot file not found on filesystem: {}", file.get().getSnapshotDataPath());
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(file.get().getSize());
        headers.setContentDisposition(ContentDisposition.attachment().filename("snapshot").build());

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}
