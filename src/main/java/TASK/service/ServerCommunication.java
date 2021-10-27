package TASK.service;

import TASK.server.ServerInfo;
import TASK.server.ServerState;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.SocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerCommunication {

    private final ServerState serverState = ServerState.getInstance();
    private final ServerInfo serverInfo = serverState.getServerInfo();
    private final JSONParser parser;
    private final SocketFactory socketfactory;

    public ServerCommunication() {
        parser = new JSONParser();
        socketfactory = (SocketFactory) SocketFactory.getDefault();
    }

    public boolean isOnline(ServerInfo serverInfo) {
        boolean online = true;
        try {
            InetSocketAddress address = new InetSocketAddress(serverInfo.getAddress(), serverInfo.getServerPort());
            final int timeOut = (int) TimeUnit.SECONDS.toMillis(7);
            final Socket shortKet = (Socket) socketfactory.createSocket();
            shortKet.connect(address, timeOut);
            shortKet.close();
        } catch (IOException e) {
            online = false;
        }
        return online;


    }



    public String commPeer(ServerInfo server, String message) {

        Socket socket = null;
        BufferedWriter writer = null;
        BufferedReader reader = null;
        try {
            socket = (Socket) socketfactory.createSocket(server.getAddress(), server.getServerPort());
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            writer.write(message + "\n");
            writer.flush();

            System.out.println("[S2S]Sending  : [" + server.getServerId()
                    + "@" + server.getAddress() + ":" + server.getServerPort() + "] " + message);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            return reader.readLine();

        } catch (IOException ioe) {
            System.out.println("Can't Connect: " + server.getServerId() + "@"
                    + server.getAddress() + ":" + server.getServerPort());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Unable to close the socket : " + e.getLocalizedMessage());
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // this exception does not affect the overall execution of the application
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // this exception does not affect the overall execution of the application
                }
            }
        }

        return null;
    }

    public void commPeerOneWay(ServerInfo server, String message) {

        Socket socket = null;
        BufferedWriter writer = null;
        try {
            socket = (Socket) socketfactory.createSocket(server.getAddress(), server.getServerPort());
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            writer.write(message + "\n");
            writer.flush();

            System.out.println("[S2S]Sending  : [" + server.getServerId()
                    + "@" + server.getAddress() + ":" + server.getServerPort() + "] " + message);
            writer.close();
        } catch (IOException ioe) {
            System.out.println("Can't Connect: " + server.getServerId() + "@"
                    + server.getAddress() + ":" + server.getServerPort());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Unable to close the socket : " + e.getLocalizedMessage());
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // this exception does not affect the overall execution of the application
                }
            }
        }
    }

    // for Room and Identity creation
    public boolean leaderApproval(String jsonMessage) {
        boolean approve = true;

        ServerInfo leader = serverState.getCoordinator();
        String resp = commPeer(leader, jsonMessage);

        JSONObject jj = null;
        try {
            jj = (JSONObject) parser.parse(resp);
        } catch (ParseException e) {
            System.out.println("Unable to parse : " + e.getLocalizedMessage());
        }
        if (jj != null) {
            System.out.println("[S2S]Receiving from leader wd ID: [" + leader.getServerId()
                    + "@" + leader.getAddress() + ":" + leader.getServerPort() + "] " + jj.toJSONString());
            // {"type":"updateIdentityConfirm","approved":"false"}
            String status = (String) jj.get("approved");
            if (status.equalsIgnoreCase("false")) {
                approve = false; // denied
            }
        }
        return approve;
    }

    public void relaySelectedPeers(List<ServerInfo> selectedPeers, String jsonMessage) {
        selectedPeers.stream()
                .filter(server -> !server.getServerId().equalsIgnoreCase(this.serverInfo.getServerId()))
                .forEach(server -> commPeerOneWay(server, jsonMessage));
    }

    public void relayPeers(String jsonMessage) {
        relaySelectedPeers(serverState.getServerInfoList(), jsonMessage);
    }
    public void relayPeersFromLeader(String jsonMessage, String serverid) {
        relaySelectedPeersFromLeader(serverState.getServerInfoList(), jsonMessage, serverid);
    }
    public void relaySelectedPeersFromLeader(List<ServerInfo> selectedPeers, String jsonMessage, String serverid) {
        selectedPeers.stream()
                .filter(server -> !server.getServerId().equalsIgnoreCase(this.serverInfo.getServerId()) && !server.getServerId().equalsIgnoreCase(serverid))
                .forEach(server -> commPeerOneWay(server, jsonMessage));
    }


    public String commServerSingleResp(ServerInfo server, String message) {
        BufferedWriter writer = null;
        BufferedReader reader = null;
        Socket socket = null;
        try {
            socket = (Socket) socketfactory.createSocket(server.getAddress(), server.getServerPort());
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));


            writer.write(message + "\n");
            writer.flush();

            System.out.println("[A52]Sending  : [" + server.getServerId()
                    + "@" + server.getAddress() + ":" + server.getServerPort() + "] " + message);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            String resp = reader.readLine();

            return resp;

        } catch (IOException ioe) {
            System.out.println("[A52]Can't Connect: " + server.getServerId() + "@"
                    + server.getAddress() + ":" + server.getServerPort());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Unable to close the socket : " + e.getLocalizedMessage());
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

}
