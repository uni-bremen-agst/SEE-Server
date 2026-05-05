package de.unibremen.swt.see.manager.config;

import io.livekit.server.RoomServiceClient;
import org.springframework.beans.factory.annotation.Value;
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
     * API URL for the LiveKit instance.
     */
    @Value("${see.app.livekit.url}")
    private String liveKitApiUrl;

    /**
     * API Key for the LiveKit instance.
     */
    @Value("${see.app.livekit.apiKey}")
    private String liveKitApiKey;

    /**
     * API Secret for the LiveKit instance.
     */
    @Value("${see.app.livekit.apiSecret}")
    private String liveKitApiSecret;

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

    /**
     * Creates a singleton bean of a livekit room client.
     *
     * @return A singleton instance of {@see RoomServiceClient}
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RoomServiceClient roomServiceClient() {
        return RoomServiceClient.createClient(liveKitApiUrl, liveKitApiKey, liveKitApiSecret);
    }
}
