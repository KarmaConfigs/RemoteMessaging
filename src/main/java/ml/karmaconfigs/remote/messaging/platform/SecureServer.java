package ml.karmaconfigs.remote.messaging.platform;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import java.nio.file.Path;

/**
 * Remote messaging SSL server interface
 */
public abstract class SecureServer extends Server {

    /**
     * Set the server debug status
     *
     * @param status the server debug status
     * @return this instance
     */
    public abstract SecureServer debug(final boolean status);

    /**
     * Set the current protocol
     *
     * @param p the protocol
     * @return this instance
     */
    public abstract SecureServer protocol(final String p);

    /**
     * Set the server max connections
     *
     * @param m the server max connections amount
     * @return this instance
     */
    public abstract SecureServer maxConnections(final int m);

    /**
     * Set the allowed protocols
     *
     * @param p the protocols
     * @return this instance
     */
    public abstract SecureServer allowedProtocol(final String... p);

    /**
     * Set the allowed ciphers
     *
     * @param c the ciphers
     * @return this instance
     */
    public abstract SecureServer allowedCiphers(final String... c);

    /**
     * Set the certificates location
     *
     * @param location the certificates location
     * @return the certificates location
     */
    public abstract SecureServer certsLocation(final Path location);
}
