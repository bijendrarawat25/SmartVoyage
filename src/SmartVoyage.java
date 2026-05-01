import java.util.*;

class InvalidBudgetException extends Exception{
    InvalidBudgetException(String msg){
        super(msg);
    }
}

class Route{
    String source;
    String destination;
    double travelCost;
    double distance;

    Route(String s,String d,double t,double km){
        source=s;
        destination=d;
        travelCost=t;
        distance=km;
    }
}

class Destination{
    String city;
    double hotelPerDay;
    double foodPerDay;

    Destination(String c,double h,double f){
        city=c;
        hotelPerDay=h;
        foodPerDay=f;
    }
}

class Booking{
    String customerName;
    String source;
    String destination;
    double distance;
    double totalCost;

    Booking(String n,String s,String d,double km,double cost){
        customerName=n;
        source=s;
        destination=d;
        distance=km;
        totalCost=cost;
    }
}

class BookingThread extends Thread{
    String name;
    String destination;

    BookingThread(String n,String d){
        name=n;
        destination=d;
    }

    public void run(){
        System.out.println("Processing booking for "+name);
        try{Thread.sleep(1000);}catch(Exception e){}
        System.out.println("Booking confirmed for "+destination);
    }
}

class NotificationThread extends Thread{
    public void run(){
        System.out.println("Notification: Trip Confirmed");
    }
}

class Admin{

    String name;
    String password="1234";

    List<String> sources=new ArrayList<>();
    List<Destination> destinations=new ArrayList<>();
    List<Route> routes=new ArrayList<>();
    Map<String,Integer> bookingCount=new HashMap<>();
    List<Booking> bookings=new ArrayList<>();

    int totalTrips=0;

    Admin(String n){
        name=n;
    }

    boolean login(String n,String p){
        return name.equals(n) && password.equals(p);
    }

    void addSource(String s){
        sources.add(s);
    }

    void addDestination(String c,double h,double f){
        destinations.add(new Destination(c,h,f));
        bookingCount.put(c,0);
    }

    void addRoute(String s,String d,double km){
        double cost=km*4;
        routes.add(new Route(s,d,cost,km));
    }

    Route getRoute(String s,String d){
        for(Route r:routes){
            if(r.source.equalsIgnoreCase(s) &&
               r.destination.equalsIgnoreCase(d)){
                return r;
            }
        }
        return null;
    }

    Destination getDestination(String d){
        for(Destination des:destinations){
            if(des.city.equalsIgnoreCase(d)){
                return des;
            }
        }
        return null;
    }

    void dashboard(){

        System.out.println("\n===== ADMIN DASHBOARD =====");
        System.out.println("Total Trips: "+totalTrips);

        for(String d:bookingCount.keySet()){
            System.out.println(d+" : "+bookingCount.get(d)+"/4 booked");
        }
    }

    void viewSources(){
        for(int i=0;i<sources.size();i++){
            System.out.println((i+1)+". "+sources.get(i));
        }
    }

    void viewDestinations(){
        for(int i=0;i<destinations.size();i++){
            Destination d=destinations.get(i);
            System.out.println((i+1)+". "+d.city+
                    " Hotel:"+d.hotelPerDay+
                    " Food:"+d.foodPerDay);
        }
    }

    void viewBookings(){

        System.out.println("\n===== CUSTOMER TRIPS =====");

        if(bookings.size()==0){
            System.out.println("No trips booked yet");
            return;
        }

        for(Booking b:bookings){

            System.out.println("Customer: "+b.customerName);
            System.out.println("Source: "+b.source);
            System.out.println("Destination: "+b.destination);
            System.out.println("Distance: "+b.distance+" km");
            System.out.println("Total Cost: "+b.totalCost);
            System.out.println("---------------------------");
        }
    }

    void generateRoutes(){

        addRoute("Dehradun","Rishikesh",45);
        addRoute("Dehradun","Shimla",230);
        addRoute("Dehradun","Agra",380);
        addRoute("Dehradun","Manali",480);
        addRoute("Dehradun","Goa",1900);

        addRoute("Delhi","Rishikesh",240);
        addRoute("Delhi","Shimla",340);
        addRoute("Delhi","Agra",230);
        addRoute("Delhi","Manali",540);
        addRoute("Delhi","Goa",1900);

        addRoute("Chandigarh","Shimla",120);
        addRoute("Chandigarh","Manali",300);
        addRoute("Chandigarh","Rishikesh",210);
        addRoute("Chandigarh","Agra",450);
        addRoute("Chandigarh","Goa",1800);

        addRoute("Mumbai","Goa",590);
        addRoute("Mumbai","Agra",1200);
        addRoute("Mumbai","Rishikesh",1700);
        addRoute("Mumbai","Shimla",1800);
        addRoute("Mumbai","Manali",1900);

        addRoute("Jaipur","Agra",240);
        addRoute("Jaipur","Rishikesh",420);
        addRoute("Jaipur","Shimla",520);
        addRoute("Jaipur","Manali",720);
        addRoute("Jaipur","Goa",1600);
    }
}

