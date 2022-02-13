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

import ml.karmaconfigs.remote.messaging.platform.Client;
import ml.karmaconfigs.remote.messaging.platform.Server;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;
import ml.karmaconfigs.remote.messaging.worker.tcp.TCPClient;
import ml.karmaconfigs.remote.messaging.worker.tcp.TCPServer;

/**
 * Remote messaging factory
 */
public final class Factory {

    private final WorkLevel level;

    /**
     * Initialize the factory
     *
     * @param lvl the work level
     */
    public Factory(final WorkLevel lvl) {
        level = lvl;
    }

    /**
     * Create a new client
     *
     * @return a new client
     * @throws IllegalArgumentException if work level is UDP
     */
    public Client createClient() throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPClient();
            case UDP:
            default:
                throw new IllegalArgumentException("UDP client is under maintenance and is not available");
        }
    }

    /**
     * Create a new client
     *
     * @param target_port the server port
     * @return a new client
     * @throws IllegalArgumentException if work level is UDP
     */
    public Client createClient(final int target_port) throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPClient("127.0.0.1", target_port);
            case UDP:
            default:
                throw new IllegalArgumentException("UDP client is under maintenance and is not available");
        }
    }

    /**
     * Create a new client
     *
     * @param target_host the server host
     * @param target_port the server port
     * @return a new client
     * @throws IllegalArgumentException if work level is UDP
     */
    public Client createClient(final String target_host, final int target_port) throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPClient(target_host, target_port);
            case UDP:
            default:
                throw new IllegalArgumentException("UDP client is under maintenance and is not available");
        }
    }

    /**
     * Create a new client
     *
     * @param target_host the server host
     * @param target_port the server port
     * @param local_port the client port
     * @return a new client
     * @throws IllegalArgumentException if work level is UDP
     */
    public Client createClient(final String target_host, final int target_port, final int local_port) throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPClient(local_port, target_host, target_port);
            case UDP:
            default:
                throw new IllegalArgumentException("UDP client is under maintenance and is not available");
        }
    }

    /**
     * Create a new server
     *
     * @return a new server
     * @throws IllegalArgumentException if work level is UDP
     */
    public Server createServer() throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPServer();
            case UDP:
            default:
                throw new IllegalArgumentException("UDP server is under maintenance and is not available");
        }
    }

    /**
     * Create a new server
     *
     * @param target_port the server port
     * @return a new server
     * @throws IllegalArgumentException if work level is UDP
     */
    public Server createServer(final int target_port) throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPServer(target_port);
            case UDP:
            default:
                throw new IllegalArgumentException("UDP server is under maintenance and is not available");
        }
    }

    /**
     * Create a new server
     *
     * @param target_host the server address
     * @param target_port the server port
     * @return a new server
     * @throws IllegalArgumentException if work level is UDP
     */
    public Server createServer(final String target_host, final int target_port) throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPServer(target_host, target_port);
            case UDP:
            default:
                throw new IllegalArgumentException("UDP server is under maintenance and is not available");
        }
    }
}
