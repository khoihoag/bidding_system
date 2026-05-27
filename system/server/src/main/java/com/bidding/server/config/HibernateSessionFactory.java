package com.bidding.server.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Shared Hibernate SessionFactory for all repositories.
 */
public final class HibernateSessionFactory {
    private static final String CONFIG_RESOURCE = "hibernate.cfg.xml";
    private static SessionFactory sessionFactory;

    private HibernateSessionFactory() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration().configure(CONFIG_RESOURCE);
            applyConnectionOverrides(configuration);
            return configuration.buildSessionFactory();
        } catch (RuntimeException e) {
            throw new IllegalStateException(buildStartupErrorMessage(e), e);
        }
    }

    private static void applyConnectionOverrides(Configuration configuration) {
        applyOverride(configuration, "hibernate.connection.url", "connection.url", "db.url", "DB_URL", false);
        applyOverride(configuration, "hibernate.connection.username", "connection.username", "db.username", "DB_USERNAME", false);
        applyOverride(configuration, "hibernate.connection.password", "connection.password", "db.password", "DB_PASSWORD", true);
    }

    private static void applyOverride(Configuration configuration,
                                      String hibernateProperty,
                                      String shortProperty,
                                      String systemProperty,
                                      String environmentVariable,
                                      boolean allowBlank) {
        String value = System.getProperty(systemProperty);
        if (value == null) {
            value = System.getenv(environmentVariable);
        }
        if (value == null || (!allowBlank && value.isBlank())) {
            return;
        }

        configuration.setProperty(hibernateProperty, value);
        configuration.setProperty(shortProperty, value);
    }

    private static String buildStartupErrorMessage(Exception exception) {
        return "Cannot connect to MySQL for the bidding server. "
                + "Check that MySQL is running, database 'bidding_system' exists, "
                + "and the credentials in system/server/src/main/resources/hibernate.cfg.xml are correct. "
                + "You can override them with -Ddb.url, -Ddb.username, -Ddb.password "
                + "or environment variables DB_URL, DB_USERNAME, DB_PASSWORD. "
                + "Root cause: " + rootCauseMessage(exception);
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName()
                : message;
    }
}
