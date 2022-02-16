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
import ml.karmaconfigs.api.common.timer.SourceSimpleTimer;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import ml.karmaconfigs.api.common.utils.PrefixConsoleData;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerDisconnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerMessageEvent;
import ml.karmaconfigs.remote.messaging.platform.SecureClient;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;
import ml.karmaconfigs.remote.messaging.util.message.*;
import ml.karmaconfigs.remote.messaging.util.message.type.MergeType;
import ml.karmaconfigs.remote.messaging.worker.ssl.remote.SSLRemoteServer;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remote message client interface
 */
public final class SSLClient extends SecureClient {
    
    private final Set<byte[]> data_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private RemoteServer remote = null;

    private String client_name = "client_" + new Random().nextInt(Integer.MAX_VALUE);
    private String server = "127.0.0.1";
    private String key = "";

    private int sv_port = 49305;
    private int client = 49300;
    private int disconnect = 10;

    private boolean disconnecting = false;
    private boolean debug = false;
    private boolean operative = false;
    private boolean instant_close = false;
    private boolean award_connection = false;
    private boolean tryingConnect = true;

    private SSLSocket socket;

    private final String password;
    private final String name;
    private final String extension;
    private final String type;
    private final Console console = new Console(this);

    private String protocol = "TLSv1.3";

    private Path parent = getDataPath().resolve("certs");

    /**
     * Initialize a default client that
     * will connect to local server at
     * default port 49305
     */
    public SSLClient(final String pwd, final String nm, final String ext, final String tp) {
        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[SSL Client (&aOK&3)]&b ");
        data.setInfoPrefix("&3[SSL Client (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[SSL Client (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[SSL Client (&4ERROR&3)]&b ");

        password = pwd;
        name = nm;
        extension = ext;
        type = tp;
    }

