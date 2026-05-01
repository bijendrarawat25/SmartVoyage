import java.util.*;

class InvalidBudgetException extends Exception {
    InvalidBudgetException(String msg) {
        super(msg);
    }
}

class Admin {
    String adminName = "admin";
    String password = "1234";
    List<Destination> destinations = new ArrayList<>();
    List<String> sources = new ArrayList<>();
    int totalTrips = 0;
    double hotelPerDay = 800;
    double foodPerDay = 500;
    boolean login(String n, String p) {
        return adminName.equals(n) && password.equals(p);
    }
    void addDestination(String city, double cost) {
        destinations.add(new Destination(city, cost));
    }
    void addSource(String s) {
        sources.add(s);
    }
    void viewSources() {
        for (int i = 0; i < sources.size(); i++) {
            System.out.println((i + 1) + ". " + sources.get(i));
        }
    }

    void viewDestinations() {
        for (int i = 0; i < destinations.size(); i++) {
            System.out.println((i + 1) + ". " + destinations.get(i).city);
        }
    }

    void viewDestinationsForAdmin() {
        for (int i = 0; i < destinations.size(); i++) {
            System.out.println((i + 1) + ". " + destinations.get(i).city +
                    " | Base Cost: " + destinations.get(i).baseBudget);
        }
    }

    void changeDestinationCost(int index, double newCost) {
        destinations.get(index).baseBudget = newCost;
    }

    void adminDashboard() {
        System.out.println("\n----- ADMIN DASHBOARD -----");
        System.out.println("Total Trips Booked: " + totalTrips);
        System.out.println("Total Cities Available: " + destinations.size());
        System.out.println("Hotel Cost/Day: " + hotelPerDay);
        System.out.println("Food Cost/Day: " + foodPerDay);
    }

    double getTravelCost(String source, String destination) {
        if (source.equalsIgnoreCase("Dehradun") && destination.equalsIgnoreCase("Rishikesh")) return 500;
        if (source.equalsIgnoreCase("Delhi") && destination.equalsIgnoreCase("Rishikesh")) return 2000;
        if (source.equalsIgnoreCase("Delhi") && destination.equalsIgnoreCase("Manali")) return 4000;
        if (source.equalsIgnoreCase("Chandigarh") && destination.equalsIgnoreCase("Shimla")) return 1500;
        if (source.equalsIgnoreCase("Mumbai") && destination.equalsIgnoreCase("Goa")) return 3000;
        return 2500;
    }
}

class Destination {
    String city;
    double baseBudget;

    Destination(String c, double b) {
        city = c;
        baseBudget = b;
    }
}

public class SmartVoyage {
    public static void main(String[] st) {

        Scanner sc = new Scanner(System.in);
        Admin admin = new Admin();

        admin.addSource("Delhi");
        admin.addSource("Dehradun");
        admin.addSource("Mumbai");
        admin.addSource("Chandigarh");
        admin.addSource("Jaipur");

        admin.addDestination("Manali", 12000);
        admin.addDestination("Goa", 15000);
        admin.addDestination("Shimla", 8000);
        admin.addDestination("Agra", 4800);
        admin.addDestination("Rishikesh", 5000);

        while (true) {

            System.out.println("\n1. Admin Login");
            System.out.println("2. Customer Panel");
            System.out.println("3. Exit");

            int choice = sc.nextInt();

            if (choice == 1) {

                sc.nextLine();
                System.out.print("Enter Admin Name: ");
                String n = sc.nextLine();

                System.out.print("Enter Password: ");
                String p = sc.nextLine();

                if (admin.login(n, p)) {

                    while (true) {
                        System.out.println("\n1. View Dashboard");
                        System.out.println("2. View Cities");
                        System.out.println("3. Change City Budget");
                        System.out.println("4. Exit");

                        int a = sc.nextInt();

                        if (a == 1) {
                            admin.adminDashboard();
                        } else if (a == 2) {
                            admin.viewDestinationsForAdmin();
                        } else if (a == 3) {
                            admin.viewDestinationsForAdmin();
                            System.out.print("Select City Number: ");
                            int index = sc.nextInt() - 1;

                            System.out.print("Enter New Budget: ");
                            double newCost = sc.nextDouble();

                            admin.changeDestinationCost(index, newCost);
                            System.out.println("Budget Updated Successfully");
                        } else {
                            break;
                        }
                    }

                } else {
                    System.out.println("Invalid Login");
                }
            }

            else if (choice == 2) {

                try {
                    sc.nextLine();

                    System.out.print("Enter Your Name: ");
                    String name = sc.nextLine();

                    System.out.print("Enter Your Budget: ");
                    double budget = sc.nextDouble();
                    if (budget <= 0)
                        throw new InvalidBudgetException("Invalid Budget");
                    System.out.print("Enter Days of Stay: ");
                    int days = sc.nextInt();

                    System.out.println("\nSelect Source:");
                    admin.viewSources();
                    String source = admin.sources.get(sc.nextInt() - 1);

                    System.out.println("\nSelect Destination:");
                    admin.viewDestinations();
                    String destination = admin.destinations.get(sc.nextInt() - 1).city;

                    double hotel = days * admin.hotelPerDay;
                    double food = days * admin.foodPerDay;
                    double travel = admin.getTravelCost(source, destination);

                    double total = hotel + food + travel;

                    if (budget >= total) {
                        admin.totalTrips++;
                        System.out.println("\n----- Trip Cost Details -----");
                        System.out.println("Customer: " + name);
                        System.out.println("Hotel Cost: " + hotel);
                        System.out.println("Food Cost: " + food);
                        System.out.println("Travel Cost: " + travel);
                        System.out.println("Total Trip Cost: " + total);
                        System.out.println("Remaining Money: " + (budget - total));

                        System.out.println("\nYes, you can do the trip");

                    } else {
                        System.out.println("\nTrip is not available");
                        boolean found = false;
                        System.out.println("\nYou can travel to these destinations in your budget:");
                        for (Destination d : admin.destinations) {
                            double tCost = admin.getTravelCost(source, d.city);
                            double hCost = days * admin.hotelPerDay;
                            double fCost = days * admin.foodPerDay;

                            double possibleTotal = tCost + hCost + fCost;

                            if (budget >= possibleTotal) {
                                found = true;
                                System.out.println(source + " -> " + d.city + " | Cost: " + possibleTotal);
                            }
                        }

                        if (!found) {
                            System.out.println("No destinations available in your budget");
                        }
                    }

                } catch (InvalidBudgetException e) {
                    System.out.println(e.getMessage());
                }
            }
            else {
                break;
            }
        }
    }
}