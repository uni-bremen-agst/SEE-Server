package de.unibremen.swt.see.manager.service;

import de.unibremen.swt.see.manager.model.Server;
import de.unibremen.swt.see.manager.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FileServiceTest {
    @TempDir
    Path tempDir;

    private FileService fileService;
    private Server server;
    private Path projectPath;
    private Path outside;

    @BeforeEach
    void setUp() throws IOException {
        fileService = new FileService(mock(FileRepository.class));
        ReflectionTestUtils.setField(fileService, "fileStorageRoot", tempDir.resolve("storage").toString());
        server = new Server();
        ReflectionTestUtils.setField(server, "id", UUID.randomUUID());
        projectPath = Files.createDirectories(tempDir.resolve("storage").resolve(server.getId().toString()).resolve("SEECity"));
        Files.writeString(projectPath.resolve("source.txt"), "original");
        outside = Files.createDirectories(tempDir.resolve("outside"));
        Files.writeString(outside.resolve("target.txt"), "untouched");
    }

    @Test
    void rejectsAbsoluteProjectTypeBeforeChangingFiles() throws IOException {
        assertRejected(outside.toString(), "target.txt");
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../../outside", "../SEECity", "unknown", ""})
    void rejectsInvalidProjectTypeBeforeChangingFiles(String projectType) throws IOException {
        assertRejected(projectType, "target.txt");
    }

    @Test
    void rejectsAbsoluteFilePaths() throws IOException {
        assertRejected("SEECity", outside.resolve("target.txt").toString());
    }

    @Test
    void rejectsTraversalPaths() throws IOException {
        assertRejected("SEECity", "../../../outside/target.txt");
    }

    @Test
    void rejectsSymbolicLinkDirectories() throws IOException {
        Files.createSymbolicLink(projectPath.resolve("link"), outside);
        assertRejected("SEECity", "link/target.txt");
        assertRejected("SEECity", "link/new-directory/new.txt");
        assertFalse(Files.exists(outside.resolve("new-directory")));
    }

    @Test
    void rejectsSymbolicLinkFilesIncludingDanglingLinks() throws IOException {
        Files.createSymbolicLink(projectPath.resolve("link.txt"), outside.resolve("target.txt"));
        assertRejected("SEECity", "link.txt");
        Files.createSymbolicLink(projectPath.resolve("dangling.txt"), outside.resolve("missing.txt"));
        assertRejected("SEECity", "dangling.txt");
        assertFalse(Files.exists(outside.resolve("missing.txt")));
    }

    @Test
    void rejectsSymbolicLinkProjectRoot() throws IOException {
        Files.createSymbolicLink(projectPath.getParent().resolve("DiffCity"), outside);
        assertRejected("DiffCity", "target.txt");
    }

    @Test
    void allowsNestedFileOperationsAndRebuildsArchive() throws IOException {
        fileService.updateFileInProject(server, "SEECity", "nested/new.txt", "new content");
        assertEquals("new content", Files.readString(projectPath.resolve("nested/new.txt")));
        fileService.renameFileInProject(server, "SEECity", "nested/new.txt", "renamed/file.txt");
        assertFalse(Files.exists(projectPath.resolve("nested/new.txt")));
        assertEquals("new content", Files.readString(projectPath.resolve("renamed/file.txt")));
        fileService.deleteFileInProject(server, "SEECity", "renamed/file.txt");
        assertFalse(Files.exists(projectPath.resolve("renamed/file.txt")));
        assertTrue(Files.size(projectPath.getParent().resolve("SEECity.zip")) > 0);
    }

    private void assertRejected(String projectType, String filePath) throws IOException {
        assertThrows(IllegalArgumentException.class,
                () -> fileService.updateFileInProject(server, projectType, filePath, "overwritten"));
        assertThrows(IllegalArgumentException.class,
                () -> fileService.deleteFileInProject(server, projectType, filePath));
        assertThrows(IllegalArgumentException.class,
                () -> fileService.renameFileInProject(server, projectType, filePath, "renamed.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> fileService.renameFileInProject(server, projectType, "source.txt", filePath));
        assertEquals("untouched", Files.readString(outside.resolve("target.txt")));
        assertEquals("original", Files.readString(projectPath.resolve("source.txt")));
        assertFalse(Files.exists(projectPath.getParent().resolve("SEECity.zip")));
    }
}
