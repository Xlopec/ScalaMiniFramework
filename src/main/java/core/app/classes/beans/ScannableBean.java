package core.app.classes.beans;

import core.app.classes.service.GreetingService;
import core.app.classes.ScannableComponent;
import core.di.annotation.Autowiring;
import core.di.annotation.Component;

@Component(id = "MyScannableBean")
public class ScannableBean {

    private final GreetingService constructorsService;
    private final ScannableComponent constructorTypedDependency;
    private final String constructorStringProperty;

    @Autowiring(named = "someProp")
    private String fieldStringProperty;
    @Autowiring(named = "greetingService")
    private GreetingService fieldService;
    @Autowiring
    private ScannableComponent fieldTypedDependency;

    public ScannableBean(@Autowiring(named = "greetingService") GreetingService service,
                         ScannableComponent constructorTypedDependency,
                         @Autowiring(named = "someProp") String constructorStringProperty) {
        this.constructorsService = service;
        this.constructorTypedDependency = constructorTypedDependency;
        this.constructorStringProperty = constructorStringProperty;
    }

    public GreetingService getConstructorsService() {
        return constructorsService;
    }

    public ScannableComponent getConstructorTypedDependency() {
        return constructorTypedDependency;
    }

    public String getConstructorStringProperty() {
        return constructorStringProperty;
    }

    public String getFieldStringProperty() {
        return fieldStringProperty;
    }

    public GreetingService getFieldService() {
        return fieldService;
    }

    public ScannableComponent getFieldTypedDependency() {
        return fieldTypedDependency;
    }

    @Override
    public String toString() {
        return String.format("Constructors injections: service %s, scannable component %s, string property %s\n" +
                        "Field injections: service %s, scannable component %s, string property %s\n",
                constructorsService.getMessage(), constructorTypedDependency, constructorStringProperty,
                fieldService.getMessage(), fieldTypedDependency, fieldStringProperty);
    }

}
