package com.epam.digital.data.platform.excerpt.api.repository;

import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface RecordRepository extends CrudRepository<ExcerptRecord, UUID> {

}
