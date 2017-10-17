package core.app.classes;

import core.di.annotation.Component;

@Component
public class ScannedDependency {

    public ScannedDependency() {}

    @Override
    public String toString() {
        return "I'm scanned dependency";
    }

}
