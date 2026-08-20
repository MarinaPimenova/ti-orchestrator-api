package com.wk.ti.listener;

import com.wk.ti.event.ImportCompletedEvent;
import com.wk.ti.event.ImportFailedEvent;
import com.wk.ti.rabbit.config.RabbitConfig;
import com.wk.ti.sse.SseEmitterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportResultListener {

    private final SseEmitterRegistry sseRegistry;
    private final MeterRegistry meterRegistry;

    @RabbitListener(queues = RabbitConfig.QUEUE_IMPORT_COMPLETED)
    public void handleSuccess(ImportCompletedEvent event) {
        log.info("Import completed successfully for importId={}. Rows processed: {}", event.importId(), event.processedRows());
        sseRegistry.sendAndComplete(event.importId(), event);
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_IMPORT_FAIL)
    public void handleFailure(ImportFailedEvent event) {
        log.error("Import failed for importId={}. Cause: {}", event.importId(), event.reason());
        meterRegistry.counter("import.failed.count", "reason", event.reason()).increment();
        sseRegistry.sendAndComplete(event.importId(), event);
    }
}
