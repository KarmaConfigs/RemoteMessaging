package ml.karmaconfigs.test.listener;

import ml.karmaconfigs.remote.messaging.listener.RemoteMessagingListener;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerMessageEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientCommandEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientConnectEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;
import ml.karmaconfigs.remote.messaging.util.message.MessageDataOutput;
import ml.karmaconfigs.remote.messaging.util.message.MessageInput;
import ml.karmaconfigs.remote.messaging.util.message.MessageOutput;

public class ClientSideListener implements RemoteMessagingListener {

    public void onClientConnect(ServerConnectEvent e) {
        RemoteServer server = e.getServer();
        System.out.println("Successfully connected at " + server.getHost() + ":" + server.getPort() + " (MAC: " + server.getMAC() + ")!");
    }

    public void onClientCommand(ServerMessageEvent e) {
        RemoteServer server = e.getServer();
        MessageOutput output = new MessageDataOutput();

        System.out.println(server.getMAC() + " => " + e.getMessage().getString("INFO"));
    }
}
