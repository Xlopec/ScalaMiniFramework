package core.app.classes;

public class Car implements Transport {

    private String name;
    private Integer wheelCount;

    public Car(String name, int wheelCount) {
        this.name = name;
        this.wheelCount = wheelCount;
    }

    public Car(String name, String wheelCount) {
        this.name = name;
        this.wheelCount = Integer.decode(wheelCount);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getWheelCount() {
        return wheelCount;
    }

    public void setWheelCount(Integer wheelCount) {
        this.wheelCount = wheelCount;
    }    

    public String toString() {
        StringBuffer res = new StringBuffer();
        res.append(name + ": ");
        res.append(wheelCount);
        return res.toString();
    }

    public void getTransport() {
        // TODO Auto-generated method stub
    }
}
