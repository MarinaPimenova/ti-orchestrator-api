package com.wk.ti.upload.event;

import java.io.Serializable;

public record UploadFailedEvent(
        String uploadId,
        String reason
) implements Serializable {}
