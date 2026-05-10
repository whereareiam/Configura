package me.whereareiam.configura.common.duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Duration Format")
class DurationFormatTest {
	@Test
	@DisplayName("Parses human readable durations")
	void parsesHumanReadableDurations() {
		assertEquals(Duration.ofMinutes(10), DurationFormat.parse("10m"));
		assertEquals(Duration.ofHours(2).plusMinutes(30), DurationFormat.parse("2h30m"));
		assertEquals(Duration.ofDays(1).plusSeconds(5).plusMillis(250), DurationFormat.parse("1d5s250ms"));
	}

	@Test
	@DisplayName("Keeps bare numbers as minutes")
	void keepsBareNumbersAsMinutes() {
		assertEquals(Duration.ofMinutes(15), DurationFormat.parse("15"));
	}

	@Test
	@DisplayName("Accepts ISO durations")
	void acceptsIsoDurations() {
		assertEquals(Duration.ofMinutes(10), DurationFormat.parse("PT10M"));
	}

	@Test
	@DisplayName("Formats durations for config files")
	void formatsDurationsForConfigFiles() {
		assertEquals("10m", DurationFormat.format(Duration.ofMinutes(10)));
		assertEquals("2h30m", DurationFormat.format(Duration.ofHours(2).plusMinutes(30)));
		assertEquals("1d5s250ms", DurationFormat.format(Duration.ofDays(1).plusSeconds(5).plusMillis(250)));
		assertEquals("0m", DurationFormat.format(Duration.ZERO));
	}

	@Test
	@DisplayName("Preserves null and rejects invalid text")
	void preservesNullAndRejectsInvalidText() {
		assertNull(DurationFormat.parse(null));
		assertNull(DurationFormat.parse(" "));
		assertNull(DurationFormat.format(null));
		assertThrows(IllegalArgumentException.class, () -> DurationFormat.parse("soon"));
	}
}
