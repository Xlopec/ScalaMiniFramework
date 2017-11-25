package core.db.test;

import core.db.annotation.Entity;
import core.db.annotation.Id;
import core.db.annotation.Property;

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

}
