package com.technokratos.taskservice.service;

import com.technokratos.common.dto.TaskAdminView;
import com.technokratos.common.dto.TaskPublicView;
import com.technokratos.common.dto.TaskShortView;
import com.technokratos.common.enums.TaskDifficulty;
import com.technokratos.common.enums.TaskStatus;
import com.technokratos.taskservice.dto.request.AdminTaskCreateRequest;
import com.technokratos.taskservice.dto.request.AdminTaskUpdateRequest;
import com.technokratos.taskservice.dto.request.TestCaseRequest;
import com.technokratos.taskservice.exception.TaskNotFoundException;
import com.technokratos.taskservice.mapper.TaskMapper;
import com.technokratos.taskservice.model.Tag;
import com.technokratos.taskservice.model.Task;
import com.technokratos.taskservice.model.TestCase;
import com.technokratos.taskservice.repository.TagRepository;
import com.technokratos.taskservice.repository.TaskRepository;
import com.technokratos.taskservice.repository.TaskSpecifications;
import com.technokratos.taskservice.repository.TaskUserStatsRepository;
import com.technokratos.taskservice.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final TaskUserStatsRepository taskUserStatsRepository;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "'public:' + #id")
    public TaskPublicView getPublicById(UUID id) {
        Task t = taskRepository.findById(id).orElseThrow(() -> TaskNotFoundException.byId(id));
        if (t.getStatus() != TaskStatus.PUBLISHED) {
            throw TaskNotFoundException.byId(id);
        }
        return taskMapper.toPublicView(t);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "'admin:' + #id")
    public TaskAdminView getAdminById(UUID id) {
        return taskMapper.toAdminView(taskRepository.findById(id)
                .orElseThrow(() -> TaskNotFoundException.byId(id)));
    }

    @Transactional(readOnly = true)
    public Page<TaskShortView> listPublic(TaskDifficulty difficulty, List<String> tagSlugs, Pageable pageable) {
        Specification<Task> spec = TaskSpecifications.hasStatus(TaskStatus.PUBLISHED)
                .and(TaskSpecifications.hasDifficulty(difficulty))
                .and(TaskSpecifications.hasAllTags(tagSlugs));
        return taskRepository.findAll(spec, pageable).map(taskMapper::toShortView);
    }

    @Transactional(readOnly = true)
    public Page<TaskAdminView> listAdmin(TaskStatus status, TaskDifficulty difficulty,
                                         List<String> tagSlugs, Pageable pageable) {
        Specification<Task> spec = TaskSpecifications.hasStatus(status)
                .and(TaskSpecifications.hasDifficulty(difficulty))
                .and(TaskSpecifications.hasAllTags(tagSlugs));

        return taskRepository.findAll(spec, pageable).map(taskMapper::toAdminView);
    }

    @Transactional(readOnly = true)
    public Map<UUID, TaskShortView> shortViewBatch(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return taskRepository.findAllByIdWithTagsAndTestCases(ids).stream()
                .collect(Collectors.toMap(Task::getId, taskMapper::toShortView));
    }

    @Transactional
    public TaskAdminView create(AdminTaskCreateRequest req, UUID authorId) {
        Task task = Task.builder()
                .title(req.title())
                .slug(buildUniqueSlug(req.title()))
                .condition(req.condition())
                .difficulty(req.difficulty())
                .type(req.type())
                .status(TaskStatus.DRAFT)
                .points(req.points())
                .timeLimitMs(req.timeLimitMs())
                .memoryLimitMb(req.memoryLimitMb())
                .authorId(authorId)
                .build();

        task.setTags(resolveTags(req.tagSlugs()));

        for (TestCaseRequest tc : req.testCases()) {
            task.getTestCases().add(taskMapper.toEntity(tc, task));
        }

        Task saved = taskRepository.saveAndFlush(task);
        log.info("Created task id={} slug={} by author={}", saved.getId(), saved.getSlug(), authorId);
        return taskMapper.toAdminView(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "'public:' + #id"),
            @CacheEvict(value = "tasks", key = "'admin:' + #id")
    })
    public TaskAdminView update(UUID id, AdminTaskUpdateRequest req) {
        Task task = taskRepository.findById(id).orElseThrow(() -> TaskNotFoundException.byId(id));

        if (req.title() != null) task.setTitle(req.title());
        if (req.condition() != null) task.setCondition(req.condition());
        if (req.difficulty() != null) task.setDifficulty(req.difficulty());
        if (req.status() != null) task.setStatus(req.status());
        if (req.points() != null) task.setPoints(req.points());
        if (req.timeLimitMs() != null) task.setTimeLimitMs(req.timeLimitMs());
        if (req.memoryLimitMb() != null) task.setMemoryLimitMb(req.memoryLimitMb());
        if (req.tagSlugs() != null) task.setTags(resolveTags(req.tagSlugs()));

        log.info("Task {} updated successfully", id);
        return taskMapper.toAdminView(task);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "'public:' + #id"),
            @CacheEvict(value = "tasks", key = "'admin:' + #id")
    })
    public TaskAdminView publish(UUID id, boolean publish) {
        Task task = taskRepository.findById(id).orElseThrow(() -> TaskNotFoundException.byId(id));
        task.setStatus(publish ? TaskStatus.PUBLISHED : TaskStatus.DRAFT);
        log.info("Task {} status set to {}", id, task.getStatus());
        return taskMapper.toAdminView(task);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "'public:' + #id"),
            @CacheEvict(value = "tasks", key = "'admin:' + #id")
    })
    public void delete(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw TaskNotFoundException.byId(id);
        }
        taskRepository.deleteById(id);
        log.info("Deleted task id={}", id);
    }

    private String buildUniqueSlug(String title) {
        String base = SlugUtil.slugify(title);
        String candidate = base;
        int n = 2;
        while (taskRepository.existsBySlug(candidate)) {
            candidate = base + "-" + n++;
        }
        return candidate;
    }

    private Set<Tag> resolveTags(Set<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return new HashSet<>();
        }
        Set<Tag> found = tagRepository.findAllBySlugIn(slugs);
        if (found.size() != slugs.size()) {
            Set<String> foundSlugs = found.stream()
                    .map(Tag::getSlug)
                    .collect(Collectors.toSet());
            Set<String> missing = new HashSet<>(slugs);
            missing.removeAll(foundSlugs);
            throw new IllegalArgumentException("Unknown tag slugs: " + missing);
        }
        return new HashSet<>(found);
    }
}

