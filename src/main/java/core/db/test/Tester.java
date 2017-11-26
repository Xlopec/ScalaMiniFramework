package core.db.test;

import core.db.Dao;
import core.db.DaoManager;
import core.di.BeanContext;
import core.di.BeanFactory;
import core.di.imp.XmlBeanContext;

import java.io.File;

public class Tester {

    public static void main(String...args) {

        final BeanContext context = new XmlBeanContext(new File("/Users/max/IdeaProjects/ScalaMiniFramework/src/main/resources/Db_Config.xml"));
        final BeanFactory factory = context.getBeanFactory();
        final DaoManager daoManager = factory.instantiate(DaoManager.class);

        final Dao<Student, Long> dao = daoManager.getDao(Student.class);
        final Student student = dao.read(1L);

        System.out.println(student);
    }

}
