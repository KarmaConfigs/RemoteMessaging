package ml.karmaconfigs.remote.messaging;

import ml.karmaconfigs.remote.messaging.util.WorkLevel;
import ml.karmaconfigs.remote.messaging.worker.tcp.TCPClient;
import ml.karmaconfigs.remote.messaging.worker.tcp.TCPServer;

public final class Factory {

    private final WorkLevel level;

    public Factory(final WorkLevel lvl) {
        level = lvl;
    }

    public Client createClient() throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPClient();
            case UDP:
            default:
                throw new IllegalArgumentException("UDP client is under maintenance and is not available");
        }
    }

    public Client createClient(final int target_port) throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPClient("127.0.0.1", target_port);
            case UDP:
            default:
                throw new IllegalArgumentException("UDP client is under maintenance and is not available");
        }
    }

    public Client createClient(final String target_host, final int target_port) throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPClient(target_host, target_port);
            case UDP:
            default:
                throw new IllegalArgumentException("UDP client is under maintenance and is not available");
        }
    }

    public Client createClient(final String target_host, final int target_port, final int local_port) throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPClient(local_port, target_host, target_port);
            case UDP:
            default:
                throw new IllegalArgumentException("UDP client is under maintenance and is not available");
        }
    }

    public Server createServer(final int target_port) throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPServer(target_port);
            case UDP:
            default:
                throw new IllegalArgumentException("UDP server is under maintenance and is not available");
        }
    }

    public Server createServer() throws IllegalArgumentException {
        switch (level) {
            case TCP:
                return new TCPServer();
            case UDP:
            default:
                throw new IllegalArgumentException("UDP server is under maintenance and is not available");
        }
    }
}
