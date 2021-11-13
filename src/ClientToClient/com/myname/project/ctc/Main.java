package com.myname.project.ctc;

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
        RemoteMessagingListener listener;

        if (Arrays.asList(args).contains("--server")) {
            Server server = new Server(SERVER_PORT);
            listener = new Listener(server);
            RemoteListener.register(listener);

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
            listener = new Listener(null);
            RemoteListener.register(listener);

            client.connect().whenComplete((a, b) -> {
                client.rename("clientTest");

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeBoolean(false);
                out.writeUTF("Hello server!");

                client.send(out.toByteArray());

                out = ByteStreams.newDataOutput();
                out.writeBoolean(true);
                out.writeUTF("Hello clientTest2");

                ByteArrayDataOutput subMessage = ByteStreams.newDataOutput();
                subMessage.writeUTF("Hello sir");

                byte[] data = subMessage.toByteArray();
                for (byte bd : data)
                    out.writeByte(bd);

                client.send(out.toByteArray());
            });
        }
    }
}
