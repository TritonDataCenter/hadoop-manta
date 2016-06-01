package com.joyent.hadoop.fs.manta;

import com.google.common.base.Preconditions;
import com.joyent.manta.config.ConfigContext;
import com.joyent.manta.config.MapConfigContext;
import org.apache.hadoop.conf.Configuration;

import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_HTTPS_CIPHERS;
import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_HTTPS_PROTOCOLS;
import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_HTTP_RETRIES;
import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_HTTP_TIMEOUT;
import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_HTTP_TRANSPORT;
import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_MANTA_URL;
import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_MAX_CONNS;
import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_SIGNATURE_CACHE_TTL;
import static com.joyent.manta.config.DefaultsConfigContext.MANTA_KEY_PATH;

/**
 * Manta configuration context implementation that wraps the Hadoop {@link Configuration}
 * passed in the constructor.
 */
public class HadoopConfigurationContext implements ConfigContext {
    /**
     * Wrapped Hadoop configuration instance.
     */
    private final Configuration configuration;

    /**
     * Creates a new configuration context based on the passed Hadoop configuration object.
     * @param configuration Hadoop configuration object
     */
    public HadoopConfigurationContext(final Configuration configuration) {
        Preconditions.checkNotNull(configuration, "Hadoop configuration object must be not be null");
        this.configuration = configuration;
    }

    @Override
    public String getMantaURL() {
        return configuration.get(MapConfigContext.MANTA_URL_KEY, DEFAULT_MANTA_URL);
    }

    @Override
    public String getMantaUser() {
        return configuration.get(MapConfigContext.MANTA_USER_KEY);
    }

    @Override
    public String getMantaKeyId() {
        return configuration.get(MapConfigContext.MANTA_KEY_ID_KEY);
    }

    @Override
    public String getMantaKeyPath() {
        return configuration.get(MapConfigContext.MANTA_KEY_PATH_KEY, MANTA_KEY_PATH);
    }

    @Override
    public String getPrivateKeyContent() {
        return configuration.get(MapConfigContext.MANTA_PRIVATE_KEY_CONTENT_KEY);
    }

    @Override
    public String getPassword() {
        return configuration.get(MapConfigContext.MANTA_PASSWORD_KEY);
    }

    @Override
    public Integer getTimeout() {
        return configuration.getInt(MapConfigContext.MANTA_TIMEOUT_KEY, DEFAULT_HTTP_TIMEOUT);
    }

    @Override
    public String getMantaHomeDirectory() {
        return ConfigContext.deriveHomeDirectoryFromUser(getMantaUser());
    }

    @Override
    public Integer getRetries() {
        return configuration.getInt(MapConfigContext.MANTA_RETRIES_KEY, DEFAULT_HTTP_RETRIES);
    }

    @Override
    public Integer getMaximumConnections() {
        return configuration.getInt(MapConfigContext.MANTA_MAX_CONNS_KEY, DEFAULT_MAX_CONNS);
    }

    @Override
    public String getHttpTransport() {
        return configuration.get(MapConfigContext.MANTA_HTTP_TRANSPORT_KEY, DEFAULT_HTTP_TRANSPORT);
    }

    @Override
    public String getHttpsProtocols() {
        return configuration.get(MapConfigContext.MANTA_HTTPS_PROTOCOLS_KEY, DEFAULT_HTTPS_PROTOCOLS);
    }

    @Override
    public String getHttpsCipherSuites() {
        return configuration.get(MapConfigContext.MANTA_HTTPS_CIPHERS_KEY, DEFAULT_HTTPS_CIPHERS);
    }

    @Override
    public Boolean noAuth() {
        return configuration.getBoolean(MapConfigContext.MANTA_NO_AUTH_KEY, false);
    }

    @Override
    public Boolean disableNativeSignatures() {
        return configuration.getBoolean(MapConfigContext.MANTA_NO_NATIVE_SIGS_KEY, false);
    }

    @Override
    public Integer getSignatureCacheTTL() {
        return configuration.getInt(MapConfigContext.MANTA_SIGS_CACHE_TTL_KEY, DEFAULT_SIGNATURE_CACHE_TTL);
    }
}
