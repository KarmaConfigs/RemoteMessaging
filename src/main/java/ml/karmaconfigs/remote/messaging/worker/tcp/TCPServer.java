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
import ml.karmaconfigs.api.common.utils.file.PathUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.remote.messaging.Server;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientCommandEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientDisconnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.util.DisconnectReason;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;
import ml.karmaconfigs.remote.messaging.worker.tcp.remote.TCPRemoteClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remote message client interface
 */
@SuppressWarnings("UnstableApiUsage")
public final class TCPServer extends Server {

    private final static ByteBuffer BUFFER = ByteBuffer.allocate(4056);

    private final static Map<String, RemoteClient> clients = new ConcurrentHashMap<>();

    private final static Set<String> banned = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final static Set<String> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static String server = "127.0.0.1";

    private static int sv_port = 49305;

    private static boolean debug = false;
    private static boolean operative = true;

    private static ServerSocketChannel socket;

    /**
     * Initialize a default client that
     * will connect to local server at
     * default port 49305
     */
    public TCPServer() {}

    /**
     * Initialize a client with a custom port
     * that will connect to the specified server at the
     * specified port
     *
     * @param port the server port
     */
    public TCPServer(final int port) {
        sv_port = port;
    }

    /**
     * Initialize a client that will connect
     * to the specified server at specified port
     *
     * @param host the server
     * @param port the server port
     */
    public TCPServer(final String host, final int port) {
        server = host;
        sv_port = port;
    }

    /**
     * Set the client debug status
     *
     * @param status the client debug status
     * @return this instance
     */
    @Override
    public Server debug(final boolean status) {
        debug = status;

        return this;
    }

