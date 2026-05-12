package com.technokratos.taskservice.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.technokratos.taskservice.configuration.MapperConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class YamlFileParser implements FileParser {

    private final ObjectMapper yamlMapper = MapperConfiguration.createYamlMapper();

    @Override
    public ImportFileType getType() {
        return ImportFileType.YAML;
    }

    @Override
    public List<TaskImportRequest> parse(MultipartFile file) throws IOException {
        return yamlMapper.readValue(
                file.getInputStream(),
                yamlMapper.getTypeFactory().constructCollectionType(
                        List.class, TaskImportRequest.class)
        );
    }
}
