import core.di.ConfigParser;
import core.di.imp.Parsers;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Created by Максим on 9/19/2017.
 */
public class ConfigParserTest {
    private final File xmlFilePath = new File("D:\\Workspace Intellij Idea\\ScalaFramework\\src\\main\\resources\\GS_SpringXMLConfig.xml");

    @Test
    public void testCreation() {
        ConfigParser parser = Parsers.createXmlParser(xmlFilePath);
        Assert.assertNotNull(parser);
    }

    @Test
    public void testGetBeanList() {
        ConfigParser parser = Parsers.createXmlParser(xmlFilePath);
        Assert.assertNotNull(parser.parse());
    }

    @Test
    public void testToString() {
        ConfigParser parser = Parsers.createXmlParser(xmlFilePath);
        Assert.assertNotNull(parser.toString());
    }
}
