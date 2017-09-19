package core.app.classes;

public class GreetingServiceImpl implements GreetingService, InterfaceInjectable {
    private String message;
    private String something;
    private Transport transport;
    
    public GreetingServiceImpl(String message, Transport transport) {
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
