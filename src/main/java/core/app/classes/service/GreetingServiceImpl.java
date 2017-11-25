package core.app.classes.service;

import core.app.classes.Bus;
import core.app.classes.InterfaceInjectable;
import core.app.classes.Transport;
import core.di.annotation.Autowiring;
import core.di.annotation.Component;
import core.di.annotation.Singleton;

@Component(id = "greetingService")
@Singleton
public class GreetingServiceImpl implements GreetingService, InterfaceInjectable {
    private String message;
    private String something;
    private Transport transport;
    
    public GreetingServiceImpl(@Autowiring(named = "someProp") String message, Bus transport) {
        this.message = message;
        this.transport = transport;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }

    public void transportMessage() {
        transport.getTransport();
    }

    @Override
    public String getSomething() {
        return something;
    }

    @Override
    public void injectSomething(String s) {
        this.something = s;
    }
}
