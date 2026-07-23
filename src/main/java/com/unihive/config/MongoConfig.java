package com.unihive.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB Atlas configuration for UniHive.
 *
 * <p>Reads the connection URI and database name from
 * {@code application.properties} and configures the Spring Data MongoDB
 * infrastructure, including auditing support for automatic {@code createdAt}
 * population on {@link com.unihive.model.User} documents.
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.unihive.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {

    /**
     * Full MongoDB Atlas connection string, including credentials and cluster host.
     * Injected from {@code spring.data.mongodb.uri} in {@code application.properties}.
     */
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    /**
     * Logical database name inside the Atlas cluster.
     * Defaults to {@code "unihive"} — matches the database segment of the URI.
     */
    @Value("${spring.data.mongodb.database:unihive}")
    private String databaseName;

    // ─────────────────────────────────────────────────────────────
    //  AbstractMongoClientConfiguration overrides
    // ─────────────────────────────────────────────────────────────

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public MongoClient mongoClient() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                // Connection pool tuning — sensible defaults for a CLI app
                .applyToConnectionPoolSettings(builder ->
                        builder
                                .maxSize(10)
                                .minSize(1)
                                .maxConnectionIdleTime(30, TimeUnit.SECONDS)
                                .maxWaitTime(5, TimeUnit.SECONDS))
                // Socket-level timeouts to fail fast if Atlas is unreachable
                .applyToSocketSettings(builder ->
                        builder
                                .connectTimeout(10, TimeUnit.SECONDS)
                                .readTimeout(30, TimeUnit.SECONDS))
                .build();

        return MongoClients.create(settings);
    }

    /**
     * Enables automatic index creation from {@code @Indexed} annotations
     * on {@link com.unihive.model.User}. This ensures the unique constraints
     * on {@code username} and {@code email} are created on first run.
     *
     * <p><b>Note:</b> In production, manage indexes via Atlas UI or migration scripts.
     * Auto-index creation is acceptable for v1 / development use.
     */
    @Override
    protected boolean autoIndexCreation() {
        return true;
    }
}
