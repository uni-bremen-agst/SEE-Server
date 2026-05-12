package de.unibremen.swt.see.manager.model.livekitmessages;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Message object for file updates.
 * This will be sent via livekit.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FileUpdate extends FileMessage {

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
        super(fileName, projectType);
        this.content = content;
    }
}
