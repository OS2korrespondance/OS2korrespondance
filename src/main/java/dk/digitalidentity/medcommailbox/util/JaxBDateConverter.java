package dk.digitalidentity.medcommailbox.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class JaxBDateConverter {

	static final DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	static final DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
	static final DateTimeFormatter tf = DateTimeFormatter.ISO_LOCAL_TIME;

	public static LocalDateTime parseDateTime(String s) {
		try {
			if (s.trim().isEmpty()) {
				return null;
			}
			return LocalDateTime.parse(s, dtf);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static String printDateTime(LocalDateTime d) {
		try {
			if (d == null) {
				return null;
			}
			return d.format(dtf);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static LocalDate parseDate(String s) {
		try {
			if (s.trim().isEmpty()) {
				return null;
			}
			return LocalDate.parse(s, df);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static String printDate(LocalDate d) {
		try {
			if (d == null) {
				return null;
			}
			return d.format(df);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static LocalTime parseTime(String s) {
		try {
			if (s.trim().isEmpty()) {
				return null;
			} else {
			}
			return LocalTime.parse(s, tf);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static String printTime(LocalTime d) {
		try {
			if (d == null) {
				return null;
			}
			return d.format(tf);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

}