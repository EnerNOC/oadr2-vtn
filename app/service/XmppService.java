package service;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Result;
import protocol.XMPPService;
import tasks.EventPushTask;
import test.*;


import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeFactory;

import models.VEN;

import org.enernoc.open.oadr2.model.EiCreatedEvent;
import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiRequestEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.EventResponses;
import org.enernoc.open.oadr2.model.EventResponses.EventResponse;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent.OadrEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;
import org.enernoc.open.oadr2.model.OptTypeType;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;

public class XmppService {

    private static volatile XmppService instance = null;
    
    static final String OADR2_XMLNS = "http://openadr.org/oadr-2.0a/2012/03";
        
    private ConnectionConfiguration connConfig = new ConnectionConfiguration("msawant-mbp.local", 5222);
    
    private static XMPPConnection vtnConnection;
    
    //@Inject static PushService pushService;
    static PushService pushService = new PushService();  
    
    //TODO add these to a config file like spring config or something, hardcoded for now
    private String vtnUsername = "xmpp-vtn";
    private String vtnPassword = "xmpp-pass";
    
    static Marshaller marshaller;
    DatatypeFactory xmlDataTypeFac;
    
    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();
        
    public XmppService() throws XMPPException, InstantiationException, IllegalAccessException, JAXBException{
        //Add for debugging
        //
        //Connection.DEBUG_ENABLED = true;
        if(vtnConnection == null){
            vtnConnection = connect(vtnUsername, vtnPassword, "vtn");
        }
        
        //testConnection = connect("test", "xmpp-pass", "vtn");        

        JAXBManager jaxb = new JAXBManager();
        marshaller = jaxb.createMarshaller();        
    }
    
