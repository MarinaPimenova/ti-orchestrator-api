package com.wk.ti.job;

import com.wk.ti.job.model.FileProcessingEvent;
import com.wk.ti.job.model.FileProcessingResponse;
import com.wk.ti.job.model.FileProcessingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractFileProcessingService {

    private final RabbitTemplate rabbitTemplate;

    protected abstract String storageDirectory();

    protected abstract String exchangeName();

    protected abstract String requestRoutingKey();

    protected abstract FileProcessingType fileProcessingType();

    public FileProcessingResponse process(MultipartFile file) {

        String jobId = UUID.randomUUID().toString();

        try {
            Path storagePath = Paths.get(storageDirectory());

            Files.createDirectories(storagePath);
            String storedFileName = jobId + "_" + file.getOriginalFilename();

            Path storedFilePath = storagePath.resolve(storedFileName);

            Files.copy(
                    file.getInputStream(),
                    storedFilePath,
                    StandardCopyOption.REPLACE_EXISTING);

            log.info("Saved incoming file for jobId={} to path={}",
                    jobId,
                    storedFilePath);

            FileProcessingEvent event = new FileProcessingEvent(
                    jobId,
                    fileProcessingType().name(),
                    file.getOriginalFilename(),
                    storedFilePath.toString()
            );
            // check how to use event in convertAndSend
            rabbitTemplate.convertAndSend(
                    exchangeName(),
                    requestRoutingKey(),
                    event
            );

            return new FileProcessingResponse(jobId);

        } catch (IOException e) {
            log.error("Failed to store file for jobId={}", jobId, e);

            throw new IllegalStateException("File storage failure", e);
        }
    }
}
