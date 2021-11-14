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
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Remote client information
 */
public final class TCPRemoteClient extends RemoteClient {

    private final String name;
    private final String MAC;
    private final InetAddress host;
    private final int port;
    private final SocketChannel serverSocket;

    /**
     * Initialize the remote client
     *
     * @param client the client name
     * @param m the client MAC address
     * @param address the client address
     * @param incoming_port the client port
     * @param server the server active socket
     */
    public TCPRemoteClient(final String client, final String m, final InetAddress address, final int incoming_port, final SocketChannel server) {
        name = client;
        MAC = m;
        host = address;
        port = incoming_port;
        serverSocket = server;
    }

    /**
     * Get the client name
     *
     * @return the client name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the client MAC address
     *
     * @return the client MAC address
     */
    @Override
    public String getMAC() {
        return MAC;
    }

    /**
     * Get the client address
     *
     * @return the client address
     */
    @Override
    public InetAddress getHost() {
        return host;
    }

    /**
     * Get the client port
     *
     * @return the client port
     */
    @Override
    public int getPort() {
        return port;
    }

    /**
     * Send a message to the client
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
            BUFFER.clear();
            BUFFER.put(out.toByteArray());
            BUFFER.flip();

            serverSocket.write(BUFFER);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
