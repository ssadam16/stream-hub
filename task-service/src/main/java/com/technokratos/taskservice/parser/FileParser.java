package com.technokratos.taskservice.parser;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileParser {
    ImportFileType getType();

    List<TaskImportRequest> parse(MultipartFile file) throws IOException;
}
