package com.example.Tribes.service.UserSystem;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.example.Tribes.service.actors.Actor;

public class Main {
    public static void main(String[] args){
        //start broker server
        ActorSystem system = ActorSystem.create();
        ActorRef ref = system.actorOf(Props.create(Actor.class), "userSystem");
//        ref.tell(null, null);
//        ActorSelection selection =
//                system.actorSelection("akka.tcp://default@127.0.0.1:2551/user/triber");
//        selection.tell("register", ref);
    }
}
