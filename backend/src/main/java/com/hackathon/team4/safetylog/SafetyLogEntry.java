package com.hackathon.team4.safetylog;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One fact of the safety log. Entries are only appended, never changed.
 */
@Entity
@Table(name = "safety_log_entry")
public class SafetyLogEntry {

    @Id
    private UUID id;

    @Column(name = "at", nullable = false)
    private Instant at;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LogEntryType type;

    @Column(name = "rvc")
    private Integer rvc;

    @Column(name = "outlet_name")
    private String outletName;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "room_number")
    private String roomNumber;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "member_name")
    private String memberName;

    @Column(name = "item")
    private String item;

    @Enumerated(EnumType.STRING)
    @Column(name = "allergen")
    private Allergen allergen;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity;

    @Column(name = "actor")
    private String actor;

    /** What happened in words, e.g. the guest's consent statement. */
    @Column(name = "statement")
    private String statement;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SafetyLogEntry() {
    }

    private SafetyLogEntry(Builder builder) {
        this.id = UUID.randomUUID();
        this.at = builder.at;
        this.type = builder.type;
        this.rvc = builder.rvc;
        this.outletName = builder.outletName;
        this.reservationId = builder.reservationId;
        this.roomNumber = builder.roomNumber;
        this.guestName = builder.guestName;
        this.memberName = builder.memberName;
        this.item = builder.item;
        this.allergen = builder.allergen;
        this.severity = builder.severity;
        this.actor = builder.actor;
        this.statement = builder.statement;
        this.createdAt = builder.at;
        this.updatedAt = builder.at;
    }

    public static Builder of(LogEntryType type, Instant at) {
        return new Builder(type, at);
    }

    public UUID id() {
        return id;
    }

    public Instant at() {
        return at;
    }

    public LogEntryType type() {
        return type;
    }

    public Integer rvc() {
        return rvc;
    }

    public String outletName() {
        return outletName;
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

    public String memberName() {
        return memberName;
    }

    public String item() {
        return item;
    }

    public Allergen allergen() {
        return allergen;
    }

    public Severity severity() {
        return severity;
    }

    public String actor() {
        return actor;
    }

    public String statement() {
        return statement;
    }

    /** Most entries fill only a few fields, so they are built by name. */
    public static final class Builder {

        private final LogEntryType type;
        private final Instant at;
        private Integer rvc;
        private String outletName;
        private String reservationId;
        private String roomNumber;
        private String guestName;
        private String memberName;
        private String item;
        private Allergen allergen;
        private Severity severity;
        private String actor;
        private String statement;

        private Builder(LogEntryType type, Instant at) {
            this.type = type;
            this.at = at;
        }

        public Builder outlet(Integer rvc, String outletName) {
            this.rvc = rvc;
            this.outletName = outletName;
            return this;
        }

        public Builder reservation(String reservationId, String roomNumber, String guestName) {
            this.reservationId = reservationId;
            this.roomNumber = roomNumber;
            this.guestName = guestName;
            return this;
        }

        public Builder member(String memberName) {
            this.memberName = memberName;
            return this;
        }

        public Builder item(String item) {
            this.item = item;
            return this;
        }

        public Builder allergen(Allergen allergen, Severity severity) {
            this.allergen = allergen;
            this.severity = severity;
            return this;
        }

        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder statement(String statement) {
            this.statement = statement;
            return this;
        }

        public SafetyLogEntry build() {
            return new SafetyLogEntry(this);
        }
    }
}
