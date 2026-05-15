package net.behavr.collector.support;

import java.net.InetSocketAddress;
import net.behavr.collector.model.ServerContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public final class ServerContextResolver {

	private ServerContextResolver() {}

	public static ServerContext resolve(ServerWebExchange exchange, String requestId) {
		String ip = null;
		var remote = exchange.getRequest().getRemoteAddress();
		if (remote instanceof InetSocketAddress inet) {
			if (inet.getAddress() != null) {
				ip = inet.getAddress().getHostAddress();
			}
		}
		String requestUserAgent = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
		return new ServerContext(ip, requestUserAgent, requestId);
	}
}
