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
import ml.karmaconfigs.api.common.utils.PrefixConsoleData;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.remote.messaging.platform.Client;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerDisconnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;
import ml.karmaconfigs.remote.messaging.util.message.*;
import ml.karmaconfigs.remote.messaging.util.message.type.MergeType;
import ml.karmaconfigs.remote.messaging.worker.tcp.remote.TCPRemoteServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remote message client interface
 */
public final class TCPClient extends Client {
    
    private final Set<byte[]> data_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private RemoteServer remote = null;

    private String client_name = "client_" + new Random().nextInt(Integer.MAX_VALUE);
    private String server = "127.0.0.1";
    private String key = "";

    private int sv_port = 49305;
    private int client = 49300;

    private boolean debug = false;
    private boolean operative = false;
    private boolean instant_close = false;
    private boolean award_connection = false;
    private boolean tryingConnect = true;

    private SocketChannel socket;

    private final Console console = new Console(this);

    /**
     * Initialize a default client that
     * will connect to local server at
     * default port 49305
     */
    public TCPClient() {
        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[TCP Client (&aOK&3)]&b ");
        data.setInfoPrefix("&3[TCP Client (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[TCP Client (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[TCP Client (&4ERROR&3)]&b ");
    }

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

        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[TCP Client (&aOK&3)]&b ");
        data.setInfoPrefix("&3[TCP Client (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[TCP Client (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[TCP Client (&4ERROR&3)]&b ");
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

        PrefixConsoleData data = console.getData();
        data.setOkPrefix("&3[TCP Client (&aOK&3)]&b ");
        data.setInfoPrefix("&3[TCP Client (&7INFO&3)]&b ");
        data.setWarnPrefix("&3[TCP Client (&eWARNING&3)]&b ");
        data.setGravePrefix("&3[TCP Client (&4ERROR&3)]&b ");
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
    public LateScheduler<Boolean> connect() {
        if (!operative) {
            LateScheduler<Boolean> result = new AsyncLateScheduler<>();

            Thread thread = new Thread(() -> {
                try {
                    if (debug) {
                        console.send("Initializing the connection with the server", Level.INFO);
                    }

                    socket = SocketChannel.open().bind(new InetSocketAddress(client));
                    socket.configureBlocking(false);
                    socket.connect(new InetSocketAddress(server, sv_port));

                    while (!socket.finishConnect()) {
                        if (tryingConnect) {
                            if (debug) {
                                console.send("Trying to establish a connection with {0}/{1}", Level.INFO, server, sv_port);
                            }

                            tryingConnect = false;
                        }
                    }

                    award_connection = true;
                    tryingConnect = true;

                    if (debug) {
                        console.send("The connection has been established but the client is still waiting for server confirmation, data can be started to be sent", Level.WARNING, server, sv_port);
                    }

                    while (award_connection) {
                        if (instant_close) {
                            close();
                            tryingConnect = false;
                            award_connection = false;
                            operative = false;
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

                            ByteBuffer tmp = ByteBuffer.wrap(compile);
                            socket.write(tmp);

                            tryingConnect = false;
                        }

                        ByteBuffer tmpBuffer = ByteBuffer.allocate(5120);
                        int read = socket.read(tmpBuffer);

                        if (read != 0) {
                            ByteBuffer readBuffer = DataFixer.fixBuffer(tmpBuffer);

                            if (!operative) {
                                MessageInput input = new MessageDataInput(readBuffer.array());
                                if (input.getBoolean("COMMAND_ENABLED")) {
                                    String sequence = input.getString("COMMAND");
                                    String mac = input.getString("MAC");
                                    if (sequence != null && mac != null) {
                                        if (sequence.equalsIgnoreCase("accept")) {
                                            remote = new TCPRemoteServer(mac, InetAddress.getByName(server), sv_port, socket);

                                            if (debug) {
                                                console.send("Connection has been validated by the server", Level.OK);
                                            }

                                            for (byte[] data : data_queue) {
                                                ByteBuffer tmp = ByteBuffer.wrap(data);

                                                socket.write(tmp);
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

                    result.complete(true);

                    while (operative) {
                        ByteBuffer tmpBuffer = ByteBuffer.allocate(4056);
                        int read = socket.read(tmpBuffer);

                        if (read == 0) {
                            for (byte[] queue : data_queue) {
                                ByteBuffer tmp = ByteBuffer.wrap(queue);

                                socket.write(tmp);
                                data_queue.remove(queue);
                            }
                        } else {
                            ByteBuffer readBuffer = DataFixer.fixBuffer(tmpBuffer);

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

                                                close();
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
                ByteBuffer tmp = ByteBuffer.wrap(compile);

                socket.write(tmp);
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
                ByteBuffer tmp = ByteBuffer.wrap(compile);

                socket.write(tmp);
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
            try {
                MessageOutput output = new MessageDataOutput();
                output.write("MAC", getMAC());
                output.write("COMMAND_ENABLED", true);
                output.write("COMMAND", "disconnect");
                output.write("ARGUMENT", "Client disconnect request");

                data_queue.add(output.compile());
            } catch (Throwable ex) {
                ex.printStackTrace();
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
        return "TCP Client";
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
