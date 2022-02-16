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
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;

/**
 * Receive command from client to server event
 */
public class ClientCommandEvent extends ServerEvent {

    private final RemoteServer server;
    private final String command;
    private final String information;

    /**
     * Initialize the server event
     *
     * @param remote the remote client
     * @param sv the remote server
     * @param cmd the command
     * @param inf the command information
     */
    public ClientCommandEvent(final RemoteClient remote, final RemoteServer sv, final String cmd, final String inf) {
        super(remote);

        server = sv;
        command = cmd;
        information = inf;
    }

    /**
     * Get the remote server
     *
     * @return the remote server
     */
    public RemoteServer getServer() {
        return server;
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
}
