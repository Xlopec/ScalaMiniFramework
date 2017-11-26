package core.db.test;

import core.db.annotation.Entity;
import core.db.annotation.Id;
import core.db.annotation.Property;

@Entity(table = "something")
public class Something {

    @Id(autoincrement = true, name = "id")
    private long id;

    @Property(name = "message", nullable = true)
    private String message;

}
