package com.wk.ti.controller;

import com.wk.ti.sse.SseEmitterRegistry;
import com.wk.ti.upload.model.ImportResponse;
import com.wk.ti.upload.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final SseEmitterRegistry sseRegistry;

    @GetMapping(value = "/subscribe/{importId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String importId) {
        return sseRegistry.createEmitter(importId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.upload(file));
    }
}
