package protocol;

import javax.xml.bind.JAXBException;

import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

import play.Logger;

import service.EiEventService;
import xmpp.OADR2PacketExtension;

public class XMPPService implements ConnectionListener, PacketListener, PacketFilter{
    
    private static volatile XMPPService instance = null;
    
    static final String OADR2_XMLNS = "http://openadr.org/oadr-2.0a/2012/03";
    private ConnectionConfiguration connConfig = new ConnectionConfiguration("msawant-mbp.local", 5222);

    static final String username = System.getProperty("xmpp-username");
    static final String password = System.getProperty("xmpp-pass");
    
    private XMPPService(){
        
    }
    
    public static XMPPService getInstance(){
        if(instance == null){
            synchronized(XMPPService.class){
                if(instance == null){
                    instance = new XMPPService();
                }
            }
        }
        return instance;
    }

    @Override
    public boolean accept(Packet packet) {
        return packet.getExtension(OADR2_XMLNS) != null;
    }

    @Override
    public void processPacket(Packet packet) {
        OADR2PacketExtension extension = (OADR2PacketExtension)packet.getExtension(OADR2_XMLNS);
        Object packetObject = null;
        try {
            packetObject = EiEventService.unmarshalRequest(extension.toXML().getBytes());
        } catch (JAXBException e) {Logger.warn("JAXBException caught in processPacket");}
        if(packetObject instanceof OadrRequestEvent){
            OadrRequestEvent requestEvent = (OadrRequestEvent)packetObject;
            EiEventService.persistFromRequestEvent(requestEvent);
            /*
            try {
                sendXMPPDistribute(requestEvent);
            } catch (JAXBException e) {Logger.warn("JAXBException from sendXMPPDistribute");
            }
            */
        }
        else if(packetObject instanceof OadrCreatedEvent){
            OadrCreatedEvent createdEvent = (OadrCreatedEvent)packetObject;
            EiEventService.persistFromCreatedEvent(createdEvent);
            /*
            try {
                sendXMPPResponse(createdEvent);
            } catch (JAXBException e) {Logger.warn("JAXBException from sendXMPPResponse");
            }
            */
        }
    }

    @Override
    public void connectionClosed() {
        Logger.info("Connection closed");
        XMPPConnection c = new XMPPConnection(connConfig);
        try {
            c.connect();
        } catch (XMPPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    @Override
    public void connectionClosedOnError(Exception arg0) {
        Logger.info("Connection closed on error");
        
    }

    @Override
    public void reconnectingIn(int arg0) {
        Logger.info("Reconnecting in:");
        
    }

    @Override
    public void reconnectionFailed(Exception arg0) {
        Logger.info("Reconnection failed");
    }
    
    @Override
    public void reconnectionSuccessful() {
        Logger.info("Reconnection successful");
        
    }
    
    public void connect() throws XMPPException{
        XMPPConnection c = new XMPPConnection(connConfig);
        c.connect();
        c.login(username,  password, "vtn");
    }

}