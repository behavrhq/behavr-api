package net.behavr.collector.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class BehavrCollectorProperties {

	private int maxBatchSize = 100;

	private List<String> allowedEventTypes = new ArrayList<>(
			List.of(
					"page_view",
					"search",
					"product_view",
					"product_click",
					"add_to_cart",
					"checkout_start",
					"purchase"));
}
