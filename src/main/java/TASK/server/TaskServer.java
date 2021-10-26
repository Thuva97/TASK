package TASK.server;

import TASK.consensus.BullyElection;
import TASK.consensus.HeartBeat;
import TASK.model.LocalChatRoom;
import TASK.service.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quartz.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskServer {
    private ServerState serverState = ServerState.getInstance();
    private ServerInfo serverInfo;
    private ExecutorService servicePool;
    private String mainHall;

    private Configuration systemProperties;

    public TaskServer(String[] args) {
        try {
            CmdLineParser cmdLineParser = new CmdLineParser(this);
            logger.info("Parsing args...");
            cmdLineParser.parseArgument(args);

            logger.info("option: -n " + serverId);
            logger.info("option: -l " + serverConfig);
            logger.info("option: -d " + debug);

            logger.info("Reading server config");
            readServerConfiguration();

            logger.info("option: -c " + systemPropertiesFile.toString());
            logger.info("Reading system properties file: " + systemPropertiesFile.toString());
            try {
                Configurations configs = new Configurations();
                systemProperties = configs.properties(systemPropertiesFile);
            } catch (ConfigurationException e) {
                logger.error("Configuration error :  " + e.getLocalizedMessage());
            }


            logger.info("Init server state");
            serverState.initServerState(serverId);

            serverInfo = serverState.getServerInfo();

            serverState.setElectionAnswerTimeout(systemProperties.getLong("election.answer.timeout"));
            serverState.setElectionCoordinatorTimeout(systemProperties.getLong("election.coordinator.timeout"));

            serverState.setupConnectedServers();



            mainHall = "MainHall-" + serverInfo.getServerId();
            LocalChatRoom localChatRoomInfo = new LocalChatRoom();
            localChatRoomInfo.setOwner(""); //The owner of the MainHall in each server is "" (empty string)
            localChatRoomInfo.setChatRoomId(mainHall);
            serverState.getLocalChatRooms().put(mainHall, localChatRoomInfo);

            startUpConnections();

            syncChatRooms();

            initiateCoordinator();

            startHeartBeat();


        } catch (CmdLineException e) {
            logger.error(e.getMessage());
        }
    }

    private void initiateCoordinator() {
        logger.debug("Starting initial coordinator election...");

        new BullyElection().startElection(serverState.getServerInfo(), serverState.getCandidateServerInfoList());

        new BullyElection().startWaitingForAnswerMessage(serverState.getServerInfo(),
                        serverState.getElectionAnswerTimeout());


    }

    private void startHeartBeat() {
        try {

            JobDetail aliveJob = JobBuilder.newJob(HeartBeat.class)
                    .withIdentity("ALIVE_JOB", "group1").build();

            aliveJob.getJobDataMap().put("aliveErrorFactor", systemProperties.getInt("alive.error.factor"));

            Trigger aliveTrigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity("ALIVE_JOB_TRIGGER", "group1")
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(systemProperties.getInt("alive.interval")).repeatForever())
                    .build();

            Scheduler scheduler = Quartz.getInstance().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(aliveJob, aliveTrigger);

        } catch (SchedulerException e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    private void readServerConfiguration() {
        ColumnPositionMappingStrategy<ServerInfo> strategy = new ColumnPositionMappingStrategy<>();
        strategy.setType(ServerInfo.class);
        CsvToBean<ServerInfo> csvToBean = new CsvToBean<>();
        try {
            serverState.setServerInfoList(csvToBean.parse(strategy, new CSVReader(new FileReader(serverConfig), '\t')));
        } catch (FileNotFoundException e) {
            logger.error("Can not load config file from location: " + serverConfig);
            logger.trace(e.getMessage());
        }
    }


    private void startUpConnections() {
        servicePool = Executors.newFixedThreadPool(SERVER_SOCKET_POOL);
        try {
            servicePool.execute(new ClientService(serverInfo.getClientPort(), CLIENT_SOCKET_POOL));
            servicePool.execute(new ManagementService(serverInfo.getServerPort(), serverState.getServerInfoList().size()));
        } catch (IOException e) {
            logger.trace(e.getMessage());
            servicePool.shutdown();
        }
    }


    /**
     * TODO Spec #4 improve server self register into system
     * This is working by utilising the existing protocols, i.e. by calling a few protocols.
     * A better approach might be, to create a new protocol to handle this.
     */
    private void syncChatRooms() {
        ServerCommunication serverCommunication = new ServerCommunication();
        JSONBuilder messageBuilder = JSONBuilder.getInstance();
        JSONParser parser = new JSONParser();

        for (ServerInfo server : serverState.getServerInfoList()) {
            if (server.equals(this.serverInfo)) continue;

            if (serverCommunication.isOnline(server)) {
                // promote my main hall
                serverCommunication.commPeer(server, messageBuilder.serverUpMessage());
                serverCommunication.commPeer(server, messageBuilder.updateRoomLeader(serverInfo.getServerId() , this.mainHall));
                //TODO serverUpMessage to send even earlier?
                //String[] messages = {messageBuilder.serverUpMessage(), messageBuilder.lockRoom(this.mainHall), messageBuilder.releaseRoom(this.mainHall, "true")};
                //peerClient.commPeer(server, messages);

                // accept theirs
                String resp = serverCommunication.commServerSingleResp(server, messageBuilder.listRoomsClient());
                if (resp != null) {
                    try {
                        JSONObject jsonMessage = (JSONObject) parser.parse(resp);
                        logger.trace("syncChatRooms: " + jsonMessage.toJSONString());
                        JSONArray ja = (JSONArray) jsonMessage.get(Protocol.rooms.toString());
                        for (Object o : ja.toArray()) {
                            String room = (String) o;
                            if (serverState.isRoomExistedRemotely(room)) continue;
                            RemoteChatRoomInfo remoteRoom = new RemoteChatRoomInfo();
                            remoteRoom.setChatRoomId(room);
                            String serverId = server.getServerId();
                            if (room.startsWith("MainHall")) { // every server has MainHall-s* duplicated
                                String sid = room.split("-")[1];
                                if (!sid.equalsIgnoreCase(serverId)) {
                                    //serverId = sid; // Or skip
                                    continue;
                                }
                            }
                            remoteRoom.setManagingServer(serverId);
                            serverState.getRemoteChatRooms().put(room, remoteRoom);
                        }
                    } catch (ParseException e) {
                        //e.printStackTrace();
                        logger.trace(e.getMessage());
                    }
                }
            }
        }
    }


    private static final int SERVER_SOCKET_POOL = 2;
    private static final int CLIENT_SOCKET_POOL = 100;
    private static final Logger logger = LogManager.getLogger(TaskServer.class);
}
