package de.unibremen.swt.see.manager.service;

import de.unibremen.swt.see.manager.model.ProjectFile;
import de.unibremen.swt.see.manager.model.ProjectType;
import de.unibremen.swt.see.manager.model.Server;
import de.unibremen.swt.see.manager.repository.FileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Service class for managing file-related operations.
 * <p>
 * This service provides high-level operations for file management, including
 * creating, retrieving, updating, and deleting files. It encapsulates the
 * business logic and acts as an intermediary between the controller layer and
 * the data access layer.
 *
 * @see FileRepository
 * @see de.unibremen.swt.see.manager.controller.FileController
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FileService {

    /**
     * Enables file data persistence and retrieval for this service.
     */
    private final FileRepository fileRepo;

    /**
     * Contains the file storage path on the local file system.
     * <p>
     * The value is configured in the application properties and gets injected
     * during class initialization.
     */
    @Value("${see.app.filestorage.dir}")
    private String fileStorageRoot;

    /**
     * Creates a new file from the provided attributes.
     * <p>
     * The file metadata is stored in the database and the content is stored on
     * the local file system. A reference to the associated server and file type
     * is stored in the metadata.
     *
     * @param server the server instance this file belongs to
     * @param projectType the type of the project
     * @param multipartFile the file content from the API request
     * @return the created file, or {@code null} if the file content is empty.
     * @throws java.io.IOException if there was an I/O error while storing the
     * file
     */
    public ProjectFile create(Server server, ProjectType projectType, MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }

        ProjectFile projectFile = new ProjectFile();
        projectFile.setName(projectType.toString() + ".zip");
        projectFile.setContentType(multipartFile.getContentType());
        projectFile.setServer(server);
        projectFile.setProjectType(projectType);

        Path path;
        try {
            path = storeProjectFile(projectFile, multipartFile, projectType);
        } catch (IOException e) {
            throw new IOException("Error persisting file.", e);
        }
        projectFile.setSize(Files.size(path));

        return fileRepo.save(projectFile);
    }

    /**
     * Sanitizes the path to a local file in a project and makes sure the file exist when {@code checkExist} is set to true.
     * <p>
     * If the file path lead to a location outside the project (e.g. {@code ../../../../../random-system.file}) an @see {@link IOException} is thrown.
     *
     * @param projectPath The path of the project directory.
     * @param filePath The file path, relative to {@code projectPath}.
     * @param checkExist Set to true, to check if the file exist.
     * @return The absolute file path.
     * @throws IOException When the file is outside the project directory or doesn't exist.
     */
    private Path getFilePathSanitized(Path projectPath, String filePath, boolean checkExist) throws IOException {
        Path localFilePath = projectPath.resolve(filePath);
        if (!localFilePath.normalize().startsWith(projectPath)) {
            throw new IllegalArgumentException("File path is outside of project directory: " + filePath);
        }
        if (checkExist && !Files.exists(localFilePath)) {
            throw new IOException("File does not exist: " + localFilePath);
        }
        return localFilePath;
    }

    /**
     * Rebuilds the zip cache of a given project.
     *
     * @param server The server, the project belong to.
     * @param projectType The type of the project.
     * @throws IOException If the zip file can't be built.
     */
    private void rebuildZipCacheFile(Server server, String projectType) throws IOException {

        Path zipPath = getServerUploadPath(server).resolve(projectType + ".zip");
        Files.delete(zipPath);

        // Rebuild Zip file with the updated file
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            zipFile.addFolder(getServerUploadPath(server).resolve(projectType).toFile());
        }

        Optional<ProjectFile> file = fileRepo.findByServerIdAndProjectType(server.getId(), ProjectType.valueOf(projectType));

        if (file.isPresent()) {
            ProjectFile projectFileEntity = file.get();
            projectFileEntity.setSize(Files.size(zipPath));
            fileRepo.save(projectFileEntity);
        } else {
            log.warn("ProjectFile not found for server {} and type {}, database record not updated", server.getId(), projectType);
        }
    }

    /**
     * Updates the content of a file in a project.
     *
     * @param server the server this file belongs to.
     * @param projectTypeStr the project type of the file.
     * @param filePath the path of the file, relative to the project directory.
     * @param fileContents the new content of the file.
     * @throws IOException will be thrown, when the file cant be written
     * @throws IllegalArgumentException will be thrown when the file path is outside the project directory (to prevent path traversals).
     */
    public void updateFileInProject(Server server, String projectTypeStr, String filePath, String fileContents) throws IOException {
        Path projectPath = getServerUploadPath(server).resolve(projectTypeStr);
        Path localFilePath = getFilePathSanitized(projectPath, filePath);

        Files.writeString(localFilePath, fileContents);

        rebuildZipCacheFile(server, projectTypeStr, projectPath);
//        Path zipPath = getServerUploadPath(server).resolve(projectTypeStr + ".zip");
//        Files.delete(zipPath);
//
//        // Rebuild Zip file with the updated file
//        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
//            zipFile.addFolder(projectPath.toFile());
//        }
//
//        Optional<ProjectFile> file = fileRepo.findByServerIdAndProjectType(server.getId(), ProjectType.valueOf(projectTypeStr));
//
//        if (file.isPresent()) {
//            ProjectFile projectFileEntity = file.get();
//            projectFileEntity.setSize(Files.size(zipPath));
//            fileRepo.save(projectFileEntity);
//        } else {
//            log.warn("ProjectFile not found for server {} and type {}, database record not updated", server.getId(), projectTypeStr);
//        }
    }

    public void renameFileInProject(Server server, String projectType, String filePath, String newFilePath) throws IOException {
        Path projectPath = getServerUploadPath(server).resolve(projectType);
        Path localOldFilePath = getFilePathSanitized(projectPath, filePath);
        //Path localNewFilePath = projectPath.resolve(filePath);
        Path localNewFilePath = projectPath.resolve(newFilePath);

        Files.move(localOldFilePath, localNewFilePath, REPLACE_EXISTING);
        rebuildZipCacheFile(server, projectType, projectPath);
    }

    public void deleteFileInProject(Server server, String projectType, String filePath) throws IOException {
        Path projectPath = getServerUploadPath(server).resolve(projectType);
        Path localFilePath = getFilePathSanitized(projectPath, filePath);

        Files.delete(localFilePath);
        rebuildZipCacheFile(server, projectType, projectPath);
    }

    /**
     * Retrieves a file by its ID.
     *
     * @param fileId the ID of the file to retrieve
     * @return the file if found, or {@code null} if not found
     */
    public ProjectFile get(UUID fileId) {
        log.info("Fetching file by id {}", fileId);
        Optional<ProjectFile> optFile = fileRepo.findById(fileId);
        if (optFile.isEmpty()) {
            log.error("File not found in db: {}", fileId);
            return null;
        }
        return optFile.get();
    }

    /**
     * Retrieves a file by its associated server and project type.
     *
     * @param serverId the ID of the server the file belongs to
     * @param projectType the project type of the file
     * @return file if found, or {@code null} if not found
     * @throws EntityNotFoundException if server or file could not be found
     */
    public ProjectFile getByServerAndProjectType(UUID serverId, ProjectType projectType) {
        log.info("Fetching file for server and project type: {}; {}", serverId, projectType);

        Optional<ProjectFile> optFile = fileRepo.findByServerIdAndProjectType(serverId, projectType);
        if (optFile.isEmpty()) {
            throw new EntityNotFoundException("File not found by project type " + projectType);
        }
        return optFile.get();
    }

    /**
     * Deletes a file.
     * <p>
     * Deletes the file from local file system and the file metadata object from
     * database.
     * <p>
     * Does not throw I/O exception if the file to delete was not found.
     *
     * @param projectFile the file to be deleted
     * @throws java.io.IOException if there was an I/O error while deleting the
     * file
     */
    public void delete(ProjectFile projectFile) throws IOException {
        Path filePath = getPath(projectFile);
        log.info("Removing file {}", filePath);

        if (Files.exists(filePath) && !Files.isRegularFile(filePath)) {
            throw new IOException("File not deleted. Not a regular file: " + filePath);
        }

        try {
            Files.delete(filePath);
        } catch (NoSuchFileException e) {
            log.warn("File to delete does not exist: {}", filePath);
        }
        fileRepo.delete(projectFile);
    }

    /**
     * Convenience function to delete a file by its ID.
     * <p>
     * The file is retrieved by its ID and then deleted.
     * <p>
     * Does not throw I/O exception if the file to delete was not found.
     *
     * @param fileId ID of the file to be deleted
     * @throws java.io.IOException if {@link #delete(ProjectFile)} throws one
     * @throws EntityNotFoundException if no file exists with given ID
     * @see #get(UUID)
     * @see #delete(ProjectFile)
     */
    public void delete(UUID fileId) throws IOException {
        ProjectFile projectFile = get(fileId);
        if (projectFile == null) {
            throw new EntityNotFoundException("No entity found with ID " + fileId);
        }
        delete(projectFile);
    }

    /**
     * Retrieves all files of a server.
     *
     * @param server the server that the files belong to
     * @return a list containing all files of the given server
     */
    public List<ProjectFile> getByServer(Server server) {
        return fileRepo.findByServer(server);
    }

    /**
     * Deletes all files of a server.
     *
     * @param server the server to delete files for
     * @throws IOException if a file cannot be deleted
     */
    public void deleteFilesByServer(Server server) throws IOException {
        List<ProjectFile> projectFiles = getByServer(server);

        for (ProjectFile projectFile : projectFiles) {
            delete(projectFile);
        }

        Files.delete(getServerUploadPath(server));
    }


    /**
     * Stores given file on local file system.
     *
     * @param projectFile the prepared file metadata
     * @param multipartFile the file content
     * @param projectType
     * @return the path to where the file was stored
     * @throws IOException if there was an I/O error while storing the file
     */
    private Path storeProjectFile(ProjectFile projectFile, MultipartFile multipartFile, ProjectType projectType) throws IOException {
        Path filePath = getServerUploadPath(projectFile.getServer()).resolve(projectType + ".zip");
        var dir = getServerUploadPath(projectFile.getServer()).resolve(projectType.toString());
        if (filePath.toString().endsWith(".zip")) {
            try (InputStream inputStream = multipartFile.getInputStream()) {
                Files.copy(inputStream, filePath);
            } catch (IOException e) {
                throw new IOException("Unable to save file: " + projectFile.getName(), e);
            }
            Files.createDirectories(dir);
            ZipFile zipFile = new ZipFile(filePath.toString());
            zipFile.extractAll(dir.toString());
            zipFile.close();
        }

        return filePath;
    }

    /**
     * Generates the file system path of the directory where all files of a
     * specific server are stored.
     * <p>
     * Tries to create the directory if it does not yet exist. Server ID must
     * not be {@code null}.
     *
     * @param server the server that the upload path belongs to
     * @return the path where all files of given server are stored
     * @throws IOException if there is a problem accessing or creating the
     * directory, or if the path is not a directory
     */
    public Path getServerUploadPath(Server server) throws IOException {
        Path basePath = Paths.get(fileStorageRoot).toAbsolutePath();
        Path uploadPath = basePath.resolve(server.getId().toString());
        if (!Files.exists(uploadPath)) {
            try {
                return Files.createDirectories(uploadPath);
            } catch (IOException e) {
                throw new IOException("File Storage Path does not exist and could not be created: " + uploadPath.toString(), e);
            }
        }
        if (!Files.isDirectory(uploadPath, NOFOLLOW_LINKS)) {
            throw new IOException("File Storage Path is not a directory!");
        }
        return uploadPath;
    }

    /**
     * Generates the file system path for the given file.
     * <p>
     * Gets the server path and appends the file name. File name and server must
     * not be {@code null}.
     *
     * @param projectFile the file to which the path should be assembled
     * @return file system path for the given file
     * @throws IOException if one is thrown by
     * {@link #getServerUploadPath(Server)}
     * @see #getServerUploadPath(Server)
     */
    public Path getPath(ProjectFile projectFile) throws IOException {
        String fileName = projectFile.getName();
        if (fileName == null || fileName.isEmpty()) {
            throw new RuntimeException("File name must not be empty!");
        }
        return getServerUploadPath(projectFile.getServer()).resolve(fileName);
    }

    /**
     * Extracts the file extension from given file name.
     *
     * @param fileName file name to extract the extension from
     * @return extension of the given file if existent, or else empty
     * {@code String}
     */
    private static String getFileExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return (idx != -1) ? fileName.substring(idx + 1) : "";
    }


}
