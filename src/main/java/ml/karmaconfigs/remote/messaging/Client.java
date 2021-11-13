package ml.karmaconfigs.remote.messaging;

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

import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerDisconnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Remote message client interface
 */
public final class Client {

    private final static byte[] BUFFER = new byte[1024];
    private final static String client_name = "client_" + new Random().nextInt(Integer.MAX_VALUE);
    private final static Set<DatagramPacket> packet_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static String server = "127.0.0.1";

    private static int sv_port = 49305;
    private static int client = 49300;

    private static boolean debug = false;
    private static boolean operative = false;
    private static boolean award_connection = false;
    private static boolean instant_close = false;

    private static DatagramSocket socket;

    /**
     * Initialize a default client that
     * will connect to local server at
     * default port 49305
     */
    public Client() {}

    /**
     * Initialize a client that will connect
     * to the specified server at specified port
     *
     * @param server_host the server
     * @param server_port the server port
     */
    public Client(final String server_host, final int server_port) {
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
    public Client(final int client_port, final String server_host, final int server_port) {
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
    public Client debug(final boolean status) {
        debug = status;

        return this;
    }

    /**
     * Try to connect to the server
     *
     * @return a completable future when the client connects
     */
    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> result = new CompletableFuture<>();

        Thread thread = new Thread(() -> {
            try {
                socket = new DatagramSocket(client);

                if (debug) {
                    System.out.println("Client........................ Initialized RM client");
                }

                DatagramPacket connect_packet = createPacket("{COMMAND:RM_CONNECT:" + client_name + "}");

                if (connect_packet != null) {
                    socket.send(connect_packet);
                    award_connection = true;
                }

                result.complete(null);

                while (award_connection) {
                    DatagramPacket packet = new DatagramPacket(BUFFER, BUFFER.length);
                    socket.receive(packet);

                    InetAddress incoming = packet.getAddress();
                    int port = packet.getPort();

                    RemoteServer remote = new RemoteServer(incoming, port, socket);
                    if (incoming.getHostAddress().equals(server) && port == sv_port) {
                        try {
                            String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                            if (text.startsWith("{COMMAND:") && text.endsWith("}")) {
                                text = text.replaceFirst("\\{COMMAND:", "");
                                text = text.substring(0, text.length() - 1);

                                if (text.contains(":")) {
                                    String[] data = text.split(":");
                                    String command = data[0];
                                    String information = text.replaceFirst(command + ":", "");

                                    switch (command.toLowerCase()) {
                                        case "rm_disconnect":
                                            if (debug) {
                                                System.out.println("Client........................ Connection killed by server: " + information);

                                                ServerDisconnectEvent event = new ServerDisconnectEvent(remote, information);
                                                RemoteListener.callClientEvent(event);
                                            }
                                            break;
                                        case "rm_accept":
                                            if (debug) {
                                                System.out.println("Client........................ Connection accepted by server: " + information);
                                            }

                                            ServerConnectEvent event = new ServerConnectEvent(remote);
                                            RemoteListener.callClientEvent(event);

                                            operative = true;
                                            award_connection = false;
                                            for (DatagramPacket queue : packet_queue)
                                                socket.send(queue);

                                            packet_queue.clear();

                                            if (instant_close) {
                                                close();
                                            }

                                            break;
                                        default:
                                            if (debug) {
                                                System.out.println("Client........................ Unknown command from " + server + ":" + port + " -> " + command + ": " + information);
                                            }
                                            break;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }

                while (operative) {
                    DatagramPacket packet = new DatagramPacket(BUFFER, BUFFER.length);
                    socket.receive(packet);

                    InetAddress incoming = packet.getAddress();
                    int port = packet.getPort();

                    RemoteServer remote = new RemoteServer(incoming, port, socket);
                    if (incoming.getHostAddress().equals(server) && port == sv_port) {
                        try {
                            String text = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                            if (text.startsWith("{COMMAND:") && text.endsWith("}")) {
                                text = text.replaceFirst("\\{COMMAND:", "");
                                text = text.substring(0, text.length() - 1);

                                if (text.contains(":")) {
                                    String[] data = text.split(":");
                                    String command = data[0];
                                    String information = text.replaceFirst(command + ":", "");

                                    if (command.equalsIgnoreCase("rm_disconnect")) {
                                        if (debug) {
                                            System.out.println("Client........................ Connection killed by server: " + information);
                                        }

                                        socket.close();
                                        socket = null;

                                        ServerDisconnectEvent event = new ServerDisconnectEvent(remote, information);
                                        RemoteListener.callClientEvent(event);

                                        operative = false;
                                    }
                                }
                            } else {
                                ServerMessageEvent event = new ServerMessageEvent(remote, Arrays.copyOfRange(packet.getData(), 0, packet.getLength()));
                                RemoteListener.callClientEvent(event);
                            }
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        });
        thread.start();

        return result;
    }

    /**
     * Rename the client on the server interface
     *
     * @param name the client name
     */
    public void rename(final String name) {
        try {
            DatagramPacket assign_name_packet = createPacket("{COMMAND:RM_ASSIGN_NAME:" + name + "}");
            if (assign_name_packet != null) {
                socket.send(assign_name_packet);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Send data to the server
     *
     * @param data the data to send
     */
    public void send(final byte[] data) {
        if (award_connection) {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(server), sv_port);
                packet_queue.add(packet);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } else {
            if (operative) {
                try {
                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(server), sv_port);
                    socket.send(packet);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Close the connection
     */
    public void close() {
        try {
            if (!award_connection) {
                DatagramPacket packet = createPacket("{COMMAND:RM_DISCONNECT:Connection killed by client}");
                socket.send(packet);

                socket.close();
                socket = null;

                operative = false;
            } else {
                instant_close = true;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Create a packet
     *
     * @param data the packet data
     * @return the packet
     */
    private DatagramPacket createPacket(final String data) {
        try {
            return new DatagramPacket(data.getBytes(StandardCharsets.UTF_8), data.length(), InetAddress.getByName(server), sv_port);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
