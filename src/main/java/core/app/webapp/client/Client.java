package core.app.webapp.client;

import core.di.annotation.Autowiring;
import core.di.annotation.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

@Component
public final class Client {

    private final String host;
    private final int port;
    private final IRenderer dateRenderer;

    private Socket socket;

    public Client(@Autowiring(named = "host") String host, @Autowiring(named = "port") int port, IRenderer dateRenderer) {
        this.host = host;
        this.port = port;
        this.dateRenderer = dateRenderer;
    }

    public void connect() throws IOException {
        disconnect();
        socket = new Socket(host, port);
        dateRenderer.render(new InputStreamReader(socket.getInputStream()));
    }

    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

}
