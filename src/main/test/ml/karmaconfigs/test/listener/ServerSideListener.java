package ml.karmaconfigs.test.listener;

import ml.karmaconfigs.remote.messaging.listener.RemoteMessagingListener;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientCommandEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientConnectEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;

public class ServerSideListener implements RemoteMessagingListener {

    public void onClientConnect(ClientConnectEvent e) {
        RemoteClient client = e.getClient();
        System.out.println(client.getName() + " ( " + client.getMAC() + " ) has connected to server!");
    }

    public void onClientCommand(ClientCommandEvent e) {
        RemoteClient client = e.getClient();
        System.out.println(client.getName() + " ran command: " + e.getCommand());
    }
}
