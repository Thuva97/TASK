package TASK.server;

import TASK.consensus.BullyElection;
import TASK.consensus.HeartBeat;
import TASK.model.LocalChatRoom;
import TASK.model.RemoteChatRoom;
import TASK.model.RemoteUserInfo;
import TASK.service.*;

import TASK.service.Scheduler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quartz.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class TaskServer {
    private static final int SERVER_SOCKET_POOL = 2;
    private static final int CLIENT_SOCKET_POOL = 100;
    private static ServerState serverState = ServerState.getInstance();
    private static ServerInfo serverInfo;
    private static ExecutorService servicePool;
    private static String mainHall;
    private static Integer alive_interval = 5;
    private static Integer alive_error_factor = 2;

    public static void main(String[] args) {

        try {


            String serverID = null;
            String configPath = null;
            CmdLineValues values = new CmdLineValues();
            CmdLineParser parser = new CmdLineParser(values);
            try {
                parser.parseArgument(args);
                serverID = values.getServerid();
                configPath = values.getConfigpath();
            } catch (CmdLineException e) {
                System.err.println("Error while parsing cmd line arguments: " + e.getLocalizedMessage());
            }
//            Scanner scanner = new Scanner(System.in);
//            System.out.println("Enter server id");
//            String serverID = scanner.nextLine();  // Read user input

            System.out.println("Reading server config");
            serverState.initializeWithConfigs(serverID,configPath);



            serverState.setupConnectedServers();
            serverInfo = serverState.getServerInfo();

            mainHall = "MainHall-" + serverInfo.getServerId();
            LocalChatRoom localChatRoomInfo = new LocalChatRoom();
            localChatRoomInfo.setOwner("");
            localChatRoomInfo.setChatRoomId(mainHall);
            serverState.getLocalChatRooms().put(mainHall, localChatRoomInfo);

            startUpConnections();

            initiateCoordinator();

            while (true) {
                if (serverState.getCoordinator() != null ){
                    sync();
                    break;
                }
            }

            startHeartBeat();


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void initiateCoordinator() {

        if (!serverState.getOngoingElection()) {
            serverState.setOngoingElection(true);
            System.out.println("Starting initial coordinator election...");

            new BullyElection().startWaitingForAnswerMessage();

            new BullyElection().startElection(serverState.getServerInfo(), serverState.getCandidateServerInfoList());
        }

    }

    private static void startHeartBeat() {
        try {

            JobDetail aliveJob = JobBuilder.newJob(HeartBeat.class)
                    .withIdentity("ALIVE_JOB", "group1").build();

            aliveJob.getJobDataMap().put("aliveErrorFactor", alive_error_factor);

            Trigger aliveTrigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity("ALIVE_JOB_TRIGGER", "group1")
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(alive_interval).repeatForever())
                    .build();

            org.quartz.Scheduler scheduler = Scheduler.getInstance().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(aliveJob, aliveTrigger);

        } catch (SchedulerException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }


    private static void startUpConnections() {
        servicePool = Executors.newFixedThreadPool(SERVER_SOCKET_POOL);
        try {
            servicePool.execute(new ClientService(serverInfo.getClientPort(), CLIENT_SOCKET_POOL));
            servicePool.execute(new ManagementService(serverInfo.getServerPort(), serverState.getServerInfoList().size()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            servicePool.shutdown();
        }
    }

    private static void sync() {
        ServerCommunication serverCommunication = new ServerCommunication();
        JSONBuilder messageBuilder = JSONBuilder.getInstance();
        JSONParser parser = new JSONParser();

        for (ServerInfo server : serverState.getServerInfoList()) {
            if (server.getServerId().equals(serverState.getServerInfo().getServerId())) continue;
            if (serverCommunication.isOnline(server)) {

                // send main hall
                serverCommunication.commPeer(server, messageBuilder.serverUpMessage());
                serverCommunication.commPeer(server, messageBuilder.updateRoomServer(serverInfo.getServerId(), mainHall));


                // accept theirs
                String resp = serverCommunication.commServerSingleResp(server, messageBuilder.getUpdate());
                if (resp != null) {
                    try {
                        JSONObject jsonMessage = (JSONObject) parser.parse(resp);
                        JSONArray rooms = (JSONArray) jsonMessage.get("rooms");
                        JSONArray clients = (JSONArray) jsonMessage.get("clients");
                        String serverId = server.getServerId();

                        for (Object o : clients.toArray()){
                            String client = (String) o;
                            if (serverState.isUserExistedRemotely(client)) continue;
                            RemoteUserInfo remoteUserInfo = new RemoteUserInfo(client, serverId);
                            serverState.getRemoteClients().put(client, remoteUserInfo);
                        }

                        for (Object o : rooms.toArray()) {
                            String room = (String) o;
                            if (serverState.isRoomExistedRemotely(room)) continue;
                            RemoteChatRoom remoteRoom = new RemoteChatRoom();
                            remoteRoom.setChatRoomId(room);
                            if (room.startsWith("MainHall")) {
                                String sid = room.split("-")[1];
                                if (!sid.equalsIgnoreCase(serverId)) {
                                    continue;
                                }
                            }
                            remoteRoom.setManagingServer(serverId);
                            serverState.getRemoteChatRooms().put(room, remoteRoom);
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }
    }


}
