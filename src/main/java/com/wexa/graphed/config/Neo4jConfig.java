
package com.wexa.graphed.config;
import java.util.concurrent.TimeUnit;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Wires up a single shared {@link Driver} pointed at CognoDB.
 *
 * CognoDB speaks openCypher over the Bolt protocol, so the official Neo4j
 * Java driver connects to it without modification - we just point it at the
 * bolt+s:// URI CognoDB Cloud gives you.
 *
 * Connection details are read from environment variables only. Nothing here
 * is hard-coded, and nothing sensitive is committed to the repo.
 */
@Configuration
public class Neo4jConfig {

    @Value("${cognodb.uri}")
    private String uri;

    @Value("${cognodb.username}")
    private String username;

    @Value("${cognodb.password}")
    private String password;

    @Bean(destroyMethod = "close")
    public Driver driver() {
        Driver driver = GraphDatabase.driver(
                uri,
                AuthTokens.basic(username, password),
                Config.builder()
                        .withMaxConnectionPoolSize(10)
                        .withConnectionAcquisitionTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .withConnectionTimeout(10, TimeUnit.SECONDS)
                        .build()
        );
        return driver;
    }
}
