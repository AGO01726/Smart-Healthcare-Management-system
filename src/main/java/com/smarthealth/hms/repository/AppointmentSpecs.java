package com.smarthealth.hms.repository;

import com.smarthealth.hms.domain.Appointment;
import com.smarthealth.hms.domain.AppointmentStatus;
import com.smarthealth.hms.domain.Patient;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Dynamic filters for the appointment list (all parameters optional). */
public final class AppointmentSpecs {

    private AppointmentSpecs() {
    }

    public static Specification<Appointment> filter(String q, AppointmentStatus status, Long doctorId,
                                                    Long patientId, LocalDate date) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (doctorId != null) {
                predicates.add(cb.equal(root.get("doctor").get("id"), doctorId));
            }
            if (patientId != null) {
                predicates.add(cb.equal(root.get("patient").get("id"), patientId));
            }
            if (date != null) {
                predicates.add(cb.equal(root.get("appointmentDate"), date));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Join<Appointment, Patient> patient = root.join("patient", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(patient.get("firstName")), like),
                        cb.like(cb.lower(patient.get("lastName")), like),
                        cb.like(cb.lower(patient.get("patientCode")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
