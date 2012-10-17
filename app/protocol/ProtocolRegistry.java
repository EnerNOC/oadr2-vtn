package protocol;

import java.util.HashMap;
import java.util.Map;

import play.Logger;

public class ProtocolRegistry {
    
    private Map<String, IProtocol> registry = new HashMap<String, IProtocol>();
    private static volatile ProtocolRegistry instance;

    public static ProtocolRegistry getInstance(){
        if(instance == null){
            synchronized(ProtocolRegistry.class){
                if(instance == null){
                    instance = new ProtocolRegistry();                    
                }
            }
        }
        return instance;
    }
    
    public IProtocol getProtocol(String uri){
        if(!registry.containsKey(uri)){
            addProtocol(uri);
        }
        return registry.get(uri);
    }
    
    public void addProtocol(String uri){
        if(uri.length() > 0){
            if(uri.contains("http")){
                registry.put(uri, new HTTPProtocol());
            }
            else{
                Logger.info("URI is: " + uri);
                registry.put(uri, new XMPPProtocol());
            }
        }
    }
}
