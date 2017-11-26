package core.db.test;

import core.db.annotation.*;

import java.util.List;

@Entity(table = "Student")
public final class Student {

    @Id(autoincrement = true)
    private long id;

    @Property(name = "first_name")
    private String firstName;

    @Property(name = "last_name")
    private String lastName;

    @Property(name = "year_of_study")
    private int yearOfStudy;

    @ToOne(joinedEntity = Supervisor.class)// todo remove useless type hint
    private Supervisor supervisor;

    @ToMany(joinedEntity = Lesson.class)// todo extract generic type instead
    private List<Lesson> lessons;

    @JoinEntity(joiningEntity = JoinTable.class, sourceProperty = "STUDENT_ID", targetProperty = "SOMETHING_ID")
    private List<Something> somethings;

}
