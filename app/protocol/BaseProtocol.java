package protocol;

import javax.persistence.Embeddable;


@Embeddable
public abstract class BaseProtocol implements IProtocol{  
    
    private ProtocolType protocolType;

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }
    
}