    /**
     * Try to start the server
     *
     * @return a completable future when the server starts
     */
    @Override
    public CompletableFuture<Boolean> start() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        Thread thread = new Thread(() -> {
            try {
                socket = ServerSocketChannel.open().bind(new InetSocketAddress(server, sv_port));
                socket.configureBlocking(false);

                result.complete(true);

                while (operative) {
                    try {
                        SocketChannel channel = socket.accept();
                        if (channel != null) {
                            new Thread(() -> {
                                while (channel.isConnected()) {
                                    try {
                                        BUFFER.clear();
                                        channel.read(BUFFER);

                                        InetAddress incoming = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
                                        int port = ((InetSocketAddress) channel.getRemoteAddress()).getPort();
                                        String default_name = incoming.getHostAddress() + "/" + port;

                                        ByteArrayDataInput input = ByteStreams.newDataInput(BUFFER.array());
                                        String mac = input.readUTF();

                                        RemoteClient client = getClient(default_name, mac, incoming, port, channel);
                                        if (input.readBoolean()) {
                                            String command = input.readUTF();
                                            String argument = input.readUTF();

                                            switch (command.toLowerCase()) {
                                                case "connect":
                                                    if (!banned.contains(mac)) {
                                                        if (debug) {
                                                            APISource.getConsole().send("Client {0} connected as {1}", Level.INFO, default_name, argument);
                                                        }

                                                        client = new TCPRemoteClient(argument, mac, incoming, port, channel);
                                                        clients.put(default_name, client);
                                                        connections.add(default_name);

                                                        ClientConnectEvent event = new ClientConnectEvent(client);
                                                        RemoteListener.callServerEvent(event);

                                                        ByteBuffer writeBuffer = ByteBuffer.allocate(10240);

                                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                                        out.writeBoolean(true);
                                                        out.writeUTF("accept");
                                                        out.writeUTF(getMAC());

                                                        writeBuffer.put(out.toByteArray());
                                                        writeBuffer.flip();

                                                        channel.write(writeBuffer);
                                                    } else {
                                                        ByteBuffer writeBuffer = ByteBuffer.allocate(10240);

                                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                                        out.writeUTF(getMAC());
                                                        out.writeBoolean(true);
                                                        out.writeUTF("You are banned from this server!");

                                                        writeBuffer.put(out.toByteArray());
                                                        writeBuffer.flip();

                                                        channel.write(writeBuffer);
                                                    }
                                                    break;
                                                case "rename":
                                                    if (connections.contains(default_name)) {
                                                        if (debug) {
                                                            APISource.getConsole().send("Client {0} is now known as {1}", Level.INFO, client.getName(), argument);
                                                        }

                                                        client = new TCPRemoteClient(argument, mac, incoming, port, channel);
                                                        clients.put(default_name, client);

                                                        ClientCommandEvent event = new ClientCommandEvent(client, command, argument, BUFFER.array());
                                                        RemoteListener.callServerEvent(event);

                                                        ByteBuffer writeBuffer = ByteBuffer.allocate(10240);

                                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                                        out.writeUTF(getMAC());
                                                        out.writeBoolean(true);
                                                        out.writeUTF("success");
                                                        out.writeUTF("rename");
                                                        out.writeUTF(argument);

                                                        writeBuffer.put(out.toByteArray());
                                                        writeBuffer.flip();

                                                        channel.write(writeBuffer);
                                                    } else {
                                                        ByteBuffer writeBuffer = ByteBuffer.allocate(10240);

                                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                                        out.writeUTF(getMAC());
                                                        out.writeBoolean(true);
                                                        out.writeUTF("failed");
                                                        out.writeUTF("rename");
                                                        out.writeUTF(argument);
                                                        out.writeUTF("You are not connected to this server!");

                                                        writeBuffer.put(out.toByteArray());
                                                        writeBuffer.flip();

                                                        channel.write(writeBuffer);
                                                    }
                                                    break;
                                                case "disconnect":
                                                    if (connections.contains(default_name)) {
                                                        if (debug) {
                                                            APISource.getConsole().send("Client {0} left the server ( {1} )", Level.INFO, client.getName(), argument);
                                                        }

                                                        clients.remove(default_name);
                                                        connections.remove(default_name);

                                                        ClientDisconnectEvent event = new ClientDisconnectEvent(client, DisconnectReason.KILLED_BY_CLIENT, argument);
                                                        RemoteListener.callServerEvent(event);
                                                    } else {
                                                        ByteBuffer writeBuffer = ByteBuffer.allocate(10240);

                                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                                        out.writeUTF(getMAC());
                                                        out.writeBoolean(true);
                                                        out.writeUTF("failed");
                                                        out.writeUTF("disconnect");
                                                        out.writeUTF("You are not connected to this server!");

                                                        writeBuffer.put(out.toByteArray());
                                                        writeBuffer.flip();

                                                        channel.write(writeBuffer);
                                                    }
                                                    break;
                                                default:
                                                    if (connections.contains(default_name)) {
                                                        if (debug) {
                                                            APISource.getConsole().send("Unknown command from {0}: {1} ( {2} )", Level.WARNING, client.getName(), command, argument);
                                                        }

                                                        ClientCommandEvent event = new ClientCommandEvent(client, command, argument, BUFFER.array());
                                                        RemoteListener.callServerEvent(event);
                                                    } else {
                                                        ByteBuffer writeBuffer = ByteBuffer.allocate(10240);

                                                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                                        out.writeUTF(getMAC());
                                                        out.writeBoolean(true);
                                                        out.writeUTF("failed");
                                                        out.writeUTF("unknown");
                                                        out.writeUTF(command);
                                                        out.writeUTF(argument);
                                                        out.writeUTF("You are not connected to this server!");

                                                        writeBuffer.put(out.toByteArray());
                                                        writeBuffer.flip();

                                                        channel.write(writeBuffer);
                                                    }
                                                    break;
                                            }
                                        } else {
                                            if (connections.contains(default_name)) {
                                                int offset = input.readInt();
                                                int max = BUFFER.array().length;

                                                List<Byte> newArray = new ArrayList<>();
                                                for (int i = 0; i < max; i++) {
                                                    if (i >= offset) {
                                                        newArray.add(BUFFER.array()[i]);
                                                    }
                                                }

                                                byte[] fixedArray = new byte[newArray.size()];
                                                for (int i = 0; i < newArray.size(); i++)
                                                    fixedArray[i] = newArray.get(i);

                                                String line = new String(fixedArray, StandardCharsets.UTF_8);

                                                ByteBuffer writeBuffer = ByteBuffer.allocate(90128);

                                                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                                out.writeUTF(getMAC());
                                                out.writeBoolean(true);
                                                out.writeUTF("success");
                                                out.writeUTF("message");
                                                out.writeUTF(client.getName());

                                                writeBuffer.put(out.toByteArray());
                                                writeBuffer.flip();

                                                channel.write(writeBuffer);

                                                ClientMessageEvent event = new ClientMessageEvent(client, fixedArray);
                                                RemoteListener.callServerEvent(event);
                                            } else {
                                                if (debug) {
                                                    APISource.getConsole().send("Denying message from {0} because he's not connected to server", Level.WARNING, default_name);
                                                }

                                                ByteBuffer writeBuffer = ByteBuffer.allocate(10240);

                                                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                                                out.writeUTF(getMAC());
                                                out.writeBoolean(true);
                                                out.writeUTF("failed");
                                                out.writeUTF("message");
                                                out.writeUTF("You are not connected to this server!");

                                                writeBuffer.put(out.toByteArray());
                                                writeBuffer.flip();

                                                channel.write(writeBuffer);
                                            }
                                        }
                                    } catch (Throwable ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        System.exit(1);
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
     * Get the server MAC address
     *
     * @return the server MAC address
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
     * Get the connected clients
     *
     * @return a list of connected clients
     */
    @Override
    public Set<RemoteClient> getClients() {
        return new HashSet<>(clients.values());
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
     * Close the connection
     */
    @Override
    public void close() {
        operative = false;
        try {
            socket.close();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Export the list of bans
     *
     * @param destination the file were to store
     *                    the ban list
     */
    @Override
    public void exportBans(final Path destination) {
        try {
            PathUtilities.create(destination);

            String serialized = StringUtils.serialize(new ArrayList<>(banned));
            Files.write(destination, serialized.getBytes(), StandardOpenOption.CREATE);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Load the list of bans
     *
     * @param bans the file were the banned mac
     *             addresses are stored
     */
    @Override
    public void loadBans(final Path bans) {
        try {
            PathUtilities.create(bans);

            byte[] result = Files.readAllBytes(bans);
            Object serialized = StringUtils.load(new String(result));

            if (serialized instanceof ArrayList) {
                ArrayList<?> list = (ArrayList<?>) serialized;
                for (Object obj : list)
                    ban(String.valueOf(obj));
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Send a message to each connected client
     *
     * @param data the data to send
     */
    @Override
    public void broadcast(final byte[] data) {
        for (RemoteClient client : clients.values()) {
            client.sendMessage(data);
        }
    }

    /**
     * Redirect a message to the specified client
     *
     * @param name the client name
     * @param data the message
     */
    @Override
    public void redirect(final String name, final byte[] data) {
        for (RemoteClient client : clients.values()) {
            if (client.getName().equals(name) || client.getMAC().equals(name)) {
                client.sendMessage(data);
            }
        }
    }

    /**
     * Ban an address from the server
     *
     * @param macAddresses the addresses to ban
     */
    @Override
    public void ban(final String... macAddresses) {
        banned.addAll(Arrays.asList(macAddresses));

        for (RemoteClient client : clients.values()) {
            if (banned.contains(client.getMAC())) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(getMAC());
                out.writeBoolean(true);
                out.writeUTF("failed");
                out.writeUTF("disconnect");
                out.writeUTF("You have been banned from this server!");

                client.sendMessage(out.toByteArray());
            }
        }
    }

    /**
     * Kick an address from the server
     *
     * @param macAddresses the addresses to kick
     */
    @Override
    public void kick(final String... macAddresses) {
        List<String> macs = Arrays.asList(macAddresses);

        for (RemoteClient client : clients.values()) {
            if (macs.contains(client.getMAC())) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(getMAC());
                out.writeBoolean(true);
                out.writeUTF("failed");
                out.writeUTF("disconnect");
                out.writeUTF("You have been kicked from this server!");

                client.sendMessage(out.toByteArray());
            }
        }
    }

    /**
     * Unban an address from the server
     *
     * @param macAddresses the addresses to unban
     */
    @Override
    public void unBan(final String... macAddresses) {
        Arrays.asList(macAddresses).forEach(banned::remove);
    }

    /**
     * Get the client
     *
     * @param name the client name
     * @param address the client address
     * @param port the client port
     * @param socket the client socket
     * @return the client
     */
    private RemoteClient getClient(final String name, final String mac, final InetAddress address, final int port, final SocketChannel socket) {
        RemoteClient client = clients.getOrDefault(address.getHostAddress() + "/" + port, null);
        if (client == null) {
            client = new TCPRemoteClient(name, mac, address, port, socket);
            clients.put(address.getHostAddress() + "/" + port, client);
        }

        return client;
    }
}