    public static XmppService getInstance(){
        if(instance == null){
            synchronized(XMPPService.class){
                if(instance == null){
                    try {
                        instance = new XmppService();
                    } catch (XMPPException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (JAXBException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        return instance;
    }//
    
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
                Object packetObject = null;
                try {
                    packetObject = EiEventService.unmarshalRequest(extension.toXML().getBytes());
                } catch (JAXBException e) {}
                if(packetObject instanceof OadrRequestEvent || packetObject instanceof OadrCreatedEvent){
                    if(packetObject instanceof OadrRequestEvent){
                        OadrRequestEvent requestEvent = (OadrRequestEvent)packetObject;
                        EiEventService.persistFromRequestEvent(requestEvent);
                        try {
                            sendXMPPDistribute(requestEvent);
                        } catch (JAXBException e) {Logger.info("JAXBException from sendXMPPDistribute");
                        }
                    }
                    else if(packetObject instanceof OadrCreatedEvent){
                        OadrCreatedEvent createdEvent = (OadrCreatedEvent)packetObject;
                        EiEventService.persistFromCreatedEvent(createdEvent);
                        try {
                            sendXMPPResponse(createdEvent);
                        } catch (JAXBException e) {Logger.info("JAXBException from sendXMPPResponse");
                        }
                    }
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
    
    @Transactional
    public void sendXMPPDistribute(OadrRequestEvent request) throws JAXBException{
        createNewEm();
        
        String eventId = (String)entityManager.createQuery("SELECT s.eventID FROM StatusObject s WHERE s.venID = :ven")
            .setParameter("ven", request.getEiRequestEvent().getVenID())
            .getSingleResult();
                
        EiEvent event = (EiEvent)entityManager.createQuery("SELECT event FROM EiEvent event, EiEvent$EventDescriptor " +
                "descriptor WHERE descriptor.eventID = :id and event.hjid = descriptor.hjid")
                .setParameter("id", eventId)
                .getSingleResult();
        
        OadrDistributeEvent distributeEvent = new OadrDistributeEvent().withOadrEvent(new OadrEvent().withEiEvent(event))
                .withEiResponse(new EiResponse().withResponseCode("200"));
        
        StringWriter out = new StringWriter();
        marshaller.marshal(distributeEvent, out);
        Logger.info(out.toString());
        
        OADR2IQ iq = new OADR2IQ(new OADR2PacketExtension(distributeEvent, marshaller));
        //TODO Need to find the actual user from the query for who the Customer is, Customer.getJID etc..
        //iq.setTo(jid);
        iq.setTo("xmpp-ven@msawant-mbp.local/msawant-mbp");
        iq.setType(IQ.Type.SET);
        vtnConnection.sendPacket(iq);
        
    }
    
    public void sendXMPPResponse(OadrCreatedEvent createdEvent) throws JAXBException{
        OadrResponse response = new OadrResponse();
        response.withEiResponse(new EiResponse().withRequestID(createdEvent.getEiCreatedEvent().getEiResponse().getRequestID())
                .withResponseCode("200"));
        StringWriter out = new StringWriter();
        marshaller.marshal(response, out);
        
        IQ iq = new OADR2IQ(new OADR2PacketExtension(response, marshaller));
        //TODO Need to find the actual user from the query for who the Customer is, Customer.getJID etc..
        iq.setTo("xmpp-ven@msawant-mbp.local/msawant-mbp");
        iq.setType(IQ.Type.SET);
        vtnConnection.sendPacket(iq);
        
    }
        
    public static void createNewEm(){
        entityManager = entityManagerFactory.createEntityManager();
        if(!entityManager.getTransaction().isActive()){
            entityManager.getTransaction().begin();
        }
    }
    
    @SuppressWarnings("unchecked")
    @Transactional
    public static void sendEventOnCreate(EiEvent e) throws JAXBException{
        createNewEm();
        
        List<VEN> customers = entityManager.createQuery("SELECT c from Customers c WHERE c.programId = :uri")
                .setParameter("uri", e.getEventDescriptor().getEiMarketContext().getMarketContext())
                .getResultList();
        
        for(VEN customer: customers){
            OadrDistributeEvent distribute = new OadrDistributeEvent()
            .withOadrEvent(new OadrEvent().withEiEvent(e))
            .withVtnID(vtnConnection.getUser());
            IQ iq = new OADR2IQ(new OADR2PacketExtension(distribute, marshaller));
            for(PacketExtension p : iq.getExtensions()){
                Logger.info("Namespace: " + p.getNamespace());
            }
            Logger.info("Customer Client URI: " + customer.getClientURI());
            //iq.setTo(customer.getClientURI());
            iq.setTo("xmpp-ven@msawant-mbp.local/msawant-mbp");
            iq.setType(IQ.Type.SET);
            vtnConnection.sendPacket(iq); //throws a null pointer exception, check if vtn is connected or not kthxbai            
        }        
    }
    
    public void sendObjectToJID(Object o, String jid){
        IQ iq = new OADR2IQ(new OADR2PacketExtension(o, marshaller));
        for(PacketExtension p : iq.getExtensions()){
            Logger.info("Namespace: " + p.getNamespace());
        }
        Logger.info("Sending to: " + jid);
        iq.setTo(jid);
        iq.setType(IQ.Type.SET);
        vtnConnection.sendPacket(iq);
    }
    
    @SuppressWarnings("unchecked")
    @Transactional
    public static void populateThreadPool(EiEvent e) throws JAXBException{
        createNewEm();
        
        List<VEN> customers = entityManager.createQuery("SELECT c FROM Customers c WHERE c.programId = :uri and c.clientURI != ''")
                .setParameter("uri", e.getEventDescriptor().getEiMarketContext().getMarketContext())
                .getResultList();
        
        Logger.info("customers: " + customers.size());
        //for(VEN c : customers){
        for(int i = 0; i < customers.size(); i++){
            OadrDistributeEvent distribute = new OadrDistributeEvent()
            .withOadrEvent(new OadrEvent().withEiEvent(e))
            .withVtnID(vtnConnection.getUser());
            
            //TODO pushService.provide(new EventPushTask(customer.getClientURI(), distribute));
            distribute.setEiResponse(new EiResponse().withRequestID("Request ID")
                    .withResponseCode("200")
                    .withResponseDescription("Response Description"));
            distribute.getOadrEvent().add(new OadrEvent().withEiEvent(e));
            distribute.setRequestID("Request ID");
            distribute.setVtnID("VTN ID");
            pushService.provide(new EventPushTask(customers.get(i).getClientURI(), distribute));     
        }
        pushService.executeTask();
    }
    
}
