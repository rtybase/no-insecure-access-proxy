package com.rtybase.simpleserver.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public abstract class AbstractHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHandler.class);

	public static final int HTTP_OK_RESPONSE = 200;
	public static final int HTTP_REDIRECT_RESPONSE = 303;
	public static final int HTTP_NOT_FOUND_RESPONSE = 404;

	public static final String CONTENT_TYPE = "Content-Type";
	public static final String CONTENT_ENCODING = "Content-Encoding";

	private static final int BUFFER_SIZE = 512;

	private final String rootPath;
	private final Set<String> supportedMethods;

	protected AbstractHandler(String rootPath, String... method) {
		this.rootPath = Objects.requireNonNull(rootPath, "rootPath must not be null!");
		supportedMethods = new HashSet<>(Arrays.asList(method));
	}

	protected abstract void exec(HttpExchange exchange) throws IOException;

	@Override
	public final void handle(HttpExchange exchange) throws IOException {
		logRequestHeaders(exchange);

		drainRequestBody(exchange);

		if (isPathAllowed(exchange) && isMethodAllowed(exchange)) {
			exec(exchange);
		} else {
			doNotFound(exchange);
		}

		LOGGER.info("Completed request for '{}' request from '{}' for '{}'.", exchange.getRequestMethod(),
				exchange.getRemoteAddress(), exchange.getRequestURI());
		exchange.close();
	}

	protected static void sendFile(File file, HttpExchange exchange) {
		byte[] buffer = new byte[BUFFER_SIZE];
		int len = 0;

		try (FileInputStream in = new FileInputStream(file); OutputStream out = exchange.getResponseBody()) {

			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
				out.flush();
			}
		} catch (Exception ex) {
			LOGGER.warn("Exception sending file to the client.", ex);
		}
	}

	protected static void doNotFound(HttpExchange exchange) throws IOException {
		exchange.sendResponseHeaders(HTTP_NOT_FOUND_RESPONSE, -1);
	}

	private boolean isPathAllowed(HttpExchange exchange) {
		String path = exchange.getRequestURI().getPath();
		boolean result = false;

		if (path != null) {
			Path pth = Paths.get(path);
			Path npth = pth.normalize();
			result = npth.startsWith(rootPath + "/");
		}

		return result;
	}

	private boolean isMethodAllowed(HttpExchange exchange) {
		return supportedMethods.contains(exchange.getRequestMethod());
	}

	private static void logRequestHeaders(HttpExchange exchange) {
		if (LOGGER.isDebugEnabled()) {
			Headers headers = exchange.getRequestHeaders();
			final StringBuilder sb = new StringBuilder();
			headers.entrySet().forEach(entry -> {
				sb.append(entry.getKey());
				sb.append(": ");
				entry.getValue().forEach(v -> {
					sb.append(v);
					sb.append(" ");
				});
				sb.append("\n");
			});

			LOGGER.debug("New request for '{}' request from '{}' for '{}' with headers '\n{}\n'.",
					exchange.getRequestMethod(), exchange.getRemoteAddress(), exchange.getRequestURI(), sb);
		}
	}

	private static void drainRequestBody(HttpExchange exchange) throws IOException {
		InputStream is = exchange.getRequestBody();
		while (is.read() != -1) {
			is.skip(0x10000);
		}
		is.close();
	}
}
