package ml.karmaconfigs.remote.messaging.listener.event.client;

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

import ml.karmaconfigs.remote.messaging.listener.ClientEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;

/**
 * Server accept connection event
 */
public class ServerConnectEvent extends ClientEvent {

    private final RemoteClient client;

    /**
     * Initialize the client event
     *
     * @param remote the remote server
     * @param cl the remote client
     */
    public ServerConnectEvent(final RemoteServer remote, final RemoteClient cl) {
        super(remote);
        client = cl;
    }

    /**
     * Get the remote client
     *
     * @return the remote client
     */
    public RemoteClient getClient() {
        return client;
    }
}
