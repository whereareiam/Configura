package me.whereareiam.configura;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import me.whereareiam.configura.annotation.DocumentVersion;

/**
 * Convenience base class for root config documents that persist schema version in {@code _version}.
 */
@Setter
@Getter
public abstract class ConfigDocument {
	@DocumentVersion
	@JsonProperty("_version")
	protected Integer version;
}
