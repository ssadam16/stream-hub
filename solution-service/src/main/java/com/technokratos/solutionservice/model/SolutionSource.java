package com.technokratos.solutionservice.model;

import com.technokratos.common.enums.ProgrammingLanguage;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "solution_sources")
@CompoundIndex(def = "{'taskId': 1, 'language': 1}")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolutionSource {
    @Id
    private String id;

    private UUID solutionId;               // связь с Postgres
    private UUID taskId;                   // для поиска по задаче
    private ProgrammingLanguage language;  // для фильтрации по языку
    private String code;                   // сам код
    private String codeHash;              // SHA-256
    private String normalizedCode;         // для нечёткого сравнения
}
