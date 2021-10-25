package TASK.service;

import TASK.model.LocalChatRoom;
import TASK.model.UserInfo;
import TASK.server.ServerState;
import TASK.service.ClientCommunication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientConnection implements Runnable {

    private BufferedReader reader;
    private BufferedWriter writer;
    private ExecutorService pool;
    private JSONParser parser;
    private JSONBuilder messageBuilder;
    private ServerCommunication serverCommunication;
    private ClientCommunication clientCommunication;

    private Socket clientSocket;
    private UserInfo userInfo;
    private boolean routed = false;

    private ServerState serverState = ServerState.getInstance();

    public ClientConnection(Socket clientSocket) {
        try {
            this.clientSocket = clientSocket;
            this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            this.writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
            this.parser = new JSONParser();
            this.messageBuilder = JSONBuilder.getInstance();
            this.pool = Executors.newSingleThreadExecutor();
            this.serverCommunication = new ServerCommunication();
            this.clientCommunication = new ClientCommunication();
        } catch (Exception e) {
            logger.trace(e.getMessage());
        }
    }

    @Override
    public void run() {

        try {

            while (true) {

                String msg = reader.readLine();

                // convert received message to json object
                JSONObject jsonMessage = (JSONObject) parser.parse(msg);
                logger.debug("Receiving: " + msg);

//                if (!msg.isFromClient() && msg.getMessage().equalsIgnoreCase("exit")) {
//                    //The client program is abruptly terminated (e.g. using Ctrl-C)
//                    ProtocolHandlerFactory.newClientHandler(null, this).handle();
//                    logger.trace("EOF");
//                    break;
//                }


                String type = (String) jsonMessage.get("type");

                if (type.equalsIgnoreCase("list")){
                    write(messageBuilder.listRooms());
                }
                else if (type.equalsIgnoreCase("newidentity")) {

                    String mainHall = "MainHall-" + serverState.getServerInfo().getServerId();
                    String requestIdentity = (String) jsonMessage.get("identity");

                    boolean isUserExisted = serverState.isUserExisted(requestIdentity);

                    if (isUserExisted) {
                        // {"type" : "newidentity", "approved" : "false"}
                        this.write(messageBuilder.newIdentityResp("false"));
                    } else {

                        boolean approved = serverCommunication.leaderApproval(messageBuilder.updateIdentityLeader(serverState.getServerInfo().getServerId(), requestIdentity));

                        if (approved) {
                            userInfo = new UserInfo();
                            userInfo.setIdentity(requestIdentity);
                            userInfo.setCurrentChatRoom(mainHall);
                            userInfo.setManagingThread(this);
                            userInfo.setSocket(clientSocket);

                            this.setUserInfo(userInfo);

                            serverState.getConnectedClients().put(requestIdentity, userInfo);
                            serverState.getLocalChatRooms().get(mainHall).addMember(requestIdentity);

                            logger.info("Client connected: " + requestIdentity);

                            //{"type" : "newidentity", "approved" : "true"}
                            this.write(messageBuilder.newIdentityResp("true"));

                            //{"type" : "roomchange", "identity" : "Adel", "former" : "", "roomid" : "MainHall-s1"}
                            clientCommunication.broadcastMessageToRoom(messageBuilder.roomChange("", mainHall, userInfo.getIdentity()), mainHall);
                        } else {
                            this.write(messageBuilder.newIdentityResp("false"));
                        }

                    }

                }

                else if (type.equalsIgnoreCase("createroom")) {

                    String requestRoomId = (String) jsonMessage.get("roomid");
                    boolean isRoomExisted = serverState.isRoomExistedGlobally(requestRoomId);
                    if (isRoomExisted) {
                        // {"type" : "newidentity", "approved" : "false"}
                        this.write(messageBuilder.createRoomResp(requestRoomId, "false"));
                    } else {

                        boolean approved = serverCommunication.leaderApproval(messageBuilder.updateRoomLeader(serverState.getServerInfo().getServerId(), requestRoomId));

                        if (approved) {

                            LocalChatRoom newRoom = new LocalChatRoom();
                            newRoom.setChatRoomId(requestRoomId);
                            newRoom.setOwner(userInfo.getIdentity());
                            newRoom.addMember(userInfo.getIdentity());
                            serverState.getLocalChatRooms().put(requestRoomId, newRoom);

                            String former = userInfo.getCurrentChatRoom();
                            serverState.getLocalChatRooms().get(former).removeMember(userInfo.getIdentity());

                            userInfo.setCurrentChatRoom(requestRoomId);
                            userInfo.setRoomOwner(true);

                            this.write(messageBuilder.createRoomResp(requestRoomId, "true"));
                            this.write(messageBuilder.roomChange(former, userInfo.getCurrentChatRoom(), userInfo.getIdentity()));
                            clientCommunication.broadcastMessageToRoom(messageBuilder.roomChange(former, userInfo.getCurrentChatRoom(), userInfo.getIdentity()), former);

                        } else {
                            this.write(messageBuilder.createRoomResp(requestRoomId, "false"));
                        }

                    }

                }

                else if (){


                }


            }

            pool.shutdown();
            writer.close();
            reader.close();
            clientSocket.close();

            if (userInfo != null) {
                logger.info("Client disconnected: " + userInfo.getIdentity());
            }

        } catch (Exception e) {
            logger.debug(e.getMessage());
            pool.shutdownNow();
        } finally {

            // Let close the socket at this point as no longer use.
            // So that no side effect.
            if (!clientSocket.isClosed()) {
                try {
                    writer.close();
                    reader.close();
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void write(String msg) {
        try {
            writer.write(msg + "\n");
            writer.flush();

            logger.trace("Message flush");

        } catch (IOException e) {
            logger.trace(e.getMessage());
        }
    }


    public Socket getClientSocket() {
        return clientSocket;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public boolean isRouted() {
        return routed;
    }

    public void setRouted(boolean routed) {
        this.routed = routed;
    }


    private static final Logger logger = LogManager.getLogger(ClientConnection.class);
}
