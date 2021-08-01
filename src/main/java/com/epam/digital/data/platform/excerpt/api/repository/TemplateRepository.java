package com.epam.digital.data.platform.excerpt.api.repository;

import com.epam.digital.data.platform.excerpt.dao.ExcerptTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface TemplateRepository extends CrudRepository<ExcerptTemplate, UUID> {

  Optional<ExcerptTemplate> findFirstByTemplateName(String templateName);

}
