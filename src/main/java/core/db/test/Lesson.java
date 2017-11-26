package core.db.test;

import core.db.annotation.Entity;
import core.db.annotation.Id;
import core.db.annotation.Property;
import core.db.annotation.ToOne;

@Entity(table = "lesson")
public class Lesson {

    @Id(autoincrement = true, name = "id")
    private long id;

    @Property(name = "title")
    private String title;

    @Property(name = "description")
    private String description;

    @ToOne(joinedEntity = Student.class)
    private Student student;

}
