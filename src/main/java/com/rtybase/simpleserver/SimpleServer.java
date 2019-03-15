package com.rtybase.simpleserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rtybase.simpleserver.impl.ProxySplashPageHandler;
import com.rtybase.simpleserver.impl.FileHandler;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class SimpleServer {
	static {
		System.setProperty("logback.configurationFile", "logback.xml");
	}

	private static final String DEFAULT_FILE_PORT = "8181";
	private static final String DEFAULT_PROXY_PORT = "3000";

	private static final String APP_PROPERTIES_FILE = "app.properties";
	private static final String FILE_PORT_PARAM = "file-port";
	private static final String PROXY_PORT_PARAM = "proxy-port";
	private static final String REDIRECT_PAGE_PARAM = "redirect-page";
	private static final String FILES_FOLDER_PARAM = "files-folder";

	private static final int INCOMING_CONNECTIONS_BACKLOG = 500;

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleServer.class);

	public static void main(String[] args) throws Exception {

		Properties properties = loadProperties();

		startFileServer(Integer.parseInt(properties.getProperty(FILE_PORT_PARAM, DEFAULT_FILE_PORT)),
				properties.getProperty(FILES_FOLDER_PARAM));

		startProxyServer(Integer.parseInt(properties.getProperty(PROXY_PORT_PARAM, DEFAULT_PROXY_PORT)),
				properties.getProperty(REDIRECT_PAGE_PARAM));
	}

	private static void startFileServer(int filePort, String filesFolder) throws IOException {
		InetSocketAddress adr = new InetSocketAddress(filePort);
		HttpServer server = HttpServer.create(adr, INCOMING_CONNECTIONS_BACKLOG);

		server.createContext(FileHandler.PATH, new FileHandler(filesFolder));

		server.setExecutor(Executors.newFixedThreadPool(10));
		server.start();
		LOGGER.info("Started file server on port '{}' with root folder '{}'.", filePort, filesFolder);
	}

	private static void startProxyServer(int proxyPort, String redirectPageUrl) throws IOException {
		InetSocketAddress adr = new InetSocketAddress(proxyPort);
		HttpServer server = HttpServer.create(adr, INCOMING_CONNECTIONS_BACKLOG);

		server.createContext(ProxySplashPageHandler.PATH, new ProxySplashPageHandler(redirectPageUrl));

		server.setExecutor(Executors.newFixedThreadPool(40));
		server.start();
		LOGGER.info("Started proxy server on port '{}' with redirect URL folder '{}'.", proxyPort, redirectPageUrl);
	}

	protected static Properties loadProperties() throws Exception {
		Properties props = new Properties();
		FileInputStream input = new FileInputStream(APP_PROPERTIES_FILE);
		props.load(input);
		input.close();
		addSunPropertiesToSystem(props);
		return props;
	}

	private static void addSunPropertiesToSystem(Properties props) {
		props.entrySet().forEach(e -> {
			checkAndRegisterSystemProperty(e);
		});
	}

	private static void checkAndRegisterSystemProperty(Entry<Object, Object> e) {
		String key = e.getKey().toString();
		if (key.startsWith("sun.")) {
			System.setProperty(key, e.getValue().toString());
			LOGGER.info("Registered Sun property '{}' to value '{}'.", key, e.getValue().toString());
		}
	}
}
