package core.app.webapp.server;

import core.di.annotation.Autowiring;
import core.di.annotation.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

@Component
public final class Server {
    private final int port;
    private final IDateService service;
    private ServerSocket listener;

    public Server(@Autowiring(named = "port") int port, IDateService service) {
        this.port = port;
        this.service = service;
    }

    public void start() throws IOException {
        stop();
        listener = new ServerSocket(port);
        System.out.println(String.format("Server is listening on the port: %d", port));

        try {
            while (true) {
                final Socket socket = listener.accept();

                try {
                    final PrintWriter out =
                            new PrintWriter(socket.getOutputStream(), true);

                    final Date date = service.getCurrentDate();

                    System.out.println(String.format("Returning date to the client %s", date));
                    out.println(date.toString());
                } finally {
                    socket.close();
                }
            }
        } finally {
            listener.close();
        }
    }

    public void stop() throws IOException {
        if (listener != null) {
            listener.close();
        }
    }

}
