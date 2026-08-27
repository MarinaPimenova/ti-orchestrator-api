package com.wk.ti.job.model;

public record FileProcessingEvent(
        String jobId,
        FileProcessingType type,
        String originalFilename,
        String storedFilePath
) {
}
