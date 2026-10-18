package com.hackathon.team4.kitchen;

import com.hackathon.team4.allergen.Allergen;
import com.hackathon.team4.allergen.DangerFor;
import com.hackathon.team4.allergen.Verdict;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.List;

/**
 * One line of a kitchen ticket with its verdict against the party's passport.
 */
@Embeddable
public class TicketLine {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Convert(converter = TicketLineConverters.Strings.class)
    @Column(name = "condiments", nullable = false)
    private List<String> condiments;

    @Convert(converter = TicketLineConverters.Allergens.class)
    @Column(name = "allergens", nullable = false)
    private List<Allergen> allergens;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false)
    private Verdict verdict;

    @Convert(converter = TicketLineConverters.Dangers.class)
    @Column(name = "danger_for", nullable = false)
    private List<DangerFor> dangerFor;

    @Column(name = "for_member_name")
    private String forMemberName;

    @Column(name = "waiter_confirmed", nullable = false)
    private boolean waiterConfirmed;

    /** The name the guest typed when consenting; {@code null} when no consent was needed or given. */
    @Column(name = "consent_by")
    private String consentBy;

    protected TicketLine() {
    }

    public TicketLine(String name, int quantity, List<String> condiments, List<Allergen> allergens, Verdict verdict,
            List<DangerFor> dangerFor, String forMemberName, boolean waiterConfirmed, String consentBy) {
        this.name = name;
        this.quantity = quantity;
        this.condiments = List.copyOf(condiments);
        this.allergens = List.copyOf(allergens);
        this.verdict = verdict;
        this.dangerFor = List.copyOf(dangerFor);
        this.forMemberName = forMemberName;
        this.waiterConfirmed = waiterConfirmed;
        this.consentBy = consentBy;
    }

    public String name() {
        return name;
    }

    public int quantity() {
        return quantity;
    }

    public List<String> condiments() {
        return condiments;
    }

    public List<Allergen> allergens() {
        return allergens;
    }

    public Verdict verdict() {
        return verdict;
    }

    public List<DangerFor> dangerFor() {
        return dangerFor;
    }

    public String forMemberName() {
        return forMemberName;
    }

    public boolean waiterConfirmed() {
        return waiterConfirmed;
    }

    public String consentBy() {
        return consentBy;
    }
}
