package com.wk.ti.event;

import java.io.Serializable;

public record ImportEvent(
        String importId,
        String originalFileName,
        String storedFilePath
        //String traceId
) implements Serializable {}
