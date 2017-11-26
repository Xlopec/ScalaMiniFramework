package core.db.test;

import core.db.annotation.Entity;
import core.db.annotation.Id;
import core.db.annotation.Property;

@Entity(table = "Supervisor")
public final class Supervisor {

    @Id(autoincrement = true)
    private long id;

    @Property(name = "first_name")
    private String firstName;

    @Property(name = "last_name")
    private String lastName;

}