public class SmartVoyage{

    public static void main(String[] args){

        Scanner sc=new Scanner(System.in);

        Admin admin=new Admin("admin");

        admin.addSource("Delhi");
        admin.addSource("Mumbai");
        admin.addSource("Chandigarh");
        admin.addSource("Jaipur");
        admin.addSource("Dehradun");

        admin.addDestination("Manali",1200,700);
        admin.addDestination("Goa",1500,800);
        admin.addDestination("Shimla",900,500);
        admin.addDestination("Agra",700,400);
        admin.addDestination("Rishikesh",800,500);

        admin.generateRoutes();

        while(true){

            System.out.println("\n1 Admin Panel");
            System.out.println("2 Customer Panel");
            System.out.println("3 Exit");

            int choice=sc.nextInt();

            switch(choice){

                case 1:

                    sc.nextLine();

                    System.out.print("Enter Admin Name: ");
                    String n=sc.nextLine();

                    System.out.print("Enter Password: ");
                    String p=sc.nextLine();

                    if(admin.login(n,p)){

                        while(true){

                            System.out.println("\n1 Dashboard");
                            System.out.println("2 View Sources");
                            System.out.println("3 View Destinations");
                            System.out.println("4 View Bookings");
                            System.out.println("5 Exit");

                            int a=sc.nextInt();

                            switch(a){
                                case 1: admin.dashboard(); break;
                                case 2: admin.viewSources(); break;
                                case 3: admin.viewDestinations(); break;
                                case 4: admin.viewBookings(); break;
                                case 5: break;
                            }

                            if(a==5) break;
                        }

                    }else{
                        System.out.println("Invalid Login");
                    }

                    break;

                case 2:

                    try{

                        sc.nextLine();

                        System.out.print("Enter Name: ");
                        String name=sc.nextLine();

                        System.out.print("Enter Budget: ");
                        double budget=sc.nextDouble();

                        if(budget<=0)
                            throw new InvalidBudgetException("Invalid Budget");

                        System.out.print("Enter Days: ");
                        int days=sc.nextInt();

                        System.out.println("\nSelect Source:");
                        admin.viewSources();
                        String source=admin.sources.get(sc.nextInt()-1);

                        System.out.println("\nSelect Destination:");
                        for(int i=0;i<admin.destinations.size();i++){
                            System.out.println((i+1)+". "+
                                    admin.destinations.get(i).city);
                        }

                        String dest=admin.destinations.get(sc.nextInt()-1).city;

                        Route route=admin.getRoute(source,dest);

                        if(route==null){
                            System.out.println("Route not available");
                            break;
                        }

                        Destination d=admin.getDestination(dest);

                        double travel=route.travelCost;
                        double hotel=d.hotelPerDay*days;
                        double food=d.foodPerDay*days;

                        double total=travel+hotel+food;

                        if(admin.bookingCount.get(dest)>=4){
                            System.out.println("Hotel already booked");
                            break;
                        }

                        if(budget>=total){

                            System.out.println("\nTrip Possible");
                            System.out.println("Customer: "+name);
                            System.out.println("Source: "+source);
                            System.out.println("Destination: "+dest);
                            System.out.println("Distance: "+route.distance+" km");
                            System.out.println("Total Cost: "+total);

                            System.out.println("\n1 Confirm");
                            System.out.println("2 Cancel");

                            int c=sc.nextInt();

                            if(c==1){

                                admin.bookingCount.put(dest,
                                        admin.bookingCount.get(dest)+1);

                                admin.totalTrips++;

                                admin.bookings.add(
                                        new Booking(
                                                name,
                                                source,
                                                dest,
                                                route.distance,
                                                total
                                        )
                                );

                                new BookingThread(name,dest).start();
                                new NotificationThread().start();
                            }

                        }else{

                            System.out.println("\nTrip not possible");
                            System.out.println("\nAvailable Trips in Budget\n");

                            boolean found=false;

                            for(Destination des:admin.destinations){

                                Route r=admin.getRoute(source,des.city);

                                if(r==null) continue;

                                double tot=r.travelCost +
                                        des.hotelPerDay*days +
                                        des.foodPerDay*days;

                                if(budget>=tot &&
                                        admin.bookingCount.get(des.city)<4){

                                    found=true;

                                    System.out.println(source+
                                            " -> "+des.city);

                                    System.out.println("Distance: "+
                                            r.distance+" km");

                                    System.out.println("Total: "+tot);
                                    System.out.println();
                                }
                            }

                            if(!found){
                                System.out.println(
                                        "No recommendations available");
                            }
                        }

                    }catch(InvalidBudgetException e){
                        System.out.println(e.getMessage());
                    }

                    break;

                case 3:
                    System.out.println("Exiting...");
                    sc.close();
                    System.exit(0);
            }
        }
    }
}