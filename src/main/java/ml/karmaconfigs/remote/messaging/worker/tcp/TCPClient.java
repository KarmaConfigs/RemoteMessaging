package ml.karmaconfigs.remote.messaging.worker.tcp;

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

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.remote.messaging.Client;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remote message client interface
 */
@SuppressWarnings("UnstableApiUsage")
public final class TCPClient extends Client {

    private final static ByteBuffer BUFFER = ByteBuffer.allocate(2048);
    private final static Set<byte[]> data_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static String client_name = "client_" + new Random().nextInt(Integer.MAX_VALUE);
    private static String server = "127.0.0.1";

    private static int sv_port = 49305;
    private static int client = 49300;

    private static boolean debug = false;
    private static boolean operative = false;
    private static boolean instant_close = false;
    private static boolean award_connection = false;
    private static boolean tryingConnect = true;

    private static SocketChannel socket;

    /**
     * Initialize a default client that
     * will connect to local server at
     * default port 49305
     */
    public TCPClient() {}

    /**
     * Initialize a client that will connect
     * to the specified server at specified port
     *
     * @param server_host the server
     * @param server_port the server port
     */
    public TCPClient(final String server_host, final int server_port) {
        server = server_host;
        sv_port = server_port;
    }

    /**
     * Initialize a client with a custom port
     * that will connect to the specified server at the
     * specified port
     *
     * @param client_port the client port
     * @param server_host the server
     * @param server_port the server port
     */
    public TCPClient(final int client_port, final String server_host, final int server_port) {
        client = client_port;
        server = server_host;
        sv_port = server_port;
    }

    /**
     * Set the client debug status
     *
     * @param status the client debug status
     * @return this instance
     */
    @Override
    public Client debug(final boolean status) {
        debug = status;

        return this;
    }

    /**
     * Try to connect to the server
     *
     * @return a completable future when the client connects
     */
    @Override
    public CompletableFuture<Boolean> connect() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        Thread thread = new Thread(() -> {
            try {
                if (debug) {
                    APISource.getConsole().send("Initializing the connection with the server", Level.INFO);
                }

                socket = SocketChannel.open().bind(new InetSocketAddress(client));
                socket.configureBlocking(false);
                socket.connect(new InetSocketAddress(server, sv_port));

                while (!socket.finishConnect()) {
                    if (tryingConnect) {
                        if (debug) {
                            APISource.getConsole().send("Trying to establish a connection with {0}/{1}", Level.INFO, server, sv_port);
                        }

                        tryingConnect = false;
                    }
                }

                award_connection = true;
                tryingConnect = true;

                if (debug) {
                    APISource.getConsole().send("The connection has been established, waiting for the server to validate the connection", Level.INFO, server, sv_port);
                }

                while (award_connection) {
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    socket.read(readBuffer);

                    if (tryingConnect) {
                        BUFFER.clear();
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF(getMAC());
                        out.writeBoolean(true);
                        out.writeUTF("connect");
                        out.writeUTF(client_name);

                        BUFFER.put(out.toByteArray());
                        BUFFER.flip();

                        socket.write(BUFFER);

                        tryingConnect = false;
                    } else {
                        if (!operative) {
                            ByteArrayDataInput input = ByteStreams.newDataInput(readBuffer.array());
                            if (input.readBoolean()) {
                                if (input.readUTF().equalsIgnoreCase("accept")) {
                                    if (debug) {
                                        APISource.getConsole().send("Connection has been validated by the server", Level.INFO);
                                    }

                                    for (byte[] data : data_queue) {
                                        BUFFER.clear();
                                        BUFFER.put(data);
                                        BUFFER.flip();

                                        socket.write(BUFFER);
                                        data_queue.remove(data);
                                    }

                                    award_connection = false;
                                    operative = true;

                                    if (instant_close) {
                                        close();
                                    }
                                }
                            }
                        }
                    }
                }

                result.complete(true);

                while (operative) {
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    int read = socket.read(readBuffer);

                    if (read == 0) {
                        for (byte[] queue : data_queue) {
                            BUFFER.clear();
                            BUFFER.put(queue);
                            BUFFER.flip();

                            socket.write(BUFFER);
                            data_queue.remove(queue);
                        }
                    } else {
                        ByteArrayDataInput input = ByteStreams.newDataInput(readBuffer.array());
                        if (input.readBoolean()) {
                            String command = input.readUTF();
                            String argument = input.readUTF();

                            switch (command.toLowerCase()) {
                                case "success":
                                    switch (argument.toLowerCase()) {
                                        case "rename":
                                            client_name = input.readUTF();

                                            if (debug) {
                                                APISource.getConsole().send("Server accepted the new client name: {0}", Level.INFO, client_name);
                                            }
                                            break;
                                        case "message":
                                            if (debug) {
                                                APISource.getConsole().send("{0} to server: {1}", Level.INFO, input.readUTF(), input.readUTF());
                                            }
                                            break;
                                        case "unknown":
                                            if (debug) {
                                                APISource.getConsole().send("{0} ran custom command: {1} ( {2} )", Level.INFO, input.readUTF(), input.readUTF(), input.readUTF());
                                            }
                                            break;
                                        default:
                                            if (debug) {
                                                APISource.getConsole().send("Unknown command from server: {0} ( {1} )", Level.WARNING, command, argument);
                                            }
                                            break;
                                    }
                                    break;
                                case "failed":
                                    switch (argument.toLowerCase()) {
                                        case "connect":
                                            APISource.getConsole().send("Server declined connection as {0}, because: {1}", Level.GRAVE, input.readUTF(), input.readUTF());
                                            break;
                                        case "rename":
                                            APISource.getConsole().send("Failed to change client name to {0}: {1}", Level.GRAVE, input.readUTF(), input.readUTF());
                                            break;
                                        case "disconnect":
                                            APISource.getConsole().send("Failed while trying to disconnect the server ( you've been disconnected anyway ): {0}", Level.GRAVE, input.readUTF());
                                            break;
                                        case "message":
                                            APISource.getConsole().send("Failed while trying to send a message to server: {0}", Level.GRAVE, input.readUTF());
                                            break;
                                        case "unknown":
                                            APISource.getConsole().send("Failed while trying to execute custom command {0} with argument {1}: {2}", Level.GRAVE, input.readUTF(), input.readUTF(), input.readUTF());
                                            break;
                                        default:
                                            if (debug) {
                                                APISource.getConsole().send("Unknown command from server: {0} ( {1} )", Level.WARNING, command, argument);
                                            }
                                            break;
                                    }
                            }
                        } else {
                            APISource.getConsole().send("New message from server", Level.INFO);
                        }
                    }
                }
            } catch (Throwable ex) {
                result.complete(false);
            }
        });
        thread.start();

