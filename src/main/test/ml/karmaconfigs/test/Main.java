package ml.karmaconfigs.test;

import ml.karmaconfigs.remote.messaging.SSLFactory;
import ml.karmaconfigs.remote.messaging.platform.SecureClient;
import ml.karmaconfigs.remote.messaging.platform.SecureServer;

public class Main {

    public static void main(String[] args) {
        SSLFactory sslServer = new SSLFactory("12345678", "remoteServer", "pfx", "PKCS12");
        SSLFactory sslClient = new SSLFactory("12345678", "remoteClient", "pfx", "PKCS12");

        SecureServer server = sslServer.createServer().debug(true);
        SecureClient client = sslClient.createClient().debug(true);

        server.start().whenComplete((rs, ex) -> {
            if (rs) {
                client.connect().whenComplete((cRs, cEx) -> {
                    if (cRs) {
                        client.rename("test_name");
                        client.rename("no_name");
                        client.close();
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
