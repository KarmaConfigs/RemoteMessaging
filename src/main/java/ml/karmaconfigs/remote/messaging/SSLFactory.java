package ml.karmaconfigs.remote.messaging;

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

import ml.karmaconfigs.remote.messaging.platform.SecureClient;
import ml.karmaconfigs.remote.messaging.platform.SecureServer;
import ml.karmaconfigs.remote.messaging.worker.ssl.SSLClient;
import ml.karmaconfigs.remote.messaging.worker.ssl.SSLServer;

/**
 * Remote messaging factory
 */
public final class SSLFactory {

    private final String password;
    private final String name;
    private final String extension;
    private final String type;

    /**
     * Initialize the factory
     *
     * @param pwd the certificate password
     * @param nm the certificate base name
     * @param ext the certificate file extension
     * @param tp the certificate type
     */
    public SSLFactory(final String pwd, final String nm, final String ext, final String tp) {
        password = pwd;
        name = nm;
        extension = ext;
        type = tp;
    }

    /**
     * Create a new client
     *
     * @return a new client
     */
    public SecureClient createClient() {
        return new SSLClient(password, name, extension, type);
    }

    /**
     * Create a new client
     *
     * @param target_port the server port
     * @return a new client
     */
    public SecureClient createClient(final int target_port) {
        return new SSLClient(password, name, extension, type, "127.0.0.1", target_port);
    }

    /**
     * Create a new client
     *
     * @param target_host the server host
     * @param target_port the server port
     * @return a new client
     */
    public SecureClient createClient(final String target_host, final int target_port) {
        return new SSLClient(password, name, extension, type, target_host, target_port);
    }

    /**
     * Create a new client
     *
     * @param target_host the server host
     * @param target_port the server port
     * @param local_port the client port
     * @return a new client
     */
    public SecureClient createClient(final String target_host, final int target_port, final int local_port) {
        return new SSLClient(password, name, extension, type, local_port, target_host, target_port);
    }

    /**
     * Create a new server
     *
     * @return a new server
     */
    public SecureServer createServer() {
        return new SSLServer(password, name, extension, type);
    }

    /**
     * Create a new server
     *
     * @param target_port the server port
     * @return a new server
     */
    public SecureServer createServer(final int target_port) throws IllegalArgumentException {
        return new SSLServer(password, name, extension, type, target_port);
    }

    /**
     * Create a new server
     *
     * @param target_host the server address
     * @param target_port the server port
     * @return a new server
     */
    public SecureServer createServer(final String target_host, final int target_port) {
        return new SSLServer(password, name, extension, type, target_host, target_port);
    }
}
