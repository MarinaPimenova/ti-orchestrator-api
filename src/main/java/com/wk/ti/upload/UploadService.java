package com.wk.ti.upload;

import com.wk.ti.upload.model.UploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {
    public UploadResponse upload(MultipartFile file) {
        return new UploadResponse(UUID.randomUUID().toString());
    }
}
