package core.app.webapp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;

/**
 * Trivial client for the date server.
 */
public class DateClient {

    /**
     * Runs the client as an application.  First it displays a dialog
     * box asking for the IP address or hostname of a host running
     * the date server, then connects to it and displays the date that
     * it serves.
     */
    public static void main(String[] args) throws IOException {
        final String serverAddress;

        if (args == null || args.length == 0) {
            serverAddress = "127.0.0.1";
        } else {
            if (args.length != 1) {
                throw new IllegalArgumentException(String.format("Invalid input args, were %s", Arrays.toString(args)));
            }
            serverAddress = args[0];
        }

        Socket s = new Socket(serverAddress, 9090);
        BufferedReader input =
                new BufferedReader(new InputStreamReader(s.getInputStream()));
        String answer = input.readLine();

        System.out.println(answer);
        System.exit(0);
    }
}
