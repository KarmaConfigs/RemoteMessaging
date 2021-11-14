package ml.karmaconfigs.remote.messaging.worker.tcp.remote;

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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Remote server information
 */
public final class TCPRemoteServer extends RemoteServer {

    private final String MAC;
    private final InetAddress host;
    private final int port;
    private final SocketChannel clientSocket;

    /**
     * Initialize the remote server
     *
     * @param m the server MAC address
     * @param address the server address
     * @param incoming_port the server port
     * @param client the client active socket
     */
    public TCPRemoteServer(final String m, final InetAddress address, final int incoming_port, final SocketChannel client) {
        MAC = m;
        host = address;
        port = incoming_port;
        clientSocket = client;
    }

    /**
     * Get the server address
     *
     * @return the server address
     */
    @Override
    public InetAddress getHost() {
        return host;
    }

    /**
     * Get the server MAC address
     *
     * @return the server MAC address
     */
    @Override
    public String getMAC() {
        return MAC;
    }

    /**
     * Get the server port
     *
     * @return the server port
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * Send a message to the server
     *
     * @param message the message to send
     * @return if the message could be sent
     */
    @Override
    @SuppressWarnings("UnstableApiUsage")
    public boolean sendMessage(final byte[] message) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(getMAC());
            out.writeBoolean(false);
            out.writeInt((out.toByteArray().length + 4));
            out.write(message);

            ByteBuffer BUFFER = ByteBuffer.allocate(4056);
            BUFFER.put(out.toByteArray());
            BUFFER.flip();

            clientSocket.write(BUFFER);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
