package net.behavr.collector.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "behavr")
public class BehavrProperties {

	@NestedConfigurationProperty
	private BehavrCollectorProperties collector = new BehavrCollectorProperties();

	@NestedConfigurationProperty
	private BehavrKafkaProperties kafka = new BehavrKafkaProperties();

	/**
	 * Site id to API key (see {@code X-Behavr-Site-Key}) for non-local profiles.
	 */
	private Map<String, String> sites = new LinkedHashMap<>();
}
