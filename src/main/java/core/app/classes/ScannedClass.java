package core.app.classes;

import core.di.annotation.Autowiring;
import core.di.annotation.Component;

@Component(id = "MyScannedClass")
public class ScannedClass {

    private final GreetingService greetingService;
    private final ScannedDependency dependency;
    private final String var;

    public ScannedClass(@Autowiring(named = "greetingService") GreetingService greetingService,
                        ScannedDependency dependency,
                        @Autowiring(named = "someProp") String var) {
        this.greetingService = greetingService;
        this.dependency = dependency;
        this.var = var;
    }

    public String greet() {
        return greetingService.getMessage() + " " + dependency.toString() + " " + var;
    }

}
