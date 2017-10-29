package core.app.bean;

import core.di.BeanContext;
import core.di.BeanFactory;
import core.di.imp.XmlBeanContext;

import java.io.File;

public final class Client {

    public static void main(String... args) {
        final BeanContext context = new XmlBeanContext(new File("/Users/max/IdeaProjects/ScalaMiniFramework/src/main/resources/BeanConfig.xml"));
        final BeanFactory factory = context.getBeanFactory();
        final StatelessBean statelessBean = factory.instantiate(StatelessBean.class);

        System.out.println(String.format("Stateless bean %s", statelessBean.getEchoString("Hello Max")));

        final StatefulBean statefullBean = factory.instantiate(StatefulBean.class);

        statefullBean.calculate("+", 10);
        statefullBean.calculate("-", 4);

        System.out.println(String.format("Statefull bean %s", statefullBean.getValue()));

        final MessageBean messageBean = factory.instantiate(MessageBean.class);


    }

}
