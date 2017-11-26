package core.db.test;

import core.db.annotation.Entity;
import core.db.annotation.Id;
import core.db.annotation.ToOne;

@Entity(table = "student_to_something")
public class JoinTable {

    @Id(name = "id", autoincrement = true)
    private long id;

    @ToOne(joinedEntity = Student.class)
    private Student student_Id;

    @ToOne(joinedEntity = Something.class)
    private Something something_Id;

}
