package com.technokratos.taskservice.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Component
public class FileParserFactory {
    private final Map<ImportFileType, FileParser> parsers;


    public FileParserFactory(List<FileParser> parsers) {
        this.parsers = parsers.stream()
                .collect(Collectors.toMap(FileParser::getType, Function.identity()));

        log.info("Registered file parsers: {}", this.parsers.keySet());
    }

    public FileParser getParser(String filename) {
        if (filename.endsWith(".json")) {
            return parsers.get(ImportFileType.JSON);
        } else if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
            return parsers.get(ImportFileType.YAML);
        }
        throw new UnsupportedOperationException("Unsupported file type: " + filename);
    }
}
