package com.mailshield.repository;

import com.mailshield.model.ClassificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassificationRepository extends JpaRepository<ClassificationRecord, Long> {

    List<ClassificationRecord> findAllByOrderByTimestampDesc();

    long countByPredictedLabelIgnoreCase(String label);
}
