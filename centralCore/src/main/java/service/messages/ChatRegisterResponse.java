package service.messages;

import lombok.Getter;
import lombok.Setter;
import service.centralCore.Tribe;

public class ChatRegisterResponse implements MySerializable {
    @Getter
    @Setter
    private Tribe tribe;

    public ChatRegisterResponse(){}
    public ChatRegisterResponse(Tribe tribe){
        this.tribe = tribe;
    }
}
