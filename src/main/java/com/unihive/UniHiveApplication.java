package com.unihive;
import com.unihive.websocket.ChatServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;

/**
 * UniHive Application Entry Point.
 *
 * <p>Bootstraps the Spring Boot application context and exposes the
 * {@link ChatServer} as a Spring bean so it can be injected into
 * {@link com.unihive.controller.ConsoleController}.
 *
 * <p>The {@link ChatServer} is started automatically here via a
 * {@link org.springframework.boot.CommandLineRunner} — it must be
 * listening <em>before</em> {@link com.unihive.controller.ConsoleController}
 * tries to create a client connection on first login.
 *
 * <p>Startup order:
 * <ol>
 *   <li>Spring context loads all beans (MongoDB, BCrypt, services, etc.).
 *   <li>{@link #chatServer()} bean is created and the server thread started.
 *   <li>{@link com.unihive.controller.ConsoleController#run} is called by
 *       Spring Boot, which drives the interactive CLI.
 * </ol>
 */
@Slf4j
@SpringBootApplication
public class UniHiveApplication {

    @Value("${unihive.websocket.port:8887}")
    private int wsPort;

    @Value("${unihive.websocket.host:localhost}")
    private String wsHost;

    // ─────────────────────────────────────────────────────────────
    //  main
    // ─────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Suppress Spring Boot's default startup banner so the UniHive
        // banner in ConsoleController is the first thing the user sees.
        SpringApplication app = new SpringApplication(UniHiveApplication.class);
        app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
        app.run(args);
    }

    // ─────────────────────────────────────────────────────────────
    //  ChatServer Bean
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates, starts, and exposes the {@link ChatServer} as a singleton
     * Spring bean.
     *
     * <p>The server is started on a daemon thread so it does not prevent
     * JVM shutdown when the main menu loop exits.
     *
     * @return the running {@link ChatServer} instance
     */
    
    @Bean
    public ChatServer chatServer() {

        log.info("Port: {}", wsPort);

        InetSocketAddress address = new InetSocketAddress(wsPort);

        ChatServer server = new ChatServer(address);

        Thread serverThread = new Thread(() -> {
            try {
                server.run();
            } catch (Exception e) {
                log.error("ChatServer encountered a fatal error", e);
            }
        }, "unihive-ws-server");

        serverThread.setDaemon(true);
        serverThread.start();

        return server;
    }
}
