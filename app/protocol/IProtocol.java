package protocol;

import javax.persistence.Embeddable;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;

import models.VTN;

@Embeddable
public interface IProtocol {
    
    public enum ProtocolType{ XMPP, HTTP }
    
    public void send(VTN vtn, OadrResponse oadrResponse);
    public void send(VTN vtn, EiEvent eiEvent);
    public void send(VTN vtn, OadrDistributeEvent oadrDistributeEvent);
    public void send(VTN vtn, OadrCreatedEvent oadrCreatedEvent);
    public void send(VTN vtn, OadrRequestEvent oadrRequestEvent);

}