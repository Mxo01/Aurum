package com.backend.aurum.domain.asset.validation;

import java.time.Clock;
import java.time.LocalDate;

/**
 * The reference date is a calendar date chosen by a user whose timezone the server does not know,
 * so the comparison against the server's UTC clock tolerates the skew between the two instead of
 * rejecting a legitimate "today".
 */
public final class ReferenceDatePolicy {

	private static final long TIMEZONE_SKEW_DAYS = 1;

	private ReferenceDatePolicy() {}

	public static boolean isTooFarInTheFuture(LocalDate referenceDate, Clock clock) {
		return referenceDate.isAfter(LocalDate.now(clock).plusDays(TIMEZONE_SKEW_DAYS));
	}
}
