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
import ml.karmaconfigs.remote.messaging.util.DisconnectReason;

/**
 * Client disconnect from server event
 */
public class ClientDisconnectEvent extends ServerEvent {

    private DisconnectReason reason;
    private String message;

    /**
     * Initialize the server event
     *
     * @param remote the remote client
     * @param r the disconnect reason
     * @param m the disconnect message
     */
    public ClientDisconnectEvent(final RemoteClient remote, final DisconnectReason r, final String m) {
        super(remote);

        reason = r;
        message = m;
    }

    /**
     * Set the disconnect reason
     *
     * @param r the disconnect reason
     */
    public final void setDisconnectReason(final DisconnectReason r) {
        reason = r;
    }

    /**
     * Set the disconnect message
     *
     * @param m the disconnect message
     */
    public final void setDisconnectMessage(final String m) {
        message = m;
    }

    /**
     * Get the disconnect reason
     *
     * @return the disconnect reason
     */
    public final DisconnectReason getReason() {
        return reason;
    }

    /**
     * Get the disconnect message
     *
     * @return the disconnect message
     */
    public final String getDisconnectMessage() {
        return message;
    }
}
