package com.hackathon.team4.passport;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A person of the party: the PMS keeps one profile per reservation, the family lives here.
 */
@Entity
@Table(name = "passport_member")
public class PassportMember {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passport_id", nullable = false)
    private Passport passport;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "age")
    private Integer age;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "position", nullable = false)
    private int position;

    @ElementCollection
    @CollectionTable(name = "passport_member_allergy", joinColumns = @JoinColumn(name = "member_id"))
    private Set<MemberAllergy> allergies = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PassportMember() {
    }

    PassportMember(UUID id, Passport passport, int position, Instant now) {
        this.id = id;
        this.passport = passport;
        this.position = position;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, Integer age, boolean primary, Collection<MemberAllergy> allergies, Instant now) {
        this.name = name;
        this.age = age;
        this.primary = primary;
        this.allergies.clear();
        this.allergies.addAll(allergies);
        this.updatedAt = now;
    }

    void moveTo(int position) {
        this.position = position;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Integer age() {
        return age;
    }

    public boolean primary() {
        return primary;
    }

    public int position() {
        return position;
    }

    public Set<MemberAllergy> allergies() {
        return Set.copyOf(allergies);
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
