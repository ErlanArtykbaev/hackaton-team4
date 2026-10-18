package com.hackathon.team4.passport.dto;

import com.hackathon.team4.passport.PassportSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * The whole passport to save: members missing from the list are removed. {@code rvc} is the waiter's outlet.
 */
public record PassportUpdateRequest(
        @NotEmpty @Valid List<PassportMemberRequest> members,
        @NotBlank String updatedBy,
        @NotNull PassportSource source,
        Integer rvc) {
}
