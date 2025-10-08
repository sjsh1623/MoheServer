package com.mohe.spring.service.image;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageService {
    private final Path imageStorageLocation = Paths.get("build/images");

    public ImageService() {
        try {
            Files.createDirectories(this.imageStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public List<String> downloadAndSaveImages(Long placeId, String placeName, List<String> imageUrls) {
        List<String> savedImagePaths = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            String fileName = placeId + "_" + placeName + "_" + (i + 1) + ".jpeg";
            try (InputStream in = new URL(imageUrl).openStream()) {
                Path targetPath = this.imageStorageLocation.resolve(fileName);
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                savedImagePaths.add(targetPath.toString());
            } catch (IOException e) {
                // Log the error, but continue processing other images
                e.printStackTrace();
            }
        }
        return savedImagePaths;
    }
}
