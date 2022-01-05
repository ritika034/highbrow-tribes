//package service.tribersystem;
//
//import service.centralCore.*;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class TriberSystem {
//    public List<Tribe> GetTribeSuggestions(UserInfo userInfo){
//        List<Tribe> ExistingTribes;
//        List<Tribe> FilteredTribes = new ArrayList<>();
//
//        ExistingTribes = GetExistingTribes();
//
//        for(Tribe ExistingTribe:ExistingTribes){
//            if(userInfo.getInterests().getProgrammingLanguages().contains(ExistingTribe.getProgrammingLanguage())){
//                FilteredTribes.add(ExistingTribe);
//            }
//        }
//
//        return FilteredTribes;
//    }
//
//    public List<Tribe> GetExistingTribes(){
//        List<Tribe> ExistingTribes = new ArrayList<>();
//        Interests siddharthInterests = new Interests();
//        Interests mansoorInterests = new Interests();
//        Interests ritikaInterests = new Interests();
//
//        siddharthInterests.addProgrammingLanguages("C#");
//        mansoorInterests.addProgrammingLanguages("Java");
//        ritikaInterests.addProgrammingLanguages("Java");
//
//        UserInfo siddharth = new UserInfo("Siddharth", "", 101);
//        UserInfo mansoor = new UserInfo("Mansoor", "", 102);
//        UserInfo ritika = new UserInfo("Ritika", "", 103);
//
////        siddharth.setProgrammingLanguages(siddharthInterests.getProgrammingLanguages());
////        mansoor.setProgrammingLanguages(mansoorInterests.getProgrammingLanguages());
//        mansoor.setPortNumber(2555);
//        mansoor.setUniqueId(105105);
////        ritika.setProgrammingLanguages(ritikaInterests.getProgrammingLanguages());
//        ritika.setPortNumber(2558);
//        ritika.setUniqueId(105106);
//
//        Tribe CSharpTribe = new Tribe(101, "CSharp Tribe", "C#", Arrays.asList(siddharth));
//        Tribe JavaTribe = new Tribe(102, "Java Tribe", "Java", Arrays.asList(ritika, mansoor));
//
//        ExistingTribes = Arrays.asList(JavaTribe, CSharpTribe);
//
//        return ExistingTribes;
//    }
//}
