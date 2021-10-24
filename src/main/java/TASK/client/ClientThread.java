package TASK.client;

import TASK.server.ServerState;
import TASK.service.JSONBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static TASK.service.ClientCommunication.broadcastMessageToRoom;
import static TASK.service.ClientCommunication.write;

public class ClientThread extends Thread {

    private static final Logger logger = LogManager.getLogger(ClientThread.class);
    private final Socket clientSocket;
    private Client client;
    private final JSONParser parser;
    private final JSONBuilder messageBuilder;
    private BufferedReader reader;



    public ClientThread(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        parser = new JSONParser();
        messageBuilder = JSONBuilder.getInstance();
        this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public static boolean hasKey(JSONObject jsonObject, String key) {
        return (jsonObject != null && jsonObject.get(key) != null);
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    @Override
    public void run() {
        try {
            System.out.println("INFO : THE CLIENT" + " " + clientSocket.getInetAddress()
                    + ":" + clientSocket.getPort() + " IS CONNECTED ");


            while (true) {

                String jsonStringFromClient = reader.readLine();

                if (jsonStringFromClient.equalsIgnoreCase("exit")) {
                    break;
                }

                try {
                    //convert received message to json object
                    Object object = null;
                    JSONParser jsonParser = new JSONParser();
                    object = jsonParser.parse(jsonStringFromClient);
                    JSONObject j_object = (JSONObject) object;

                    if (hasKey(j_object, "type")) {
                        //check new identity format
                        if (j_object.get("type").equals("newidentity") && j_object.get("identity") != null) {
                            String newClientID = j_object.get("identity").toString();
                            newID(newClientID, clientSocket);
                        } //check create room
                        if (j_object.get("type").equals("createroom") && j_object.get("roomid") != null) {
                            String newRoomID = j_object.get("roomid").toString();
                            createRoom(newRoomID, clientSocket);
                        } //check who
                        if (j_object.get("type").equals("who")) {
                            who(clientSocket);
                        } //check list
                        if (j_object.get("type").equals("list")) {
                            list(clientSocket);
                        } //check join room
                        if (j_object.get("type").equals("joinroom")) {
                            String roomID = j_object.get("roomid").toString();
                            joinRoom(roomID, clientSocket);
                        } //check delete room
                        if (j_object.get("type").equals("deleteroom")) {
                            String roomID = j_object.get("roomid").toString();
                            deleteRoom(roomID, clientSocket);
                        } //check message
                        if (j_object.get("type").equals("message")) {
                            String content = j_object.get("content").toString();
                            message(content, clientSocket);
                        } //check quit
                        if (j_object.get("type").equals("movejoin")) {
                            moveJoin(j_object);
                        }
                        if (j_object.get("type").equals("quit")) {
                            quit(clientSocket);
                        }
                    } else {
                        System.out.println("WARN : Command error, Corrupted JSON");
                    }

                } catch (ParseException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }






    public void moveJoin(JSONObject jsonMessage) {
        // {"type" : "movejoin", "former" : "MainHall-s1", "roomid" : "jokes", "identity" : "Maria"}
        String joiningRoomId = (String) jsonMessage.get("roomid");
        String former = (String) jsonMessage.get("former");
        String identity = (String) jsonMessage.get("identity");
        boolean roomExistedLocally = ServerState.getInstance().isRoomExistedLocally(joiningRoomId);

        String roomId;
        if (roomExistedLocally) {
            roomId = joiningRoomId;
        } else {
            // room has gone, place in MainHall
            roomId = ServerState.getInstance().getMainHall().getChatRoomId();
        }
        Client client = new Client(identity, roomId, clientSocket);

        setClient(client);
        ServerState.getInstance().getConnectedClients().put(identity, this);
        ServerState.getInstance().getLocalChatRooms().get(roomId).addMember(client);

        logger.info("Client connected: " + identity);

        write(clientSocket, messageBuilder.serverChange("true", ServerState.getInstance().getServerInfo().getServerId()));
        broadcastMessageToRoom(messageBuilder.roomChange(former, roomId, client.getClientID()), roomId);
    }



}
