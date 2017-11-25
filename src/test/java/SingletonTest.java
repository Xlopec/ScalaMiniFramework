import core.app.classes.beans.ScannableBean;
import core.app.classes.service.GreetingService;
import core.di.BeanContext;
import core.di.BeanFactory;
import core.di.imp.XmlBeanContext;
import org.junit.Assert;
import org.junit.Test;

public class SingletonTest {

    @Test
    public void testSingleton() {

        final BeanContext context = new XmlBeanContext(AllTests.TEST_CONFIG_FILE);
        final BeanFactory factory = context.getBeanFactory();
        final GreetingService greetingService1 = factory.instantiate("greetingService");
        final GreetingService greetingService2 = factory.instantiate("greetingService");

        Assert.assertTrue(greetingService1 == greetingService2);
    }

    @Test
    public void testPrototype() {
        final BeanContext context = new XmlBeanContext(AllTests.TEST_CONFIG_FILE);
        final BeanFactory factory = context.getBeanFactory();
        final ScannableBean scannableBean1 = factory.instantiate("MyScannableBean");
        final ScannableBean scannableBean2 = factory.instantiate("MyScannableBean");

        Assert.assertFalse(scannableBean1 == scannableBean2);
    }

}
