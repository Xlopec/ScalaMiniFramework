package core.app.bean;

import core.di.annotation.Component;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import java.rmi.RemoteException;

@Stateless
@Component
public final class StatelessBean implements SessionBean {

    public String getEchoString(String clientString) {
        return clientString + " - from session bean";
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
