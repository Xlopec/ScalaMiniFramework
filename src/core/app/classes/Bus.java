package core.app.classes;

public class Bus implements Transport {
    private String message;
    
    public Bus() {
        message = "I am the Bus!";
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void getTransport() {
        System.out.println(message);
    }
}
