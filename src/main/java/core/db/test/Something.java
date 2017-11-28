package core.db.test;

import core.db.annotation.Column;
import core.db.annotation.Id;
import core.db.annotation.Table;

@Table(tableName = "something")
public class Something {

    @Id(autoincrement = true, name = "id")
    private long id;

    @Column(name = "message", nullable = true, generatedId = true)
    private String message;

}
