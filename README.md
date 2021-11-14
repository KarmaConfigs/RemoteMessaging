# RemoteMessaging

This API allows you to setup client to server communication using<br>
TCP or UDP (under maintenance) sockets.

##Usage
The usage of this API is very simple, just create a Factory instance<br>
with the work level you want to have (TCP or UDP).

TPC
- Makes sure the packet arrieves the destination
- Slow

UDP
- The packet won't arrive always
- Fast

### The listener class is optional

```java
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.remote.messaging.listener.RemoteMessagingListener;
import ml.karmaconfigs.remote.messaging.listener.event.client.ServerMessageEvent;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.remote.RemoteServer;
import ml.karmaconfigs.remote.messaging.util.MessagePacker;

import java.nio.file.Path;

public class MyListener implements RemoteMessagingListener {

    private final Server server;

    public MyListener(final Server sv) {
        server = sv;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void serverReceiveMessage(ClientMessageEvent e) {
        RemoteClient client = e.getClient();

        ByteArrayDataInput input = ByteStreams.newDataInput(e.getMessage());
        String tag = input.readUTF();
        if (tag.equalsIgnoreCase("test_message")) {
            System.out.println(input.readUTF());
            System.out.println("Has custom name? " + input.readBoolean());

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("redirect_message");
            out.writeUTF("client_test_2");
            //Or also
            //out.writeUTF(<mac address>);

            MessagePacker packer = new MessagePacker(e.getMessage());
            out.writeUTF(packer.pack());

            client.sendMessage(out.toByteArray());
        } else {
            sv.ban(client.getMAC());

            Path bansPath = new File("./bans.txt").toPath();
            sv.exportBans(bansPath);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public void clientReceiveMessage(ServerMessageEvent e) {
        RemoteServer server = e.getServer();

        ByteArrayDataInput input = ByteStreams.newDataInput(e.getMessage());
        String tag = input.readUTF();
        if (tag.equalsIgnoreCase("redirect_message")) {
            String client = input.readUTF();
            MessagePacker unpacked = MessagePacker.unpack(input.readUTF());
            if (unpacked != null) {
                if (!client.equals("*")) {
                    server.redirect(client, unpacked.getData());
                } else {
                    server.broadcast(unpacket.getData());
                }
            }
        }
    }
}
```

### Util class

```java
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ml.karmaconfigs.remote.messaging.Client;
import ml.karmaconfigs.remote.messaging.Factory;
import ml.karmaconfigs.remote.messaging.Server;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.RemoteMessagingListener;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;

public class MyClass {

    @SuppressWarnings("UnstableApiUsage")
    public static void main(String[] args) {
        Factory factory = new Factory(WorkLevel.TCP);

        RemoteMessagingListener listener = new MyListener();
        UUID listenerId = RemoteListener.register(listener);

        Server server = factory.createServer();
        Client client = factory.createClient();
        //Or if you want with debug:
        //Server server = factory.createServer().debug(true);
        //Client client = factory.createClient().debug(true);

        Path bansPath = new File("./bans.txt").toPath();
        server.loadBans(bansPath);
        
        server.start().whenComplete((sv_result, sv_error) -> {
            if (sv_result) {
                client.connect().whenComplete((cl_result, cl_error) -> {
                    if (cl_result) {
                        client.rename("client_name");

                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("test_message");
                        out.writeUTF("Hello world!");
                        out.writeBoolean(client.getName().equalsIgnoreCase("client_name"));

                        client.send(out.toByteArray());
                    } else {
                        if (cl_error != null)
                            cl_error.printStackTrace();

                        System.out.println("Failed to connect to server");
                        System.exit(1);
                        
                        RemoteListener.unRegister(listenerId);
                    }
                });
            } else {
                if (sv_error != null)
                    sv_error.printStackTrace();

                System.out.println("Failed to setup server");
            }
        });
    }
}
```