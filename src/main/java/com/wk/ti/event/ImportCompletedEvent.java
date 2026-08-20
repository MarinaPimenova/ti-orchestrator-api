package com.wk.ti.event;

import java.io.Serializable;

public record ImportCompletedEvent(
        String importId,
        int processedRows
) implements Serializable {}
