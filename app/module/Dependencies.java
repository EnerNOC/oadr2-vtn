package module;

import com.google.inject.Provides;
import javax.inject.Singleton;

import play.Logger;
import service.*;

public class Dependencies {
  
    @Provides
    @Singleton
    public XmppService makeXmppService(){
        Logger.info("In make XmppService");
        return new XmppService();
    }
    
    @Provides
    @Singleton
    public EiEventService makeEiEventService(){
        return new EiEventService();
    }
  
}