package com.technokratos.taskservice.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JsonFileParser implements FileParser {

    private final ObjectMapper objMapper;

    @Override
    public ImportFileType getType() {
        return ImportFileType.JSON;
    }

    @Override
    public List<TaskImportRequest> parse(MultipartFile file) throws IOException {
        return objMapper.readValue(file.getInputStream(),
                objMapper.getTypeFactory().constructCollectionType(
                        List.class, TaskImportRequest.class
                ));
    }
}
