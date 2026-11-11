package com.smarthealth.hms.repository;

import com.smarthealth.hms.domain.DiagnosticTest;
import com.smarthealth.hms.domain.Patient;
import com.smarthealth.hms.domain.TestStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Dynamic filters for the diagnostics list (all parameters optional). */
public final class DiagnosticSpecs {

    private DiagnosticSpecs() {
    }

    public static Specification<DiagnosticTest> filter(String q, TestStatus status, Long doctorId, Long patientId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (doctorId != null) {
                predicates.add(cb.equal(root.get("requestedBy").get("id"), doctorId));
            }
            if (patientId != null) {
                predicates.add(cb.equal(root.get("patient").get("id"), patientId));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Join<DiagnosticTest, Patient> patient = root.join("patient", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("testName")), like),
                        cb.like(cb.lower(patient.get("firstName")), like),
                        cb.like(cb.lower(patient.get("lastName")), like),
                        cb.like(cb.lower(patient.get("patientCode")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
