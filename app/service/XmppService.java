package service;

import play.Logger;
import play.mvc.Result;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

public class XmppService {

    static final String OADR2_XMLNS = "http://openadr.org/oadr-2.0a/2012/03";
        
    private ConnectionConfiguration connConfig = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
    private String username = System.getProperty("xmpp-username");
    private String password = System.getProperty("xmpp-pass");
        
    public XmppService(){
        Logger.info("Built XmppService");
    }
    
    public XmppService(String username, String password){
        this.username = username;
        this.password = password;
    }
        
    public Result test(){
        Logger.info("In Xmpp Test");
        return play.mvc.Action.ok("Test");
    }
    
    public PacketListener oadrPacketListener(){
        return new PacketListener(){
            @Override
            public void processPacket(Packet packet){
                Logger.info("Got packet.");
                Logger.info(packet.toString());
            }
        };
    }
    
    public PacketFilter oadrPacketFilter(){
        return new PacketFilter(){
            @Override
            public boolean accept(Packet packet){
                Logger.info("Accepted a packet.");
                return packet.getExtension(OADR2_XMLNS) != null;
            }
        };
    }
        
    public void receiveXmppMessage() throws InstantiationException, IllegalAccessException, XMPPException{
        XMPPConnection connection = connect("vtn");
        connection.addPacketListener(oadrPacketListener(), oadrPacketFilter());
    }

    public XMPPConnection connect(String resource) throws InstantiationException, IllegalAccessException, XMPPException{
       XMPPConnection connection = new XMPPConnection(connConfig);
       connection.connect();
       connection.login(username, password, resource);
      
       
       return connection;
    }

}
