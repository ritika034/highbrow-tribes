package service.actor;

import akka.actor.*;
import service.centralCore.Tribe;
import service.centralCore.UserInfo;
import service.messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Random;

public class Client extends AbstractActor {
    private static ActorSystem system;
    private static ActorRef ref;
    private static ActorSelection communicationSelection;
    private static ActorSelection triberSelection;
    private static UserInfo userInfo;
    private static UserRequest userRequest;
    private static Boolean IsInChat = false;

    public static void main(String [] args) throws IOException {
        system = ActorSystem.create();
        communicationSelection =
                system.actorSelection("akka.tcp://default@127.0.0.1:2556/user/communicator");
        triberSelection =
                system.actorSelection("akka.tcp://default@127.0.0.1:2557/user/triber");
        Random rand = new Random();

        userInfo = new UserInfo();
        long uniqueId = rand.nextInt() + rand.nextInt();
        uniqueId = Math.abs(uniqueId);
        //userInfo.setUniqueId(uniqueId);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
//        System.out.println("Enter your Port number");
//        String portNumber = reader.readLine();
        userInfo.setPortNumber(2555);
        System.out.println("Enter your name");
        String userName = reader.readLine();
        userInfo.setName(userName);
        System.out.println("Enter your GitHub Id");
        String githubId = reader.readLine();
        userInfo.setGitHubId(githubId);
        setReference(uniqueId);

        userRequest = new UserRequest(uniqueId,userInfo);

        System.out.println("User has sent newUserRequest to Triber");

        triberSelection.tell(userRequest, ref);
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return  receiveBuilder()
            .match(ChatRegisterResponse.class, msg->{
                Tribe currentTribe = msg.getTribe();
                System.out.println("You have joined the community dialogue of " + currentTribe.getTribeName());
                Thread thread = new Thread(() -> {
                    IsInChat = true;
                    ParticipateInChat();
                });
                thread.start();
            })
            .match(ChatMessageReceive.class, msg->{
                System.out.println("[" + msg.getSentTime() + "] " + msg.getSenderName() + ": " + msg.getMessage());

                Thread thread = new Thread(this::ParticipateInChat);

                if(!IsInChat){
                    IsInChat = true;
                    thread.start();
                }
            })
            .match(UserCreationResponse.class,msg->{
                long uniqueId = msg.getUniqueId();
                System.out.println("Your ID for future reference :"+uniqueId);
                System.out.println("You are redirected to register for the Tribe's group chat...");
                userRequest.setUniqueId(uniqueId);
                userInfo.setTribeId(msg.getTribeId());
                setReference(uniqueId);
                ChatRegisterRequest chatRegisterRequest = new ChatRegisterRequest(uniqueId,userInfo);
                communicationSelection.tell(chatRegisterRequest,getSelf());
            })
            .match(UserResponse.class,msg->{
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(System.in));
                System.out.println("Enter your GitHub Id");
                String githubId = reader.readLine();
                userInfo.setGitHubId(githubId);
                triberSelection.tell(userRequest, ref);
            })
            .match(TribeSuggestionRequest.class, msg->{
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(System.in));
                StringBuilder sb;
                System.out.println("Received following tribe suggestions for you !");
                //String tribeId = reader.readLine();
                for(Tribe tribe:msg.getSuggestedTribes()){
                    sb = new StringBuilder();

                    for(UserInfo UI:tribe.getMembers()){
                        sb.append(UI.getName()).append(", ");
                    }

                    System.out.println(tribe);
                    //System.out.println("Tribe ID: " + tribe.getTribeId() + ", Programming Language: " + tribe.getTribeLanguages() + ", Tribe Name: " + tribe.getTribeName() + "_Tribe, Members: " + sb);
                }
                System.out.println("Enter the ID of the tribe you would like to join");
                String tribeIdString = reader.readLine();
                try {
                    long tribeId = Long.parseLong(tribeIdString);
//                    String tribeName = msg.getSuggestedTribes()
//                            .stream().filter(tribe->tribe.getTribeId()==tribeId)
//                            .findFirst().get().getTribeName();
                    userRequest.setUniqueId(msg.getUniqueId());
                    setReference(msg.getUniqueId());
                    userInfo.setTribeId(tribeId);

                    TribeSuggestionResponse tribeSuggestionResponse = new TribeSuggestionResponse(msg.getUniqueId(),tribeId);
                    triberSelection.tell(tribeSuggestionResponse, getSelf());
                }
                catch(Exception ex){
                    System.out.print("Invalid Input! Restart application");
                }
            }).build();
    }

    private void ParticipateInChat() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));

            // Reading data using readLine
            String message;
            Timestamp ts;
            while(true) {
                message = reader.readLine();
                ts = new Timestamp(System.currentTimeMillis());

                // Printing the read line
                communicationSelection.tell(new ChatMessageSend(userInfo.getName(), ts, userRequest.getUniqueId(), userInfo.getTribeId(), message), getSelf());
            }
        }
        catch(IOException ex){
            System.out.println("Error occured!");
        }
    }

    private static void setReference(Long UniqueId){
        ref = system.actorOf(Props.create(Client.class), UniqueId.toString());
    }
}
