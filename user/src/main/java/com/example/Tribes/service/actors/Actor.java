package com.example.Tribes.service.actors;

import akka.actor.AbstractActor;
import com.example.Tribes.Repo.Constants;
import com.example.Tribes.TribesApplication;
import service.messages.*;

public class Actor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(HeartBeat.class,
                msg -> {
//                    System.out.println("Pulse received!");
                    getSender().tell(new HeartBeat("Persistance Module"), null);
                })
            .match(UserCreationRequest.class,
            msg -> {
                if(Constants.configurableApplicationContext == null) {
                    System.out.println("Inside If...");
                    TribesApplication.main(new String[0]);
                }
                TribesApplication.setUserInfo(msg.getUniqueId(),msg.getNewUser(),msg.getTribeLanguage());
                System.out.println("Send user creation response to the triber system..For Unique ID: "+msg.getUniqueId()+" Port: "
                +msg.getNewUser().getPortNumber()+" Tribe Language="+msg.getTribeLanguage());

                UserCreationResponse userCreationResponse = new UserCreationResponse();
                userCreationResponse.setUniqueId(msg.getUniqueId());
                userCreationResponse.setTribeId(msg.getNewUser().getTribeId());
                getSender().tell(userCreationResponse,self());
            })
            .match(String.class,
            msg -> {

                if(Constants.configurableApplicationContext == null) {
                    TribesApplication.main(new String[0]);
                }
                if(msg.equals("InitializeTriberSystem")){
                    getSender().tell(TribesApplication.getAllUserInfo(),self());
                }
            }).build();
    }
}