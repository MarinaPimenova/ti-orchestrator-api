package com.wk.ti.imports.event;

import java.io.Serializable;

public record ImportCompletedEvent(
        String importId,
        int processedRows
) implements Serializable {}
