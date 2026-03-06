package de.unibremen.swt.see.manager.controller.message;

import lombok.Data;

@Data
public class FileUpdate {
    private String fileName;

    private String content;

    public FileUpdate(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
    }
}
