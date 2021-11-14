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

import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Remote message server interface
 */
public abstract class Server {

    /**
     * Set the server debug status
     *
     * @param status the server debug status
     * @return this instance
     */
    public abstract Server debug(final boolean status);

    /**
     * Try to start the server
     *
     * @return a completable future when the server starts
     */
    public abstract CompletableFuture<Boolean> start();

    /**
     * Get the server MAC address
     *
     * @return the server MAC address
     */
    public abstract String getMAC();

    /**
     * Get the connected clients
     *
     * @return a list of connected clients
     */
    public abstract Set<RemoteClient> getClients();

    /**
     * Get the server work level
     *
     * @return the server work level
     */
    public abstract WorkLevel getWorkLevel();

    /**
     * Completely close the server
     */
    public abstract void close();

    /**
     * Export the list of bans
     *
     * @param destination the file were to store
     *                    the ban list
     */
    public abstract void exportBans(final Path destination);

    /**
     * Load the list of bans
     *
     * @param bans the file were the banned mac
     *             addresses are stored
     */
    public abstract void loadBans(final Path bans);

    /**
     * Send a message to each connected client
     *
     * @param data the data to send
     */
    public abstract void broadcast(final byte[] data);

    /**
     * Redirect a message to the specified client
     *
     * @param name the client name
     * @param data the message
     */
    public abstract void redirect(final String name, final byte[] data);

    /**
     * Ban an address from the server
     *
     * @param macAddresses the addresses to ban
     */
    public abstract void ban(final String... macAddresses);

    /**
     * Kick an address from the server
     *
     * @param macAddresses the addresses to kick
     */
    public abstract void kick(final String... macAddresses);

    /**
     * Unban an address from the server
     *
     * @param macAddresses the addresses to unban
     */
    public abstract void unBan(final String... macAddresses);
}
