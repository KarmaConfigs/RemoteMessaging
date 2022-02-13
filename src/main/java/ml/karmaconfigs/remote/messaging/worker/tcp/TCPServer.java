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

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.ConcurrentList;
import ml.karmaconfigs.api.common.utils.PrefixConsoleData;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.PathUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.remote.messaging.platform.Server;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientCommandEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientDisconnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.util.DisconnectReason;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;
import ml.karmaconfigs.remote.messaging.util.message.*;
import ml.karmaconfigs.remote.messaging.worker.tcp.remote.TCPRemoteClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Remote message client interface
 */
public final class TCPServer extends Server {
    
    private final List<MessageInput> queue = new ConcurrentList<>();

    private final Map<String, RemoteClient> clients = new ConcurrentHashMap<>();

    private final Set<String> banned = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private String server = "127.0.0.1";

    private int sv_port = 49305;

    private boolean debug = false;
    private boolean operative = false;

    private ServerSocketChannel socket;
    private int processed = 0;

    private String key = "";

    private final Console console = new Console(this);
    
    /**
     * Initialize a default client that
     * will connect to local server at
     * default port 49305
     */
    public TCPServer() {
        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[TCP Server (&aOK&3)]&b ");
        data.setInfoPrefix("&3[TCP Server (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[TCP Server (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[TCP Server (&4ERROR&3)]&b ");
    }

    /**
     * Initialize a client with a custom port
     * that will connect to the specified server at the
     * specified port
     *
     * @param port the server port
     */
    public TCPServer(final int port) {
        sv_port = port;

        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[TCP Server (&aOK&3)]&b ");
        data.setInfoPrefix("&3[TCP Server (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[TCP Server (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[TCP Server (&4ERROR&3)]&b ");
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

        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[TCP Server (&aOK&3)]&b ");
        data.setInfoPrefix("&3[TCP Server (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[TCP Server (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[TCP Server (&4ERROR&3)]&b ");
    }

