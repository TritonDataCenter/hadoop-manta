package com.joyent.hadoop.fs.manta;

import com.google.common.base.Preconditions;
import com.joyent.manta.config.ConfigContext;
import com.joyent.manta.config.EncryptionObjectAuthenticationMode;
import com.joyent.manta.config.MapConfigContext;
import com.joyent.manta.util.MantaUtils;
import org.apache.hadoop.conf.Configuration;

import java.util.Base64;

import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_HTTPS_CIPHERS;
import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_HTTPS_PROTOCOLS;
import static com.joyent.manta.config.DefaultsConfigContext.DEFAULT_HTTP_TRANSPORT;

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
        return configuration.get(MapConfigContext.MANTA_URL_KEY);
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
        return configuration.get(MapConfigContext.MANTA_KEY_PATH_KEY);
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
        return getIntDefaultToNull(MapConfigContext.MANTA_TIMEOUT_KEY);
    }

    @Override
    public String getMantaHomeDirectory() {
        return ConfigContext.deriveHomeDirectoryFromUser(getMantaUser());
    }

    @Override
    public Integer getRetries() {
        return getIntDefaultToNull(MapConfigContext.MANTA_RETRIES_KEY);
    }

    @Override
    public Integer getMaximumConnections() {
        return getIntDefaultToNull(MapConfigContext.MANTA_MAX_CONNS_KEY);
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
        return getIntDefaultToNull(MapConfigContext.MANTA_SIGS_CACHE_TTL_KEY);
    }

    @Override
    public Boolean isClientEncryptionEnabled() {
        return getBooleanDefaultToNull(MapConfigContext.MANTA_CLIENT_ENCRYPTION_ENABLED_KEY);
    }

    @Override
    public Boolean permitUnencryptedDownloads() {
        return getBooleanDefaultToNull(MapConfigContext.MANTA_PERMIT_UNENCRYPTED_DOWNLOADS_KEY);
    }

    @Override
    public EncryptionObjectAuthenticationMode getEncryptionAuthenticationMode() {
        String val = configuration.get(MapConfigContext.MANTA_ENCRYPTION_AUTHENTICATION_MODE_KEY);

        if (val == null) {
            return null;
        }

        String valueString = val.trim();

        if (valueString.isEmpty()) {
            return null;
        }

        return EncryptionObjectAuthenticationMode.valueOf(val);
    }

    @Override
    public String getEncryptionPrivateKeyPath() {
        return configuration.get(MapConfigContext.MANTA_ENCRYPTION_PRIVATE_KEY_PATH_KEY);
    }

    @Override
    public byte[] getEncryptionPrivateKeyBytes() {
        String base64 = configuration.get(MapConfigContext.MANTA_ENCRYPTION_PRIVATE_KEY_BYTES_BASE64_KEY);

        if (base64 == null) {
            return null;
        }

        String valueString = base64.trim();

        if (valueString.isEmpty()) {
            return null;
        }

        return Base64.getDecoder().decode(base64);
    }

    /**
     * Get the value of the <code>name</code> property as an <code>int</code>.
     *
     * If no such property exists, null is returned.
     *
     * @param name property name.
     * @throws NumberFormatException when the value is invalid
     * @return property value as an <code>int</code>,
     *         or <code>null</code>.
     */
    private Integer getIntDefaultToNull(final String name) {
        String val = configuration.get(name);

        if (val == null) {
            return null;
        }

        String valueString = val.trim();

        if (valueString.isEmpty()) {
            return null;
        }

        String hexString = getHexDigits(valueString);
        if (hexString != null) {
            return Integer.parseInt(hexString, 16);
        }
        return Integer.parseInt(valueString);
    }

    /**
     * Get the value of the <code>name</code> property as a <code>boolean</code>.
     * If no such property is specified, or if the specified value is not a valid
     * <code>boolean</code>, then <code>null</code> is returned.
     *
     * @param name property name.
     * @return property value as a <code>boolean</code>,
     *         or <code>null</code>.
     */
    private Boolean getBooleanDefaultToNull(final String name) {
        return MantaUtils.parseBooleanOrNull(name);
    }

    /**
     * Reformats a hex string so it can be parsed from {@link Integer#parseInt(String, int)}.
     * This is a copy of {@link org.apache.hadoop.conf.Configuration#getHexDigits(String)}
     * @param value value to reformat
     * @return hex string or null
     */
    private String getHexDigits(final String value) {
        boolean negative = false;
        String str = value;
        String hexString;
        if (value.startsWith("-")) {
            negative = true;
            str = value.substring(1);
        }
        if (str.startsWith("0x") || str.startsWith("0X")) {
            hexString = str.substring(2);
            if (negative) {
                hexString = "-" + hexString;
            }
            return hexString;
        }
        return null;
    }
}
