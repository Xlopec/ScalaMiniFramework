package core.app.webapp.client;

import core.di.BeanContext;
import core.di.BeanFactory;
import core.di.imp.XmlBeanContext;

import java.io.File;
import java.io.IOException;

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
        final BeanContext context = new XmlBeanContext(new File("/Users/max/IdeaProjects/ScalaMiniFramework/src/main/resources/ServerConfig.xml"));
        final BeanFactory factory = context.getBeanFactory();
        final Client client = factory.instantiate(Client.class);

        client.connect();
    }
}
