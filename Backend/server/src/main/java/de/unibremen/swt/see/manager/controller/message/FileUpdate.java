package de.unibremen.swt.see.manager.controller.message;

import lombok.Data;

@Data
public class FileUpdate {
    private String fileName;

    private String content;

    private String projectType;

    public FileUpdate(String fileName, String content, String projectType) {
        this.fileName = fileName;
        this.content = content;
        this.projectType = projectType;
    }
}
