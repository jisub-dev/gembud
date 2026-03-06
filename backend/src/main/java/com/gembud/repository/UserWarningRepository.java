package com.gembud.repository;

import com.gembud.entity.UserWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for user warning history.
 */
@Repository
public interface UserWarningRepository extends JpaRepository<UserWarning, Long> {

    boolean existsByReportId(Long reportId);
}
