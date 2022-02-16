package ml.karmaconfigs.test;

import ml.karmaconfigs.remote.messaging.SSLFactory;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.platform.SecureClient;
import ml.karmaconfigs.remote.messaging.platform.SecureServer;
import ml.karmaconfigs.remote.messaging.util.message.MessageDataOutput;
import ml.karmaconfigs.remote.messaging.util.message.MessageOutput;
import ml.karmaconfigs.test.listener.ClientSideListener;
import ml.karmaconfigs.test.listener.ServerSideListener;

public class Main {

    public static void main(String[] args) {
        SSLFactory sslServer = new SSLFactory("@Doritos1007", "server", "pfx", "PKCS12");
        SSLFactory sslClient = new SSLFactory("LockLogin", "client", "pfx", "PKCS12");

        SecureServer server = sslServer.createServer().debug(true);
        SecureClient client = sslClient.createClient().debug(true);

        RemoteListener.register(new ClientSideListener());
        RemoteListener.register(new ServerSideListener());

        server.start().whenComplete((rs, ex) -> {
            if (rs) {
                client.connect().whenComplete((cRs, cEx) -> {
                    if (cRs) {
                        client.rename("test_user");

                        MessageOutput output = new MessageDataOutput();
                        output.write("TEST", "Hello World!");

                        server.redirect(client.getMAC(), output);
                    } else {
                        if (cEx != null) {
                            cEx.printStackTrace();
                        }
                    }
                });
            } else {
                if (ex != null) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
