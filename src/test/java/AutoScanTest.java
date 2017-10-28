import core.app.classes.beans.ScannableBean;
import core.di.BeanContext;
import core.di.imp.XmlBeanContext;
import org.junit.Assert;
import org.junit.Test;

public class AutoScanTest {

    private final BeanContext context;

    public AutoScanTest() {
        this.context = new XmlBeanContext(AllTests.TEST_CONFIG_FILE);
    }

    @Test
    public void testScanInjection() {
        final ScannableBean bean = context.getBeanFactory().instantiate("MyScannableBean");

        Assert.assertNotNull(bean);
        // Constructors
        Assert.assertNotNull(bean.getConstructorsService());
        Assert.assertNotNull(bean.getConstructorTypedDependency());
        Assert.assertNotNull(bean.getConstructorStringProperty());
        System.out.println(String.format("Constructors out -> %s", bean.toString()));
        // Fields
        Assert.assertNotNull(bean.getFieldService());
        Assert.assertNotNull(bean.getFieldTypedDependency());
        Assert.assertNotNull(bean.getFieldStringProperty());
        System.out.println(String.format("Fields out -> %s", bean.toString()));
    }

}
