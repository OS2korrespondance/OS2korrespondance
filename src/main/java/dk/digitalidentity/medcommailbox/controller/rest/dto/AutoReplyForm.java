package dk.digitalidentity.medcommailbox.controller.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AutoReplyForm(
	@NotNull String inboxEan,
	boolean enabled,
	String subject,
	String message,
	@JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
	@JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate
) {}
