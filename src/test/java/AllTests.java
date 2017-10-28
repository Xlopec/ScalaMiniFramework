import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import java.io.File;

@RunWith(Suite.class)
@SuiteClasses({ ConfigParserTest.class, XmlAppContextTest.class })
public class AllTests {

    public static final File TEST_CONFIG_FILE = new File("/Users/max/IdeaProjects/ScalaMiniFramework/src/main/resources/GS_SpringXMLConfig.xml");

}