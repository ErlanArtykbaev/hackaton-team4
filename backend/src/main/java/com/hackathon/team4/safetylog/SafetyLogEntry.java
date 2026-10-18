package com.hackathon.team4.safetylog;

import com.hackathon.team4.allergy.Allergen;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

    @Column(name = "member_name")
    private String memberName;

    @Column(name = "item")
    private String item;

    @Enumerated(EnumType.STRING)
    @Column(name = "allergen")
    private Allergen allergen;

    @Column(name = "actor")
    private String actor;

    @Column(name = "reason")
    private String reason;

    @Column(name = "minutes_to_ack")
    private BigDecimal minutesToAck;

    @Column(name = "ticket_id")
    private UUID ticketId;

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
        this.memberName = builder.memberName;
        this.item = builder.item;
        this.allergen = builder.allergen;
        this.actor = builder.actor;
        this.reason = builder.reason;
        this.minutesToAck = builder.minutesToAck;
        this.ticketId = builder.ticketId;
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

    public String memberName() {
        return memberName;
    }

    public String item() {
        return item;
    }

    public Allergen allergen() {
        return allergen;
    }

    public String actor() {
        return actor;
    }

    public String reason() {
        return reason;
    }

    public BigDecimal minutesToAck() {
        return minutesToAck;
    }

    public UUID ticketId() {
        return ticketId;
    }

    /** Most entries fill only a few fields, so they are built by name. */
    public static final class Builder {

        private final LogEntryType type;
        private final Instant at;
        private Integer rvc;
        private String outletName;
        private String reservationId;
        private String roomNumber;
        private String memberName;
        private String item;
        private Allergen allergen;
        private String actor;
        private String reason;
        private BigDecimal minutesToAck;
        private UUID ticketId;

        private Builder(LogEntryType type, Instant at) {
            this.type = type;
            this.at = at;
        }

        public Builder outlet(Integer rvc, String outletName) {
            this.rvc = rvc;
            this.outletName = outletName;
            return this;
        }

        public Builder reservation(String reservationId, String roomNumber) {
            this.reservationId = reservationId;
            this.roomNumber = roomNumber;
            return this;
        }

        public Builder member(String memberName) {
            this.memberName = memberName;
            return this;
        }

        public Builder item(String item, Allergen allergen) {
            this.item = item;
            this.allergen = allergen;
            return this;
        }

        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder minutesToAck(BigDecimal minutesToAck) {
            this.minutesToAck = minutesToAck;
            return this;
        }

        public Builder ticket(UUID ticketId) {
            this.ticketId = ticketId;
            return this;
        }

        public SafetyLogEntry build() {
            return new SafetyLogEntry(this);
        }
    }
}
