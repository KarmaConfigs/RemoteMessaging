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
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientCommandEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientDisconnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.util.DisconnectReason;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remote message server interface
 */
public final class Server {

    private static int port = 49305;

    private static boolean debug = false;
    private static boolean operative = true;

    private static DatagramSocket socket;

    private final static byte[] BUFFER = new byte[1024];

    private final static Set<String> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final static Map<String, String> names = new ConcurrentHashMap<>();
    private final static Map<String, RemoteClient> clients = new ConcurrentHashMap<>();
    private final static Set<String> banned = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Initialize a default server on the
     * default port 49305
     */
    public Server() {}

    /**
     * Initialize the server on the specified
     * port
     *
     * @param custom_port the server port
     */
    public Server(final int custom_port) {
        port = custom_port;
    }

    /**
     * Set the server debug status
     *
     * @param status the server debug status
     * @return this instance
     */
    public Server debug(final boolean status) {
        debug = status;

        return this;
    }

    /**
     * Try to start the server
     *
     * @return a completable future when the server starts
     */
    public CompletableFuture<Void> start() {
        CompletableFuture<Void> result = new CompletableFuture<>();

        Thread thread = new Thread(() -> {
            try {
                socket = new DatagramSocket(port);

                if (debug) {
                    System.out.println("Server........................ Initialized RM server");
                }

                result.complete(null);

                while (operative) {
                    DatagramPacket packet = new DatagramPacket(BUFFER, BUFFER.length);
                    socket.receive(packet);

                    InetAddress incoming = packet.getAddress();
                    int port = packet.getPort();

                    RemoteClient client = getClient(incoming, port);
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
                                    case "rm_connect":
                                        if (!banned.contains(incoming.getHostAddress())) {
                                            if (!connections.contains(incoming.getHostAddress() + "/" + port)) {
                                                if (debug) {
                                                    System.out.println("Server........................ Connected new client from " + getName(incoming, port) + " -> " + command + ": " + information);
                                                }

                                                connections.add(incoming.getHostAddress() + "/" + port);
                                                names.put(incoming.getHostAddress() + "/" + port, information.replaceAll("\\s", "_").toLowerCase());

                                                String rText = "{COMMAND:RM_ACCEPT:Connection established successfully}";

                                                DatagramPacket response = new DatagramPacket(rText.getBytes(StandardCharsets.UTF_8), rText.getBytes(StandardCharsets.UTF_8).length, incoming, port);
                                                socket.send(response);

                                                ClientConnectEvent event = new ClientConnectEvent(client);
                                                RemoteListener.callServerEvent(event);
                                            } else {
                                                if (debug) {
                                                    System.out.println("Server........................ Client " + getName(incoming, port) + " tried to access the server but he's already connected!");
                                                }

                                                ClientDisconnectEvent event = new ClientDisconnectEvent(client, DisconnectReason.BANNED, "You are already connected to the server from another machine!");
                                                RemoteListener.callServerEvent(event);

                                                String rText = "{COMMAND:RM_DISCONNECT:" + event.getDisconnectMessage() + "!}";

                                                DatagramPacket response = new DatagramPacket(rText.getBytes(StandardCharsets.UTF_8), rText.getBytes(StandardCharsets.UTF_8).length, incoming, port);
                                                socket.send(response);
                                            }
                                        } else {
                                            if (debug) {
                                                System.out.println("Server........................ Banned client " + getName(incoming, port) + " tried to access the server");
                                            }

                                            connections.remove(incoming.getHostAddress() + "/" + port);
                                            names.remove(incoming.getHostAddress() + "/" + port);
                                            clients.remove(incoming.getHostAddress() + "/" + port);

                                            ClientDisconnectEvent event = new ClientDisconnectEvent(client, DisconnectReason.BANNED, "You are banned from this server!");
                                            RemoteListener.callServerEvent(event);

                                            String rText = "{COMMAND:RM_DISCONNECT:" + event.getDisconnectMessage() + "!}";

                                            DatagramPacket response = new DatagramPacket(rText.getBytes(StandardCharsets.UTF_8), rText.getBytes(StandardCharsets.UTF_8).length, incoming, port);
                                            socket.send(response);
                                        }
                                        break;
                                    case "rm_assign_name":
                                        if (connections.contains(incoming.getHostAddress() + "/" + port)) {
                                            if (debug) {
                                                System.out.println("Server........................ Client changed name " + getName(incoming, port) + " -> " + information);
                                            }

                                            names.put(incoming.getHostAddress() + "/" + port, information.replaceAll("\\s", "_").toLowerCase());
                                            clients.put(incoming.getHostAddress() + "/" + port, new RemoteClient(information, incoming, port, socket));
                                        }
                                        break;
                                    case "rm_disconnect":
                                        if (connections.contains(incoming.getHostAddress() + "/" + port)) {
                                            if (debug) {
                                                System.out.println("Server........................ Client " + getName(incoming, port) + " disconnected -> " + information);
                                            }

                                            connections.remove(incoming.getHostAddress() + "/" + port);
                                            names.remove(incoming.getHostAddress() + "/" + port);
                                            clients.remove(incoming.getHostAddress() + "/" + port);

                                            ClientDisconnectEvent event = new ClientDisconnectEvent(client, DisconnectReason.KILLED_BY_CLIENT, information);
                                            RemoteListener.callServerEvent(event);
                                        }
                                        break;
                                    default:
                                        if (connections.contains(incoming.getHostAddress() + "/" + port)) {
                                            if (debug) {
                                                System.out.println("Server........................ Unknown command from " + getName(incoming, port) + " -> " + command + ": " + information);
                                            }
                                        }
                                        break;
                                }

                                if (connections.contains(incoming.getHostAddress() + "/" + port)) {
                                    ClientCommandEvent event = new ClientCommandEvent(client, command, information, Arrays.copyOfRange(packet.getData(), 0, packet.getLength()));
                                    RemoteListener.callServerEvent(event);
                                }
                            }
                        } else {
                            if (connections.contains(incoming.getHostAddress() + "/" + port)) {
                                ClientMessageEvent event = new ClientMessageEvent(client, Arrays.copyOfRange(packet.getData(), 0, packet.getLength()));
                                RemoteListener.callServerEvent(event);
                            } else {
                                if (debug) {
                                    System.out.println("Refused message from " + incoming.getHostAddress() + "/" + port + " ( " + text + " ) -> PLEASE CONNECT BEFORE SENDING A MESSAGE");
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
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
     * Completely close the server
     */
    public void close() {
        operative = false;
    }

    /**
     * Send a message to each connected client
     *
     * @param data the data to send
     */
    public void broadcast(final byte[] data) {
        try {
            for (RemoteClient client : clients.values()) {
                client.sendMessage(data);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Redirect a message to the specified client
     *
     * @param name the client name
     * @param data the message
     */
    public void redirect(final String name, final byte[] data) {
        try {
            for (RemoteClient client : clients.values()) {
                if (client.getName().equals(name)) {
                    client.sendMessage(data);
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Ban an address from the server
     *
     * @param address the addresses to ban
     */
    public void ban(final String... address) {
        try {
            List<String> ban = Arrays.asList(address);
            for (RemoteClient client : clients.values()) {
                InetAddress ip = client.getHost();
                if (ban.contains(ip.getHostAddress())) {
                    int port = client.getPort();

                    ClientDisconnectEvent event = new ClientDisconnectEvent(client, DisconnectReason.BANNED, "You are banned from this server!");
                    RemoteListener.callServerEvent(event);

                    String rText = "{COMMAND:RM_DISCONNECT:" + event.getDisconnectMessage() + "}";

                    DatagramPacket response = new DatagramPacket(rText.getBytes(StandardCharsets.UTF_8), rText.getBytes(StandardCharsets.UTF_8).length, ip, port);
                    socket.send(response);

                    connections.remove(ip.getHostAddress() + "/" + port);
                    names.remove(ip.getHostAddress() + "/" + port);
                    clients.remove(ip.getHostAddress() + "/" + port);
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Kick an address from the server
     *
     * @param address the addresses to kick
     */
    public void kick(final String... address) {
        try {
            List<String> ban = Arrays.asList(address);
            for (RemoteClient client : clients.values()) {
                InetAddress ip = client.getHost();
                if (ban.contains(ip.getHostAddress())) {
                    int port = client.getPort();

                    ClientDisconnectEvent event = new ClientDisconnectEvent(client, DisconnectReason.BANNED, "You have been disconnected by server!");
                    RemoteListener.callServerEvent(event);

                    String rText = "{COMMAND:RM_DISCONNECT:" + event.getDisconnectMessage() + "}";

                    DatagramPacket response = new DatagramPacket(rText.getBytes(StandardCharsets.UTF_8), rText.getBytes(StandardCharsets.UTF_8).length, ip, port);
                    socket.send(response);

                    connections.remove(ip.getHostAddress() + "/" + port);
                    names.remove(ip.getHostAddress() + "/" + port);
                    clients.remove(ip.getHostAddress() + "/" + port);
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Unban an address from the server
     *
     * @param address the addresses to unban
     */
    public void unBan(final String... address) {
        Arrays.asList(address).forEach(banned::remove);
    }

    /**
     * Get the name of a client
     *
     * @param address the client address
     * @param port the client port
     * @return the client name
     */
    private String getName(final InetAddress address, final int port) {
        return names.getOrDefault(address.getHostAddress() + "/" + port, address.getHostAddress() + "/" + port);
    }

    /**
     * Get a client
     *
     * @param address the client address
     * @param port the client port
     * @return the client
     */
    private RemoteClient getClient(final InetAddress address, final int port) {
        RemoteClient client = clients.getOrDefault(
                address.getHostAddress() + "/" + port,
                null);

        if (client == null) {
            client = new RemoteClient(getName(address, port), address, port, socket);
            clients.put(address.getHostAddress() + "/" + port, client);
        }

        return client;
    }
}
