package core.app.classes;

import core.app.classes.service.GreetingService;

public class Bus implements Transport {
    private String message;
    private final GreetingService greetingService;

    public Bus() {
        message = "I am the Bus!";
        greetingService = null;
    }

    public Bus(GreetingService greetingService) {
        message = "I am the Bus!";
        this.greetingService = greetingService;
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
