package com.januworld.birthday.photo;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {
    private static final long MAX_PHOTO_SIZE = 8 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final PhotoRepository repository;

    public PhotoController(PhotoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PhotoMetadata> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@org.springframework.web.bind.annotation.PathVariable long id) {
        PhotoContent photo = repository.findContent(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileName(photo.fileName()) + "\"")
                .body(photo.data());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoMetadata> upload(
            @RequestParam("photo") MultipartFile photo) {
        String contentType = photo.getContentType() == null ? "" : photo.getContentType().toLowerCase();
        if (photo.isEmpty() || photo.getSize() > MAX_PHOTO_SIZE || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose a JPEG, PNG, or WebP image no larger than 8 MB");
        }
        try {
            String fileName = safeFileName(photo.getOriginalFilename() == null ? "photo" : photo.getOriginalFilename());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(repository.save(fileName, contentType, photo.getBytes()));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded photo");
        }
    }

    private String safeFileName(String fileName) {
        return fileName.replaceAll("[\\r\\n\"/\\\\]", "_");
    }
}
