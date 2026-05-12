package com.technokratos.taskservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfiguration {

    public static ObjectMapper createYamlMapper() {
        return new ObjectMapper(new YAMLFactory())
                .findAndRegisterModules();
    }
}
