package service.actor;

import akka.actor.*;
import scala.concurrent.duration.Duration;
import service.centralCore.*;
import service.messages.*;
//import service.tribersystem.TriberSystem;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Triber extends AbstractActor {
    private static ActorSystem system;
    private static long userUniqueId = 1;
    private static long tribeUniqueId = 1;
    private static ActorSelection interestsActor, persistanceActor, communicationActor;
    private static boolean isPersistanceModuleUp = true;
    private static boolean isCommunicatorModuleUp = true;
    private static boolean isRestModuleUp = true;
    private static boolean wasPersistanceModuleDown = false;
    private static boolean wasCommunicatorModuleDown = false;
    private static boolean wasRestModuleDown = false;
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32;9m";

    HashMap<Long, UserInfo> requestsToUserInfoMap = new HashMap<>();
    HashMap<Long, Long> uniqueIdMap = new HashMap<>();
    ArrayList<Tribe> allTribes = new ArrayList<>();
    ArrayList<UserInfo> allUserInfo = new ArrayList<>();

    public static void main(String[] args){
        system = ActorSystem.create();
        ActorRef ref = system.actorOf(Props.create(Triber.class), "triber");
        interestsActor = system.actorSelection("akka.tcp://default@127.0.0.1:2554/user/interests");
        persistanceActor = system.actorSelection("akka.tcp://default@127.0.0.1:2552/user/userSystem");
        communicationActor = system.actorSelection("akka.tcp://default@127.0.0.1:2556/user/communicator");

        persistanceActor.tell("InitializeTriberSystem", ref);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(HeartBeat.class,
                        msg -> {
                            switch (msg.getModule()){
                                case "Rest Module":
//                                    System.out.println("Pulse received from rest service");
                                    if(wasRestModuleDown){
                                        wasRestModuleDown = false;
                                        System.out.println(ANSI_GREEN + "YaYY!! The Interests Module is back up!" + ANSI_RESET);
                                    }
                                    isRestModuleUp = true;
                                    break;
                                case "Persistance Module":
//                                    System.out.println("Pulse received from persistance service");
                                    if(wasPersistanceModuleDown){
                                        wasPersistanceModuleDown = false;
                                        System.out.println(ANSI_GREEN + "YaYY!! The Persistance Module is back up!" + ANSI_RESET);
                                    }
                                    isPersistanceModuleUp = true;
                                    break;
                                case "Communicator Module":
//                                    System.out.println("Pulse received from communicator service");
                                    if(wasCommunicatorModuleDown){
                                        wasCommunicatorModuleDown = false;
                                        System.out.println(ANSI_GREEN + "YaYY!! The Communicator Module is back up!" + ANSI_RESET);
                                    }
                                    isCommunicatorModuleUp = true;
                            }
                        })
            .match(TriberInitializationResponse.class,
                msg -> {
                    allUserInfo = msg.getAllUsers();
                    allTribes = msg.getAllTribes();
                    userUniqueId = msg.getMaxUserId();
                    tribeUniqueId = msg.getMaxTribeId();
                    getContext().system().scheduler().scheduleOnce(
                        Duration.create(5, TimeUnit.SECONDS),
                        getSelf(),
                        "Send Pulse",
                        getContext().dispatcher(), null);
                })
            .match(String.class,
                msg->{
//                    System.out.println("Repeat message received");
                    if(msg.equals("Send Pulse")) {
                        if (isRestModuleUp) {
//                            System.out.println("Pulse sent for rest service");
                            isRestModuleUp = false;
                            interestsActor.tell(new HeartBeat(), getSelf());
                        } else {
                            System.out.println(ANSI_RED + "CRITICAL ERROR: Interests Module Down!" + ANSI_RESET);
                            wasRestModuleDown = true;
                            interestsActor.tell(new HeartBeat(), getSelf());
                        }
                        if (isPersistanceModuleUp) {
//                            System.out.println("Pulse sent for persistance service");
                            isPersistanceModuleUp = false;
                            persistanceActor.tell(new HeartBeat(), getSelf());
                        } else {
                            System.out.println(ANSI_RED + "CRITICAL ERROR: Persistance Module Down!" + ANSI_RESET);
                            persistanceActor.tell("InitializeTriberSystem", getSelf());
                            wasPersistanceModuleDown = true;
                            persistanceActor.tell(new HeartBeat(), getSelf());
                        }
                        if (isCommunicatorModuleUp) {
//                            System.out.println("Pulse sent for communicator service");
                            isCommunicatorModuleUp = false;
                            communicationActor.tell(new HeartBeat(), getSelf());
                        } else {
                            System.out.println(ANSI_RED + "CRITICAL ERROR: Communicator Module Down!" + ANSI_RESET);
                            wasCommunicatorModuleDown = true;
                            communicationActor.tell(new HeartBeat(), getSelf());
                        }
                    }
                    getContext().system().scheduler().scheduleOnce(
                            Duration.create(5, TimeUnit.SECONDS),
                            getSelf(),
                            "Send Pulse",
                            getContext().dispatcher(), null);
                })
            .match(UserRequest.class,
                msg -> {
                    System.out.println("User creation request received for : " + msg.getNewUser().getName() + " with Unique ID: " + msg.getUniqueId());
                    int validationStatus = validateInputRequest(msg);
                    // 0 -> new user
                    // 1 -> incorrect details
                    // 2 -> relogging

                    if(validationStatus == 0) {
                        //Adding new User
                        long currUserUniqueId = ++userUniqueId;
                        uniqueIdMap.put(currUserUniqueId, msg.getUniqueId());
                        requestsToUserInfoMap.put(currUserUniqueId, msg.getNewUser());
                        //Interests request sent to Interests System
                        interestsActor.tell(new InterestsRequest(currUserUniqueId, msg.getNewUser().getGitHubId()), getSelf());
                    }
                    else if(validationStatus == 1){
                        UserResponse userResponse = new UserResponse(msg.getUniqueId(),"Invalid user data, please try a different user detail");
                        ActorSelection clientActor = system.actorSelection("akka.tcp://default@127.0.0.1:"+msg.getNewUser().getPortNumber()+"/user/"+msg.getUniqueId());
                        clientActor.tell(userResponse, null);
                    }
                    else
                    {
                        UserResponse userResponse = new UserResponse(msg.getUniqueId(),"Github ID already registered or Invalid user data, please try a different ");
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
                    Set<Tribe> suggestedTribe = getTribeSuggestions(msg.getInterest(), requestsToUserInfoMap.get(msg.getUniqueId()));

                    if(suggestedTribe.size() == 0){
                        System.out.println("New Tribe Creation as no tribe for the user interests exists");
                        String tribeLanguage;
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
                .build();
    }

    private int validateInputRequest(UserRequest userRequest){
        if(allUserInfo!= null && allUserInfo.size()>0 && allUserInfo.stream().filter(user->user.getGitHubId()
                .equalsIgnoreCase(userRequest.getNewUser().getGitHubId()) && user.getName().equalsIgnoreCase(userRequest.getNewUser().getName()))
                .collect(Collectors.toList()).size() > 0){
            //User is logging in again
            return 2;
        }
        else if(allUserInfo!= null && allUserInfo.size()>0 && allUserInfo.stream().filter(user->user.getGitHubId()
                .equalsIgnoreCase(userRequest.getNewUser().getGitHubId()) && !user.getName().equalsIgnoreCase(userRequest.getNewUser().getName()))
                .collect(Collectors.toList()).size() > 0){
            //User has incorrect details
            return 1;
        }
        else{
            //New user
            return 0;
        }
    }
    private String getTribeName(Tribe tribe) {
        return allTribes.stream()
                .filter(t->t.getTribeId() == tribe.getTribeId())
                .collect(Collectors.toList()).get(0).getTribeName();
    }

    private Set<Tribe> getTribeSuggestions(Interests interests, UserInfo userInfo){
        Set<Tribe> filteredTribes = new HashSet<>();
        Set<String> remainingLanguages = interests.getProgrammingLanguages();

        for(Tribe existingTribe:allTribes){
            Set<String> temp
                    = Stream.of(existingTribe.getTribeLanguages().trim().split("\\s*,\\s*"))
                    .collect(Collectors.toSet());
            if(containsMatch(interests.getProgrammingLanguages(),temp)){
                filteredTribes.add(existingTribe);
            }
            remainingLanguages.remove(existingTribe.getTribeName());
        }

        if(remainingLanguages.stream().count() > 0){
            for(String language:remainingLanguages){
                Tribe tempTribe = new Tribe(++tribeUniqueId, language, interests.getProgrammingLanguages().stream().collect(Collectors.joining(",")), Arrays.asList(userInfo));
                filteredTribes.add(tempTribe);
                allTribes.add(tempTribe);
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
        else return false;
    }
}
