package service.actor;

import akka.actor.*;
import service.centralCore.*;
import service.messages.*;
//import service.tribersystem.TriberSystem;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Triber extends AbstractActor {
    private static ActorSystem system;
    //private static TriberSystem triberSystem;
    private static long userUniqueId = 1;
    private static long tribeUniqueId = 1;
    private static ActorSelection interestsActor, persistanceActor;

    HashMap<Long, UserInfo> requestsToUserInfoMap = new HashMap<>();
    HashMap<Long, Long> uniqueIdMap = new HashMap<>();
    ArrayList<Tribe> allTribes = new ArrayList<>();
    ArrayList<UserInfo> allUserInfo = new ArrayList<>();

    public static void main(String[] args){
        System.out.println("1) Test here");
        system = ActorSystem.create();
        ActorRef ref = system.actorOf(Props.create(Triber.class), "triber");
        interestsActor = system.actorSelection("akka.tcp://default@127.0.0.1:2554/user/interests");
        persistanceActor = system.actorSelection("akka.tcp://default@127.0.0.1:2552/user/userSystem");

        System.out.println("2) Test here");
        persistanceActor.tell("InitializeTriberSystem", ref);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(TriberInitializationResponse.class,
                msg -> {
                    allUserInfo = msg.getAllUsers();
                    allTribes = msg.getAllTribes();
                    userUniqueId = msg.getMaxUserId();
                    tribeUniqueId = msg.getMaxTribeId();
                })
            .match(UserRequest.class,
                msg -> {
                    System.out.println("User creation request received for : " + msg.getNewUser().getName() + " with Unique ID: " + msg.getUniqueId());
                    if(validateInputRequest(msg)) {
                        //Adding new User
                        long currUserUniqueId = ++userUniqueId;
                        uniqueIdMap.put(currUserUniqueId, msg.getUniqueId());
                        requestsToUserInfoMap.put(currUserUniqueId, msg.getNewUser());
                        //Interests request sent to Interests System
                        interestsActor.tell(new InterestsRequest(currUserUniqueId, msg.getNewUser().getGitHubId()), getSelf());
                    }else
                    {
                        UserResponse userResponse = new UserResponse(msg.getUniqueId(),"Github ID already registered, Please use a different GitHub Id");
                        ActorSelection clientActor = system.actorSelection("akka.tcp://default@127.0.0.1:"+msg.getNewUser().getPortNumber()+"/user/"+msg.getUniqueId());
                        clientActor.tell(userResponse, null);
                    }
                    })
            .match(InterestsResponse.class,
                msg -> {
                    System.out.println("Received Interests response from Interests System for User :"+requestsToUserInfoMap.get(msg.getUniqueId()).getName()+" Unique ID:"+ msg.getUniqueId());
                    requestsToUserInfoMap.get(msg.getUniqueId()).setInterests(msg.getInterest());

                    TribeSuggestionRequest tribeSuggestionRequest = new TribeSuggestionRequest();
                    //Fetch tribe suggestion for the user based on his interest
                    Set<Tribe> suggestedTribe = getTribeSuggestions(msg.getInterest());

                    if(suggestedTribe.size() == 0){
                        System.out.println("New Tribe Creation as no tribe for the user interests exists");
                        String tribeLanguage = "";
                        tribeLanguage = msg.getInterest().getProgrammingLanguages().stream().findFirst().get();
                        long tribeID = ++tribeUniqueId;
                        requestsToUserInfoMap.get(msg.getUniqueId()).setTribeId(tribeID);
                        UserCreationRequest userCreationRequest = new UserCreationRequest(msg.getUniqueId(),requestsToUserInfoMap.get(msg.getUniqueId()),tribeLanguage);
                        persistanceActor.tell(userCreationRequest, getSelf());
                    }
                    else if(suggestedTribe.size() == 1){
                        System.out.println("One member tribe exists for the user interests");
                        Tribe tribe = suggestedTribe.stream().findFirst().get();
                        UserInfo currUser = requestsToUserInfoMap.get(msg.getUniqueId());
                        String tribeLanguage = getTribeName(tribe);
                        currUser.setTribeId(tribe.getTribeId());
                        requestsToUserInfoMap.get(msg.getUniqueId()).setTribeId(tribe.getTribeId());
                        UserCreationRequest UCR = new UserCreationRequest(msg.getUniqueId(), currUser,tribeLanguage);
                        persistanceActor.tell(UCR, getSelf());
                    }
                    else{
                        System.out.println("User must select the tribe he wishes to join");
                        UserInfo userInfo = requestsToUserInfoMap.get(msg.getUniqueId());
                        ActorSelection clientActor = system.actorSelection("akka.tcp://default@127.0.0.1:" + userInfo.getPortNumber() + "/user/" + uniqueIdMap.get(msg.getUniqueId()));
                        tribeSuggestionRequest.setUniqueId(msg.getUniqueId());
                        tribeSuggestionRequest.setSuggestedTribes(suggestedTribe);
                        //Prompting the client to make tribe selection
                        clientActor.tell(tribeSuggestionRequest, null);
                    }
                })
            .match(TribeDetailRequest.class,
                msg -> {
                    Tribe tribe = getTribeById(msg.getTribeId());
                    getSender().tell(new TribeDetailResponse(msg.getUniqueId(), tribe), null);
                })
                .match(TribeSuggestionResponse.class,
                        msg->{
                            requestsToUserInfoMap.get(msg.getUniqueId()).setTribeId(msg.getTribeId());
                            UserCreationRequest userCreationRequest = new UserCreationRequest(msg.getUniqueId(),requestsToUserInfoMap.get(msg.getUniqueId()),getTribeName(getTribeById(msg.getTribeId())));
                            persistanceActor.tell(userCreationRequest,getSelf());
                        })
                .match(UserCreationResponse.class,
                        msg->{
                            System.out.println("You are successfully registered in the System");
                            long clientID = uniqueIdMap.get(msg.getUniqueId());
                            UserInfo userInfo = requestsToUserInfoMap.get(msg.getUniqueId());
                            ActorSelection clientActor = system.actorSelection("akka.tcp://default@127.0.0.1:"+userInfo.getPortNumber()+"/user/"+clientID);
                            persistanceActor.tell("InitializeTriberSystem", getSelf());

                            clientActor.tell(msg,null);
                        })
                .match(Long.class,msg->{
                    System.out.println(msg);
                }).build();
    }

    private boolean validateInputRequest(UserRequest userRequest){
        if(allUserInfo!= null && allUserInfo.size()>0 && allUserInfo.stream().filter(user->user.getGitHubId()
                .equalsIgnoreCase(userRequest.getNewUser().getGitHubId()))
                .collect(Collectors.toList()).size() > 0)
            return false;
        else
            return true;
    }
    private String getTribeName(Tribe tribe) {
        return allTribes.stream()
                .filter(t->t.getTribeId() == tribe.getTribeId())
                .collect(Collectors.toList()).get(0).getTribeName();
    }

    private Set<Tribe> getTribeSuggestions(Interests interests){
        Set<Tribe> filteredTribes = new HashSet<>();

        for(Tribe existingTribe:allTribes){

            Set<String> temp
                    = Stream.of(existingTribe.getTribeLanguages().trim().split("\\s*,\\s*"))
                    .collect(Collectors.toSet());
            if(containsMatch(interests.getProgrammingLanguages(),temp)){
                filteredTribes.add(existingTribe);
            }

        }

        return filteredTribes;
    }

    private Tribe getTribeById(long tribeId){
        return allTribes.stream().filter(x->x.getTribeId() == tribeId).collect(Collectors.toList()).get(0);
    }

    private boolean containsMatch(Set<String> a, Set<String> b){

        Set<String> intersectSet = a.stream()
                .filter(b::contains)
                .collect(Collectors.toSet());
        if(intersectSet.size()>0){
            return true;
        }
        else {
            return false;
        }

    }
}
