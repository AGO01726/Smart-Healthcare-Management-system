package com.smarthealth.hms.repository;

import com.smarthealth.hms.domain.DiagnosticTest;
import com.smarthealth.hms.domain.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface DiagnosticTestRepository extends JpaRepository<DiagnosticTest, Long>, JpaSpecificationExecutor<DiagnosticTest> {

    long countByStatus(TestStatus status);

    long countByStatusIn(Collection<TestStatus> statuses);

    long countByRequestedByIdAndStatusIn(Long doctorId, Collection<TestStatus> statuses);

    long countByPatientId(Long patientId);

    long countByRequestedById(Long doctorId);

    boolean existsByRequestedByIdAndPatientId(Long doctorId, Long patientId);

    List<DiagnosticTest> findTop5ByPatientIdOrderByRequestedAtDesc(Long patientId);
}
