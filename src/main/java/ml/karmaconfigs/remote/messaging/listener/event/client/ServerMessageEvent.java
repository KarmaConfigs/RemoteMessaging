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
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;
import ml.karmaconfigs.remote.messaging.util.message.MessageInput;

/**
 * Server sends message to client event
 */
public class ServerMessageEvent extends ClientEvent {

    private final MessageInput message;

    /**
     * Initialize the client event
     *
     * @param remote the remote server
     * @param data the message
     */
    public ServerMessageEvent(final RemoteServer remote, final MessageInput data) {
        super(remote);

        message = data;
    }

    public final MessageInput getMessage() {
        return message;
    }
}
