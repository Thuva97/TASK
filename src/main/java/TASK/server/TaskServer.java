package TASK.server;

import TASK.consensus.BullyElection;
import TASK.consensus.HeartBeat;
import TASK.model.LocalChatRoom;
import TASK.model.RemoteChatRoom;
import TASK.service.*;

//import org.apache.logging.log4j.Level;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.spi.LoggerContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quartz.*;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskServer {
    private ServerState serverState = ServerState.getInstance();
    private ServerInfo serverInfo;
    private ExecutorService servicePool;
    private String mainHall;
    private static Integer alive_interval = 5;
    private static Integer alive_error_factor = 2;
    private static long election_answer_timeout = 10;
    private static long election_coordinator_timeout = 10;

    public TaskServer() {
        try {
            Scanner scanner = new Scanner(System.in);
            String serverID = scanner.nextLine();  // Read user input

            System.out.println("Reading server config");
            serverState.initializeWithConfigs(serverID,"./src/main/java/TASK/config/server.txt");


            serverState.setElectionAnswerTimeout(election_answer_timeout);
            serverState.setElectionCoordinatorTimeout(election_coordinator_timeout);

            serverState.setupConnectedServers();
            serverInfo = serverState.getServerInfo();

            mainHall = "MainHall-" + serverInfo.getServerId();
            LocalChatRoom localChatRoomInfo = new LocalChatRoom();
            localChatRoomInfo.setOwner("");
            localChatRoomInfo.setChatRoomId(mainHall);
            serverState.getLocalChatRooms().put(mainHall, localChatRoomInfo);

            startUpConnections();

            syncChatRooms();

            initiateCoordinator();

            startHeartBeat();


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void initiateCoordinator() {
        System.out.println("Starting initial coordinator election...");
        new BullyElection().startElection(serverState.getServerInfo(), serverState.getCandidateServerInfoList());

        new BullyElection().startWaitingForAnswerMessage(serverState.getServerInfo(),
                        serverState.getElectionAnswerTimeout());


    }

    private void startHeartBeat() {
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

            Scheduler scheduler = Quartz.getInstance().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(aliveJob, aliveTrigger);

        } catch (SchedulerException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }


    private void startUpConnections() {
        servicePool = Executors.newFixedThreadPool(SERVER_SOCKET_POOL);
        try {
            servicePool.execute(new ClientService(serverInfo.getClientPort(), CLIENT_SOCKET_POOL));
            servicePool.execute(new ManagementService(serverInfo.getServerPort(), serverState.getServerInfoList().size()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            servicePool.shutdown();
        }
    }

    private void syncChatRooms() {
        ServerCommunication serverCommunication = new ServerCommunication();
        JSONBuilder messageBuilder = JSONBuilder.getInstance();
        JSONParser parser = new JSONParser();

        for (ServerInfo server : serverState.getServerInfoList()) {
            if (server.getServerId().equals(serverState.getServerInfo().getServerId())) continue;
            if (serverCommunication.isOnline(server)) {

                // send main hall
                serverCommunication.commPeer(server, messageBuilder.serverUpMessage());
                serverCommunication.commPeer(server, messageBuilder.updateRoomServer(serverInfo.getServerId(), this.mainHall));


                // accept theirs
//                String resp = serverCommunication.commServerSingleResp(server, messageBuilder.listRoomsClient());
//                if (resp != null) {
//                    try {
//                        JSONObject jsonMessage = (JSONObject) parser.parse(resp);
//                        logger.trace("syncChatRooms: " + jsonMessage.toJSONString());
//                        JSONArray ja = (JSONArray) jsonMessage.get("rooms");
//                        for (Object o : ja.toArray()) {
//                            String room = (String) o;
//                            if (serverState.isRoomExistedRemotely(room)) continue;
//                            RemoteChatRoom remoteRoom = new RemoteChatRoom();
//                            remoteRoom.setChatRoomId(room);
//                            String serverId = server.getServerId();
//                            if (room.startsWith("MainHall")) { // every server has MainHall-s* duplicated
//                                String sid = room.split("-")[1];
//                                if (!sid.equalsIgnoreCase(serverId)) {
//                                    //serverId = sid; // Or skip
//                                    continue;
//                                }
//                            }
//                            remoteRoom.setManagingServer(serverId);
//                            serverState.getRemoteChatRooms().put(room, remoteRoom);
//                        }
//                    } catch (ParseException e) {
//                        //e.printStackTrace();
//                        logger.trace(e.getMessage());
//                    }
//                }
            }
        }
    }




    private static final int SERVER_SOCKET_POOL = 2;
    private static final int CLIENT_SOCKET_POOL = 100;
//    private static final Logger logger = LogManager.getLogger(TaskServer.class);
}
