package de.unibremen.swt.see.manager.model.livekitmessages;

import lombok.Data;

@Data
public class FileRename extends FileMessage{

    private String newFileName;


    public FileRename(String fileName, String projectType, String newFileName) {
        super(fileName, projectType);
        this.newFileName = newFileName;
    }
}
