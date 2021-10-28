package TASK.service;

import org.kohsuke.args4j.Option;

public class CmdLineValues {
    @Option(required=false, name = "-i", aliases="--id", usage="Server host address")
    private char serverid = '1';

    @Option(required=false, name="-p", aliases="--configpath", usage="Server port number")
    private String configpath = "src/main/java/TASK/config/server.txt";

    public String getServerid() {
        return String.valueOf(serverid);
    }

    public String getConfigpath() {
        return configpath;
    }
}