        return result;
    }

    /**
     * Get the client name
     *
     * @return the client name
     */
    @Override
    public String getName() {
        return client_name;
    }

    /**
     * Get the client MAC address
     *
     * @return the client MAC address
     */
    @Override
    public String getMAC() {
        try {
            NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            byte[] macArray = network.getHardwareAddress();

            StringBuilder str = new StringBuilder();
            for (int i = 0; i < macArray.length; i++) {
                str.append(String.format("%02X%s", macArray[i], (i < macArray.length - 1) ? ":" : ""));
            }

            return str.toString();
        } catch (Throwable ex) {
            System.out.println("Failed to locate MAC address...");
            System.exit(1);
            return null;
        }
    }

    /**
     * Get the connected remote server
     *
     * @return the connected remote server
     */
    @Override
    public RemoteServer getServer() {
        return null;
    }

    /**
     * Get the client work level
     *
     * @return the client work level
     */
    @Override
    public WorkLevel getWorkLevel() {
        return WorkLevel.TCP;
    }

    /**
     * Rename the client on the server interface
     *
     * @param name the client name
     */
    @Override
    public void rename(final String name) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(getMAC());
        out.writeBoolean(true);
        out.writeUTF("rename");
        out.writeUTF(name);

        try {
            if (debug) {
                APISource.getConsole().send("Trying to inform the server about the name change request to {0}", Level.INFO, name);
            }

            BUFFER.clear();
            BUFFER.put(out.toByteArray());
            BUFFER.flip();

            socket.write(BUFFER);
        } catch (Throwable ex) {
            data_queue.add(out.toByteArray());
        }
    }

    /**
     * Send data to the server
     *
     * @param data the data to send
     */
    @Override
    public void send(final byte[] data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(getMAC());
        out.writeBoolean(false);
        out.writeInt((out.toByteArray().length + 4));
        out.write(data);

        try {
            BUFFER.clear();
            BUFFER.put(out.toByteArray());
            BUFFER.flip();

            socket.write(BUFFER);
        } catch (Throwable ex) {
            data_queue.add(out.toByteArray());
        }
    }

    /**
     * Close the connection
     */
    @Override
    public void close() {
        if (operative) {
            try {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(getMAC());
                out.writeBoolean(true);
                out.writeUTF("disconnect");
                out.writeUTF("Client disconnect request");

                data_queue.add(out.toByteArray());
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } else {
            instant_close = true;
        }
    }
}
