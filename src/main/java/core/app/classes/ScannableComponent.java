package core.app.classes;

import core.di.annotation.Component;

@Component
public class ScannableComponent {

    public ScannableComponent() {}

    @Override
    public String toString() {
        return "I'm scanned dependency";
    }

}
