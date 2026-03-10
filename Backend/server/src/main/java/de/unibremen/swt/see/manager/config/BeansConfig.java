package de.unibremen.swt.see.manager.config;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * Configuration class for beans used in the application.
 */
@Configuration
public class BeansConfig {

    /**
     * Creates a {@code LockRegistry} bean used to manage file locks in the application.
     *
     * @return A singleton instance of {@link LockRegistry}
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public LockRegistry fileLockRegistry() {
        return new DefaultLockRegistry();
    }
}
