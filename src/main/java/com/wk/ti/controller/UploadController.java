package com.wk.ti.controller;

import com.wk.ti.sse.SseEmitterRegistry;
import com.wk.ti.upload.UploadService;
import com.wk.ti.upload.model.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;
    private final SseEmitterRegistry sseRegistry;

    @GetMapping(value = "/subscribe/{uploadId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String uploadId) {
        return sseRegistry.createEmitter(uploadId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(uploadService.upload(file));
    }
}
