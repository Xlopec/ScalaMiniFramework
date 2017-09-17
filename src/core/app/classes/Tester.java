package core.app.classes;

import core.di.BeanContext;
import core.di.BeanFactory;
import core.di.imp.XmlBeanContext;

import java.io.File;
import java.util.RandomAccess;

/**
 * Created by Максим on 9/10/2017.
 */
public class Tester {

    public static void main(String...args) throws NoSuchMethodException {

        BeanContext context = new XmlBeanContext(new File("D:\\Workspace Intellij Idea\\ScalaFramework\\src\\resources\\GS_SpringXMLConfig.xml"));

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

interface A {
    void f();
}

class C {
    public void l(){}
}

class B extends C implements A, RandomAccess {

    @Override
    public void f() {

    }

    public void z() {

    }

    /*@Override
    public void l() {
        super.l();
    }*/
}