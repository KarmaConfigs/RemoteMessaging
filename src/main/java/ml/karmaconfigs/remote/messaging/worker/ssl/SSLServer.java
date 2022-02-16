package ml.karmaconfigs.remote.messaging.worker.ssl;

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
import ml.karmaconfigs.remote.messaging.platform.SecureServer;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientCommandEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientDisconnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.util.DisconnectReason;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;
import ml.karmaconfigs.remote.messaging.util.message.*;
import ml.karmaconfigs.remote.messaging.worker.ssl.remote.SSLRemoteClient;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Remote message client interface
 */
public final class SSLServer extends SecureServer {
    
    private final List<MessageInput> queue = new ConcurrentList<>();

    private final Map<String, RemoteClient> clients = new ConcurrentHashMap<>();
    private final Map<Socket, String> connections = new ConcurrentHashMap<>();

    private final Set<String> banned = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private String server = "127.0.0.1";

    private int sv_port = 49305;

    private boolean debug = false;
    private boolean operative = false;

    private SSLServerSocket socket;
    private int processed = 0;

    private String key = "";

    private final Console console = new Console(this);

    private final String password;
    private final String name;
    private final String extension;
    private final String type;

    private String protocol = "TLSv1.3";
    private int max_connections = 50;

    private String[] protocols = new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"};
    private String[] ciphers = new String[]{"TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"};

    private Path parent = getDataPath().resolve("certs");

    /**
     * Initialize a default client that
     * will connect to local server at
     * default port 49305
     *
     * @param pwd the certificate password
     * @param nm the certificate base name
     * @param ext the certificate file extension
     * @param tp the certificate type
     */
    public SSLServer(final String pwd, final String nm, final String ext, final String tp) {
        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[SSL Server (&aOK&3)]&b ");
        data.setInfoPrefix("&3[SSL Server (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[SSL Server (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[SSL Server (&4ERROR&3)]&b ");

        password = pwd;
        name = nm;
        extension = ext;
        type = tp;
    }

    /**
     * Initialize a client with a custom port
     * that will connect to the specified server at the
     * specified port
     *
     * @param pwd the certificate password
     * @param nm the certificate base name
     * @param ext the certificate file extension
     * @param tp the certificate type
     * @param port the server port
     */
    public SSLServer(final String pwd, final String nm, final String ext, final String tp, final int port) {
        sv_port = port;

        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[SSL Server (&aOK&3)]&b ");
        data.setInfoPrefix("&3[SSL Server (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[SSL Server (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[SSL Server (&4ERROR&3)]&b ");

        password = pwd;
        name = nm;
        extension = ext;
        type = tp;
    }

    /**
     * Initialize a client that will connect
     * to the specified server at specified port
     *
     * @param pwd the certificate password
     * @param nm the certificate base name
     * @param ext the certificate file extension
     * @param tp the certificate type
     * @param host the server
     * @param port the server port
     */
    public SSLServer(final String pwd, final String nm, final String ext, final String tp, final String host, final int port) {
        server = host;
        sv_port = port;

        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[SSL Server (&aOK&3)]&b ");
        data.setInfoPrefix("&3[SSL Server (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[SSL Server (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[SSL Server (&4ERROR&3)]&b ");

        password = pwd;
        name = nm;
        extension = ext;
        type = tp;
    }

    /**
     * Set the server debug status
     *
     * @param status the server debug status
     * @return this instance
     */
    @Override
    public SecureServer debug(final boolean status) {
        debug = status;

        return this;
    }

    /**
     * Set the current protocol
     *
     * @param p the protocol
     * @return this instance
     */
    @Override
    public SecureServer protocol(final String p) {
        protocol = p;

        return this;
    }

    /**
     * Set the server max connections
     *
     * @param m the server max connections amount
     * @return this instance
     */
    @Override
    public SecureServer maxConnections(final int m) {
        max_connections = m;

        return this;
    }

    /**
     * Set the allowed protocols
     *
     * @param p the protocols
     * @return this instance
     */
    @Override
    public SecureServer allowedProtocol(final String... p) {
        protocols = p;

        return this;
    }

