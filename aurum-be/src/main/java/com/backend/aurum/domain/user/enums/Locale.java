package com.backend.aurum.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Locale {
	EN_US("en-US"),
	FR("fr-FR"),
	IT("it-IT");

	private final String tag;

	Locale(String tag) {
		this.tag = tag;
	}

	@JsonValue
	public String getTag() {
		return tag;
	}

	@JsonCreator
	public static Locale fromTag(String tag) {
		for (Locale locale : values()) {
			if (locale.tag.equalsIgnoreCase(tag)) {
				return locale;
			}
		}
		throw new IllegalArgumentException("Unknown locale: " + tag);
	}
}
