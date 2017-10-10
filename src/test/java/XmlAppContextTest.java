import core.app.classes.Bus;
import core.app.classes.InterfaceInjectable;
import core.di.BeanContext;
import core.di.imp.Parsers;
import core.di.imp.XmlBeanContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.rules.ExpectedException;

import java.io.File;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;


public class XmlAppContextTest {

    private final File xmlFilePath = new File("D:\\Workspace Intellij Idea\\ScalaFramework\\src\\main\\resources\\GS_SpringXMLConfig.xml");

    @Rule
    public final ExpectedException rule = ExpectedException.none();

    @Test
    public void testCreationFileConstructor() {
        BeanContext tester = new XmlBeanContext(xmlFilePath);
        assertNotNull(tester);
    }

    @Test
    public void testCreationParserConstructor() {
        BeanContext tester = new XmlBeanContext(Parsers.createXmlParser(xmlFilePath));
        assertNotNull(tester);
    }

    @Test
    public void testGetBean() {
        BeanContext tester = new XmlBeanContext(xmlFilePath);
        assertNotNull(tester.getBeanFactory().instantiate("bus"));
    }

    @Test
    public void testGetBeanGeneric() {
        BeanContext tester = new XmlBeanContext(xmlFilePath);
        assertNotNull(tester.getBeanFactory().instantiate(Bus.class));
    }

    @Test
    public void testInterfaceInjection() {
        BeanContext tester = new XmlBeanContext(xmlFilePath);
        InterfaceInjectable injectable = tester.getBeanFactory().instantiate("greetingService");

        assertEquals(injectable.getSomething(), "Whatever");
    }

    @Test
    public void testSetterInjection() {
        BeanContext tester = new XmlBeanContext(xmlFilePath);
        Bus bus = tester.getBeanFactory().instantiate("bus");

        assertEquals(bus.getMessage(), "Transport type is Bus");
    }

    @Test
    public void testCyclicDependency() {
        BeanContext tester = new XmlBeanContext(xmlFilePath);

        rule.expect(IllegalArgumentException.class);
        rule.expectMessage(JUnitMatchers.containsString("requirement failed: Cyclic dependency was found for constructor of bean"));
        tester.getBeanFactory().instantiate("cyclicGreetingService");
    }

}
