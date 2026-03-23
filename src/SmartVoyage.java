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
    int totalTrips=0;

    Admin(String n){
        name=n;
    }

    boolean login(String n,String p){
        return name.equals(n)&&password.equals(p);
    }

    void addSource(String s){
        sources.add(s);
    }

    void addDestination(String c,double h,double f){
        destinations.add(new Destination(c,h,f));
        bookingCount.put(c,0);
    }

    void addRoute(String s,String d,double t,double km){
        routes.add(new Route(s,d,t,km));
    }

    Route getRoute(String s,String d){
        for(Route r:routes){
            if(r.source.equalsIgnoreCase(s)&&r.destination.equalsIgnoreCase(d)){
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
        System.out.println("\n========== ADMIN DASHBOARD ==========");
        System.out.println("Admin: "+name);
        System.out.println("Total Trips: "+totalTrips);
        System.out.println("Sources: "+sources.size());
        System.out.println("Destinations: "+destinations.size());
        System.out.println("\nBooking Status");
        for(String d:bookingCount.keySet()){
            System.out.println(d+" : "+bookingCount.get(d)+"/4 booked");
        }
        System.out.println("=====================================");
    }

    void viewSources(){
        for(int i=0;i<sources.size();i++){
            System.out.println((i+1)+". "+sources.get(i));
        }
    }

    void viewDestinations(){
        for(int i=0;i<destinations.size();i++){
            Destination d=destinations.get(i);
            System.out.println((i+1)+". "+d.city+" Hotel:"+d.hotelPerDay+" Food:"+d.foodPerDay);
        }
    }

    void viewBookings(){
        for(String d:bookingCount.keySet()){
            System.out.println(d+" : "+bookingCount.get(d)+"/4 booked");
        }
    }

    void generateRoutes(){
        Random r=new Random();
        routes.clear();
        for(String s:sources){
            for(Destination d:destinations){
                double km=100+r.nextInt(600);
                double cost=km*5;
                routes.add(new Route(s,d.city,cost,km));
            }
        }
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

                case 1 -> {
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
                            System.out.println("4 Add Source");
                            System.out.println("5 Add Destination");
                            System.out.println("6 Regenerate Routes");
                            System.out.println("7 View Bookings");
                            System.out.println("8 Exit");
                            int a=sc.nextInt();

                            switch(a){
                                case 1 -> admin.dashboard();
                                case 2 -> admin.viewSources();
                                case 3 -> admin.viewDestinations();
                                case 4 -> {
                                    sc.nextLine();
                                    System.out.print("Enter Source: ");
                                    admin.addSource(sc.nextLine());
                                    admin.generateRoutes();
                                }
                                case 5 -> {
                                    sc.nextLine();
                                    System.out.print("City: ");
                                    String c=sc.nextLine();
                                    System.out.print("Hotel: ");
                                    double h=sc.nextDouble();
                                    System.out.print("Food: ");
                                    double f=sc.nextDouble();
                                    admin.addDestination(c,h,f);
                                    admin.generateRoutes();
                                }
                                case 6 -> admin.generateRoutes();
                                case 7 -> admin.viewBookings();
                                case 8 -> { break; }
                            }
                            if(a==8) break;
                        }
                    }else{
                        System.out.println("Invalid Login");
                    }
                }

                case 2 -> {
                    try{
                        sc.nextLine();
                        System.out.print("Enter Name: ");
                        String name=sc.nextLine();

                        System.out.print("Enter Budget: ");
                        double budget=sc.nextDouble();
                        if(budget<=0) throw new InvalidBudgetException("Invalid Budget");

                        System.out.print("Enter Days: ");
                        int days=sc.nextInt();

                        System.out.println("\nSelect Source:");
                        admin.viewSources();
                        String source=admin.sources.get(sc.nextInt()-1);

                        System.out.println("\nSelect Destination:");
                        for(int i=0;i<admin.destinations.size();i++){
                            System.out.println((i+1)+". "+admin.destinations.get(i).city);
                        }
                        String dest=admin.destinations.get(sc.nextInt()-1).city;

                        Route route=admin.getRoute(source,dest);
                        Destination d=admin.getDestination(dest);

                        double travel=route.travelCost;
                        double hotel=d.hotelPerDay*days;
                        double food=d.foodPerDay*days;
                        double total=travel+hotel+food;

                        if(admin.bookingCount.get(dest)>=4){
                            System.out.println("Hotel already booked for "+dest);
                            break;
                        }

                        if(budget>=total){
                            System.out.println("\nTrip Possible");
                            System.out.println("Customer: "+name);
                            System.out.println("Source: "+source);
                            System.out.println("Destination: "+dest);
                            System.out.println("Distance: "+route.distance+" km");
                            System.out.println("Travel: "+travel);
                            System.out.println("Hotel: "+hotel);
                            System.out.println("Food: "+food);
                            System.out.println("Total: "+total);
                            System.out.println("Remaining: "+(budget-total));

                            System.out.println("\n1 Confirm Trip");
                            System.out.println("2 Cancel");
                            int c=sc.nextInt();

                            if(c==1){
                                admin.bookingCount.put(dest,admin.bookingCount.get(dest)+1);
                                admin.totalTrips++;

                                BookingThread bt=new BookingThread(name,dest);
                                NotificationThread nt=new NotificationThread();
                                bt.start();
                                nt.start();
                            }

                        }else{
                            System.out.println("\nTrip not possible");
                            System.out.println("\nAvailable Trips in Your Budget\n");

                            boolean found=false;

                            for(Destination des:admin.destinations){
                                Route r=admin.getRoute(source,des.city);
                                double t=r.travelCost;
                                double h=des.hotelPerDay*days;
                                double f=des.foodPerDay*days;
                                double tot=t+h+f;

                                if(budget>=tot && admin.bookingCount.get(des.city)<4){
                                    found=true;
                                    System.out.println(source+" -> "+des.city);
                                    System.out.println("Distance: "+r.distance+" km");
                                    System.out.println("Travel: "+t);
                                    System.out.println("Hotel: "+h);
                                    System.out.println("Food: "+f);
                                    System.out.println("Total: "+tot);
                                    System.out.println();
                                }
                            }

                            if(!found){
                                System.out.println("No recommendations available");
                            }
                        }

                    }catch(InvalidBudgetException e){
                        System.out.println(e.getMessage());
                    }
                }

                case 3 -> {
                    System.out.println("Exiting...");
                    System.exit(0);
                }
            }
        }
    }
}