package de.unibremen.swt.see.manager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Represents the data model of a server snapshot.
 * <p>
 * A server snapshot is a point-in-time representation of a server's state with all it's code cities.
 */
@Getter
@Entity
@Table(name = "serverSnapshots")
@RequiredArgsConstructor
public class ServerSnapshot {

    /**
     * ID of the server snapshot.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    /**
     * The server this snapshot belongs to.
     */
    @Setter
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "server_id")
    private Server server;

    /**
     * The name of the server snapshot.
     */
    @Setter
    @Column(name = "creation_time", updatable = false)
    private ZonedDateTime creationTime;

    /**
     * The path to the snapshot data.
     * <p>
     * This is the path where the snapshot data is stored on the server.
     */
    @Setter
    @Column(name = "snapshot_data_path")
    private String snapshotDataPath;

    /**
     * Size of the snapshot file in bytes.
     */
    @Setter
    @Column(name = "size")
    private long size;

    /**
     * The type of the project.
     */
    @Setter
    @Column(name = "city_name")
    private String cityName;

}
