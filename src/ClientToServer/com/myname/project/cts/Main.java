package com.myname.project.cts;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.remote.messaging.Client;
import ml.karmaconfigs.remote.messaging.Server;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.RemoteMessagingListener;

import java.util.Arrays;

@SuppressWarnings("UnstableApiUsage")
public final class Main {

    private static int SERVER_PORT = 49305;
    private static int CLIENT_PORT = 49300;

    public static void main(final String[] args) {
        RemoteMessagingListener listener = new Listener();
        RemoteListener.register(listener);

        if (Arrays.asList(args).contains("--server")) {
            Server server = new Server(SERVER_PORT);
            server.start().whenComplete((r, b) -> {
               System.out.println("Server initialized successfully");

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("This message is sent to everyone!");

                server.broadcast(out.toByteArray());

                out = ByteStreams.newDataOutput();
                out.writeUTF("Hi sir, this message can be seen only by you!");

                server.redirect("clientTest", out.toByteArray());
            });
        } else {
            Client client = new Client(CLIENT_PORT, "127.0.0.1", SERVER_PORT);
            client.connect().whenComplete((a, b) -> {
                client.rename("clientTest");

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Hello server!");

                client.send(out.toByteArray());
            });
        }
    }
}
