package ml.karmaconfigs.remote.messaging.listener.event.server;

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

import ml.karmaconfigs.remote.messaging.listener.ServerEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;

/**
 * Receive command from client to server event
 */
public class ClientCommandEvent extends ServerEvent {

    private final String command;
    private final String information;
    private final byte[] raw;

    /**
     * Initialize the server event
     *
     * @param remote the remote client
     * @param cmd the command
     * @param inf the command information
     * @param t the raw command
     */
    public ClientCommandEvent(final RemoteClient remote, final String cmd, final String inf, final byte[] t) {
        super(remote);

        command = cmd;
        information = inf;
        raw = t;
    }

    /**
     * Get the command
     *
     * @return the command
     */
    public final String getCommand() {
        return command;
    }

    /**
     * Get the command information
     *
     * @return the command information
     */
    public final String getInformation() {
        return information;
    }

    /**
     * Get the raw command
     *
     * @return the raw command
     */
    public final byte[] getRawCommand() {
        return raw;
    }
}
