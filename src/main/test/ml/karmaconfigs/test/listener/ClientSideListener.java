package ml.karmaconfigs.test.listener;

import ml.karmaconfigs.remote.messaging.listener.RemoteMessagingListener;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;

public class ClientSideListener implements RemoteMessagingListener {

    public void onClientConnect(ServerConnectEvent e) {
        RemoteServer server = e.getServer();
        RemoteClient client = e.getClient();

        System.out.println("Successfully connected as " + client.getName() + "/" + client.getMAC() + " at " + server.getHost() + ":" + server.getPort() + " (MAC: " + server.getMAC() + ")!");
    }

    public void onClientCommand(ServerMessageEvent e) {
        RemoteServer server = e.getServer();
        RemoteClient client = e.getClient();

        System.out.println(server.getMAC() + " => " + client.getName() + ": " + e.getMessage().getString("TEST"));
    }
}
