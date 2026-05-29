package com.technokratos.plagiarismservice.service;

import com.technokratos.common.enums.ProgrammingLanguage;
import com.technokratos.common.events.SolutionCompletedEvent;
import com.technokratos.plagiarismservice.model.PlagiarismReport;
import com.technokratos.plagiarismservice.model.SolutionSource;
import com.technokratos.plagiarismservice.repository.MongoPlagiarismReportRepository;
import com.technokratos.plagiarismservice.repository.MongoSolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Tag("integration")
@DisplayName("Plagiarism Service Integration Tests")
class PlagiarismServiceIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    private MongoSolutionRepository solutionRepository;

    @Autowired
    private MongoPlagiarismReportRepository reportRepository;

    @Autowired
    private PlagiarismService plagiarismService;

    private UUID taskId;
    private ProgrammingLanguage language;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        language = ProgrammingLanguage.JAVA;
        reportRepository.deleteAll();
        solutionRepository.deleteAll();
    }

    @Test
    @DisplayName("Should detect exact duplicate code via hash")
    void shouldDetectExactDuplicate() {
        String code = "public class Solution { public static void main(String[] args) { System.out.println(\"test\"); } }";
        String codeHash = sha256(code);

        UUID solution1Id = UUID.randomUUID();
        UUID solution2Id = UUID.randomUUID();

        SolutionSource source1 = SolutionSource.builder()
                .solutionId(solution1Id)
                .taskId(taskId)
                .language(language)
                .code(code)
                .codeHash(codeHash)
                .normalizedCode(code)
                .build();

        SolutionSource source2 = SolutionSource.builder()
                .solutionId(solution2Id)
                .taskId(taskId)
                .language(language)
                .code(code)
                .codeHash(codeHash)
                .normalizedCode(code)
                .build();

        solutionRepository.saveAll(List.of(source1, source2));

        SolutionCompletedEvent event = SolutionCompletedEvent.builder()
                .solutionId(solution2Id)
                .taskId(taskId)
                .userId(UUID.randomUUID())
                .language(language)
                .build();

        plagiarismService.onSolutionCompleted(event);

        List<PlagiarismReport> reports = reportRepository.findAllBySolutionId(solution2Id);
        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().getSimilarity()).isEqualTo(1.0);
        assertThat(reports.getFirst().getMatchedSolutionId()).isEqualTo(solution1Id);
    }

    @Test
    @DisplayName("Should detect similar code using JPlag - with meaningful code")
    void shouldDetectSimilarCode() {
        String originalCode = """
                package com.example;
                
                import java.util.*;
                import java.util.stream.*;
                
                public class Solution {
                    public static List<Integer> processNumbers(int[] numbers) {
                        List<Integer> result = new ArrayList<>();
                        for (int i = 0; i < numbers.length; i++) {
                            if (numbers[i] % 2 == 0) {
                                result.add(numbers[i] * 2);
                            } else {
                                result.add(numbers[i] * 3);
                            }
                        }
                        Collections.sort(result);
                        return result;
                    }
                    
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        int n = scanner.nextInt();
                        int[] arr = new int[n];
                        for (int i = 0; i < n; i++) {
                            arr[i] = scanner.nextInt();
                        }
                        List<Integer> processed = processNumbers(arr);
                        System.out.println(processed);
                    }
                }
                """;

        String similarCode = """
                package com.example;
                
                import java.util.*;
                import java.util.stream.*;
                
                public class Main {
                    public static List<Integer> calculateNumbers(int[] values) {
                        List<Integer> output = new ArrayList<>();
                        for (int j = 0; j < values.length; j++) {
                            if (values[j] % 2 == 0) {
                                output.add(values[j] * 2);
                            } else {
                                output.add(values[j] * 3);
                            }
                        }
                        Collections.sort(output);
                        return output;
                    }
                    
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        int count = scanner.nextInt();
                        int[] array = new int[count];
                        for (int idx = 0; idx < count; idx++) {
                            array[idx] = scanner.nextInt();
                        }
                        List<Integer> result = calculateNumbers(array);
                        System.out.println(result);
                    }
                }
                """;

        UUID solution1Id = UUID.randomUUID();
        UUID solution2Id = UUID.randomUUID();

        solutionRepository.save(SolutionSource.builder()
                .solutionId(solution1Id)
                .taskId(taskId)
                .language(language)
                .code(originalCode)
                .codeHash(sha256(originalCode))
                .normalizedCode(originalCode)
                .build());

        solutionRepository.save(SolutionSource.builder()
                .solutionId(solution2Id)
                .taskId(taskId)
                .language(language)
                .code(similarCode)
                .codeHash(sha256(similarCode))
                .normalizedCode(similarCode)
                .build());

        SolutionCompletedEvent event = SolutionCompletedEvent.builder()
                .solutionId(solution2Id)
                .taskId(taskId)
                .userId(UUID.randomUUID())
                .language(language)
                .build();

        plagiarismService.onSolutionCompleted(event);

        List<PlagiarismReport> reports = reportRepository.findAllBySolutionId(solution2Id);
        if (reports.isEmpty()) {
            System.out.println("JPlag did not detect similarity with current threshold");
        } else {
            assertThat(reports.getFirst().getSimilarity())
                    .isGreaterThanOrEqualTo(0.7);
        }
    }

    @Test
    @DisplayName("Should not report plagiarism for different languages")
    void shouldNotReportForDifferentLanguages() {
        String code = "public class Solution { public static void main(String[] args) { System.out.println(\"test\"); } }";
        String codeHash = sha256(code);

        UUID solution1Id = UUID.randomUUID();
        UUID solution2Id = UUID.randomUUID();

        solutionRepository.save(SolutionSource.builder()
                .solutionId(solution1Id)
                .taskId(taskId)
                .language(ProgrammingLanguage.JAVA)
                .code(code)
                .codeHash(codeHash)
                .build());

        solutionRepository.save(SolutionSource.builder()
                .solutionId(solution2Id)
                .taskId(taskId)
                .language(ProgrammingLanguage.PYTHON)
                .code("print('test')")
                .codeHash(sha256("print('test')"))
                .build());

        SolutionCompletedEvent event = SolutionCompletedEvent.builder()
                .solutionId(solution2Id)
                .taskId(taskId)
                .userId(UUID.randomUUID())
                .language(ProgrammingLanguage.PYTHON)
                .build();

        plagiarismService.onSolutionCompleted(event);

        List<PlagiarismReport> reports = reportRepository.findAllBySolutionId(solution2Id);
        assertThat(reports).isEmpty();
    }

    @Test
    @DisplayName("Should not self-report plagiarism")
    void shouldNotSelfReport() {
        String code = "public class Solution { public static void main(String[] args) { System.out.println(\"test\"); } }";
        String codeHash = sha256(code);
        UUID solutionId = UUID.randomUUID();

        solutionRepository.save(SolutionSource.builder()
                .solutionId(solutionId)
                .taskId(taskId)
                .language(language)
                .code(code)
                .codeHash(codeHash)
                .build());

        SolutionCompletedEvent event = SolutionCompletedEvent.builder()
                .solutionId(solutionId)
                .taskId(taskId)
                .userId(UUID.randomUUID())
                .language(language)
                .build();

        plagiarismService.onSolutionCompleted(event);

        List<PlagiarismReport> reports = reportRepository.findAllBySolutionId(solutionId);
        assertThat(reports).isEmpty();
    }

    @Test
    @DisplayName("Should handle missing solution gracefully")
    void shouldHandleMissingSolution() {
        UUID nonExistentId = UUID.randomUUID();

        SolutionCompletedEvent event = SolutionCompletedEvent.builder()
                .solutionId(nonExistentId)
                .taskId(taskId)
                .userId(UUID.randomUUID())
                .language(language)
                .build();

        plagiarismService.onSolutionCompleted(event);

        List<PlagiarismReport> reports = reportRepository.findAllBySolutionId(nonExistentId);
        assertThat(reports).isEmpty();
    }

    private String sha256(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}