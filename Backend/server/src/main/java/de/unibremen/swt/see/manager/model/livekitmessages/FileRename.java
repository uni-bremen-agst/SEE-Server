package de.unibremen.swt.see.manager.model.livekitmessages;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Message object for file renames.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FileRename extends FileMessage {

    /**
     * The new name of the file.
     */
    private String newFileName;

    /**
     * Constructor.
     *
     * @param fileName The filename relative to the project directory.
     * @param projectType The type of the project.
     * @param newFileName The new name of the file.
     */
    public FileRename(String fileName, String projectType, String newFileName) {
        super(fileName, projectType);
        this.newFileName = newFileName;
    }
}
