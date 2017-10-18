package core.app.classes;

import core.di.BeanContext;
import core.di.BeanFactory;
import core.di.imp.XmlBeanContext;

import java.io.File;

/**
 * Created by Максим on 9/10/2017.
 */
public class Tester {

    public static void main(String...args) throws NoSuchMethodException {
        // TODO: 18.10.17 add tests
        BeanContext context = new XmlBeanContext(new File("/Users/max/IdeaProjects/ScalaMiniFramework/src/main/resources/GS_SpringXMLConfig.xml"));
        BeanFactory factory = context.getBeanFactory();

        final GreetingService greetingService1 = factory.instantiate("greetingService");
        final GreetingService greetingService2 = factory.instantiate("greetingService");

        System.out.println(String.format("greetingService singleton %b", greetingService1 == greetingService2));

        System.out.println(greetingService1.getMessage());

        ((GreetingServiceImpl)greetingService1).transportMessage();

        final ScannedClass scannedClass1 = factory.instantiate("MyScannedClass");
        final ScannedClass scannedClass2 = factory.instantiate("MyScannedClass");

        System.out.println(String.format("Greeter says: %s", scannedClass1.greet()));
        System.out.println(String.format("scannedClass singleton %b", scannedClass1 == scannedClass2));
    }

}