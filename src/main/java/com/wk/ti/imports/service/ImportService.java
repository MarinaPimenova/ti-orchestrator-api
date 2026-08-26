package com.wk.ti.imports.service;

import com.wk.ti.imports.event.ImportEvent;
import com.wk.ti.rabbit.config.RabbitConfig;
import com.wk.ti.imports.model.ImportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${ti.import.storage.path}")
    private String storageDirectory;

    public ImportResponse bringing(MultipartFile file) {
        String importId = UUID.randomUUID().toString();
        try {
            Path storagePath = Paths.get(storageDirectory);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            String fileName = importId + "_" + file.getOriginalFilename();
            Path targetPath = storagePath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Saved incoming file for importId={} to path={}", importId, targetPath);

            ImportEvent event = new ImportEvent(importId, file.getOriginalFilename(), targetPath.toString());
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.RK_IMPORT_REQ, event);

            return new ImportResponse(importId);
        } catch (IOException e) {
            log.error("Failed to store file for import attempt", e);
            throw new RuntimeException("File storage failure", e);
        }
    }
}
