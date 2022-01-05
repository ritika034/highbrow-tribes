package service.actor;

import akka.actor.*;
import jnr.ffi.annotations.In;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import service.centralCore.*;
import service.messages.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Interest extends AbstractActor {
    static ActorSystem system ;
    ActorSelection matcherActor = system.actorSelection("akka.tcp://default@127.0.0.1:2557/user/triber");

    //@Value("${githuburl}")
    private String githubUrl = "https://api.github.com/users/";
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InterestsRequest.class,
                        msg -> {
                            HashSet<String> programmingLanguages = getUserLanguages(msg.getGithubUserId());
                            Interests interests = new Interests(programmingLanguages);
                            InterestsResponse interestsResponse = new InterestsResponse(msg.getRequestId()
                                    ,interests);
                            matcherActor.tell(interestsResponse,getSelf());

                        }).build();
    }

    private HashSet<String> getUserLanguages(String userId){
        JSONArray outputFromGitHub = callExternalSystem(githubUrl+userId+"/repos");
        return outputFromGitHub!= null ? userProgrammingLanguageInterests(outputFromGitHub):null;
    }
    private JSONArray callExternalSystem(String url) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url,String.class);
        JSONArray jsonArray  = response.getBody()!=null ? new JSONArray(response.getBody()):null;
        return jsonArray;
    }

    private HashSet<String> userProgrammingLanguageInterests(JSONArray jsonArray){
        Set<String> programmingLanguageInterests = IntStream.range(0,jsonArray.length())
                .mapToObj(index -> ((JSONObject)jsonArray.get(index)).optString("language"))
                .filter(element-> (element!=null && element != ""))
                .collect(Collectors.toSet());

        return new HashSet<>(programmingLanguageInterests);
    }
    public static void main(String[] args){
        system = ActorSystem.create();
        ActorRef ref = system.actorOf(Props.create(Interest.class), "interests");
    }
}
