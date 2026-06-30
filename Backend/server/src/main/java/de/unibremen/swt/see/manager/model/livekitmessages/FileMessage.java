package de.unibremen.swt.see.manager.model.livekitmessages;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Base class for file update messages.
 */
@Data
@AllArgsConstructor
public class FileMessage {

    /**
     * The filename relative to the project directory.
     */
    private String fileName;

    /**
     * The type of the project.
     */
    private String projectType;

    /**
     * Default constructor.
     */
    public FileMessage() {
        // Intentionally empty.
    }
}
