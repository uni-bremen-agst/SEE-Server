package de.unibremen.swt.see.manager.controller.message;

import lombok.Data;

/**
 * Message object for file updates.
 * This will be sent via livekit.
 */
@Data
public class FileUpdate {

    /**
     * The filename relative to the project directory.
     */
    private String fileName;

    /**
     * The type of the project.
     */
    private String projectType;

    /**
     * The new content of the file.
     */
    private String content;

    /**
     * Constructor.
     *
     * @param fileName The filename relative to the project directory.
     * @param content The new content of the file.
     * @param projectType The type of the project.
     */
    public FileUpdate(String fileName, String content, String projectType) {
        this.fileName = fileName;
        this.content = content;
        this.projectType = projectType;
    }
}
