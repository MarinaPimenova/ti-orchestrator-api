package com.wk.ti.upload.event;

import java.io.Serializable;

public record UploadCompletedEvent(
        String uploadId

) implements Serializable {}
