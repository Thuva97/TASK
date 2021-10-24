package TASK.service;

import TASK.client.ClientThread;
import TASK.server.ServerState;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ClientCommunication {
    private static ServerState serverState = ServerState.getInstance();

    public static void broadcastMessageToRoom(String message, String room) {

        Map<String, ClientThread> connectedClients = serverState.getConnectedClients();

        connectedClients.values().stream()
                .filter(clientThread -> clientThread.getClient().getRoomID().equalsIgnoreCase(room))
                .forEach(clientThread -> {
                    try {
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientThread.getClientSocket().getOutputStream(), StandardCharsets.UTF_8));
                        writer.write(message + "\n");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void write(Socket clientSocket, String message) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }






}
