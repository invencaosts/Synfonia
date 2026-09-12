package com.synfonia.musicas.services;

import com.synfonia.musicas.exceptions.FileStorageException;
import com.synfonia.musicas.exceptions.FileStorageInitException;
import com.synfonia.musicas.exceptions.InvalidFileNameException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageInitException("Não foi possível criar o diretório onde os arquivos enviados serão armazenados.", ex);
        }
    }

    public String saveFile(MultipartFile file, @Nullable String subDir) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new InvalidFileNameException("O nome do arquivo original não pode ser nulo.");
        }
        
        String fileName = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(originalFilename);
        try {
            if (fileName.contains("..")) {
                throw new InvalidFileNameException("Desculpe! O nome do arquivo contém uma sequência de caminho inválida " + fileName);
            }

            Path targetLocation = subDir != null ? this.fileStorageLocation.resolve(subDir) : this.fileStorageLocation;
            Files.createDirectories(targetLocation);
            targetLocation = targetLocation.resolve(fileName);
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return (subDir != null ? subDir + "/" : "") + fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Não foi possível armazenar o arquivo " + fileName + ". Por favor, tente novamente!", ex);
        }
    }
}
