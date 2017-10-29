package core.app.webapp.server;

import core.di.BeanContext;
import core.di.BeanFactory;
import core.di.imp.XmlBeanContext;

import java.io.File;
import java.io.IOException;

/**
 * A TCP server that runs on port 9090.  When a client connects, it
 * sends the client the current date and time, then closes the
 * connection with that client.  Arguably just about the simplest
 * server you can write.
 */
public class DateServer {
    /**
     * Runs the server.
     */
    public static void main(String[] args) throws IOException {
        final BeanContext context = new XmlBeanContext(new File("/Users/max/IdeaProjects/ScalaMiniFramework/src/main/resources/ServerConfig.xml"));
        final BeanFactory factory = context.getBeanFactory();
        final Server server = factory.instantiate(Server.class);

        server.start();
    }

}
