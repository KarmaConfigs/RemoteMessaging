package com.myname.project.cts;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.remote.messaging.listener.RemoteMessagingListener;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerDisconnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerMessageEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientCommandEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientConnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientDisconnectEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;

@SuppressWarnings("UnstableApiUsage")
public final class Listener implements RemoteMessagingListener {

    public void onClientConnectToServer(ClientConnectEvent e) {
        RemoteClient client = e.getClient();

        System.out.println(client.getName() + " connected to server");
    }

    public void onClientDisconnectFromServer(ClientDisconnectEvent e) {
        RemoteClient client = e.getClient();

        System.out.println(client.getName() + " disconnected from server (" + e.getReason().name() + " -> " + e.getDisconnectMessage() + ")");
    }

    public void onClientSendCommandEvent(ClientCommandEvent e) {
        RemoteClient client = e.getClient();

        System.out.println(client.getName() + " ran command " + e.getCommand() + " ( " + e.getInformation() + " )");
    }

    public void onClientSendMessageEvent(ClientMessageEvent e) {
        RemoteClient client = e.getClient();

        ByteArrayDataInput input = ByteStreams.newDataInput(e.getMessage());

        System.out.println(client.getName() + " says: " + input.readLine());
    }

    public void onServerConnectsClient(ServerConnectEvent e) {
        RemoteServer server = e.getServer();

        System.out.println("Connected to server " + server.getHost().getHostAddress() + ":" + server.getPort());
    }

    public void onServerDisconnectsClient(ServerDisconnectEvent e) {
        RemoteServer server = e.getServer();

        System.out.println("Disconnected from server " + server.getHost().getHostAddress() + ":" + server.getPort() + " ( " + e.getReason() + " )");
    }

    public void onClientReceivesMessageFromServer(ServerMessageEvent e) {
        RemoteServer server = e.getServer();

        ByteArrayDataInput input = ByteStreams.newDataInput(e.getMessage());

        System.out.println(server.getHost().getHostAddress() + ":" + server.getPort() + " says: " + input.readLine());
    }
}
