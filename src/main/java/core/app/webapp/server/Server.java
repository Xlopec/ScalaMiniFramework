package core.app.webapp.server;

import core.di.annotation.Autowiring;
import core.di.annotation.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

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

        try {
            while (true) {
                Socket socket = listener.accept();
                try {
                    PrintWriter out =
                            new PrintWriter(socket.getOutputStream(), true);
                    out.println(service.getCurrentDate().toString());
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
