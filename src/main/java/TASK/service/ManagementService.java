package TASK.service;

import TASK.server.ServerState;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManagementService implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService pool;
    private ServerState serverState = ServerState.getInstance();

    public ManagementService(int port, int poolSize) throws IOException {
        ServerSocketFactory serversocketfactory =
                (ServerSocketFactory) ServerSocketFactory.getDefault();

        serverSocket = (ServerSocket) serversocketfactory.createServerSocket(port);
        pool = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void run() {
        try {

            System.out.println("Server listening peer on management port "+ serverSocket.getLocalPort() +" for a connection...");

            while (!serverState.isStopRunning()) {
                pool.execute(new ManagementConnection((Socket) serverSocket.accept()));
            }

            pool.shutdown();
        } catch (IOException ex) {
            pool.shutdown();
        } finally {
            pool.shutdownNow();
        }
    }

}
