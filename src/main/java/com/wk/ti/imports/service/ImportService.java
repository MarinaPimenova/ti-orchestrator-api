package com.wk.ti.imports.service;

import com.wk.ti.job.AbstractFileProcessingService;
import com.wk.ti.job.config.FileProcessingProperties;
import com.wk.ti.job.model.FileProcessingType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.nio.file.*;

@Service
public class ImportService extends AbstractFileProcessingService {

    private final FileProcessingProperties properties;

    public ImportService(
            RabbitTemplate rabbitTemplate,
            FileProcessingProperties properties) {

        super(rabbitTemplate);
        this.properties = properties;
    }

    @Override
    protected String storageDirectory() {
        return properties.getImportFlow().getStoragePath();
    }

    @Override
    protected String exchangeName() {
        return properties.getImportFlow().getExchange();
    }

    @Override
    protected String requestRoutingKey() {
        return properties.getImportFlow().getRequestRoutingKey();
    }

    @Override
    protected FileProcessingType fileProcessingType() {
        return FileProcessingType.IMPORT;
    }
}