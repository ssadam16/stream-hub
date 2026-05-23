package com.technokratos.taskservice.mapper;

import com.technokratos.common.dto.TagDto;
import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskPublicView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.common.dto.TaskStats;
import com.technokratos.common.dto.tests.TestCaseFull;
import com.technokratos.common.dto.tests.TestCasePublic;
import com.technokratos.taskservice.dto.request.TestCaseRequest;
import com.technokratos.taskservice.model.Tag;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.model.TestCase;
import com.technokratos.taskservice.repository.TaskUserStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final TagMapper tagMapper;
    private final TaskUserStatsRepository taskUserStatsRepository;

    public TaskAdminView toAdminView(Task t) {
        return TaskAdminView.builder()
                .id(t.getId())
                .title(t.getTitle())
                .slug(t.getSlug())
                .condition(t.getCondition())
                .difficulty(t.getDifficulty())
                .status(t.getStatus())
                .type(t.getType())
                .points(t.getPoints())
                .timeLimitMs(t.getTimeLimitMs())
                .memoryLimitMb(t.getMemoryLimitMb())
                .authorId(t.getAuthorId())
                .stats(stats(t))
                .tags(tags(t.getTags()))
                .testCases(toFullTestCases(t.getTestCases()))
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    public TaskPublicView toPublicView(Task t) {
        return TaskPublicView.builder()
                .id(t.getId())
                .title(t.getTitle())
                .slug(t.getSlug())
                .condition(t.getCondition())
                .difficulty(t.getDifficulty())
                .type(t.getType())
                .points(t.getPoints())
                .timeLimitMs(t.getTimeLimitMs())
                .memoryLimitMb(t.getMemoryLimitMb())
                .stats(stats(t))
                .tags(tags(t.getTags()))
                .examples(toPublicTestCases(t.getTestCases()))
                .createdAt(t.getCreatedAt())
                .build();
    }

    public TaskShortView toShortView(Task t) {
        return TaskShortView.builder()
                .id(t.getId())
                .title(t.getTitle())
                .slug(t.getSlug())
                .difficulty(t.getDifficulty())
                .points(t.getPoints())
                .stats(stats(t))
                .tags(tags(t.getTags()))
                .build();
    }

    /** Full test set including hidden — used for code execution. */
    public List<TestCasePublic> toExecutorPayload(List<TestCase> cases) {
        if (cases == null || cases.isEmpty()) {
            return Collections.emptyList();
        }
        List<TestCasePublic> out = new ArrayList<>(cases.size());
        for (TestCase c : cases) {
            out.add(TestCasePublic.builder()
                    .id(c.getId())
                    .input(c.getInput())
                    .expectedOutput(c.getExpectedOutput())
                    .build());
        }
        return out;
    }

    public TestCase toEntity(TestCaseRequest req, Task owner) {
        return TestCase.builder()
                .task(owner)
                .input(req.input())
                .expectedOutput(req.expectedOutput())
                .isHidden(req.isHidden())
                .explanation(req.explanation())
                .build();
    }

    // --- helpers ---

    private TaskStats stats(Task t) {
        return taskUserStatsRepository.getTaskStatsByTaskId(t.getId());
    }

    private List<TagDto> tags(Set<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream()
                .sorted(Comparator.comparing(Tag::getName))
                .map(tagMapper::toDto)
                .toList();
    }

    private List<TestCasePublic> toPublicTestCases(List<TestCase> cases) {
        if (cases == null) {
            return Collections.emptyList();
        }
        List<TestCasePublic> out = new ArrayList<>();
        for (TestCase c : cases) {
            if (!c.isHidden()) {
                out.add(TestCasePublic.builder()
                        .id(c.getId())
                        .input(c.getInput())
                        .expectedOutput(c.getExpectedOutput())
                        .build());
            }
        }
        return out;
    }

    private List<TestCaseFull> toFullTestCases(List<TestCase> cases) {
        if (cases == null) {
            return Collections.emptyList();
        }
        List<TestCaseFull> out = new ArrayList<>(cases.size());
        for (TestCase c : cases) {
            out.add(TestCaseFull.builder()
                    .id(c.getId())
                    .input(c.getInput())
                    .expectedOutput(c.getExpectedOutput())
                    .isHidden(c.isHidden())
                    .explanation(c.getExplanation())
                    .build());
        }
        return out;
    }
}
