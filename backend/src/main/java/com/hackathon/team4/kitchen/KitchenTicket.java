package com.hackathon.team4.kitchen;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One round sent to the kitchen ({@code pos.check.round_sent}), checked against the passport. The kitchen is
 * notified only: a ticket is never acknowledged or approved.
 */
@Entity
@Table(name = "kitchen_ticket")
public class KitchenTicket {

    @Id
    private UUID id;

    @Column(name = "rvc", nullable = false)
    private int rvc;

    @Column(name = "check_number", nullable = false)
    private String checkNumber;

    @Column(name = "check_seq", nullable = false)
    private String checkSeq;

    @Column(name = "round_number", nullable = false)
    private int round;

    @Column(name = "table_name")
    private String table;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "room_number")
    private String roomNumber;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private TicketSource source;

    @Column(name = "has_allergens", nullable = false)
    private boolean hasAllergens;

    @ElementCollection
    @CollectionTable(name = "kitchen_ticket_line", joinColumns = @JoinColumn(name = "ticket_id"))
    @OrderColumn(name = "position")
    private List<TicketLine> lines = new ArrayList<>();

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KitchenTicket() {
    }

    public KitchenTicket(int rvc, String checkNumber, String checkSeq, int round, String table, String reservationId,
            String roomNumber, String guestName, Instant sentAt, TicketSource source, List<TicketLine> lines,
            boolean hasAllergens, Instant now) {
        this.id = UUID.randomUUID();
        this.rvc = rvc;
        this.checkNumber = checkNumber;
        this.checkSeq = checkSeq;
        this.round = round;
        this.table = table;
        this.reservationId = reservationId;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.sentAt = sentAt;
        this.source = source;
        this.lines.addAll(lines);
        this.hasAllergens = hasAllergens;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public int rvc() {
        return rvc;
    }

    public String checkNumber() {
        return checkNumber;
    }

    public String checkSeq() {
        return checkSeq;
    }

    public int round() {
        return round;
    }

    public String table() {
        return table;
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

    public Instant sentAt() {
        return sentAt;
    }

    public TicketSource source() {
        return source;
    }

    /** At least one line contains an allergen of someone at the table (or any allergen when unlinked). */
    public boolean hasAllergens() {
        return hasAllergens;
    }

    public List<TicketLine> lines() {
        return List.copyOf(lines);
    }

}
