package com.hackathon.team4.passport;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

@Embeddable
public class MemberAllergy {

    @Enumerated(EnumType.STRING)
    @Column(name = "allergen", nullable = false)
    private Allergen allergen;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    protected MemberAllergy() {
    }

    public MemberAllergy(Allergen allergen, Severity severity) {
        this.allergen = Objects.requireNonNull(allergen);
        this.severity = Objects.requireNonNull(severity);
    }

    public Allergen allergen() {
        return allergen;
    }

    public Severity severity() {
        return severity;
    }

    /** One entry per allergen: equality ignores severity, so a set keeps a member's allergen once. */
    @Override
    public boolean equals(Object other) {
        return other instanceof MemberAllergy that && allergen == that.allergen;
    }

    @Override
    public int hashCode() {
        return allergen.hashCode();
    }
}
