package net.behavr.collector.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CollectorWebConfig {

	/**
	 * Permissive CORS for MVP / local SDK testing.
	 * TODO: restrict origins to registered customer domains in production.
	 */
	@Bean
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(
				List.of("http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000", "http://127.0.0.1:5173"));
		config.setAllowedMethods(List.of(HttpMethod.POST.name(), HttpMethod.OPTIONS.name()));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/v1/**", config);
		return new CorsWebFilter(source);
	}
}
