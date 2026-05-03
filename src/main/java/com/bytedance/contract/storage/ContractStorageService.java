package com.bytedance.contract.storage;

import com.bytedance.contract.model.Contract;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ContractStorageService {

    private final ObjectMapper objectMapper;
    private final Path storagePath;

    public ContractStorageService(@Value("${app.storage.path:data/contracts.json}") String storagePath) {
        this.storagePath = Path.of(storagePath);
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public synchronized List<Contract> loadAll() {
        try {
            ensureStorageReady();
            if (Files.size(storagePath) == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(storagePath.toFile(), new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("读取合同数据失败", exception);
        }
    }

    public synchronized void saveAll(List<Contract> contracts) {
        try {
            ensureStorageReady();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), contracts);
        } catch (IOException exception) {
            throw new IllegalStateException("保存合同数据失败", exception);
        }
    }

    private void ensureStorageReady() throws IOException {
        Path parent = storagePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.notExists(storagePath)) {
            Files.createFile(storagePath);
        }
    }
}