    /**
     * Set the allowed ciphers
     *
     * @param c the ciphers
     * @return this instance
     */
    @Override
    public SecureServer allowedCiphers(final String... c) {
        ciphers = c;

        return this;
    }

    /**
     * Set the certificates location
     *
     * @param location the certificates location
     * @return the certificates location
     */
    @Override
    public SecureServer certsLocation(final Path location) {
        parent = location;

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
                    try {
                        if (!Files.exists(parent))
                            Files.createDirectories(parent);

                        Path serverKeyStore = parent.resolve(name + "." + extension);
                        Path trustedKeyStore = parent.resolve(name + "_trusted." + extension);

                        InputStream internalStorage = new FileInputStream(serverKeyStore.toFile());
                        InputStream internalTruster = new FileInputStream(trustedKeyStore.toFile());

                        KeyStore keyStore = KeyStore.getInstance(type);
                        keyStore.load(internalStorage, password.toCharArray());

                        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        kmf.init(keyStore, password.toCharArray());

                        KeyStore trustedStore = KeyStore.getInstance(type);
                        trustedStore.load(internalTruster, password.toCharArray());

                        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                        tmf.init(trustedStore);

                        SSLContext sc = SSLContext.getInstance(protocol);

                        TrustManager[] trustManagers = tmf.getTrustManagers();
                        KeyManager[] keyManagers = kmf.getKeyManagers();

                        sc.init(keyManagers, trustManagers, new SecureRandom());

                        SSLServerSocketFactory ssf = sc.getServerSocketFactory();
                        socket = (SSLServerSocket) ssf.createServerSocket(sv_port, max_connections, InetAddress.getByName(server));
                        socket.setNeedClientAuth(true);
                        socket.setWantClientAuth(true);
                        socket.setEnabledProtocols(protocols);
                        socket.setEnabledCipherSuites(ciphers);
                        //socket.getChannel().configureBlocking(false);

                        result.complete(true);
                    } catch (Throwable ex) {
                        result.complete(false, ex);
                        return;
                    }

                    operative = true;

                    new Thread(() -> {
                        while (operative) {
                            for (Socket channel : connections.keySet()) {
                                if (channel.isConnected() && !channel.isClosed()) {
                                    try {
                                        //Allocate 512 of memory
                                        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream(), StandardCharsets.UTF_8));
                                        ByteBuffer BUFFER = ByteBuffer.wrap(reader.readLine().getBytes(StandardCharsets.UTF_8));

                                        InetAddress incoming = channel.getInetAddress();
                                        int port = channel.getPort();
                                        String default_name = incoming.getHostAddress() + "/" + port;
                                        String designated_name = connections.get(channel);

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

                                                                client = new SSLRemoteClient(argument, mac, incoming, port, channel);
                                                                clients.put(default_name, client);
                                                                connections.put(channel, default_name);

                                                                ClientConnectEvent event = new ClientConnectEvent(client, this);
                                                                RemoteListener.callServerEvent(event);

                                                                MessageOutput output = new MessageDataOutput();
                                                                output.write("MAC", getMAC());
                                                                output.write("COMMAND_ENABLED", true);
                                                                output.write("COMMAND", "accept");

                                                                byte[] compile = output.compile();
                                                                PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                                writer.println(new String(compile, StandardCharsets.UTF_8));
                                                                writer.flush();
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

                                                            PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                            writer.println(new String(compile, StandardCharsets.UTF_8));
                                                            writer.flush();

                                                            connections.remove(channel);
                                                        }
                                                        break;
                                                    case "rename":
                                                        if (connections.containsValue(default_name) || connections.containsValue(designated_name)) {
                                                            if (!connections.containsValue(argument)) {
                                                                if (debug) {
                                                                    console.send("Client {0} is now known as {1}", Level.WARNING, client.getName(), argument);
                                                                }

                                                                client = new SSLRemoteClient(argument, mac, incoming, port, channel);
                                                                clients.put(default_name, client);
                                                                connections.put(channel, argument);

                                                                ClientCommandEvent event = new ClientCommandEvent(client, this, command, argument);
                                                                RemoteListener.callServerEvent(event);

                                                                MessageOutput output = new MessageDataOutput();
                                                                output.write("MAC", getMAC());
                                                                output.write("COMMAND_ENABLED", true);
                                                                output.write("COMMAND", "success");
                                                                output.write("ARGUMENT", "rename");
                                                                output.write("ARGUMENT_DATA", argument);

                                                                byte[] compile = output.compile();

                                                                PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                                writer.println(new String(compile, StandardCharsets.UTF_8));
                                                                writer.flush();
                                                            } else {
                                                                MessageOutput output = new MessageDataOutput();
                                                                output.write("MAC", getMAC());
                                                                output.write("COMMAND_ENABLED", true);
                                                                output.write("COMMAND", "failed");
                                                                output.write("ARGUMENT", "rename");
                                                                output.write("ARGUMENT_DATA", argument + "," + "A client with that name already exists!");

                                                                byte[] compile = output.compile();

                                                                PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                                writer.println(new String(compile, StandardCharsets.UTF_8));
                                                                writer.flush();
                                                            }
                                                        } else {
                                                            MessageOutput output = new MessageDataOutput();
                                                            output.write("MAC", getMAC());
                                                            output.write("COMMAND_ENABLED", true);
                                                            output.write("COMMAND", "failed");
                                                            output.write("ARGUMENT", "rename");
                                                            output.write("ARGUMENT_DATA", argument + "," + "You are not connected to this server!");

                                                            byte[] compile = output.compile();

                                                            PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                            writer.println(new String(compile, StandardCharsets.UTF_8));
                                                            writer.flush();
                                                        }
                                                        break;
                                                    case "disconnect":
                                                        if (connections.containsValue(default_name) || connections.containsValue(designated_name)) {
                                                            if (debug) {
                                                                console.send("Client {0} left the server ( {1} )", Level.WARNING, client.getName(), argument);
                                                            }

                                                            MessageOutput output = new MessageDataOutput();
                                                            output.write("MAC", getMAC());
                                                            output.write("COMMAND_ENABLED", true);
                                                            output.write("COMMAND", "DISCONNECT");
                                                            output.write("ARGUMENT", "DISCONNECT");
                                                            output.write("ARGUMENT_DATA", "Disconnect requested by client");

                                                            byte[] compile = output.compile();

                                                            PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                            writer.println(new String(compile, StandardCharsets.UTF_8));
                                                            writer.flush();

                                                            clients.remove(default_name);
                                                            connections.remove(channel);

                                                            ClientDisconnectEvent event = new ClientDisconnectEvent(client, this, DisconnectReason.KILLED_BY_CLIENT, argument);
                                                            RemoteListener.callServerEvent(event);
                                                        } else {
                                                            MessageOutput output = new MessageDataOutput();
                                                            output.write("MAC", getMAC());
                                                            output.write("COMMAND_ENABLED", true);
                                                            output.write("COMMAND", "failed");
                                                            output.write("ARGUMENT", "disconnect");
                                                            output.write("ARGUMENT_DATA", "You are not connected to this server!");

                                                            byte[] compile = output.compile();

                                                            PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                            writer.println(new String(compile, StandardCharsets.UTF_8));
                                                            writer.flush();
                                                        }
                                                        break;
                                                    default:
                                                        if (connections.containsValue(default_name) || connections.containsValue(designated_name)) {
                                                            if (debug) {
                                                                console.send("Unknown command from {0}: {1} ( {2} )", Level.WARNING, client.getName(), command, argument);
                                                            }

                                                            ClientCommandEvent event = new ClientCommandEvent(client, this, command, argument);
                                                            RemoteListener.callServerEvent(event);
                                                        } else {
                                                            MessageOutput output = new MessageDataOutput();
                                                            output.write("MAC", getMAC());
                                                            output.write("COMMAND_ENABLED", true);
                                                            output.write("COMMAND", "failed");
                                                            output.write("ARGUMENT", "unknown");
                                                            output.write("ARGUMENT_DATA", command + "," + argument + "," + "You are not connected to this server!");

                                                            byte[] compile = output.compile();

                                                            PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                            writer.println(new String(compile, StandardCharsets.UTF_8));
                                                            writer.flush();
                                                        }
                                                        break;
                                                }
                                            }
                                        } else {
                                            if (connections.containsValue(default_name) || connections.containsValue(designated_name)) {
                                                MessageOutput output = new MessageDataOutput();
                                                output.write("MAC", getMAC());
                                                output.write("COMMAND_ENABLED", true);
                                                output.write("COMMAND", "success");
                                                output.write("ARGUMENT", "message");
                                                output.write("ARGUMENT_DATA", client.getName());

                                                byte[] compile = output.compile();

                                                PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                writer.println(new String(compile, StandardCharsets.UTF_8));
                                                writer.flush();

                                                ClientMessageEvent event = new ClientMessageEvent(client, this, input);
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

                                                PrintWriter writer = new PrintWriter(channel.getOutputStream());
                                                writer.println(new String(compile, StandardCharsets.UTF_8));
                                                writer.flush();
                                            }
                                        }

                                        processed++;
                                    } catch (Throwable ex) {
                                        String name = connections.remove(channel);

                                        for (String default_name : clients.keySet()) {
                                            RemoteClient client = clients.getOrDefault(default_name, null);
                                            if (client == null) {
                                                clients.remove(default_name);
                                            } else {
                                                if (client.getName().equals(name)) {
                                                    clients.remove(default_name);

                                                    if (debug) {
                                                        console.send("Client {0} left the server ( {1} )", Level.WARNING, client.getName(), "Internal server error occurred [The client may have disconnected from the server]");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }).start();

                    while (operative) {
                        try {
                            Socket channel = socket.accept();
                            if (channel != null) {
                                InetAddress incoming = channel.getInetAddress();
                                int port = channel.getPort();
                                String default_name = incoming.getHostAddress() + "/" + port;

                                connections.put(channel, default_name);
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
     * Get the server address
     *
     * @return the server address
     */
    @Override
    public InetAddress getHost() {
        try {
            try {
                return InetAddress.getByName(server);
            } catch (Throwable ignored) {}

            return InetAddress.getLocalHost();
        } catch (Throwable ex) {
            return InetAddress.getLoopbackAddress();
        }
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
     * Get the server port
     *
     * @return the server port
     */
    @Override
    public int getPort() {
        return sv_port;
    }

    /**
     * Send a message to the server
     *
     * @param message the message to send
     * @return if the message could be sent
     */
    @Override
    public boolean sendMessage(byte[] message) {
        return false;
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

        for (Socket connected : connections.keySet()) {
            //Communicate this to all the connected
            //clients
            try {
                MessageOutput output = new MessageDataOutput();
                output.write("MAC", getMAC());
                output.write("COMMAND_ENABLED", true);
                output.write("COMMAND", "DISCONNECT");
                output.write("ARGUMENT", "DISCONNECT");
                output.write("ARGUMENT_DATA", "Server closed");

                byte[] compile = output.compile();

                PrintWriter writer = new PrintWriter(connected.getOutputStream());
                writer.println(new String(compile, StandardCharsets.UTF_8));
                writer.flush();
            } catch (Throwable ignored) {}
        }
        connections.clear();

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

            for (RemoteClient client : clients.values()) {
                if (client.getName().equals(name) || client.getMAC().equals(name)) {
                    client.sendMessage(data);

                    if (debug) {
                        long sec = TimeUnit.MILLISECONDS.toSeconds(wait);
                        console.send("Sent message to {0} after {1} {2}", Level.OK, name, (sec > 0 ? sec : wait), (sec > 0 ? "seconds" : "ms"));
                    }
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
                output.write("ARGUMENT", "DISCONNECT");
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
                output.write("ARGUMENT", "DISCONNECT");
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
    private RemoteClient getClient(final String name, final String mac, final InetAddress address, final int port, final Socket socket) {
        RemoteClient client = clients.getOrDefault(address.getHostAddress() + "/" + port, null);
        if (client == null) {
            client = new SSLRemoteClient(name, mac, address, port, socket);
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
        return "SSL Server";
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