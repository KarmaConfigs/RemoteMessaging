package ml.karmaconfigs.remote.messaging.platform;

import java.nio.file.Path;

public abstract class SecureClient extends Client {

    /**
     * Set the client debug status
     *
     * @param status the client debug status
     * @return this instance
     */
    public abstract SecureClient debug(final boolean status);

    /**
     * Set the current protocol
     *
     * @param p the protocol
     * @return this instance
     */
    public abstract SecureClient protocol(final String p);

    /**
     * Set the certificates location
     *
     * @param location the certificates location
     * @return the certificates location
     */
    public abstract SecureClient certsLocation(final Path location);
}