    /**
     * Set the server debug status
     *
     * @param status the server debug status
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
    public LateScheduler<Boolean> start() {
        if (!operative) {
            LateScheduler<Boolean> result = new AsyncLateScheduler<>();

            Thread thread = new Thread(() -> {
                try {
                    socket = ServerSocketChannel.open().bind(new InetSocketAddress(server, sv_port));
                    socket.configureBlocking(false);

                    result.complete(true);

                    operative = true;

                    while (operative) {
                        try {
                            SocketChannel channel = socket.accept();
                            if (channel != null) {
                                new Thread(() -> {
                                    while (channel.isConnected()) {
                                        try {
                                            //Allocate 512 of memory
                                            ByteBuffer tmpBuffer = ByteBuffer.allocate(5120);
                                            channel.read(tmpBuffer);

                                            InetAddress incoming = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
                                            int port = ((InetSocketAddress) channel.getRemoteAddress()).getPort();
                                            String default_name = incoming.getHostAddress() + "/" + port;

                                            ByteBuffer BUFFER = DataFixer.fixBuffer(tmpBuffer);

                                            MessageInput input = new MessageDataInput(BUFFER.array());
                                            if (!queue.isEmpty()) {
                                                queue.add(input);
                                                input = queue.get(0);
                                            }

                                            String mac = input.getString("MAC");

                                            RemoteClient client = getClient(default_name, mac, incoming, port, channel);
                                            if (input.getBoolean("COMMAND_ENABLED")) {
                                                String command = input.getString("COMMAND");
                                                String argument = input.getString("ARGUMENT");

                                                if (command != null && argument != null) {
                                                    switch (command.toLowerCase()) {
                                                        case "connect":
                                                            boolean sendDecline = true;
                                                            boolean validKey = true;
                                                            if (!banned.contains(mac)) {
                                                                if (!StringUtils.isNullOrEmpty(key)) {
                                                                    String provided = input.getString("ACCESS_KEY");
                                                                    if (provided != null) {
                                                                        //Basically, an 'access' password
                                                                        validKey = provided.equals(key);
                                                                    } else {
                                                                        validKey = false;
                                                                    }
                                                                }

                                                                if (validKey) {
                                                                    sendDecline = false;
                                                                    if (debug) {
                                                                        console.send("Client {0} connected as {1}", Level.OK, default_name, argument);
                                                                    }

                                                                    client = new TCPRemoteClient(argument, mac, incoming, port, channel);
                                                                    clients.put(default_name, client);
                                                                    connections.add(default_name);

                                                                    ClientConnectEvent event = new ClientConnectEvent(client);
                                                                    RemoteListener.callServerEvent(event);

                                                                    MessageOutput output = new MessageDataOutput();
                                                                    output.write("MAC", getMAC());
                                                                    output.write("COMMAND_ENABLED", true);
                                                                    output.write("COMMAND", "accept");

                                                                    byte[] compile = output.compile();
                                                                    ByteBuffer writeBuffer = ByteBuffer.wrap(compile);

                                                                    channel.write(writeBuffer);
                                                                }
                                                            }

                                                            if (sendDecline) {
                                                                MessageOutput output = new MessageDataOutput();
                                                                output.write("MAC", getMAC());
                                                                output.write("COMMAND_ENABLED", true);
                                                                output.write("COMMAND", "decline");
                                                                output.write("ARGUMENT", "connect");
                                                                output.write("COMMAND_ARGUMENT", (validKey ? "You are banned from this server!" : "The provided access key is not valid for this server!"));

                                                                byte[] compile = output.compile();
                                                                ByteBuffer writeBuffer = ByteBuffer.wrap(compile);

                                                                channel.write(writeBuffer);
                                                            }
                                                            break;
                                                        case "rename":
                                                            if (connections.contains(default_name)) {
                                                                if (debug) {
                                                                    console.send("Client {0} is now known as {1}", Level.WARNING, client.getName(), argument);
                                                                }

                                                                client = new TCPRemoteClient(argument, mac, incoming, port, channel);
                                                                clients.put(default_name, client);

                                                                ClientCommandEvent event = new ClientCommandEvent(client, command, argument);
                                                                RemoteListener.callServerEvent(event);

                                                                MessageOutput output = new MessageDataOutput();
                                                                output.write("MAC", getMAC());
                                                                output.write("COMMAND_ENABLED", true);
                                                                output.write("COMMAND", "success");
                                                                output.write("ARGUMENT", "rename");
                                                                output.write("ARGUMENT_DATA", argument);

                                                                byte[] compile = output.compile();
                                                                ByteBuffer writeBuffer = ByteBuffer.wrap(compile);

                                                                channel.write(writeBuffer);
                                                            } else {
                                                                MessageOutput output = new MessageDataOutput();
                                                                output.write("MAC", getMAC());
                                                                output.write("COMMAND_ENABLED", true);
                                                                output.write("COMMAND", "failed");
                                                                output.write("ARGUMENT", "rename");
                                                                output.write("ARGUMENT_DATA", argument + "," + "You are not connected to this server!");

                                                                byte[] compile = output.compile();
                                                                ByteBuffer writeBuffer = ByteBuffer.wrap(compile);

                                                                channel.write(writeBuffer);
                                                            }
                                                            break;
                                                        case "disconnect":
                                                            if (connections.contains(default_name)) {
                                                                if (debug) {
                                                                    console.send("Client {0} left the server ( {1} )", Level.WARNING, client.getName(), argument);
                                                                }

                                                                clients.remove(default_name);
                                                                connections.remove(default_name);

                                                                ClientDisconnectEvent event = new ClientDisconnectEvent(client, DisconnectReason.KILLED_BY_CLIENT, argument);
                                                                RemoteListener.callServerEvent(event);
                                                            } else {
                                                                MessageOutput output = new MessageDataOutput();
                                                                output.write("MAC", getMAC());
                                                                output.write("COMMAND_ENABLED", true);
                                                                output.write("COMMAND", "failed");
                                                                output.write("ARGUMENT", "disconnect");
                                                                output.write("ARGUMENT_DATA", "You are not connected to this server!");

                                                                byte[] compile = output.compile();
                                                                ByteBuffer writeBuffer = ByteBuffer.wrap(compile);

                                                                channel.write(writeBuffer);
                                                            }
                                                            break;
                                                        default:
                                                            if (connections.contains(default_name)) {
                                                                if (debug) {
                                                                    console.send("Unknown command from {0}: {1} ( {2} )", Level.WARNING, client.getName(), command, argument);
                                                                }

                                                                ClientCommandEvent event = new ClientCommandEvent(client, command, argument);
                                                                RemoteListener.callServerEvent(event);
                                                            } else {
                                                                MessageOutput output = new MessageDataOutput();
                                                                output.write("MAC", getMAC());
                                                                output.write("COMMAND_ENABLED", true);
                                                                output.write("COMMAND", "failed");
                                                                output.write("ARGUMENT", "unknown");
                                                                output.write("ARGUMENT_DATA", command + "," + argument + "," + "You are not connected to this server!");

                                                                byte[] compile = output.compile();
                                                                ByteBuffer writeBuffer = ByteBuffer.wrap(compile);

                                                                channel.write(writeBuffer);
                                                            }
                                                            break;
                                                    }
                                                }
                                            } else {
                                                if (connections.contains(default_name)) {
                                                    MessageOutput output = new MessageDataOutput();
                                                    output.write("MAC", getMAC());
                                                    output.write("COMMAND_ENABLED", true);
                                                    output.write("COMMAND", "success");
                                                    output.write("ARGUMENT", "message");
                                                    output.write("ARGUMENT_DATA", client.getName());

                                                    byte[] compile = output.compile();
                                                    ByteBuffer writeBuffer = ByteBuffer.wrap(compile);

                                                    channel.write(writeBuffer);

                                                    ClientMessageEvent event = new ClientMessageEvent(client, input);
                                                    RemoteListener.callServerEvent(event);
                                                } else {
                                                    if (debug) {
                                                        console.send("Denying message from {0} because he's not connected to server", Level.INFO, default_name);
                                                    }

                                                    MessageOutput output = new MessageDataOutput();
                                                    output.write("MAC", getMAC());
                                                    output.write("COMMAND_ENABLED", true);
                                                    output.write("COMMAND", "failed");
                                                    output.write("ARGUMENT", "message");
                                                    output.write("ARGUMENT_DATA", "You are not connected to this server!");

                                                    byte[] compile = output.compile();
                                                    ByteBuffer writeBuffer = ByteBuffer.wrap(compile);

                                                    channel.write(writeBuffer);
                                                }
                                            }
                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                        }

                                        processed++;
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

        return null;
    }

    /**
     * Try to start the server
     *
     * @param accessKey the server access key
     * @return a completable future when the server starts
     */
    @Override
    public LateScheduler<Boolean> start(final String accessKey) {
        if (!operative) {
            key = accessKey;
            return start();
        }

        return null;
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
    public void broadcast(final MessageOutput data) {
        int max = queue.size();
        int expected = processed + max;
        new Thread(() -> {
            long wait = 0;

            //Before doing anything, we must make sure the queue is not busy, if that's the case
            //the server will keep this thread busy until it can proceed, based on the moment max tasks
            //and on the moment executed tasks
            while (processed < expected) {
                wait++;
            }

            if (debug) {
                long sec = TimeUnit.MILLISECONDS.toSeconds(wait);

                console.send("Sent message to everyone after {0} {1}", Level.OK, (sec > 0 ? sec : wait), (sec > 0 ? "seconds" : "ms"));
            }

            for (RemoteClient client : clients.values()) {
                client.sendMessage(data);
            }
        }).start();
    }

    /**
     * Redirect a message to the specified client
     *
     * @param name the client name
     * @param data the message
     */
    @Override
    public void redirect(final String name, final MessageOutput data) {
        int max = queue.size();
        int expected = processed + max;
        new Thread(() -> {
            long wait = 0;

            //Before doing anything, we must make sure the queue is not busy, if that's the case
            //the server will keep this thread busy until it can proceed, based on the moment max tasks
            //and on the moment executed tasks
            while (processed < expected) {
                wait++;
            }

            if (debug) {
                long sec = TimeUnit.MILLISECONDS.toSeconds(wait);
                console.send("Sent message to {0} after {1} {2}", Level.OK, name, (sec > 0 ? sec : wait), (sec > 0 ? "seconds" : "ms"));
            }

            for (RemoteClient client : clients.values()) {
                if (client.getName().equals(name) || client.getMAC().equals(name)) {
                    client.sendMessage(data);
                }
            }
        }).start();

        /*
        Old method, efficient but had some problems

        if (queue.isEmpty()) {
            for (RemoteClient client : clients.values()) {
                System.out.println(client.getName() + "/" + client.getMAC());

                if (client.getName().equals(name) || client.getMAC().equals(name)) {
                    client.sendMessage(data);
                }
            }
        }*/
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
                MessageOutput output = new MessageDataOutput();
                output.write("MAC", getMAC());
                output.write("COMMAND_ENABLED", true);
                output.write("COMMAND", "DISCONNECT");
                output.write("ARGUMENT", "");
                output.write("ARGUMENT_DATA", "You have been banned from this server!");

                client.sendMessage(output);
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
                MessageOutput output = new MessageDataOutput();
                output.write("MAC", getMAC());
                output.write("COMMAND_ENABLED", true);
                output.write("COMMAND", "DISCONNECT");
                output.write("ARGUMENT", "");
                output.write("ARGUMENT_DATA", "You have been kicked from this server!");

                client.sendMessage(output);
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

    /**
     * Karma source name
     *
     * @return the source name
     */
    @Override
    public String name() {
        return "TCP Server";
    }

    /**
     * Karma source version
     *
     * @return the source version
     */
    @Override
    public String version() {
        return "0";
    }

    /**
     * Karma source description
     *
     * @return the source description
     */
    @Override
    public String description() {
        return "TCP server to allow TCP Clients from RemoteMessaging API connect";
    }

    /**
     * Karma source authors
     *
     * @return the source authors
     */
    @Override
    public String[] authors() {
        return new String[]{"KarmaDev"};
    }

    /**
     * Karma source update URL
     *
     * @return the source update URL
     */
    @Override
    public String updateURL() {
        return null;
    }

    /**
     * Get the source out
     *
     * @return the source out
     */
    @Override
    public Console console() {
        return console;
    }
}
