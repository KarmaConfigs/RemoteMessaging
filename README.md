# RemoteMessaging

This API allows you to setup client to server communication using<br>
TCP, UDP (under maintenance) or SSL/TLS sockets.

## Usage
The usage of this API is very simple, just create a Factory instance<br>
with the work level you want to have (TCP or UDP).

TPC
- Makes sure the packet arrieves the destination
- Slow

UDP _Not implemented_
- The packet won't arrive always
- Fast

SSL/TLS
- Secure

### The listener class is optional

```java
import ml.karmaconfigs.remote.messaging.util.message.MessageOutput
import ml.karmaconfigs.remote.messaging.util.message.MessageInput;
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
        MessageInput msg = e.getMessage();

        String tag = msg.getString("TAG");
        if (tag != null) {
            if (tag.equalsIgnoreCase("TEST_MESSAGE")) {
                MessageOutput output = new MessageOutput();
                output.write("TAG", "REDIRECT_MESSAGE");
                output.write("SENDER", client.getName());
                output.write("MESSAGE", "Hello World!");

                server.broadcast(output);
            } else {
                sv.ban(client.getMAC());

                Path bansPath = new File("./bans.txt").toPath();
                sv.exportBans(bansPath);
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public void clientReceiveMessage(ServerMessageEvent e) {
        RemoteServer server = e.getServer();
        MessageInput msg = e.getMessage();

        String tag = msg.getString("TAG");
        if (tag.equalsIgnoreCase("REDIRECT_MESSAGE")) {
            String client = msg.getString("SENDER");
            if (client != null) {
                System.out.printf("%s -> %s%n", client, msg.getString("MESSAGE"));
            }
        }
    }
}
```

### Util class for TCP/UDP socket

```java
import ml.karmaconfigs.remote.messaging.util.message.MessageOutput
import ml.karmaconfigs.remote.messaging.platform.Client;
import ml.karmaconfigs.remote.messaging.Factory;
import ml.karmaconfigs.remote.messaging.platform.Server;
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

        //This part is completely optional
        Path bansPath = new File("./bans.txt").toPath();
        server.loadBans(bansPath);

        server.start().whenComplete((sv_result, sv_error) -> {
            if (sv_result) {
                client.connect().whenComplete((cl_result, cl_error) -> {
                    if (cl_result) {
                        client.rename("client_name");

                        MessageOutput output = new MessageOutput();
                        output.write("TAG", "TEST_MESSAGE");

                        client.send(output);
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

### Util class for SSL/TLS socket

```java
import ml.karmaconfigs.remote.messaging.util.message.MessageOutput
import ml.karmaconfigs.remote.messaging.platform.Client;
import ml.karmaconfigs.remote.messaging.Factory;
import ml.karmaconfigs.remote.messaging.platform.Server;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.RemoteMessagingListener;
import ml.karmaconfigs.remote.messaging.util.WorkLevel;

public class MyClass {

    @SuppressWarnings("UnstableApiUsage")
    public static void main(String[] args) {
        SSLFactory sslServer = new SSLFactory("12345678", "remoteServer", "pfx", "PKCS12");
        SSLFactory sslClient = new SSLFactory("12345678", "remoteClient", "pfx", "PKCS12");

        RemoteMessagingListener listener = new MyListener();
        UUID listenerId = RemoteListener.register(listener);

        SecureServer server = factory.createServer();
        SecureClient client = factory.createClient();
        //Or if you want with debug:
        //SecureServer server = factory.createServer().debug(true);
        //SecureClient client = factory.createClient().debug(true);

        //This part is completely optional
        Path bansPath = new File("./bans.txt").toPath();
        Path globalCerts = new File("./certs/").toPath();
        
        server = server.certsLocation(globalCerts);
        client = client.certsLocation(globalCerts);
        
        /*
        According to this example, the certificates path should be:
        Server: ./certs/remoteServer.pfx
        Server (trusted storage): ./certs/remoteServer_trusted.pfx
        
        Client: ./certs/remoteClient.pfx
        Client (trusted storage): ./certs/remoteClient_trusted.pfx
        
        */
        
        server.loadBans(bansPath);
        //End of optional part

        server.start().whenComplete((sv_result, sv_error) -> {
            if (sv_result) {
                client.connect().whenComplete((cl_result, cl_error) -> {
                    if (cl_result) {
                        client.rename("client_name");

                        MessageOutput output = new MessageOutput();
                        output.write("TAG", "TEST_MESSAGE");

                        client.send(output);
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

### Generating keys...

I won't explain how you can generate keys and certificates, instead I will share you a very simple batch script for Windows I made to generate key certificates in an 'easy' way

```bat
@echo off
echo Welcome to Karma key tool generator

echo Server setup...
SET /P serverAlias="Alias: "
SET /P serverKeyalg="Key algorithm: "
SET /P serverPassword="Key password: "
SET /P serverStore="Key store name: "
SET /P serverExtension="Key store extension (pfx/jks...): "
SET /P serverStoretype="Storing type: "

start cmd /k "keytool -genkey -alias %serverAlias% -keyalg %serverKeyalg% -validity 364635 -keypass %serverPassword% -storepass %serverPassword% -keystore %serverStore%.%serverExtension% -storetype %serverStoretype%"

echo Press any key to continue when you finished configuring the server key
pause >NUL

cls
echo Client setup...
SET /P clientAlias="Alias: "
SET /P clientKeyalg="Key algorithm: "
SET /P clientPassword="Key password: "
SET /P clientStore="Key store name: "
SET /P clientExtension="Key store extension (pfx/jks...): "
SET /P clientStoretype="Storing type: "

start cmd /k "keytool -genkey -alias %clientAlias% -keyalg %clientKeyalg% -validity 364635 -keypass %clientPassword% -storepass %clientPassword% -keystore %clientStore%.%clientExtension% -storetype %clientStoretype%"

echo Press any key to continue when you finished configuring the client key
pause >NUL
cls

echo Exporting certificates and creating trusted key storages...

start cmd /k "keytool -export -keystore %serverStore%.%serverExtension% -alias %serverAlias% -file %serverAlias%.cer -storepass %serverPassword%"
start cmd /k "keytool -export -keystore %clientStore%.%clientExtension% -alias %clientAlias% -file %clientAlias%.cer -storepass %clientPassword%"

start cmd /k "keytool -import -v -trustcacerts -alias %serverAlias% -file %serverAlias%.cer -keystore %clientStore%_trusted.%clientExtension% -keypass %clientPassword% -storepass %clientPassword%"
echo (1/2) Make sure to type 'yes' and then press any key to continue
pause >NUL

start cmd /k "keytool -import -v -trustcacerts -alias %clientAlias% -file %clientAlias%.cer -keystore %serverStore%_trusted.%serverExtension% -keypass %serverPassword% -storepass %serverPassword%"
echo (2/2) Make sure to type 'yes' and then press any key to continue
pause >NUL

cls
echo Done, enjoy your new certificates...

pause >NUL
exit
```