    /**
     * Initialize a client that will connect
     * to the specified server at specified port
     *
     * @param server_host the server
     * @param server_port the server port
     */
    public SSLClient(final String pwd, final String nm, final String ext, final String tp, final String server_host, final int server_port) {
        server = server_host;
        sv_port = server_port;

        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[SSL Client (&aOK&3)]&b ");
        data.setInfoPrefix("&3[SSL Client (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[SSL Client (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[SSL Client (&4ERROR&3)]&b ");

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
     * @param client_port the client port
     * @param server_host the server
     * @param server_port the server port
     */
    public SSLClient(final String pwd, final String nm, final String ext, final String tp, final int client_port, final String server_host, final int server_port) {
        client = client_port;
        server = server_host;
        sv_port = server_port;

        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[SSL Client (&aOK&3)]&b ");
        data.setInfoPrefix("&3[SSL Client (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[SSL Client (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[SSL Client (&4ERROR&3)]&b ");

        password = pwd;
        name = nm;
        extension = ext;
        type = tp;
    }

    /**
     * Set the client debug status
     *
     * @param status the client debug status
     * @return this instance
     */
    @Override
    public SecureClient debug(final boolean status) {
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
    public SecureClient protocol(final String p) {
        protocol = p;

        return this;
    }

    /**
     * Set the certificates location
     *
     * @param location the certificates location
     * @return the certificates location
     */
    @Override
    public SecureClient certsLocation(final Path location) {
        parent = location;

        return this;
    }

    /**
     * Try to connect to the server
     *
     * @return a completable future when the client connects
     */
    @Override
    public LateScheduler<Boolean> connect() {
        if (!operative) {
            tryingConnect = true;
            disconnecting = false;
            LateScheduler<Boolean> result = new AsyncLateScheduler<>();

            Thread thread = new Thread(() -> {
                try {
                    if (debug) {
                        console.send("Initializing the connection with the server", Level.INFO);
                    }

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

                        sc.init(keyManagers, trustManagers, null);
                        SSLSocketFactory ssf = sc.getSocketFactory();
                        try {
                            socket = (SSLSocket) ssf.createSocket(server, sv_port, InetAddress.getLocalHost(), client);
                        } catch (Throwable ex) {
                            socket = (SSLSocket) ssf.createSocket(server, sv_port, InetAddress.getLoopbackAddress(), client);
                        }

                        socket.startHandshake();
                    } catch (Throwable ex) {
                        result.complete(false, ex);
                        return;
                    }

                    award_connection = true;

                    if (debug) {
                        console.send("The connection has been established but the client is still waiting for server confirmation, data can be started to be sent", Level.WARNING, server, sv_port);
                    }

                    try {
                        while (award_connection) {
                            if (instant_close) {
                                tryingConnect = false;
                                award_connection = false;
                                operative = false;

                                if (socket != null) {
                                    try {
                                        socket.close();
                                    } catch (Throwable ignored) {
                                    }
                                }
                                socket = null;
                            }

                            if (tryingConnect) {
                                MessageOutput output = new MessageDataOutput();
                                output.write("MAC", getMAC());
                                output.write("COMMAND_ENABLED", true);
                                output.write("COMMAND", "connect");
                                output.write("ARGUMENT", client_name);
                                if (!StringUtils.isNullOrEmpty(key)) {
                                    output.write("ACCESS_KEY", key);
                                }

                                byte[] compile = output.compile();

                                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                                writer.println(new String(compile, StandardCharsets.UTF_8));
                                writer.flush();
                            }

                            if (!operative) {
                                if (socket != null) {
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                                    ByteBuffer readBuffer = ByteBuffer.wrap(reader.readLine().getBytes(StandardCharsets.UTF_8));

                                    MessageInput input = new MessageDataInput(readBuffer.array());
                                    if (input.getBoolean("COMMAND_ENABLED")) {
                                        String sequence = input.getString("COMMAND");
                                        String mac = input.getString("MAC");
                                        if (sequence != null && mac != null) {
                                            if (sequence.equalsIgnoreCase("accept")) {
                                                remote = new SSLRemoteServer(mac, InetAddress.getByName(server), sv_port, socket);

                                                if (debug) {
                                                    console.send("Connection has been validated by the server", Level.OK);
                                                }

                                                for (byte[] data : data_queue) {
                                                    ByteBuffer tmp = ByteBuffer.wrap(data);

                                                    socket.getChannel().write(tmp);
                                                    data_queue.remove(data);
                                                }

                                                award_connection = false;
                                                operative = true;

                                                ServerConnectEvent event = new ServerConnectEvent(remote);
                                                RemoteListener.callClientEvent(event);
                                            } else {
                                                String argument = input.getString("ARGUMENT");
                                                if (argument != null) {
                                                    if (argument.equalsIgnoreCase("connect")) {
                                                        instant_close = true;
                                                        result.complete(false);

                                                        String reason = input.getString("COMMAND_ARGUMENT");
                                                        if (reason != null) {
                                                            console.send("Connection has been declined by the server ({0})", Level.GRAVE, reason);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }

                    result.complete(true);

                    try {
                        while (operative) {
                            if (socket != null) {
                                if (!data_queue.isEmpty()) {
                                    //We won't read requests from server until he processes all our requests

                                    for (byte[] waiting : data_queue) {
                                        PrintWriter writer = new PrintWriter(socket.getOutputStream());
                                        writer.println(new String(waiting, StandardCharsets.UTF_8));
                                        writer.flush();

                                        System.out.println(new String(waiting, StandardCharsets.UTF_8));

                                        data_queue.remove(waiting);
                                    }
                                } else {
                                    if (remote != null) {
                                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                                        ByteBuffer readBuffer = ByteBuffer.wrap(reader.readLine().getBytes(StandardCharsets.UTF_8));

                                        MessageInput input = new MessageDataInput(readBuffer.array());
                                        String mac = input.getString("MAC");
                                        boolean isCommand = input.getBoolean("COMMAND_ENABLED");
                                        if (remote.getMAC().equals(mac)) {
                                            if (isCommand) {
                                                String command = input.getString("COMMAND");
                                                String argument = input.getString("ARGUMENT");

                                                if (command != null && argument != null) {
                                                    String data;

                                                    switch (command.toLowerCase()) {
                                                        case "success":
                                                            switch (argument.toLowerCase()) {
                                                                case "rename":
                                                                    client_name = input.getString("ARGUMENT_DATA");

                                                                    if (client_name != null && debug) {
                                                                        console.send("Server accepted the new client name: {0}", Level.OK, client_name);
                                                                    }
                                                                    break;
                                                                case "message":
                                                                    data = input.getString("ARGUMENT_DATA");
                                                                    if (data != null && debug) {
                                                                        console.send("{0} to server: {1}", Level.INFO, data, new String(readBuffer.array()));
                                                                    }
                                                                    break;
                                                                case "unknown":
                                                                    data = input.getString("ARGUMENT_DATA");

                                                                    if (data != null && debug) {
                                                                        String[] arg_data = data.split(",");

                                                                        console.send("{0} ran custom command: {1} ( {2} )", Level.WARNING, arg_data[0], arg_data[1], arg_data[2]);
                                                                    }
                                                                    break;
                                                                default:
                                                                    if (debug) {
                                                                        console.send("Unknown command from server: {0} ( {1} )", Level.GRAVE, command, argument);
                                                                    }
                                                                    break;
                                                            }
                                                            break;
                                                        case "failed":
                                                            switch (argument.toLowerCase()) {
                                                                case "connect":
                                                                    data = input.getString("ARGUMENT_DATA");
                                                                    if (data != null) {
                                                                        String[] connect_data = data.split(",");
                                                                        String name = connect_data[0];
                                                                        String reason = connect_data[1];

                                                                        console.send("Server declined connection as {0}, because: {1}", Level.GRAVE, name, reason);

                                                                        ServerDisconnectEvent connectEvent = new ServerDisconnectEvent(remote, reason);
                                                                        RemoteListener.callClientEvent(connectEvent);
                                                                    }

                                                                    break;
                                                                case "rename":
                                                                    data = input.getString("ARGUMENT_DATA");
                                                                    if (data != null) {
                                                                        String[] rename_data = data.split(",");
                                                                        console.send("Failed to change client name to {0}: {1}", Level.GRAVE, rename_data[0], rename_data[1]);
                                                                    }

                                                                    break;
                                                                case "disconnect":
                                                                    data = input.getString("ARGUMENT_DATA");

                                                                    if (data != null) {
                                                                        console.send("Failed while trying to disconnect the server ( you've been disconnected anyway ): {0}", Level.GRAVE, data);

                                                                        ServerDisconnectEvent disconnectEvent = new ServerDisconnectEvent(remote, "no server reason...");
                                                                        RemoteListener.callClientEvent(disconnectEvent);
                                                                    }

                                                                    break;
                                                                case "message":
                                                                    data = input.getString("ARGUMENT_DATA");
                                                                    if (data != null) {
                                                                        console.send("Failed while trying to send a message to server: {0}", Level.GRAVE, data);
                                                                    }
                                                                    break;
                                                                case "unknown":
                                                                    data = input.getString("ARGUMENT_DATA");
                                                                    if (data != null) {
                                                                        String[] unknown_data = data.split(",");
                                                                        console.send("Failed while trying to execute custom command {0} with argument {1}: {2}", Level.GRAVE, unknown_data[0], unknown_data[1], unknown_data[2]);
                                                                    }

                                                                    break;
                                                                default:
                                                                    if (debug) {
                                                                        console.send("Unknown command from server: {0} ( {1} )", Level.WARNING, command, argument);
                                                                    }
                                                                    break;
                                                            }
                                                            break;
                                                        case "disconnect":
                                                            String reason = input.getString("ARGUMENT_DATA");
                                                            if (reason != null) {
                                                                console.send("Connection killed by server: {0}", Level.GRAVE, reason);
                                                            }

                                                            ServerDisconnectEvent event = new ServerDisconnectEvent(remote, reason);
                                                            RemoteListener.callClientEvent(event);

                                                            operative = false;
                                                            award_connection = false;
                                                            tryingConnect = true;

                                                            socket.close();
                                                            socket = null;

                                                            disconnecting = false;

                                                            break;
                                                    }
                                                }
                                            } else {
                                                ServerMessageEvent event = new ServerMessageEvent(remote, input);
                                                RemoteListener.callClientEvent(event);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                } catch (Throwable ex) {
                    result.complete(false, ex);
                }
            });
            thread.start();

            return result;
        }

        return null;
    }

    /**
     * Try to connect to the server
     *
     * @param accessKey the server access key
     * @return a completable future when the client connects
     */
    @Override
    public LateScheduler<Boolean> connect(final String accessKey) {
        if (!operative) {
            key = accessKey;
            return connect();
        }

        return null;
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
        return remote;
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
     * Get if the client is trying to connect to the
     * server
     *
     * @return if the client is trying to connect to
     * the server
     */
    @Override
    public boolean isConnecting() {
        return tryingConnect || award_connection;
    }

    /**
     * Get if the client is completely connected
     * to the server
     *
     * @return if the client is connected
     */
    @Override
    public boolean isConnected() {
        try {
            return socket.getInputStream().read() != -1;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Rename the client on the server interface
     *
     * @param name the client name
     */
    @Override
    public void rename(final String name) {
        if (award_connection || operative) {
            client_name = name;

            MessageOutput output = new MessageDataOutput();
            output.write("MAC", getMAC());
            output.write("COMMAND_ENABLED", true);
            output.write("COMMAND", "rename");
            output.write("ARGUMENT", client_name);

            try {
                if (debug) {
                    console.send("Trying to inform the server about the name change request to {0}", Level.INFO, name);
                }

                byte[] compile = output.compile();

                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                writer.println(new String(compile, StandardCharsets.UTF_8));
                writer.flush();
            } catch (Throwable ex) {
                data_queue.add(output.compile());
            }
        }
    }

    /**
     * Send data to the server
     *
     * @param data the data to send
     */
    @Override
    public void send(final byte[] data) {
        if (award_connection || operative) {
            MessageOutput output = new MessageDataOutput(data, MergeType.DIFFERENCE);
            output.write("MAC", getMAC());
            output.write("COMMAND_ENABLED", false);

            try {
                byte[] compile = output.compile();

                PrintWriter writer = new PrintWriter(socket.getOutputStream());
                writer.println(new String(compile, StandardCharsets.UTF_8));
                writer.flush();
            } catch (Throwable ex) {
                data_queue.add(output.compile());
            }
        }
    }

    /**
     * Close the connection
     */
    @Override
    public void close() {
        if (operative) {
            if (!disconnecting) {
                try {
                    MessageOutput output = new MessageDataOutput();
                    output.write("MAC", getMAC());
                    output.write("COMMAND_ENABLED", true);
                    output.write("COMMAND", "disconnect");
                    output.write("ARGUMENT", "Client disconnect request");

                    byte[] compile = output.compile();

                    PrintWriter writer = new PrintWriter(socket.getOutputStream());
                    writer.println(new String(compile, StandardCharsets.UTF_8));
                    writer.flush();

                    if (debug) {
                        console.send("Trying to inform the server about the disconnect request. The client will wait for server response. If no response is given in 10 seconds the client will disconnect anyway", Level.INFO);
                    }

                    disconnect = 10;
                    disconnecting = true;

                    SimpleScheduler scheduler = new SourceSimpleTimer(this, 1, true).multiThreading(false);
                    scheduler.restartAction(() -> {
                        if (disconnecting) {
                            if (disconnect == 0) {
                                operative = false;
                                award_connection = false;
                                tryingConnect = true;

                                try {
                                    socket.close();
                                } catch (Throwable ignored) {
                                }
                                socket = null;

                                disconnecting = false;
                            } else {
                                disconnect--;
                            }
                        } else {
                            scheduler.cancel();
                        }
                    });
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            instant_close = true;
        }
    }

    /**
     * Karma source name
     *
     * @return the source name
     */
    @Override
    public String name() {
        return "SSL Client";
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
        return "TCP client to connect to a TCP server that has been created with RemoteMessaging API";
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
}
