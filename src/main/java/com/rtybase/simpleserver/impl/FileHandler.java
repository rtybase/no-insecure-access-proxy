package com.rtybase.simpleserver.impl;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.sun.net.httpserver.Headers;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
public class FileHandler extends AbstractHandler {
	private static final String GZIP_MARK = ".gzip";
	private final String rootFolder;

	public static final String PATH = "/files";

	public FileHandler(String rootFolder) {
		super(PATH, "GET");
		this.rootFolder = Objects.requireNonNull(rootFolder, "rootFolder must not be null!");
	}

	@Override
	public void exec(HttpExchange exchange) throws IOException {
		String filePath = exchange.getRequestURI().getPath();

		File file = getFileOutOfPath(filePath);

		if (file != null) {
			setResponseHeaders(exchange, filePath);

			long len = file.length();
			if (len == 0) {
				len = -1;
			}
			exchange.sendResponseHeaders(HTTP_OK_RESPONSE, len);
			sendFile(file, exchange);
		} else {
			doNotFound(exchange);
		}
	}

	private static void setResponseHeaders(HttpExchange exchange, String file) {
		Headers header = exchange.getResponseHeaders();

		String filePath = file.toLowerCase();
		if (filePath.endsWith(".jpeg")) {
			header.set(CONTENT_TYPE, "image/jpeg");
		} else if (filePath.endsWith(".png")) {
			header.set(CONTENT_TYPE, "image/png");
		} else if (filePath.endsWith(".json")) {
			header.set(CONTENT_TYPE, "application/json");
		} else if (filePath.endsWith(".txt")) {
			header.set(CONTENT_TYPE, "text/plain");
		} else if (filePath.endsWith(".pac")) {
			header.set(CONTENT_TYPE, "application/x-ns-proxy-autoconfig");
			header.set("Content-Disposition", "attachment; filename=proxy.pac");
		} else if (filePath.endsWith(".html")) {
			header.set(CONTENT_TYPE, "text/html");
		} else {
			header.set(CONTENT_TYPE, "application/octet-stream");
		}

		if (filePath.contains(GZIP_MARK)) {
			header.set(CONTENT_ENCODING, "gzip");
		}

		header.set("Connection", "close");
	}

	private File getFileOutOfPath(String path) {
		File file = new File(rootFolder, path);

		if (file.exists() && file.isFile()) {
			return file;
		} else {
			return null;
		}
	}
}
