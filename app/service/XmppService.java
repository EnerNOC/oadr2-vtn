package service;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeFactory;

import jaxb.JAXBManager;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

import play.Logger;
import play.db.jpa.Transactional;
import service.oadr.EiEventService;
import xmpp.OADR2IQ;
import xmpp.OADR2PacketExtension;

import com.google.inject.Inject;

public class XmppService {

    private static volatile XmppService instance = null;
        
    static final String OADR2_XMLNS = "http://openadr.org/oadr-2.0a/2012/07";
    
    private ConnectionConfiguration connConfig = new ConnectionConfiguration("msawant-mbp.local", 5222);
    
    private static XMPPConnection vtnConnection;
    
    //@Inject static PushService pushService;
    @Inject static PushService pushService;// = new PushService();
    @Inject static EiEventService eventService;// = new EiEventService();
    
    //TODO add these to a config file like spring config or something, hardcoded for now
    private String vtnUsername = "xmpp-vtn";
    private String vtnPassword = "xmpp-pass";
    
    private Marshaller marshaller;
    DatatypeFactory xmlDataTypeFac;
    
    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();
        
    public XmppService() throws XMPPException, InstantiationException, IllegalAccessException, JAXBException{
        //Add for debugging
        //Connection.DEBUG_ENABLED = true;
        if(vtnConnection == null){
            vtnConnection = connect(vtnUsername, vtnPassword, "vtn");
        }
        
        JAXBManager jaxb = new JAXBManager();
        marshaller = jaxb.createMarshaller();
    }
    
    public static XmppService getInstance(){
        if(instance == null){
            synchronized(XmppService.class){
                if(instance == null){
                    try {
                        instance = new XmppService();
                    } catch (XMPPException e) {
                        Logger.error("XMPPException creating XMPPService.", e);
                    } catch (InstantiationException e) {
                        Logger.error("InstantiationException creating XMPPService.", e);
                    } catch (IllegalAccessException e) {
                        Logger.error("IllegalAccessException creating XMPPService.", e);
                    } catch (JAXBException e) {
                        Logger.error("JAXBException creating XMPPService.", e);
                    }
                }
            }
        }
        return instance;
    }
    
    public XmppService(String username, String password){
        this.vtnUsername = username;
        this.vtnPassword = password;
    }
   
    @Transactional
    public PacketListener oadrPacketListener(){
        return new PacketListener(){
            @Override
            @Transactional
            public void processPacket(Packet packet){
                OADR2PacketExtension extension = (OADR2PacketExtension)packet.getExtension(OADR2_XMLNS);
                Object payload = eventService.handleOadrPayload(extension.getPayload());
                if(payload != null){
                    sendObjectToJID(payload, packet.getFrom());
                }
            }         
        };
    }
    
    public PacketFilter oadrPacketFilter(){
        return new PacketFilter(){
            @Override
            public boolean accept(Packet packet){                
                return packet.getExtension(OADR2_XMLNS) != null;
            }
        };
    }
    
    public XMPPConnection connect(String username, String password, String resource) throws InstantiationException, IllegalAccessException, XMPPException{
       XMPPConnection connection = new XMPPConnection(connConfig);
       if(!connection.isConnected()){
           connection.connect();
           if(connection.getUser() == null && !connection.isAuthenticated()){
               connection.login(username, password, resource);
               connection.addPacketListener(oadrPacketListener(), oadrPacketFilter());
           }
       }
       return connection;
    }        
    
    public void sendObjectToJID(Object o, String jid){
        Logger.info("Sending object to - " + jid);
        IQ iq = new OADR2IQ(new OADR2PacketExtension(o, marshaller));
        iq.setTo(jid);
        iq.setType(IQ.Type.SET);
        vtnConnection.sendPacket(iq);
    }
    
    public void sendObjectToJID(Object o, String jid, String packetId){
        Logger.info("Sending object to - " + jid);
        IQ iq = new OADR2IQ(new OADR2PacketExtension(o, marshaller));
        iq.setTo(jid);
        iq.setPacketID(packetId);
        iq.setType(IQ.Type.RESULT);
        vtnConnection.sendPacket(iq);
    }
    
}
