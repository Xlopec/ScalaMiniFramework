package core.app.classes;

import core.app.classes.service.GreetingService;
import core.di.annotation.Autowiring;
import core.di.annotation.Component;

@Component
public class Bus implements Transport {
    private String message;
    private final GreetingService greetingService;

    @Autowiring
    public Bus() {
        message = "I am the Bus!";
        greetingService = null;
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
