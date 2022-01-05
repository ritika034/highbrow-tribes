package service.actor;

import akka.actor.*;
import service.centralCore.*;
import service.messages.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Communicator extends AbstractActor {
    private static ActorSystem system;
    public static void main(String [] args){
        System.out.println("1) Test here");
        system = ActorSystem.create();
        system.actorOf(Props.create(Communicator.class), "communicator");
        System.out.println("2) Test here");
    }
    private ActorSelection TriberActor = system.actorSelection("akka.tcp://default@127.0.0.1:2557/user/triber");
    //private HashMap<Long, Tribe> ActiveUsers = new HashMap<>();
    private static HashMap<String, Long> gitHubIdRequestId = new HashMap<>();
    private static HashMap<Long, List<UserInfo>> ActiveUsers = new HashMap<>();
    private static HashMap<Long, Integer> UserPorts = new HashMap<>();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class,
                        msg -> {
                            System.out.println("test initializaogoe" + msg.toLowerCase(Locale.ROOT));
//                            MatcherActor.tell(new TribeDetailRequest(msg.getUniqueId()), null);
                        })
                .match(ChatRegisterRequest.class,
                        msg -> {
                            long tribeId = msg.getUserInfo().getTribeId();
                            long uniqueId = msg.getUniqueId();
                            System.out.println("Chat register request received from User : "+msg.getUserInfo().getName()+" Unique ID: " + msg.getUniqueId());
                            gitHubIdRequestId.put(msg.getUserInfo().getGitHubId(),uniqueId);
                            ActiveUsers.put(tribeId,null);
                            UserPorts.put(uniqueId, msg.getUserInfo().getPortNumber());
                            TriberActor.tell(new TribeDetailRequest(uniqueId,tribeId), getSelf());
                        })
                .match(TribeDetailResponse.class,
                        msg -> {
                            ActiveUsers.put(msg.getTribe().getTribeId(),msg.getTribe().getMembers());
                            int PortNumber = UserPorts.get(msg.getUniqueId());
                            ActorSelection clientActor = system.actorSelection("akka.tcp://default@127.0.0.1:" + PortNumber + "/user/" + msg.getUniqueId());
                            clientActor.tell(new ChatRegisterResponse(msg.getTribe()), null);
                        })
                .match(ChatMessageSend.class,
                        msg -> {
                            ActorSelection selection;
                            for(UserInfo user : ActiveUsers.get(msg.getTribeId())){
                                long uniqueId = gitHubIdRequestId.get(user.getGitHubId());
                                if( uniqueId != msg.getUniqueId()) {
                                    System.out.println("PortNumber : " + user.getPortNumber() +" Unique Id : "+ uniqueId);
                                    selection = system.actorSelection("akka.tcp://default@127.0.0.1:" + user.getPortNumber() + "/user/" + uniqueId);
                                    selection.tell(new ChatMessageReceive(msg.getSenderName(), msg.getSentTime(), msg.getUniqueId(), msg.getMessage()), null);
                                }
                            }
                        })
                .match(ChatMessageReceive.class,
                        msg -> {
//                            ActiveUsers.put(msg.getUniqueId(), msg.getTribe());
                        }).build();
    }
}
