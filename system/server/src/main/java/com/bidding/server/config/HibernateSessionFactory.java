package com.bidding.server.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Shared Hibernate SessionFactory for all repositories.
 */
public final class HibernateSessionFactory {

    private static final SessionFactory SESSION_FACTORY = new Configuration()
            .configure("hibernate.cfg.xml")
            .buildSessionFactory();

    private HibernateSessionFactory() {
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }
}
