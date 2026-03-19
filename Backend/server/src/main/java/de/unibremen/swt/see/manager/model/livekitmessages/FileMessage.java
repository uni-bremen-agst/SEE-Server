package de.unibremen.swt.see.manager.model.livekitmessages;

import lombok.AllArgsConstructor;
import lombok.Data;

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


    public FileMessage() {
    }
}
