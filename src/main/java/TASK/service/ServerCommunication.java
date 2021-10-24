package TASK.service;

import TASK.server.ServerInfo;
import org.json.simple.JSONObject;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ServerCommunication {

    public static boolean isOnline(ServerInfo serverInfo) {
        boolean online = true;
        try {
            InetSocketAddress address = new InetSocketAddress(serverInfo.getAddress(), serverInfo.getServerPort());
            final int timeOut = (int) TimeUnit.SECONDS.toMillis(5);
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            final SSLSocket shortKet = (SSLSocket) sslsocketfactory.createSocket();
            shortKet.connect(address, timeOut);
            shortKet.startHandshake();
            shortKet.close();
        } catch (IOException e) {
            //e.printStackTrace();
            online = false;
        }
        return online;


    }

    //send message to server
    public static void sendServer(JSONObject obj, ServerInfo destServer) throws IOException
    {
        Socket socket = new Socket(destServer.getAddress(),
                destServer.getServerPort());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }




}
