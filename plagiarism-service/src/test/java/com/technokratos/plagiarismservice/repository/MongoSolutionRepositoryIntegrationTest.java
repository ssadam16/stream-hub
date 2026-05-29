package com.technokratos.plagiarismservice.repository;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.plagiarismservice.model.SolutionSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
@Tag("integration")
@DisplayName("Mongo Solution Repository Integration Tests")
class MongoSolutionRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private MongoSolutionRepository solutionRepository;

    private UUID taskId;
    private UUID solutionId;
    private String codeHash;

    @BeforeEach
    void setUp() {
        solutionRepository.deleteAll();
        taskId = UUID.randomUUID();
        solutionId = UUID.randomUUID();
        codeHash = "abcdef1234567890";
    }

    @Test
    @DisplayName("Should save and find solution by ID")
    void shouldSaveAndFindBySolutionId() {
        SolutionSource source = SolutionSource.builder()
                .solutionId(solutionId)
                .taskId(taskId)
                .language(ProgrammingLanguage.JAVA)
                .code("public class Test { }")
                .codeHash(codeHash)
                .normalizedCode("public class Test { }")
                .build();

        solutionRepository.save(source);

        Optional<SolutionSource> found = solutionRepository.findBySolutionId(solutionId);

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("public class Test { }");
        assertThat(found.get().getCodeHash()).isEqualTo(codeHash);
    }

    @Test
    @DisplayName("Should find all by taskId, language and codeHash")
    void shouldFindByTaskIdAndLanguageAndCodeHash() {
        SolutionSource source1 = SolutionSource.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .language(ProgrammingLanguage.JAVA)
                .codeHash(codeHash)
                .build();

        SolutionSource source2 = SolutionSource.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .language(ProgrammingLanguage.JAVA)
                .codeHash(codeHash)
                .build();

        SolutionSource source3 = SolutionSource.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .language(ProgrammingLanguage.PYTHON)
                .codeHash(codeHash)
                .build();

        solutionRepository.saveAll(List.of(source1, source2, source3));

        List<SolutionSource> results = solutionRepository.findAllByTaskIdAndLanguageAndCodeHash(
                taskId, ProgrammingLanguage.JAVA, codeHash);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(s -> s.getLanguage() == ProgrammingLanguage.JAVA);
    }

    @Test
    @DisplayName("Should find all by taskId and language")
    void shouldFindByTaskIdAndLanguage() {
        SolutionSource source1 = SolutionSource.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .language(ProgrammingLanguage.JAVA)
                .build();

        SolutionSource source2 = SolutionSource.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .language(ProgrammingLanguage.JAVA)
                .build();

        SolutionSource source3 = SolutionSource.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId)
                .language(ProgrammingLanguage.PYTHON)
                .build();

        solutionRepository.saveAll(List.of(source1, source2, source3));

        List<SolutionSource> results = solutionRepository.findAllByTaskIdAndLanguage(
                taskId, ProgrammingLanguage.JAVA);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty list when no matches")
    void shouldReturnEmptyWhenNoMatches() {
        List<SolutionSource> results = solutionRepository.findAllByTaskIdAndLanguage(
                UUID.randomUUID(), ProgrammingLanguage.JAVA);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle compound index correctly")
    void shouldHandleCompoundIndex() {
        UUID taskId1 = UUID.randomUUID();
        UUID taskId2 = UUID.randomUUID();

        SolutionSource source = SolutionSource.builder()
                .solutionId(UUID.randomUUID())
                .taskId(taskId1)
                .language(ProgrammingLanguage.JAVA)
                .build();

        solutionRepository.save(source);

        List<SolutionSource> forTask1 = solutionRepository.findAllByTaskIdAndLanguage(taskId1, ProgrammingLanguage.JAVA);
        List<SolutionSource> forTask2 = solutionRepository.findAllByTaskIdAndLanguage(taskId2, ProgrammingLanguage.JAVA);
        List<SolutionSource> forWrongLanguage = solutionRepository.findAllByTaskIdAndLanguage(taskId1, ProgrammingLanguage.PYTHON);

        assertThat(forTask1).hasSize(1);
        assertThat(forTask2).isEmpty();
        assertThat(forWrongLanguage).isEmpty();
    }
}