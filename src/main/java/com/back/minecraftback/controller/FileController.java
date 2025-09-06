package com.back.minecraftback.controller;

import com.back.minecraftback.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final Tika tika;

    @GetMapping("/{filePath}")
    public ResponseEntity<Resource> getFile(@PathVariable String filePath) {
        System.out.println(filePath);
        Resource resource = fileStorageService.loadAsResource(filePath);

        String contentType;
        try {
            contentType = tika.detect(resource.getInputStream());
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
