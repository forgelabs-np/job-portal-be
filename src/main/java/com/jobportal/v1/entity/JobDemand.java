package com.jobportal.v1.entity;

import com.jobportal.v1.entity.master.Country;
import com.jobportal.v1.enums.GenderPreference;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.enums.SalaryPeriod;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_demands")
@Data
public class JobDemand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    private String city;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requirements;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    @Column(name = "filled_slots")
    private Integer filledSlots = 0;

    @Column(name = "remaining_slots")
    private Integer remainingSlots;

    @Column(name = "applied_count")
    private Integer appliedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.OPEN;

    @Column(name = "salary_amount", nullable = false)
    private Double salaryAmount;

    @Column(name = "salary_currency")
    private String salaryCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_period")
    private SalaryPeriod salaryPeriod = SalaryPeriod.MONTHLY;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference", nullable = false)
    private GenderPreference genderPreference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_nationalities", columnDefinition = "jsonb", nullable = false)
    private List<String> preferredNationalities = new ArrayList<>();

    @Column(name = "min_experience_years")
    private Integer minExperienceYears;

    @Column(name = "max_experience_years")
    private Integer maxExperienceYears;

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "contract_duration_years")
    private Integer contractDurationYears;

    @Column(name = "working_hours_per_week", nullable = false)
    private Integer workingHoursPerWeek;

    @Column(name = "overtime_policy", columnDefinition = "TEXT")
    private String overtimePolicy;

    @Column(name = "accommodation_provided")
    private Boolean accommodationProvided = false;

    @Column(name = "accommodation_details", columnDefinition = "TEXT")
    private String accommodationDetails;

    @Column(name = "food_provided")
    private Boolean foodProvided = false;

    @Column(name = "food_details", columnDefinition = "TEXT")
    private String foodDetails;

    @Column(name = "transportation_provided")
    private Boolean transportationProvided = false;

    @Column(name = "transportation_details", columnDefinition = "TEXT")
    private String transportationDetails;

    @Column(name = "medical_insurance_provided")
    private Boolean medicalInsuranceProvided = false;

    @Column(name = "medical_insurance_details", columnDefinition = "TEXT")
    private String medicalInsuranceDetails;

    @Column(name = "air_ticket_provided")
    private Boolean airTicketProvided = false;

    @Column(name = "air_ticket_details", columnDefinition = "TEXT")
    private String airTicketDetails;

    @Column(name = "leave_policy", columnDefinition = "TEXT")
    private String leavePolicy;

    @Column(name = "probation_period_months")
    private Integer probationPeriodMonths;

    @Column(name = "termination_clause", columnDefinition = "TEXT")
    private String terminationClause;

    @Column(name = "additional_benefits", columnDefinition = "TEXT")
    private String additionalBenefits;

    @Column(name = "is_active")
    private Boolean isActive = true;

    private LocalDateTime deadline;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    @PreUpdate
    public void calculateRemainingSlots() {
        if (totalSlots != null && filledSlots != null) {
            this.remainingSlots = totalSlots - filledSlots;
        }
    }

    public boolean isOpen() {
        return JobStatus.OPEN.equals(status) && isActive &&
                (deadline == null || deadline.isAfter(LocalDateTime.now())) &&
                remainingSlots != null && remainingSlots > 0;
    }

    public void incrementFilledSlots() {
        this.filledSlots = (this.filledSlots == null ? 0 : this.filledSlots) + 1;
        calculateRemainingSlots();
        if (remainingSlots <= 0) {
            this.status = JobStatus.COMPLETED;
        }
    }

    public void decrementFilledSlots() {
        this.filledSlots = Math.max(0, (this.filledSlots == null ? 0 : this.filledSlots) - 1);
        calculateRemainingSlots();
        if (JobStatus.COMPLETED.equals(status) && remainingSlots > 0) {
            this.status = JobStatus.OPEN;
        }
    }

    public void incrementAppliedCount() {
        this.appliedCount = (this.appliedCount == null ? 0 : this.appliedCount) + 1;
    }

    public void decrementAppliedCount() {
        this.appliedCount = Math.max(0, (this.appliedCount == null ? 0 : this.appliedCount) - 1);
    }
}