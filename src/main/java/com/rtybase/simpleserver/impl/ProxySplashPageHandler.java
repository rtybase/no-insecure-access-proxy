package com.rtybase.simpleserver.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
public class ProxySplashPageHandler extends AbstractHandler {
	public static final String PATH = "/";

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxySplashPageHandler.class);

	private final String redirectPageUrl;

	public ProxySplashPageHandler(String redirectPageUrl) {
		super(PATH, "GET", "POST", "PUT", "DELETE", "OPTIONS");
		this.redirectPageUrl = Objects.requireNonNull(redirectPageUrl, "redirectPageUrl must not be null!");
	}

	@Override
	protected void exec(HttpExchange exchange) throws IOException {
		String redirectLink = prepareRedirectLink(exchange);

		Headers header = exchange.getResponseHeaders();
		header.set(CONTENT_TYPE, "text/html");
		header.set("Location", redirectLink);
		header.set("Connection", "close");
		header.set("Proxy-Connection", "close");

		String response = String.format("Redirect to: <a href='%s'>%s</a>", redirectLink, redirectLink);
		exchange.sendResponseHeaders(HTTP_REDIRECT_RESPONSE, response.length());
		sendText(response, exchange);
	}

	private String prepareRedirectLink(HttpExchange exchange) throws UnsupportedEncodingException {
		return String.format("%s?url=%s",
				redirectPageUrl,
				Base64.getEncoder().encodeToString(
						exchange.getRequestURI().toString().getBytes("UTF-8"))
				);
	}

	private static void sendText(String text, HttpExchange exchange) {
		try {
			OutputStream out = exchange.getResponseBody();
			byte[] bytes = text.getBytes();
			out.write(bytes, 0, bytes.length);
			out.flush();
		} catch (Exception ex) {
			LOGGER.warn("Exception sending response to the client.", ex);
		}
	}
}
