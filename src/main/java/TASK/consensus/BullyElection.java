package TASK.consensus;

import TASK.server.ServerInfo;
import TASK.server.ServerState;
import TASK.service.JSONBuilder;
import TASK.service.ServerCommunication;

public class BullyElection {


    public static void sendCoordinatorMsg() {
        for ( String key : ServerState.getInstance().getServers().keySet() ) {
            if (!key.equals(ServerState.getInstance().getServerInfo().getServerId())){
                ServerInfo destServer = ServerState.getInstance().getServers().get(key);

                try {
                    ServerCommunication.sendServer(
                            JSONBuilder.getCoordinator( String.valueOf(ServerState.getInstance().getServerInfo().getServerId()) ),
                            destServer
                    );
                    System.out.println("INFO : Sent leader ID to s"+destServer.getServerId());
                }
                catch(Exception e) {
                    System.out.println("WARN : Server s"+destServer.getServerId()+
                            " has failed, it will not receive the leader");
                }
            }
        }

    }

    public static void sendOK(String serverID) {
        try {
            ServerInfo destServer = ServerState.getInstance().getServers().get(serverID);
            ServerCommunication.sendServer(
                    JSONBuilder.getOk( String.valueOf(ServerState.getInstance().getServerInfo().getServerId()) ),
                    destServer
            );
            System.out.println("INFO : Sent OK to s"+destServer.getServerId());
        }
        catch(Exception e) {
            System.out.println("INFO : Server s"+serverID+" has failed. OK message cannot be sent");
        }
    }

    public static void sendElectionRequest()
    {
        System.out.println("INFO : Election initiated");
        for ( String key : ServerState.getInstance().getServers().keySet() ) {
            if( Integer.parseInt(key) > Integer.parseInt(ServerState.getInstance().getServerInfo().getServerId()) ){
                ServerInfo destServer = ServerState.getInstance().getServers().get(key);
                try {
                    ServerCommunication.sendServer(
                            JSONBuilder.getElection( String.valueOf(ServerState.getInstance().getServerInfo().getServerId()) ),
                            destServer
                    );
                    System.out.println("INFO : Sent election request to s"+destServer.getServerId());
                }
                catch(Exception e){
                    System.out.println("WARN : Server s"+destServer.getServerId() +
                            " has failed, cannot send election request");
                }
            }

        }
    }

    public static void initialize()
    {
        // Initiate election
        sendElectionRequest();

    }

}
