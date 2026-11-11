package com.smarthealth.hms.dto;

import com.smarthealth.hms.domain.Department;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DepartmentForm {

    private Long id;

    @NotBlank(message = "Department name is required")
    @Size(max = 100, message = "Maximum 100 characters")
    private String name;

    @Size(max = 255, message = "Maximum 255 characters")
    private String description;

    public static DepartmentForm from(Department d) {
        DepartmentForm f = new DepartmentForm();
        f.id = d.getId();
        f.name = d.getName();
        f.description = d.getDescription();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
