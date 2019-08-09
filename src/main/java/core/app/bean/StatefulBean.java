package core.app.bean;

import core.di.annotation.Component;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import java.rmi.RemoteException;

@Stateful
@Component
public final class StatefulBean implements Calculator, SessionBean {
    private int _value = 0;

    @Override
    public void clearIt() {
        _value = 0;
    }

    @Override
    public void calculate(String operation, int value) {
        if (operation.equals("+")) {
            _value = _value + value;
            return;
        }
        if (operation.equals("-")) {
            _value = _value - value;
            return;
        }
    }

    @Override
    public int getValue() {
        return _value;
    }

    @Override
    public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {

    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {

    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {

    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {

    }
}
