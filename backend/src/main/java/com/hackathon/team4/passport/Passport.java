package com.hackathon.team4.passport;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The allergy passport of one reservation: its members and their allergies.
 */
@Entity
@Table(name = "passport")
public class Passport {

    @Id
    private UUID id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private String reservationId;

    @Column(name = "room_number")
    private String roomNumber;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "notes")
    private String notes;

    @Column(name = "updated_by")
    private String updatedBy;

    @OneToMany(mappedBy = "passport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private Set<PassportMember> members = new LinkedHashSet<>();

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Passport() {
    }

    public Passport(String reservationId, Instant now) {
        this.id = UUID.randomUUID();
        this.reservationId = reservationId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void describe(String roomNumber, String guestName, String notes, String updatedBy, Instant now) {
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.notes = notes;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }

    /** Adds a new member at the end of the list; the caller fills it with {@link PassportMember#update}. */
    public PassportMember addMember(Instant now) {
        return addMember(UUID.randomUUID(), now);
    }

    /** Adds a member with a known id, e.g. the default primary guest shown before the first save. */
    public PassportMember addMember(UUID id, Instant now) {
        PassportMember member = new PassportMember(id, this, members.size(), now);
        members.add(member);
        return member;
    }

    public Optional<PassportMember> member(UUID memberId) {
        return members.stream().filter(member -> member.id().equals(memberId)).findFirst();
    }

    /** Keeps only the given members, in the given order. */
    public void keepMembers(List<PassportMember> kept) {
        members.retainAll(kept);
        for (PassportMember member : members) {
            member.moveTo(kept.indexOf(member));
        }
    }

    public UUID id() {
        return id;
    }

    public String reservationId() {
        return reservationId;
    }

    public String roomNumber() {
        return roomNumber;
    }

    public String guestName() {
        return guestName;
    }

    public String notes() {
        return notes;
    }

    public String updatedBy() {
        return updatedBy;
    }

    /** Members in their display order. */
    public List<PassportMember> members() {
        return members.stream().sorted(Comparator.comparingInt(PassportMember::position)).toList();
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
