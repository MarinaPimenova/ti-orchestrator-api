package com.wk.ti.job.model;

public record FileProcessingEvent(
        String jobId,
        String type,
        String originalFilename,
        String storedFilePath
) {
}
