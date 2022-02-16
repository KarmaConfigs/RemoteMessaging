package ml.karmaconfigs.remote.messaging.worker.ssl.remote;

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

import ml.karmaconfigs.remote.messaging.remote.RemoteServer;
import ml.karmaconfigs.remote.messaging.util.message.MessageDataOutput;
import ml.karmaconfigs.remote.messaging.util.message.MessageOutput;
import ml.karmaconfigs.remote.messaging.util.message.type.MergeType;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Remote server information
 */
public class SSLRemoteServer extends RemoteServer {

    private final String MAC;
    private final InetAddress host;
    private final int port;
    private final Socket serverSocket;

    /**
     * Initialize the remote server
     *
     * @param m the server MAC address
     * @param address the server address
     * @param incoming_port the server port
     * @param socket the server active socket
     */
    public SSLRemoteServer(final String m, final InetAddress address, final int incoming_port, final Socket socket) {
        MAC = m;
        host = address;
        port = incoming_port;
        serverSocket = socket;
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
    public boolean sendMessage(final byte[] message) {
        try {
            MessageOutput output = new MessageDataOutput(message, MergeType.DIFFERENCE);
            output.write("MAC", getMAC());
            output.write("COMMAND_ENABLED", false);

            byte[] compile = output.compile();

            PrintWriter writer = new PrintWriter(serverSocket.getOutputStream());
            writer.println(new String(compile, StandardCharsets.UTF_8));
            writer.flush();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
