package com.example.micrometer;

import java.net.ServerSocket;

final class SocketUtils {

    private SocketUtils() {
    }

    public static int findAvailableTcpPort() {
        try {
            ServerSocket s = new ServerSocket(0);
            int port = s.getLocalPort();
            s.close();
            return port;
        }
        catch (Exception e) {
            // ignore
        }
        return 0;
    }

}
