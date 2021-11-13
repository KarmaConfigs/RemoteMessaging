package ml.karmaconfigs.remote.messaging.remote;

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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Remote server information
 */
public final class RemoteServer {

    private final InetAddress host;
    private final int port;
    private final DatagramSocket clientSocket;

    /**
     * Initialize the remote server
     *
     * @param address the server address
     * @param incoming_port the server port
     * @param client the client active socket
     */
    public RemoteServer(final InetAddress address, final int incoming_port, final DatagramSocket client) {
        host = address;
        port = incoming_port;
        clientSocket = client;
    }

    /**
     * Get the server address
     *
     * @return the server address
     */
    public InetAddress getHost() {
        return host;
    }

    /**
     * Get the server port
     *
     * @return the server port
     */
    public int getPort() {
        return port;
    }

    /**
     * Send a message to the server
     *
     * @param message the message to send
     * @return if the message could be sent
     */
    public boolean sendMessage(final byte[] message) {
        try {
            DatagramPacket msg = createDataPacket(message);

            if (msg != null) {
                clientSocket.send(msg);
                return true;
            }

            return false;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Create a packet
     *
     * @param data the data to include in the packet
     * @return the packet
     */
    private DatagramPacket createDataPacket(final byte[] data) {
        try {
            return new DatagramPacket(data, data.length, host, port);
        } catch (Throwable ex) {
            return null;
        }
    }
}
