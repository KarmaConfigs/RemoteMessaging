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

import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;

/**
 * Remote message client interface
 */
public abstract class Client implements KarmaSource {

    /**
     * Set the client debug status
     *
     * @param status the client debug status
     * @return this instance
     */
    public abstract Client debug(final boolean status);

    /**
     * Try to connect to the server
     *
     * @return a completable future when the client connects
     */
    public abstract LateScheduler<Boolean> connect();

    /**
     * Try to connect to the server
     *
     * @param accessKey the server access key
     * @return a completable future when the client connects
     */
    public abstract LateScheduler<Boolean> connect(final String accessKey);

    /**
     * Get the client name
     *
     * @return the client name
     */
    public abstract String getName();

    /**
     * Get the client MAC address
     *
     * @return the client MAC address
     */
    public abstract String getMAC();

    /**
     * Get the connected remote server
     *
     * @return the connected remote server
     */
    public abstract RemoteServer getServer();

    /**
     * Get the client work level
     *
     * @return the client work level
     */
    public abstract WorkLevel getWorkLevel();

    /**
     * Rename the client on the server interface
     *
     * @param name the client name
     */
    public abstract void rename(final String name);

    /**
     * Send data to the server
     *
     * @param data the data to send
     */
    public abstract void send(final byte[] data);

    /**
     * Close the connection
     */
    public abstract void close();
}
