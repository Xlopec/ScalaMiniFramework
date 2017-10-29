package core.app.bean;

import core.di.annotation.Component;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Component
public class MessageBean implements MessageListener {

    @Override
    public void onMessage(Message inMessage) {
        TextMessage msg = null;

        try {
            if (inMessage instanceof TextMessage) {
                msg = (TextMessage) inMessage;
                System.out.println("MESSAGE BEAN: Message received: " +
                        msg.getText());
            } else {
                System.err.println("Message of wrong type: " +
                        inMessage.getClass().getName());
            }
        } catch (Exception te) {
            te.printStackTrace();
        }
    }

}
