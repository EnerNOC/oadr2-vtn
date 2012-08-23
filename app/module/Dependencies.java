package module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import javax.xml.bind.JAXBException;

import org.jivesoftware.smack.XMPPException;

import play.Logger;
import service.*;

public class Dependencies {
  
    @Provides
    @Singleton
    public XmppService makeXmppService() throws XMPPException, InstantiationException, IllegalAccessException, JAXBException{
        return new XmppService();
    }
    
    @Provides
    @Singleton
    public EiEventService makeEiEventService(){
        return new EiEventService();
    }
  
}