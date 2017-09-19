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

        BeanContext context = new XmlBeanContext(new File("D:\\Workspace Intellij Idea\\ScalaFramework\\src\\main\\resources\\GS_SpringXMLConfig.xml"));

        BeanFactory factory = context.getBeanFactory();

        //Bus b = factory.instantiate(Bus.class);

        //System.out.println(b.getMessage());

        GreetingService greetingService = factory.instantiate(GreetingServiceImpl.class);

        System.out.println(greetingService.getMessage());
        ((GreetingServiceImpl)greetingService).transportMessage();

        /*System.out.println(B.class.getMethod("f").getDeclaringClass());
        System.out.println(B.class.getMethod("z").getDeclaringClass());
        System.out.println(B.class.getMethod("l").getDeclaringClass());
        System.out.println(Arrays.asList(B.class.getAnnotatedInterfaces()));*/
    }

}