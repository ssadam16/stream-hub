package com.technokratos.taskservice.repository;

import com.technokratos.common.enums.TagCategory;
import com.technokratos.taskservice.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    List<Tag> findAllByCategory(TagCategory category);

    Set<Tag> findAllBySlugIn(Set<String> slugs);

    Set<Tag> findAllByIdIn(Set<UUID> ids);
}
