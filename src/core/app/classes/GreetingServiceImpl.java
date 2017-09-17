package core.app.classes;

public class GreetingServiceImpl implements GreetingService {
    private String message;
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

    @Override
    public void injectAllah(String allah) {
        System.out.println("Allah!!! " + allah);
    }

    public void transportMessage() {
        transport.getTransport();
    }

}
