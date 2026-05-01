import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.Date;
class InvalidBudgetException extends Exception {
    InvalidBudgetException(String msg) { super(msg); }
}
class Route {
    String source, destination;
    double travelCost, distance;
    Route(String s, String d, double t, double km) {
        source = s; destination = d; travelCost = t; distance = km;
    }
}
class Destination {
    String city, photoUrl, description;
    double hotelPerDay, foodPerDay;
    Destination(String c, double h, double f, String url, String desc) {
        city = c; hotelPerDay = h; foodPerDay = f; photoUrl = url; description = desc;
    }
}
class Booking {
    String bookingId, customerName, source, destination, bookingDate, customerPhone, customerEmail;
    double distance, totalCost;
    int days;
    String status; 
    Booking(String n, String phone, String email, String s, String d,
            double km, double cost, int days) {
        customerName = n; customerPhone = phone; customerEmail = email;
        source = s; destination = d; distance = km; totalCost = cost; this.days = days;
        bookingId   = "SV" + String.format("%05d", (int)(Math.random() * 99999));
        bookingDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new Date());
        status = "CONFIRMED";
    }
    Booking(String id, String n, String phone, String email,
            String s, String d, double km, double cost, int days, String date, String st) {
        bookingId = id; customerName = n; customerPhone = phone; customerEmail = email;
        source = s; destination = d; distance = km; totalCost = cost;
        this.days = days; bookingDate = date; status = st;
    }
}
class DB {
    private static final String URL  = "jdbc:mysql://localhost:3306/smartvoyage?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "admin123";   
    private static Connection conn;
    static Connection get() {
        try {
            if (conn == null || conn.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(URL, USER, PASS);
            }
        } catch (Exception e) {
        }
        return conn;
    }
    /** Create tables if they don't exist */
    static void init() {
        Connection c = get();
        if (c == null) return;
        try (Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS bookings (" +
                "  booking_id      VARCHAR(20)  PRIMARY KEY," +
                "  customer_name   VARCHAR(120) NOT NULL," +
                "  customer_phone  VARCHAR(20)," +
                "  customer_email  VARCHAR(100)," +
                "  source          VARCHAR(80)  NOT NULL," +
                "  destination     VARCHAR(80)  NOT NULL," +
                "  distance_km     DOUBLE," +
                "  total_cost      DOUBLE," +
                "  days            INT," +
                "  booking_date    VARCHAR(60)," +
                "  status          VARCHAR(20)  DEFAULT 'CONFIRMED'" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS activity_log (" +
                "  id          INT AUTO_INCREMENT PRIMARY KEY," +
                "  event_time  DATETIME      DEFAULT CURRENT_TIMESTAMP," +
                "  action      VARCHAR(50)   NOT NULL," +
                "  booking_id  VARCHAR(20)," +
                "  customer    VARCHAR(120)," +
                "  details     VARCHAR(500)" +
                ")"
            );
        } catch (SQLException e) { e.printStackTrace(); }
    }
    /** Insert a new booking */
    static void insertBooking(Booking b) {
        Connection c = get(); if (c == null) return;
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO bookings VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, b.bookingId);   ps.setString(2, b.customerName);
            ps.setString(3, b.customerPhone); ps.setString(4, b.customerEmail);
            ps.setString(5, b.source);      ps.setString(6, b.destination);
            ps.setDouble(7, b.distance);    ps.setDouble(8, b.totalCost);
            ps.setInt   (9, b.days);        ps.setString(10, b.bookingDate);
            ps.setString(11, b.status);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        log("BOOKING", b.bookingId, b.customerName,
            b.source + " → " + b.destination + " | Rs." +
            String.format("%.0f", b.totalCost) + " | " + b.days + " days");
    }
    /** Mark a booking as CHECKED_OUT */
    static void checkoutBooking(String bookingId) {
        Connection c = get(); if (c == null) return;
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE bookings SET status='CHECKED_OUT' WHERE booking_id=?")) {
            ps.setString(1, bookingId); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        log("CHECKOUT", bookingId, null, "Customer checked out");
    }
    /** Delete a booking row entirely */
    static void deleteBooking(String bookingId) {
        Connection c = get(); if (c == null) return;
        try (PreparedStatement ps = c.prepareStatement(
            "DELETE FROM bookings WHERE booking_id=?")) {
            ps.setString(1, bookingId); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    /** Load all non-checked-out bookings */
    static List<Booking> loadActiveBookings() {
        List<Booking> list = new ArrayList<>();
        Connection c = get(); if (c == null) return list;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT * FROM bookings WHERE status='CONFIRMED' ORDER BY booking_date DESC")) {
            while (rs.next()) list.add(fromRS(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
    /** Load ALL bookings (for admin tracking log) */
    static List<Booking> loadAllBookings() {
        List<Booking> list = new ArrayList<>();
        Connection c = get(); if (c == null) return list;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT * FROM bookings ORDER BY booking_date DESC")) {
            while (rs.next()) list.add(fromRS(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
    /** Load activity log entries */
    static List<String[]> loadLog() {
        List<String[]> list = new ArrayList<>();
        Connection c = get(); if (c == null) return list;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT event_time, action, booking_id, customer, details FROM activity_log ORDER BY event_time DESC LIMIT 200")) {
            while (rs.next())
                list.add(new String[]{
                    rs.getString("event_time"), rs.getString("action"),
                    rs.getString("booking_id"), rs.getString("customer"),
                    rs.getString("details")});
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
    /** Write an activity log entry */
    static void log(String action, String bookingId, String customer, String details) {
        Connection c = get(); if (c == null) return;
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO activity_log(action,booking_id,customer,details) VALUES(?,?,?,?)")) {
            ps.setString(1, action); ps.setString(2, bookingId);
            ps.setString(3, customer); ps.setString(4, details);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    /** Count confirmed bookings for a destination */
    static int countBookings(String dest) {
        Connection c = get(); if (c == null) return 0;
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT COUNT(*) FROM bookings WHERE destination=? AND status='CONFIRMED'")) {
            ps.setString(1, dest);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    private static Booking fromRS(ResultSet rs) throws SQLException {
        return new Booking(
            rs.getString("booking_id"),   rs.getString("customer_name"),
            rs.getString("customer_phone"), rs.getString("customer_email"),
            rs.getString("source"),       rs.getString("destination"),
            rs.getDouble("distance_km"),  rs.getDouble("total_cost"),
            rs.getInt("days"),            rs.getString("booking_date"),
            rs.getString("status"));
    }
}
class Admin {
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin2024";   
    List<String>      sources      = new ArrayList<>();
    List<Destination> destinations = new ArrayList<>();
    List<Route>       routes       = new ArrayList<>();
    boolean login(String u, String p) {
        return ADMIN_USER.equals(u.trim()) && ADMIN_PASS.equals(p.trim());
    }
    void addSource(String s) { sources.add(s); }
    void addDestination(String c, double h, double f, String url, String desc) {
        destinations.add(new Destination(c, h, f, url, desc));
    }
    void addRoute(String s, String d, double km) {
        routes.add(new Route(s, d, km * 4, km));
    }
    Route getRoute(String s, String d) {
        for (Route r : routes)
            if (r.source.equalsIgnoreCase(s) && r.destination.equalsIgnoreCase(d)) return r;
        return null;
    }
    Destination getDestination(String d) {
        for (Destination des : destinations)
            if (des.city.equalsIgnoreCase(d)) return des;
        return null;
    }
    void generateRoutes() {
        addRoute("Dehradun","Rishikesh",45);   addRoute("Dehradun","Shimla",230);
        addRoute("Dehradun","Agra",380);        addRoute("Dehradun","Manali",480);
        addRoute("Dehradun","Goa",1900);
        addRoute("Delhi","Rishikesh",240);      addRoute("Delhi","Shimla",340);
        addRoute("Delhi","Agra",230);           addRoute("Delhi","Manali",540);
        addRoute("Delhi","Goa",1900);
        addRoute("Chandigarh","Shimla",120);    addRoute("Chandigarh","Manali",300);
        addRoute("Chandigarh","Rishikesh",210); addRoute("Chandigarh","Agra",450);
        addRoute("Chandigarh","Goa",1800);
        addRoute("Mumbai","Goa",590);           addRoute("Mumbai","Agra",1200);
        addRoute("Mumbai","Rishikesh",1700);    addRoute("Mumbai","Shimla",1800);
        addRoute("Mumbai","Manali",1900);
        addRoute("Jaipur","Agra",240);          addRoute("Jaipur","Rishikesh",420);
        addRoute("Jaipur","Shimla",520);        addRoute("Jaipur","Manali",720);
        addRoute("Jaipur","Goa",1600);
    }
}
class RoundedPanel extends JPanel {
    private int radius; private Color bg;
    RoundedPanel(int radius, Color bg) { this.radius=radius; this.bg=bg; setOpaque(false); }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),radius,radius);
        g2.dispose(); super.paintComponent(g);
    }
}
class GradientButton extends JButton {
    private Color c1,c2; private boolean hover=false;
    GradientButton(String text,Color c1,Color c2){
        super(text); this.c1=c1; this.c2=c2;
        setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
        setForeground(Color.WHITE); setFont(new Font("Segoe UI",Font.BOLD,13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){hover=true;repaint();}
            public void mouseExited (MouseEvent e){hover=false;repaint();}
        });
    }
    @Override protected void paintComponent(Graphics g){
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Color a=hover?c1.darker():c1, b=hover?c2.darker():c2;
        g2.setPaint(new GradientPaint(0,0,a,getWidth(),getHeight(),b));
        g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
        g2.dispose(); super.paintComponent(g);
    }
}
class LightTextField extends JTextField {
    private String ph; private boolean cleared=false;
    LightTextField(String ph){
        super(ph); this.ph=ph;
        setFont(new Font("Segoe UI",Font.PLAIN,13));
        setForeground(new Color(160,160,175)); setBackground(Color.WHITE);
        setCaretColor(new Color(67,97,238));
        setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(210,215,230),1,true),
            BorderFactory.createEmptyBorder(8,12,8,12)));
        addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e){
                if(!cleared){setText("");setForeground(new Color(22,27,58));cleared=true;}
                setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(67,97,238),1,true),
                    BorderFactory.createEmptyBorder(8,12,8,12)));
            }
            public void focusLost(FocusEvent e){
                setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(210,215,230),1,true),
                    BorderFactory.createEmptyBorder(8,12,8,12)));
            }
        });
    }
    public void reset(){cleared=false;setText(ph);setForeground(new Color(160,160,175));}
}
class LightComboBox extends JComboBox<String> {
    LightComboBox(String[] items){
        super(items);
        setFont(new Font("Segoe UI",Font.PLAIN,13));
        setBackground(Color.WHITE); setForeground(new Color(22,27,58));
        setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(210,215,230),1,true),
            BorderFactory.createEmptyBorder(4,8,4,8)));
        setPreferredSize(new Dimension(200,36));
    }
}
/** Destination card painted fully in Java2D with emoji, gradient, description */
class DestPhotoCard extends JPanel {
    static final Map<String,String[]> DEST_DATA = new LinkedHashMap<>();
    static {
        DEST_DATA.put("Manali",   new String[]{"🏔️","Snow peaks & adventure","2D6AE0"});
        DEST_DATA.put("Goa",      new String[]{"🏖️","Sun, sand & seafood","E83E8C"});
        DEST_DATA.put("Shimla",   new String[]{"🌲","Queen of the Hills","10855E"});
        DEST_DATA.put("Agra",     new String[]{"🕌","Home of the Taj Mahal","C47600"});
        DEST_DATA.put("Rishikesh",new String[]{"🌊","Yoga capital of India","7B25B0"});
    }
    private String city; private Color c1,c2;
    DestPhotoCard(String city,Color c1,Color c2){
        this.city=city; this.c1=c1; this.c2=c2;
        setOpaque(false); setPreferredSize(new Dimension(190,150));
    }
    @Override protected void paintComponent(Graphics g){
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0,0,0,22)); g2.fillRoundRect(4,6,getWidth()-5,getHeight()-5,18,18);
        g2.setPaint(new GradientPaint(0,0,c1,getWidth(),getHeight(),c2));
        g2.fillRoundRect(0,0,getWidth()-3,getHeight()-4,18,18);
        g2.setColor(new Color(255,255,255,15));
        for(int i=-20;i<getWidth();i+=20){
            g2.setStroke(new BasicStroke(14));
            g2.drawLine(i,0,i+getHeight(),getHeight());
        }
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Segoe UI",Font.BOLD,17)); g2.setColor(Color.WHITE);
        g2.drawString(city,13,28);
        String[] data = DEST_DATA.getOrDefault(city,new String[]{"✈️","","FFFFFF"});
        g2.setFont(new Font("Segoe UI Emoji",Font.PLAIN,40));
        g2.drawString(data[0],getWidth()-62,70);
        g2.setFont(new Font("Segoe UI",Font.BOLD,11));
        g2.setColor(new Color(255,255,255,210));
        g2.drawString(data[1],13,58);
        g2.dispose(); super.paintComponent(g);
    }
}
public class SmartVoyageUI extends JFrame {
    static final Color BG      = new Color(242,245,252);
    static final Color SURFACE = Color.WHITE;
    static final Color ACCENT1 = new Color(52,86,230);
    static final Color ACCENT2 = new Color(228,30,96);
    static final Color ACCENT3 = new Color(14,160,96);
    static final Color ACCENT4 = new Color(240,140,0);
    static final Color ACCENT5 = new Color(120,35,180);
    static final Color TXT1    = new Color(16,22,50);
    static final Color TXT2    = new Color(96,108,136);
    static final Color BORDER  = new Color(216,222,240);
    Admin admin;
    CardLayout cardLayout;
    JPanel mainPanel;
    JLabel       totalTripsLbl;
    JPanel       destCardsPanel;
    DefaultTableModel bookingModel;
    DefaultTableModel logModel;
    JPanel destTabCardsPanel;
    DefaultTableModel destTabModel;
    Booking     lastBooking;
    Destination lastDest;
    List<Booking> localBookings = new ArrayList<>();
    boolean dbOk = false;
    public SmartVoyageUI() {
        admin = new Admin();
        admin.addSource("Delhi");  admin.addSource("Mumbai");
        admin.addSource("Chandigarh"); admin.addSource("Jaipur"); admin.addSource("Dehradun");
        admin.addDestination("Manali",  1200,700,"","Snow-capped peaks and adventure sports in the Himalayan paradise.");
        admin.addDestination("Goa",     1500,800,"","Sun, sand and seafood on India's most iconic coastline.");
        admin.addDestination("Shimla",  900, 500,"","The Queen of Hills with colonial charm and cool mountain breezes.");
        admin.addDestination("Agra",    700, 400,"","Home of the Taj Mahal — one of the seven wonders of the world.");
        admin.addDestination("Rishikesh",800,500,"","Yoga capital of the world on the sacred Ganges river.");
        admin.generateRoutes();
        try {
            DB.init();
            dbOk = (DB.get() != null);
        } catch (Exception e) { dbOk = false; }
        if(!dbOk) loadLocal();
        setTitle("SmartVoyage — India Travel Planner");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100,760);
        setMinimumSize(new Dimension(950,660));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        cardLayout = new CardLayout();
        mainPanel  = new JPanel(cardLayout);
        mainPanel.setBackground(BG);
        mainPanel.add(buildHome(),       "HOME");
        mainPanel.add(buildCustomer(),   "CUSTOMER");
        mainPanel.add(buildAdminLogin(), "ADMIN_LOGIN");
        mainPanel.add(buildAdminDash(),  "ADMIN_DASH");
        add(mainPanel);
        cardLayout.show(mainPanel,"HOME");
        setVisible(true);
    }
    JLabel heading(String t,int sz){
        JLabel l=new JLabel(t);
        l.setFont(new Font("Segoe UI",Font.BOLD,sz)); l.setForeground(TXT1); return l;
    }
    JLabel sub(String t){
        JLabel l=new JLabel(t);
        l.setFont(new Font("Segoe UI",Font.PLAIN,13)); l.setForeground(TXT2); return l;
    }
    JPanel gap(int h){ JPanel p=new JPanel(); p.setOpaque(false); p.setPreferredSize(new Dimension(1,h)); return p; }
    JPanel card(int r){
        return new JPanel(){
            {setOpaque(false);}
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0,0,0,14)); g2.fillRoundRect(3,5,getWidth()-3,getHeight()-3,r,r);
                g2.setColor(SURFACE);             g2.fillRoundRect(0,0,getWidth()-2,getHeight()-4,r,r);
                g2.dispose(); super.paintComponent(g);
            }
        };
    }
    JTable styledTable(DefaultTableModel m){
        JTable t=new JTable(m);
        t.setBackground(SURFACE); t.setForeground(TXT1);
        t.setGridColor(BORDER);   t.setRowHeight(36);
        t.setFont(new Font("Segoe UI",Font.PLAIN,13));
        t.setSelectionBackground(new Color(52,86,230,55));
        t.setSelectionForeground(TXT1);
        t.setShowVerticalLines(false); t.setFillsViewportHeight(true);
        t.getTableHeader().setBackground(new Color(236,240,255));
        t.getTableHeader().setForeground(TXT2);
        t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER));
        t.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(
                    JTable tbl,Object v,boolean sel,boolean foc,int row,int col){
                super.getTableCellRendererComponent(tbl,v,sel,foc,row,col);
                setBackground(sel?new Color(52,86,230,55):(row%2==0?SURFACE:new Color(245,247,255)));
                setForeground(TXT1);
                setBorder(BorderFactory.createEmptyBorder(0,12,0,12));
                return this;
            }
        });
        return t;
    }
    JPanel navBar(String title,boolean home){
        JPanel bar=new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,BORDER),
            BorderFactory.createEmptyBorder(14,28,14,28)));
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); left.setOpaque(false);
        JLabel dot=new JLabel("●"); dot.setFont(new Font("Segoe UI",Font.BOLD,18)); dot.setForeground(ACCENT1);
        JLabel logo=new JLabel("SmartVoyage"); logo.setFont(new Font("Segoe UI",Font.BOLD,20)); logo.setForeground(TXT1);
        left.add(dot); left.add(logo);
        if(!title.isEmpty()){
            JLabel pg=new JLabel("  /  "+title); pg.setFont(new Font("Segoe UI",Font.PLAIN,14)); pg.setForeground(TXT2);
            left.add(pg);
        }
        JLabel dbChip = new JLabel(dbOk?"  ● MySQL" : "  ● No DB");
        dbChip.setFont(new Font("Segoe UI",Font.BOLD,11));
        dbChip.setForeground(dbOk ? ACCENT3 : ACCENT2);
        left.add(Box.createHorizontalStrut(12));
        left.add(dbChip);
        bar.add(left,BorderLayout.WEST);
        if(home){
            GradientButton hb=new GradientButton("Home",new Color(68,78,116),new Color(90,102,148));
            hb.setPreferredSize(new Dimension(115,36));
            hb.addActionListener(e->cardLayout.show(mainPanel,"HOME"));
            JPanel rp=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0)); rp.setOpaque(false); rp.add(hb);
            bar.add(rp,BorderLayout.EAST);
        }
        return bar;
    }
    JPanel buildHome(){
        JPanel root=new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setPaint(new GradientPaint(0,0,new Color(238,243,255),0,getHeight(),new Color(248,242,255)));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(new Color(52,86,230,12)); g2.fillOval(-100,-100,440,440);
                g2.setColor(new Color(228,30,96,8));  g2.fillOval(700,220,420,420);
                g2.setColor(new Color(14,160,96,9));  g2.fillOval(380,380,320,320);
                g2.dispose();
            }
        };
        root.setBackground(BG);
        root.add(navBar("",false),BorderLayout.NORTH);
        JPanel hero=new JPanel(); hero.setLayout(new BoxLayout(hero,BoxLayout.Y_AXIS));
        hero.setOpaque(false); hero.setBorder(BorderFactory.createEmptyBorder(56,76,18,76));
        JLabel badge=new JLabel("  ✈  India's #1 Smart Travel Planner  ");
        badge.setFont(new Font("Segoe UI",Font.BOLD,11)); badge.setForeground(ACCENT1);
        badge.setOpaque(true); badge.setBackground(new Color(52,86,230,22));
        badge.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(52,86,230,60),1,true),
            BorderFactory.createEmptyBorder(5,10,5,10)));
        badge.setAlignmentX(LEFT_ALIGNMENT); hero.add(badge); hero.add(gap(18));
        JLabel h1=new JLabel("<html><div style='font-size:32px;color:#101632'>Plan Your <b style='color:#3456E6'>Perfect</b> Journey</div></html>");
        h1.setAlignmentX(LEFT_ALIGNMENT); hero.add(h1); hero.add(gap(12));
        hero.add(gap(36));
        JPanel btnRow=new JPanel(new FlowLayout(FlowLayout.LEFT,14,0)); btnRow.setOpaque(false); btnRow.setAlignmentX(LEFT_ALIGNMENT);
        GradientButton bookBtn=new GradientButton("  Book a Trip  ",ACCENT1,ACCENT5);
        bookBtn.setPreferredSize(new Dimension(162,46));
        bookBtn.addActionListener(e->cardLayout.show(mainPanel,"CUSTOMER"));
        GradientButton adminBtn=new GradientButton("  Admin Panel  ",new Color(50,54,82),new Color(76,82,118));
        adminBtn.setPreferredSize(new Dimension(162,46));
        adminBtn.addActionListener(e->cardLayout.show(mainPanel,"ADMIN_LOGIN"));
        btnRow.add(bookBtn); btnRow.add(adminBtn);
        hero.add(btnRow); hero.add(gap(42));
        JLabel destLbl=heading("Popular Destinations",16); destLbl.setAlignmentX(LEFT_ALIGNMENT);
        hero.add(destLbl); hero.add(gap(14));
        JPanel destRow=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0)); destRow.setOpaque(false); destRow.setAlignmentX(LEFT_ALIGNMENT);
        Color[][] palettes={{new Color(28,82,218),new Color(80,30,200)},{new Color(198,28,88),new Color(240,80,30)},
                            {new Color(10,135,80),new Color(20,180,130)},{new Color(200,110,0),new Color(240,160,30)},
                            {new Color(110,32,175),new Color(60,90,220)}};
        int ci=0;
        for(Destination d:admin.destinations){
            DestPhotoCard card=new DestPhotoCard(d.city,palettes[ci%palettes.length][0],palettes[ci%palettes.length][1]);
            destRow.add(card); ci++;
        }
        hero.add(destRow);
        root.add(hero,BorderLayout.CENTER);
        JPanel statsBar=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        statsBar.setBackground(SURFACE);
        statsBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0,BORDER),
            BorderFactory.createEmptyBorder(14,76,14,76)));
        String[][] sv={{"5","Source Cities"},{"5","Destinations"},{"25","Routes"},{"MySQL","Database"}};
        Color[] sc={ACCENT1,ACCENT3,ACCENT4,ACCENT2};
        for(int i=0;i<sv.length;i++){
            if(i>0){JPanel div=new JPanel();div.setOpaque(false);div.setPreferredSize(new Dimension(1,50));div.setBorder(BorderFactory.createMatteBorder(0,1,0,0,BORDER));statsBar.add(div);}
            JPanel cp=new JPanel(); cp.setLayout(new BoxLayout(cp,BoxLayout.Y_AXIS)); cp.setOpaque(false); cp.setBorder(BorderFactory.createEmptyBorder(0,22,0,22));
            JLabel num=new JLabel(sv[i][0]); num.setFont(new Font("Segoe UI",Font.BOLD,24)); num.setForeground(sc[i]); num.setAlignmentX(CENTER_ALIGNMENT);
            JLabel lbl=sub(sv[i][1]); lbl.setAlignmentX(CENTER_ALIGNMENT);
            cp.add(num); cp.add(lbl); statsBar.add(cp);
        }
        root.add(statsBar,BorderLayout.SOUTH);
        return root;
    }
    JPanel buildCustomer(){
        JPanel root=new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(navBar("Book a Trip",true),BorderLayout.NORTH);
        JPanel form=card(18); form.setLayout(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(30,38,30,38));
        GridBagConstraints gc=new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(6,6,6,6);
        LightTextField nameF  =new LightTextField("Full name");
        LightTextField phoneF =new LightTextField("Mobile number (10 digits)");
        LightTextField emailF =new LightTextField("Email address");
        LightTextField budgetF=new LightTextField("e.g. 15000");
        LightTextField daysF  =new LightTextField("e.g. 3");
        LightComboBox srcBox  =new LightComboBox(admin.sources.toArray(new String[0]));
        LightComboBox destBox =new LightComboBox(admin.destinations.stream().map(d->d.city).toArray(String[]::new));
        Object[][] rows={
            {"👤  Full Name",       nameF},
            {"📱  Phone",           phoneF},
            {"📧  Email",           emailF},
            {"💰  Budget (₹)",      budgetF},
            {"📅  Days",            daysF},
            {"🛫  From",            srcBox},
            {"🛬  To (Destination)",destBox}
        };
        for(int i=0;i<rows.length;i++){
            gc.gridx=0;gc.gridy=i;gc.weightx=0.28;
            JLabel lbl=new JLabel((String)rows[i][0]);
            lbl.setFont(new Font("Segoe UI",Font.BOLD,12)); lbl.setForeground(TXT2);
            form.add(lbl,gc);
            gc.gridx=1;gc.weightx=0.72;form.add((Component)rows[i][1],gc);
        }
        JTextArea result=new JTextArea(8,30);
        result.setEditable(false); result.setBackground(new Color(244,247,255));
        result.setForeground(TXT1); result.setFont(new Font("Monospaced",Font.PLAIN,13));
        result.setBorder(BorderFactory.createEmptyBorder(12,14,12,14)); result.setLineWrap(true);
        JScrollPane sp=new JScrollPane(result);
        sp.setBorder(new LineBorder(BORDER,1,true));
        gc.gridx=0;gc.gridy=rows.length;gc.gridwidth=2;gc.weightx=1;
        form.add(gap(6),gc);gc.gridy++;
        form.add(sp,gc);gc.gridy++;
        JPanel btnRow=new JPanel(new FlowLayout(FlowLayout.CENTER,12,0)); btnRow.setOpaque(false);
        GradientButton checkBtn  =new GradientButton("Check Trip",   ACCENT1,ACCENT5);
        GradientButton confirmBtn=new GradientButton("Confirm",       ACCENT3,new Color(5,130,95));
        GradientButton pdfBtn    =new GradientButton("Download PDF",  ACCENT4,new Color(195,88,0));
        confirmBtn.setEnabled(false); pdfBtn.setEnabled(false);
        checkBtn.setPreferredSize(new Dimension(148,42));
        confirmBtn.setPreferredSize(new Dimension(140,42));
        pdfBtn.setPreferredSize(new Dimension(156,42));
        btnRow.add(checkBtn);btnRow.add(confirmBtn);btnRow.add(pdfBtn);
        form.add(btnRow,gc);
        final Route[]  selRoute={null};  final double[] selTotal={0};
        final String[] selDest={null},   selSrc={null}, cName={null}, cPhone={null}, cEmail={null};
        final int[]    selDays={1};
        checkBtn.addActionListener(e->{
            confirmBtn.setEnabled(false); pdfBtn.setEnabled(false); selRoute[0]=null;
            try{
                String nm   =nameF.getText().trim();
                String phone=phoneF.getText().trim();
                String email=emailF.getText().trim();
                double bud  =Double.parseDouble(budgetF.getText().trim());
                int    days =Integer.parseInt(daysF.getText().trim());
                if(nm.isEmpty()||nm.equals("Full name")) throw new InvalidBudgetException("Please enter your full name.");
                if(!phone.matches("\\d{10}"))            throw new InvalidBudgetException("Enter a valid 10-digit phone number.");
                if(!email.contains("@"))                 throw new InvalidBudgetException("Enter a valid email address.");
                if(bud<=0)  throw new InvalidBudgetException("Budget must be > 0.");
                if(days<=0) throw new InvalidBudgetException("Days must be at least 1.");
                String src =(String)srcBox.getSelectedItem();
                String dest=(String)destBox.getSelectedItem();
                if(src.equalsIgnoreCase(dest)){result.setText("  Source and destination cannot be the same.");return;}
                Route rt=admin.getRoute(src,dest);
                if(rt==null){result.setText("  No route found from "+src+" to "+dest);return;}
                Destination d=admin.getDestination(dest);
                int booked=countLocal(dest);
                if(booked>=4){result.setText("  ❌ Hotel fully booked at "+dest+".\n  Please choose another destination.");return;}
                double travel=rt.travelCost, hotel=d.hotelPerDay*days, food=d.foodPerDay*days, total=travel+hotel+food;
                StringBuilder sb=new StringBuilder();
                if(bud>=total){
                    sb.append("  ✅  Trip is POSSIBLE within your budget!\n\n");
                    sb.append("  Customer   : ").append(nm).append("\n");
                    sb.append("  Phone      : ").append(phone).append("\n");
                    sb.append("  Email      : ").append(email).append("\n");
                    sb.append("  From       : ").append(src).append("\n");
                    sb.append("  To         : ").append(dest).append("\n");
                    sb.append("  Distance   : ").append(String.format("%.0f",rt.distance)).append(" km\n");
                    sb.append("  Duration   : ").append(days).append(" day").append(days>1?"s":"").append("\n");
                    sb.append("  ─────────────────────────────────────────\n");
                    sb.append("  Travel     : ₹").append(String.format("%.0f",travel)).append("\n");
                    sb.append("  Hotel      : ₹").append(String.format("%.0f",hotel))
                      .append("  (").append(days).append("d × ₹").append(d.hotelPerDay).append(")\n");
                    sb.append("  Food       : ₹").append(String.format("%.0f",food))
                      .append("  (").append(days).append("d × ₹").append(d.foodPerDay).append(")\n");
                    sb.append("  ─────────────────────────────────────────\n");
                    sb.append("  TOTAL      : ₹").append(String.format("%.0f",total)).append("\n");
                    sb.append("  Remaining  : ₹").append(String.format("%.0f",bud-total)).append("\n\n");
                    sb.append("  Press 'Confirm' to complete your booking.");
                    selRoute[0]=rt;selTotal[0]=total;selDest[0]=dest;
                    selSrc[0]=src;cName[0]=nm;cPhone[0]=phone;cEmail[0]=email;selDays[0]=days;
                    confirmBtn.setEnabled(true);
                }else{
                    sb.append("  ❌  Budget insufficient for ").append(dest)
                      .append("  (₹").append(String.format("%.0f",total)).append(" needed)\n\n");
                    sb.append("  💡  Alternatives within ₹").append(String.format("%.0f",bud)).append(":\n\n");
                    boolean found=false;
                    for(Destination des:admin.destinations){
                        Route r=admin.getRoute(src,des.city); if(r==null) continue;
                        double tot=r.travelCost+des.hotelPerDay*days+des.foodPerDay*days;
                        int bk=countLocal(des.city);
                        if(bud>=tot&&bk<4){
                            sb.append("  ➤ ").append(src).append(" → ").append(des.city)
                              .append("   ₹").append(String.format("%.0f",tot))
                              .append("  (").append(String.format("%.0f",r.distance)).append(" km)\n");
                            found=true;
                        }
                    }
                    if(!found) sb.append("  No alternatives found within your budget.");
                }
                result.setText(sb.toString());
            }catch(InvalidBudgetException ex){result.setText("  ⚠  "+ex.getMessage());
            }catch(NumberFormatException ex){result.setText("  ⚠  Please enter valid numbers for Budget and Days.");}
        });
        confirmBtn.addActionListener(e->{
            if(selRoute[0]==null) return;
            Booking b=new Booking(cName[0],cPhone[0],cEmail[0],selSrc[0],selDest[0],
                selRoute[0].distance,selTotal[0],selDays[0]);
            if(dbOk) DB.insertBooking(b);
            localBookings.add(b);
            if(!dbOk) saveLocal();
            lastBooking=b; lastDest=admin.getDestination(selDest[0]);
            confirmBtn.setEnabled(false); pdfBtn.setEnabled(true);
            result.setText("  🎉  Booking Confirmed!\n\n"+
                "  Booking ID : "+b.bookingId+"\n"+
                "  Customer   : "+cName[0]+"\n"+
                "  Phone      : "+cPhone[0]+"\n"+
                "  Email      : "+cEmail[0]+"\n"+
                "  Route      : "+selSrc[0]+" → "+selDest[0]+"\n"+
                "  Duration   : "+selDays[0]+" day"+(selDays[0]>1?"s":"")+"\n"+
                "  Total Paid : ₹"+String.format("%.0f",selTotal[0])+"\n"+
                "  Date       : "+b.bookingDate+"\n\n"+
                "  ✈  Have a wonderful trip!\n"+
                "  📄  Click 'Download PDF' to save your booking.");
            refreshAdmin();
            JOptionPane.showMessageDialog(this,
                "Booking confirmed!\nID: "+b.bookingId+"\nDestination: "+selDest[0]+
                "\nTotal: ₹"+String.format("%.0f",selTotal[0]),
                "SmartVoyage — Confirmed",JOptionPane.INFORMATION_MESSAGE);
            nameF.reset(); phoneF.reset(); emailF.reset();
            budgetF.reset(); daysF.reset();
            result.setText("");
            selRoute[0]=null;
        });
        pdfBtn.addActionListener(e->{ if(lastBooking!=null) saveBookingPDF(lastBooking,lastDest); });
        JPanel center=new JPanel(new GridBagLayout()); center.setBackground(BG);
        center.setBorder(BorderFactory.createEmptyBorder(24,46,24,46));
        GridBagConstraints cc=new GridBagConstraints();
        cc.fill=GridBagConstraints.BOTH; cc.weightx=1; cc.weighty=1;
        center.add(form,cc);
        root.add(center,BorderLayout.CENTER);
        return root;
    }
    JPanel buildAdminLogin(){
        JPanel root=new JPanel(new BorderLayout()); root.setBackground(BG);
        JPanel loginCard=card(22); loginCard.setLayout(new BoxLayout(loginCard,BoxLayout.Y_AXIS));
        loginCard.setBorder(BorderFactory.createEmptyBorder(44,54,44,54));
        loginCard.setPreferredSize(new Dimension(420,340));
        JLabel icon=new JLabel("🔐  Admin Login",SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI",Font.BOLD,22)); icon.setForeground(TXT1); icon.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sub2=sub("Enter your credentials to continue.");
        sub2.setAlignmentX(CENTER_ALIGNMENT);
        JLabel err=new JLabel(" "); err.setForeground(ACCENT2);
        err.setFont(new Font("Segoe UI",Font.PLAIN,12)); err.setAlignmentX(CENTER_ALIGNMENT);
        LightTextField userF=new LightTextField("Username");
        userF.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        JPasswordField passF=new JPasswordField();
        passF.setBackground(SURFACE); passF.setForeground(TXT1); passF.setCaretColor(ACCENT1);
        passF.setFont(new Font("Segoe UI",Font.PLAIN,13));
        passF.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER,1,true),BorderFactory.createEmptyBorder(8,12,8,12)));
        passF.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        GradientButton loginBtn=new GradientButton("Login",ACCENT1,ACCENT5);
        loginBtn.setAlignmentX(CENTER_ALIGNMENT); loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        GradientButton backBtn=new GradientButton("Back",new Color(68,76,112),new Color(92,104,148));
        backBtn.setAlignmentX(CENTER_ALIGNMENT); backBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));
        backBtn.addActionListener(e->cardLayout.show(mainPanel,"HOME"));
        ActionListener doLogin=e->{
            if(admin.login(userF.getText(),new String(passF.getPassword()))){
                refreshAdmin();
                cardLayout.show(mainPanel,"ADMIN_DASH");
                userF.reset(); passF.setText(""); err.setText(" ");
            }else{
                err.setText("  Invalid credentials. Please try again.");
                passF.setText("");
            }
        };
        loginBtn.addActionListener(doLogin); passF.addActionListener(doLogin);
        loginCard.add(icon);    loginCard.add(gap(6));
        loginCard.add(sub2);    loginCard.add(gap(4));
        loginCard.add(err);     loginCard.add(gap(18));
        loginCard.add(sub("Username")); loginCard.add(gap(4));
        loginCard.add(userF);   loginCard.add(gap(12));
        loginCard.add(sub("Password")); loginCard.add(gap(4));
        loginCard.add(passF);   loginCard.add(gap(24));
        loginCard.add(loginBtn);loginCard.add(gap(10));
        loginCard.add(backBtn);
        JPanel wrap=new JPanel(new GridBagLayout()); wrap.setBackground(BG); wrap.add(loginCard);
        root.add(wrap,BorderLayout.CENTER);
        return root;
    }
    JPanel buildAdminDash(){
        JPanel root=new JPanel(new BorderLayout()); root.setBackground(BG);
        JPanel bar=new JPanel(new BorderLayout()); bar.setBackground(SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,BORDER),
            BorderFactory.createEmptyBorder(14,28,14,28)));
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); left.setOpaque(false);
        JLabel dot=new JLabel("●"); dot.setFont(new Font("Segoe UI",Font.BOLD,18)); dot.setForeground(ACCENT1);
        JLabel logo=new JLabel("SmartVoyage"); logo.setFont(new Font("Segoe UI",Font.BOLD,20)); logo.setForeground(TXT1);
        JLabel pg=new JLabel("  /  Admin Dashboard"); pg.setFont(new Font("Segoe UI",Font.PLAIN,14)); pg.setForeground(TXT2);
        left.add(dot);left.add(logo);left.add(pg); bar.add(left,BorderLayout.WEST);
        GradientButton out=new GradientButton("Logout",new Color(188,28,58),new Color(220,48,78));
        out.setPreferredSize(new Dimension(130,36));
        out.addActionListener(e->cardLayout.show(mainPanel,"HOME"));
        JPanel rp=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); rp.setOpaque(false); rp.add(out);
        bar.add(rp,BorderLayout.EAST);
        root.add(bar,BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane();
        tabs.setFont(new Font("Segoe UI",Font.BOLD,13));
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0,0,0,0));
        tabs.setBackground(BG); tabs.setForeground(TXT1);
        tabs.setBorder(BorderFactory.createEmptyBorder(8,14,8,14));
        tabs.addTab("  Overview",         buildOverviewTab());
        tabs.addTab("  Tracking Log",     buildTrackingTab());
        tabs.addTab("  Destinations",     buildDestinationsTab());
        tabs.addTab("  Sources & Routes", buildSourcesTab());
        tabs.addTab("  Checkout",         buildCheckoutTab());
        root.add(tabs,BorderLayout.CENTER);
        return root;
    }
    JPanel buildOverviewTab(){
        JPanel content=new JPanel(); content.setBackground(BG);
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(22,22,22,22));
        JPanel statRow=new JPanel(new GridLayout(1,3,14,0));
        statRow.setOpaque(false); statRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,105));
        statRow.setAlignmentX(LEFT_ALIGNMENT);
        String[][] sd={{"0","Total Bookings"},{"5","Destinations"},{"5","Source Cities"}};
        Color[]    sc={ACCENT1,ACCENT3,ACCENT4};
        for(int i=0;i<3;i++){
            JPanel c2=card(14); c2.setLayout(new BorderLayout(10,0));
            c2.setBorder(BorderFactory.createEmptyBorder(18,22,18,22));
            JPanel txt=new JPanel(); txt.setOpaque(false); txt.setLayout(new BoxLayout(txt,BoxLayout.Y_AXIS));
            JLabel num=new JLabel(sd[i][0]); num.setFont(new Font("Segoe UI",Font.BOLD,32)); num.setForeground(sc[i]);
            txt.add(num); txt.add(sub(sd[i][1])); c2.add(txt,BorderLayout.CENTER);
            if(i==0) totalTripsLbl=num;
            statRow.add(c2);
        }
        content.add(statRow); content.add(gap(22));
        JLabel bh=heading("Hotel Booking Status",15); bh.setAlignmentX(LEFT_ALIGNMENT);
        content.add(bh); content.add(gap(12));
        destCardsPanel=new JPanel(); destCardsPanel.setOpaque(false);
        destCardsPanel.setLayout(new BoxLayout(destCardsPanel,BoxLayout.X_AXIS));
        destCardsPanel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(destCardsPanel); content.add(gap(22));
        JLabel th=heading("Recent Bookings",15); th.setAlignmentX(LEFT_ALIGNMENT);
        content.add(th); content.add(gap(12));
        String[] cols={"Booking ID","Customer","Phone","Route","Days","Total","Date","Status"};
        bookingModel=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        JTable tbl=styledTable(bookingModel);
        JScrollPane sp=new JScrollPane(tbl);
        sp.setBorder(new LineBorder(BORDER,1,true)); sp.getViewport().setBackground(SURFACE);
        sp.setAlignmentX(LEFT_ALIGNMENT); sp.setMaximumSize(new Dimension(Integer.MAX_VALUE,280));
        content.add(sp);
        JScrollPane outer=new JScrollPane(content); outer.setBorder(null);
        outer.getViewport().setBackground(BG);
        JPanel wrap=new JPanel(new BorderLayout()); wrap.setBackground(BG); wrap.add(outer,BorderLayout.CENTER);
        return wrap;
    }
    JPanel buildTrackingTab(){
        JPanel content=new JPanel(new BorderLayout()); content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(22,22,22,22));
        JPanel top=new JPanel(); top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS)); top.setOpaque(false);
        JLabel title=heading("  📋  Customer Tracking Log",16); title.setAlignmentX(LEFT_ALIGNMENT);
        top.add(title); top.add(gap(6));
        top.add(sub("Full history of all bookings with customer details, timestamps and status.")); top.add(gap(18));
        JPanel filterRow=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); filterRow.setOpaque(false);
        filterRow.setAlignmentX(LEFT_ALIGNMENT);
        LightTextField filterF=new LightTextField("Search by name, destination, booking ID…");
        filterF.setPreferredSize(new Dimension(320,36));
        GradientButton refreshBtn=new GradientButton("Refresh",ACCENT1,ACCENT5);
        refreshBtn.setPreferredSize(new Dimension(110,36));
        GradientButton exportPdfBtn=new GradientButton("Export Report PDF",ACCENT4,new Color(195,88,0));
        exportPdfBtn.setPreferredSize(new Dimension(200,36));
        filterRow.add(filterF); filterRow.add(refreshBtn); filterRow.add(exportPdfBtn);
        top.add(filterRow); top.add(gap(14));
        content.add(top,BorderLayout.NORTH);
        String[] cols={"#","Booking ID","Customer Name","Phone","Email","From","To","Days","Total (₹)","Booked On","Status"};
        logModel=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        JTable tbl=styledTable(logModel);
        tbl.setAutoCreateRowSorter(false);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(36);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(90);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(130);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(105);
        tbl.getColumnModel().getColumn(4).setPreferredWidth(160);
        tbl.getColumnModel().getColumn(5).setPreferredWidth(100);
        tbl.getColumnModel().getColumn(6).setPreferredWidth(100);
        tbl.getColumnModel().getColumn(9).setPreferredWidth(155);
        tbl.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(
                    JTable t,Object v,boolean sel,boolean foc,int row,int col){
                super.getTableCellRendererComponent(t,v,sel,foc,row,col);
                String s=v==null?"":v.toString();
                setForeground("CONFIRMED".equals(s)?ACCENT3:TXT2);
                setFont(new Font("Segoe UI",Font.BOLD,12));
                setBorder(BorderFactory.createEmptyBorder(0,12,0,12));
                return this;
            }
        });
        JScrollPane sp=new JScrollPane(tbl);
        sp.setBorder(new LineBorder(BORDER,1,true)); sp.getViewport().setBackground(SURFACE);
        JPanel detail=new JPanel(new BorderLayout(10,6));
        detail.setBackground(SURFACE);
        detail.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER,1,true),
            BorderFactory.createEmptyBorder(14,18,14,18)));
        detail.setPreferredSize(new Dimension(0,100));
        JPanel detailLeft=new JPanel(); detailLeft.setOpaque(false);
        detailLeft.setLayout(new BoxLayout(detailLeft,BoxLayout.Y_AXIS));
        JLabel detailTitle=heading("Select a row to view customer details and download PDF",13);
        detailTitle.setAlignmentX(LEFT_ALIGNMENT);
        JLabel detailBody=sub(" "); detailBody.setAlignmentX(LEFT_ALIGNMENT);
        detailLeft.add(detailTitle); detailLeft.add(Box.createVerticalStrut(6)); detailLeft.add(detailBody);
        GradientButton rowPdfBtn=new GradientButton("Download PDF",ACCENT4,new Color(195,88,0));
        rowPdfBtn.setPreferredSize(new Dimension(170,40));
        rowPdfBtn.setEnabled(false);
        detail.add(detailLeft,BorderLayout.CENTER);
        detail.add(rowPdfBtn,BorderLayout.EAST);
        tbl.getSelectionModel().addListSelectionListener(ev->{
            if(ev.getValueIsAdjusting()) return;
            int row=tbl.getSelectedRow();
            if(row<0){rowPdfBtn.setEnabled(false);detailTitle.setText("Select a row to view customer details and download PDF");detailBody.setText(" ");return;}
            String id   =logModel.getValueAt(row,1).toString();
            String name =logModel.getValueAt(row,2).toString();
            String phone=logModel.getValueAt(row,3).toString();
            String email=logModel.getValueAt(row,4).toString();
            String from =logModel.getValueAt(row,5).toString();
            String to   =logModel.getValueAt(row,6).toString();
            String days =logModel.getValueAt(row,7).toString();
            String total=logModel.getValueAt(row,8).toString();
            String date =logModel.getValueAt(row,9).toString();
            String stat =logModel.getValueAt(row,10).toString();
            detailTitle.setText("Customer: "+name+"   |   Booking ID: "+id);
            detailBody.setText("Phone: "+phone+"   Email: "+email+"   Route: "+from+" to "+to+
                "   Days: "+days+"   Total: Rs."+total+"   Date: "+date+"   Status: "+stat);
            rowPdfBtn.setEnabled(true);
        });
        rowPdfBtn.addActionListener(e->{
            int row=tbl.getSelectedRow();
            if(row<0){
                JOptionPane.showMessageDialog(null,"Please click on a row in the table first.","No Row Selected",JOptionPane.WARNING_MESSAGE);
                return;
            }
            try{
                String id   =logModel.getValueAt(row,1).toString();
                String name =logModel.getValueAt(row,2).toString();
                String phone=logModel.getValueAt(row,3).toString();
                String email=logModel.getValueAt(row,4).toString();
                String from =logModel.getValueAt(row,5).toString();
                String to   =logModel.getValueAt(row,6).toString();
                int days    =Integer.parseInt(logModel.getValueAt(row,7).toString().replace("d","").trim());
                double total=Double.parseDouble(logModel.getValueAt(row,8).toString().replace(",","").replace("₹","").replace("Rs.","").trim());
                String date =logModel.getValueAt(row,9).toString();
                String stat =logModel.getValueAt(row,10).toString();
                Booking b=new Booking(id,name,phone,email,from,to,0,total,days,date,stat);
                saveBookingPDF(b,admin.getDestination(to));
            }catch(Exception ex){
                JOptionPane.showMessageDialog(null,"Error preparing PDF: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
        JPanel mainArea=new JPanel(new BorderLayout(0,12)); mainArea.setBackground(BG);
        mainArea.add(sp,BorderLayout.CENTER);
        mainArea.add(detail,BorderLayout.SOUTH);
        content.add(mainArea,BorderLayout.CENTER);
        refreshBtn.addActionListener(e->refreshLogTable(logModel,null));
        filterF.addKeyListener(new KeyAdapter(){
            @Override public void keyReleased(KeyEvent e){
                refreshLogTable(logModel,filterF.getText().trim());
            }
        });
        exportPdfBtn.addActionListener(e->exportLogPDF());
        return content;
    }
    void refreshLogTable(DefaultTableModel m, String filter){
        m.setRowCount(0);
        List<Booking> all=dbOk?DB.loadAllBookings():localBookings;
        int idx=1;
        for(Booking b:all){
            if(filter!=null&&!filter.isEmpty()){
                String q=filter.toLowerCase();
                if(!b.customerName.toLowerCase().contains(q)&&
                   !b.destination.toLowerCase().contains(q)&&
                   !b.bookingId.toLowerCase().contains(q)&&
                   !(b.customerPhone!=null&&b.customerPhone.contains(q))) continue;
            }
            m.addRow(new Object[]{idx++,b.bookingId,b.customerName,
                b.customerPhone==null?"—":b.customerPhone,
                b.customerEmail==null?"—":b.customerEmail,
                b.source,b.destination,b.days,
                String.format("%.0f",b.totalCost),b.bookingDate,b.status});
        }
    }
    JPanel buildDestinationsTab(){
        JPanel content=new JPanel(); content.setBackground(BG);
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(22,22,22,22));
        JLabel title=heading("  All Destinations",16); title.setAlignmentX(LEFT_ALIGNMENT);
        content.add(title); content.add(gap(6));
        content.add(sub("Bookable destinations with per-day hotel and food costs.")); content.add(gap(18));
        destTabCardsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,14,10)); destTabCardsPanel.setOpaque(false); destTabCardsPanel.setAlignmentX(LEFT_ALIGNMENT);
        JPanel cardRow=destTabCardsPanel;
        Color[][] palettes={{new Color(28,82,218),new Color(80,30,200)},{new Color(198,28,88),new Color(240,80,30)},
                            {new Color(10,135,80),new Color(20,180,130)},{new Color(200,110,0),new Color(240,160,30)},
                            {new Color(110,32,175),new Color(60,90,220)}};
        int ei=0;
        for(Destination d:admin.destinations){
            JPanel dc=card(14); dc.setLayout(new BoxLayout(dc,BoxLayout.Y_AXIS));
            dc.setBorder(BorderFactory.createEmptyBorder(0,0,16,0)); dc.setPreferredSize(new Dimension(190,220));
            DestPhotoCard ph=new DestPhotoCard(d.city,palettes[ei%palettes.length][0],palettes[ei%palettes.length][1]);
            ph.setPreferredSize(new Dimension(190,125)); ph.setMaximumSize(new Dimension(Integer.MAX_VALUE,125));
            JPanel info=new JPanel(); info.setOpaque(false); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS));
            info.setBorder(BorderFactory.createEmptyBorder(10,14,0,14));
            int cnt=countLocal(d.city);
            JLabel h=sub("Hotel: Rs."+String.format("%.0f",d.hotelPerDay)+"/night");
            JLabel f=sub("Food:  Rs."+String.format("%.0f",d.foodPerDay)+"/day");
            JLabel bk=new JLabel(cnt+"/4 rooms booked");
            bk.setFont(new Font("Segoe UI",Font.BOLD,11)); bk.setForeground(cnt>=4?ACCENT2:ACCENT3);
            info.add(h);info.add(gap(3));info.add(f);info.add(gap(5));info.add(bk);
            dc.add(ph);dc.add(info); cardRow.add(dc); ei++;
        }
        content.add(cardRow); content.add(gap(24));
        JLabel tTitle=heading("Pricing Table",15); tTitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(tTitle); content.add(gap(12));
        String[] cols={"#","City","Hotel/Night","Food/Day","Total/Day","Slots Left"};
        destTabModel=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        int idx=1;
        for(Destination d:admin.destinations){
            int cnt=countLocal(d.city);
            destTabModel.addRow(new Object[]{idx++,d.city,
                "Rs."+String.format("%.0f",d.hotelPerDay),"Rs."+String.format("%.0f",d.foodPerDay),
                "Rs."+String.format("%.0f",d.hotelPerDay+d.foodPerDay),(4-cnt)+" remaining"});
        }
        JTable tbl=styledTable(destTabModel);
        JScrollPane sp=new JScrollPane(tbl);
        sp.setBorder(new LineBorder(BORDER,1,true)); sp.getViewport().setBackground(SURFACE);
        sp.setAlignmentX(LEFT_ALIGNMENT); sp.setMaximumSize(new Dimension(Integer.MAX_VALUE,200));
        content.add(sp);
        JScrollPane outer=new JScrollPane(content); outer.setBorder(null); outer.getViewport().setBackground(BG);
        JPanel wrap=new JPanel(new BorderLayout()); wrap.setBackground(BG); wrap.add(outer,BorderLayout.CENTER);
        return wrap;
    }
    JPanel buildSourcesTab(){
        JPanel content=new JPanel(); content.setBackground(BG);
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(22,22,22,22));
        JLabel title=heading("  Source Cities & Routes",16); title.setAlignmentX(LEFT_ALIGNMENT);
        content.add(title); content.add(gap(6));
        content.add(sub("All departure cities with available routes to destinations.")); content.add(gap(18));
        Color[] clrs={ACCENT1,ACCENT3,ACCENT4,ACCENT2,ACCENT5};
        JPanel cardGrid=new JPanel(new FlowLayout(FlowLayout.LEFT,14,14)); cardGrid.setOpaque(false); cardGrid.setAlignmentX(LEFT_ALIGNMENT);
        int ci=0;
        for(String src:admin.sources){
            final Color col=clrs[ci%clrs.length];
            JPanel sc=new JPanel(){
                @Override protected void paintComponent(Graphics g){
                    Graphics2D g2=(Graphics2D)g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0,0,0,14)); g2.fillRoundRect(3,5,getWidth()-3,getHeight()-3,16,16);
                    g2.setColor(SURFACE); g2.fillRoundRect(0,0,getWidth()-2,getHeight()-4,16,16);
                    g2.setColor(col); g2.fillRoundRect(0,0,getWidth()-2,7,16,16); g2.fillRect(0,4,getWidth()-2,6);
                    g2.dispose(); super.paintComponent(g);
                }
            };
            sc.setOpaque(false); sc.setLayout(new BoxLayout(sc,BoxLayout.Y_AXIS));
            sc.setBorder(BorderFactory.createEmptyBorder(20,18,16,18)); sc.setPreferredSize(new Dimension(212,230));
            JLabel cityLbl=new JLabel(src); cityLbl.setFont(new Font("Segoe UI",Font.BOLD,17)); cityLbl.setForeground(col); cityLbl.setAlignmentX(LEFT_ALIGNMENT);
            sc.add(cityLbl); sc.add(gap(12));
            JLabel rh=new JLabel("Routes:"); rh.setFont(new Font("Segoe UI",Font.BOLD,11)); rh.setForeground(TXT2); rh.setAlignmentX(LEFT_ALIGNMENT);
            sc.add(rh); sc.add(gap(5));
            for(Destination d:admin.destinations){
                Route r=admin.getRoute(src,d.city); if(r==null) continue;
                JLabel rl=new JLabel("  ➤ "+d.city+"  ("+String.format("%.0f",r.distance)+" km · ₹"+String.format("%.0f",r.travelCost)+")");
                rl.setFont(new Font("Segoe UI",Font.PLAIN,12)); rl.setForeground(TXT2); rl.setAlignmentX(LEFT_ALIGNMENT);
                sc.add(rl);
            }
            cardGrid.add(sc); ci++;
        }
        content.add(cardGrid); content.add(gap(20));
        String[] cols={"Source","Destination","Distance (km)","Travel Cost (₹)"};
        DefaultTableModel model=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        for(Route r:admin.routes)
            model.addRow(new Object[]{r.source,r.destination,String.format("%.0f",r.distance)+" km","₹"+String.format("%.0f",r.travelCost)});
        JTable tbl=styledTable(model);
        JScrollPane sp=new JScrollPane(tbl);
        sp.setBorder(new LineBorder(BORDER,1,true)); sp.getViewport().setBackground(SURFACE);
        sp.setAlignmentX(LEFT_ALIGNMENT); sp.setMaximumSize(new Dimension(Integer.MAX_VALUE,320));
        content.add(sp);
        JScrollPane outer=new JScrollPane(content); outer.setBorder(null); outer.getViewport().setBackground(BG);
        JPanel wrap=new JPanel(new BorderLayout()); wrap.setBackground(BG); wrap.add(outer,BorderLayout.CENTER);
        return wrap;
    }
    JPanel buildCheckoutTab(){
        JPanel content=new JPanel(new BorderLayout()); content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(22,22,22,22));
        JPanel top=new JPanel(); top.setBackground(BG); top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
        JLabel title=heading("  🚪  Customer Checkout",16); title.setAlignmentX(LEFT_ALIGNMENT);
        top.add(title); top.add(gap(6));
        top.add(sub("Search by customer name and checkout to free up the hotel slot.")); top.add(gap(18));
        JPanel sc=card(16); sc.setLayout(new GridBagLayout());
        sc.setBorder(BorderFactory.createEmptyBorder(24,28,24,28));
        GridBagConstraints gc=new GridBagConstraints();
        gc.fill=GridBagConstraints.HORIZONTAL; gc.insets=new Insets(6,6,6,6);
        LightTextField searchF=new LightTextField("Enter customer name to search");
        searchF.setPreferredSize(new Dimension(300,40));
        gc.gridx=0;gc.gridy=0;gc.weightx=0.15;
        JLabel sl=new JLabel("Customer Name"); sl.setFont(new Font("Segoe UI",Font.BOLD,12)); sl.setForeground(TXT2);
        sc.add(sl,gc);
        gc.gridx=1;gc.weightx=0.7; sc.add(searchF,gc);
        GradientButton searchBtn=new GradientButton("Search",ACCENT1,ACCENT5);
        searchBtn.setPreferredSize(new Dimension(126,40));
        gc.gridx=2;gc.weightx=0.15; sc.add(searchBtn,gc);
        String[] cols={"Booking ID","Customer","Phone","Email","Route","Days","Total","Date"};
        DefaultTableModel cm=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        JTable ct=styledTable(cm); ct.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp=new JScrollPane(ct);
        sp.setBorder(new LineBorder(BORDER,1,true)); sp.getViewport().setBackground(SURFACE);
        sp.setPreferredSize(new Dimension(Integer.MAX_VALUE,240));
        gc.gridx=0;gc.gridy=1;gc.gridwidth=3;gc.weightx=1; sc.add(gap(8),gc); gc.gridy=2; sc.add(sp,gc); gc.gridy=3;
        JLabel rc=sub("No results yet. Search a customer name above."); sc.add(rc,gc); gc.gridy=4;
        JPanel brow=new JPanel(new FlowLayout(FlowLayout.CENTER,12,0)); brow.setOpaque(false);
        GradientButton coBtn=new GradientButton("Checkout Selected",ACCENT2,new Color(160,18,78));
        coBtn.setPreferredSize(new Dimension(220,44)); coBtn.setEnabled(false);
        GradientButton clrBtn=new GradientButton("Clear",new Color(78,84,110),new Color(100,108,140));
        clrBtn.setPreferredSize(new Dimension(98,44));
        brow.add(coBtn); brow.add(clrBtn); sc.add(brow,gc);
        searchBtn.addActionListener(e->{
            String q=searchF.getText().trim().toLowerCase();
            cm.setRowCount(0); coBtn.setEnabled(false);
            if(q.isEmpty()||q.equals("enter customer name to search")){rc.setText("  Please enter a name.");return;}
            List<Booking> all=dbOk?DB.loadActiveBookings():localBookings;
            int found=0;
            for(Booking b:all){
                if(b.customerName.toLowerCase().contains(q)){
                    cm.addRow(new Object[]{b.bookingId,b.customerName,
                        b.customerPhone==null?"—":b.customerPhone,
                        b.customerEmail==null?"—":b.customerEmail,
                        b.source+"→"+b.destination,b.days+"d",
                        "Rs."+String.format("%.0f",b.totalCost),b.bookingDate});
                    found++;
                }
            }
            if(found==0) rc.setText("  No active bookings found for \""+q+"\".");
            else{rc.setText("  Found "+found+" booking(s). Select one and click Checkout.");coBtn.setEnabled(true);}
        });
        searchF.addActionListener(searchBtn.getActionListeners()[0]);
        clrBtn.addActionListener(e->{cm.setRowCount(0);searchF.reset();rc.setText("No results yet.");coBtn.setEnabled(false);});
        coBtn.addActionListener(e->{
            int row=ct.getSelectedRow();
            if(row<0){JOptionPane.showMessageDialog(this,"Please select a row first.","No Selection",JOptionPane.WARNING_MESSAGE);return;}
            String bid =(String)cm.getValueAt(row,0);
            String cnm =(String)cm.getValueAt(row,1);
            String rt  =(String)cm.getValueAt(row,4);
            int confirm=JOptionPane.showConfirmDialog(this,
                "Checkout customer: "+cnm+"\nBooking ID: "+bid+"\nRoute: "+rt+
                "\n\nThis removes the booking and frees a hotel slot.\nAre you sure?",
                "Confirm Checkout",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
            if(confirm==JOptionPane.YES_OPTION){
                if(dbOk) DB.checkoutBooking(bid);
                else { for(Booking bk:localBookings) if(bk.bookingId.equals(bid)){ bk.status="CHECKED_OUT"; break; } if(!dbOk) saveLocal(); }
                refreshAdmin();
                cm.setRowCount(0);
                String q=searchF.getText().trim().toLowerCase();
                List<Booking> all=dbOk?DB.loadActiveBookings():localBookings;
                int found=0;
                for(Booking b:all){
                    if(b.customerName.toLowerCase().contains(q)){
                        cm.addRow(new Object[]{b.bookingId,b.customerName,
                            b.customerPhone==null?"—":b.customerPhone,
                            b.customerEmail==null?"—":b.customerEmail,
                            b.source+"→"+b.destination,b.days+"d",
                            "Rs."+String.format("%.0f",b.totalCost),b.bookingDate});
                        found++;
                    }
                }
                coBtn.setEnabled(found>0);
                rc.setText("  ✅ Checked out. "+found+" booking(s) remain in search.");
                JOptionPane.showMessageDialog(this,
                    cnm+" has been checked out.\nBooking ID "+bid+" is now marked CHECKED_OUT.",
                    "Checkout Successful",JOptionPane.INFORMATION_MESSAGE);
            }
        });
        top.add(sc); top.add(gap(14));
        JPanel note=new RoundedPanel(12,new Color(255,248,220));
        note.setLayout(new FlowLayout(FlowLayout.LEFT,10,10));
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE,56));
        note.add(new JLabel("Info:"));
        JLabel nt=new JLabel("<html><b style='color:#8B6914'>Note:</b> <span style='color:#7A5C10'>Checking out marks the booking CHECKED_OUT in MySQL and frees the hotel slot for new bookings.</span></html>");
        nt.setFont(new Font("Segoe UI",Font.PLAIN,12)); note.add(nt);
        top.add(note);
        JScrollPane outer=new JScrollPane(top); outer.setBorder(null); outer.getViewport().setBackground(BG);
        content.add(outer,BorderLayout.CENTER);
        return content;
    }
    /** Count confirmed bookings for a city - uses DB if available, else localBookings */
    static final String LOCAL_FILE = "smartvoyage_local.dat";

    void saveLocal(){
        try(PrintWriter pw=new PrintWriter(new FileWriter(LOCAL_FILE))){
            for(Booking b:localBookings){
                pw.println(b.bookingId+"|"+b.customerName+"|"+b.customerPhone+"|"+
                    b.customerEmail+"|"+b.source+"|"+b.destination+"|"+
                    b.distance+"|"+b.totalCost+"|"+b.days+"|"+b.bookingDate+"|"+b.status);
            }
        }catch(IOException e){ e.printStackTrace(); }
    }

    void loadLocal(){
        File f=new File(LOCAL_FILE);
        if(!f.exists()) return;
        try(BufferedReader br=new BufferedReader(new FileReader(f))){
            String line;
            while((line=br.readLine())!=null){
                String[] p=line.split("\\|",-1);
                if(p.length>=11){
                    Booking b=new Booking(p[0],p[1],p[2],p[3],p[4],p[5],
                        Double.parseDouble(p[6]),Double.parseDouble(p[7]),
                        Integer.parseInt(p[8]),p[9],p[10]);
                    localBookings.add(b);
                }
            }
        }catch(IOException e){ e.printStackTrace(); }
    }

    int countLocal(String city){
        if(dbOk) return DB.countBookings(city);
        int cnt=0;
        for(Booking b:localBookings)
            if(b.destination.equalsIgnoreCase(city)&&"CONFIRMED".equals(b.status)) cnt++;
        return cnt;
    }
    void refreshAdmin(){
        List<Booking> bookings=dbOk?DB.loadActiveBookings():localBookings;
        if(totalTripsLbl!=null) totalTripsLbl.setText(String.valueOf(bookings.size()));
        if(destCardsPanel!=null){
            destCardsPanel.removeAll();
            for(Destination d:admin.destinations){
                int cnt=countLocal(d.city);
                JPanel dc=card(12); dc.setLayout(new BoxLayout(dc,BoxLayout.Y_AXIS));
                dc.setBorder(BorderFactory.createEmptyBorder(12,16,12,16)); dc.setPreferredSize(new Dimension(168,88));
                JLabel city=new JLabel(d.city); city.setFont(new Font("Segoe UI",Font.BOLD,13)); city.setForeground(TXT1); city.setAlignmentX(CENTER_ALIGNMENT);
                JProgressBar pb=new JProgressBar(0,4); pb.setValue(cnt); pb.setStringPainted(true);
                pb.setString(cnt+"/4 booked"); pb.setFont(new Font("Segoe UI",Font.BOLD,11));
                pb.setForeground(cnt>=4?ACCENT2:ACCENT3); pb.setBackground(new Color(226,232,252));
                pb.setBorder(BorderFactory.createEmptyBorder()); pb.setMaximumSize(new Dimension(Integer.MAX_VALUE,18));
                dc.add(city);dc.add(gap(6));dc.add(pb);
                destCardsPanel.add(dc); destCardsPanel.add(Box.createHorizontalStrut(10));
            }
            destCardsPanel.revalidate(); destCardsPanel.repaint();
        }
        if(bookingModel!=null){
            bookingModel.setRowCount(0);
            for(Booking b:bookings)
                bookingModel.addRow(new Object[]{b.bookingId,b.customerName,
                    b.customerPhone==null?"—":b.customerPhone,
                    b.source+"→"+b.destination,b.days+"d",
                    "Rs."+String.format("%.0f",b.totalCost),b.bookingDate,b.status});
        }
        if(logModel!=null) refreshLogTable(logModel,null);
        if(destTabCardsPanel!=null){
            destTabCardsPanel.removeAll();
            Color[][] palettes={{new Color(28,82,218),new Color(80,30,200)},{new Color(198,28,88),new Color(240,80,30)},
                                {new Color(10,135,80),new Color(20,180,130)},{new Color(200,110,0),new Color(240,160,30)},
                                {new Color(110,32,175),new Color(60,90,220)}};
            int ei2=0;
            for(Destination d:admin.destinations){
                int cnt=countLocal(d.city);
                JPanel dc=card(14); dc.setLayout(new BoxLayout(dc,BoxLayout.Y_AXIS));
                dc.setBorder(BorderFactory.createEmptyBorder(0,0,16,0)); dc.setPreferredSize(new Dimension(190,220));
                DestPhotoCard ph=new DestPhotoCard(d.city,palettes[ei2%palettes.length][0],palettes[ei2%palettes.length][1]);
                ph.setPreferredSize(new Dimension(190,125)); ph.setMaximumSize(new Dimension(Integer.MAX_VALUE,125));
                JPanel info=new JPanel(); info.setOpaque(false); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS));
                info.setBorder(BorderFactory.createEmptyBorder(10,14,0,14));
                JLabel h=sub("Hotel: Rs."+String.format("%.0f",d.hotelPerDay)+"/night");
                JLabel f=sub("Food:  Rs."+String.format("%.0f",d.foodPerDay)+"/day");
                JLabel bk=new JLabel(cnt+"/4 rooms booked");
                bk.setFont(new Font("Segoe UI",Font.BOLD,11)); bk.setForeground(cnt>=4?ACCENT2:ACCENT3);
                info.add(h);info.add(gap(3));info.add(f);info.add(gap(5));info.add(bk);
                dc.add(ph);dc.add(info); destTabCardsPanel.add(dc); ei2++;
            }
            destTabCardsPanel.revalidate(); destTabCardsPanel.repaint();
        }
        if(destTabModel!=null){
            destTabModel.setRowCount(0);
            int didx=1;
            for(Destination d:admin.destinations){
                int cnt=countLocal(d.city);
                destTabModel.addRow(new Object[]{didx++,d.city,
                    "Rs."+String.format("%.0f",d.hotelPerDay),"Rs."+String.format("%.0f",d.foodPerDay),
                    "Rs."+String.format("%.0f",d.hotelPerDay+d.foodPerDay),(4-cnt)+" remaining"});
            }
        }
    }
    void saveBookingPDF(Booking b, Destination dest){
        JFileChooser ch=new JFileChooser();
        ch.setDialogTitle("Save Booking PDF");
        ch.setSelectedFile(new File("SmartVoyage_"+b.bookingId+".pdf"));
        ch.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files","pdf"));
        if(ch.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File file=ch.getSelectedFile();
        if(!file.getName().toLowerCase().endsWith(".pdf")) file=new File(file.getAbsolutePath()+".pdf");
        try{
            writePDF(b,dest,file);
            JOptionPane.showMessageDialog(this,"PDF saved:\n"+file.getAbsolutePath(),"PDF Saved",JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Could not save PDF:\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    void exportLogPDF(){
        JFileChooser ch=new JFileChooser();
        ch.setDialogTitle("Save Tracking Report PDF");
        ch.setSelectedFile(new File("SmartVoyage_Report_"+new SimpleDateFormat("yyyyMMdd").format(new Date())+".pdf"));
        ch.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files","pdf"));
        if(ch.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File file=ch.getSelectedFile();
        if(!file.getName().toLowerCase().endsWith(".pdf")) file=new File(file.getAbsolutePath()+".pdf");
        try{
            writeReportPDF(file);
            JOptionPane.showMessageDialog(this,"Report saved:\n"+file.getAbsolutePath(),"Report Saved",JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){
            JOptionPane.showMessageDialog(this,"Could not save report:\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    void writePDF(Booking b, Destination dest, File out) throws Exception {
        int W=794,H=1123;
        BufferedImage img=new BufferedImage(W*2,H*2,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(2,2);
        g.setColor(new Color(244,246,253)); g.fillRect(0,0,W,H);
        g.setColor(new Color(52,86,230,9));  g.fillOval(-60,-60,360,360);
        g.setColor(new Color(120,35,180,7)); g.fillOval(500,700,380,380);
        g.setPaint(new GradientPaint(0,0,new Color(40,76,215),W,90,new Color(100,36,195)));
        g.fillRect(0,0,W,90);
        g.setColor(Color.WHITE); g.setFont(new Font("Segoe UI",Font.BOLD,28));
        g.drawString("SmartVoyage",36,50);
        g.setFont(new Font("Segoe UI",Font.PLAIN,13));
        g.drawString("India's Smart Travel Planner",36,70);
        g.setFont(new Font("Segoe UI",Font.BOLD,14));
        g.drawString("BOOKING CONFIRMATION",W-252,44);
        g.setFont(new Font("Segoe UI",Font.PLAIN,11));
        g.drawString("ID: "+b.bookingId,W-252,64);
        g.setColor(new Color(230,236,255)); g.fillRoundRect(30,100,W-60,36,8,8);
        g.setColor(new Color(40,76,215));
        g.setFont(new Font("Segoe UI",Font.BOLD,11));
        g.drawString("Booking Ref: "+b.bookingId+"      Date: "+b.bookingDate,44,123);
        int y=160;
        g.setColor(new Color(16,22,50)); g.setFont(new Font("Segoe UI",Font.BOLD,16));
        g.drawString("Customer Information",36,y);
        g.setColor(new Color(40,76,215)); g.fillRect(36,y+5,164,3); y+=22;
        String[][] cust={
            {"Full Name",   b.customerName},
            {"Phone",       b.customerPhone==null?"—":b.customerPhone},
            {"Email",       b.customerEmail==null?"—":b.customerEmail},
            {"Status",      b.status}
        };
        for(int i=0;i<cust.length;i++){
            if(i%2==0){g.setColor(new Color(241,244,255));g.fillRoundRect(30,y-2,W-60,26,4,4);}
            g.setColor(new Color(96,108,136)); g.setFont(new Font("Segoe UI",Font.BOLD,11));
            g.drawString(cust[i][0],44,y+16);
            g.setColor(new Color(16,22,50)); g.setFont(new Font("Segoe UI",Font.PLAIN,11));
            g.drawString(cust[i][1],230,y+16); y+=28;
        }
        y+=14;
        g.setColor(new Color(16,22,50)); g.setFont(new Font("Segoe UI",Font.BOLD,16));
        g.drawString("Trip Details",36,y);
        g.setColor(new Color(40,76,215)); g.fillRect(36,y+5,100,3); y+=22;
        String[][] trip={
            {"Departure City",b.source},
            {"Destination",   b.destination},
            {"Distance",      String.format("%.0f km",b.distance)},
            {"Duration",      b.days+" day"+(b.days>1?"s":"")}
        };
        for(int i=0;i<trip.length;i++){
            if(i%2==0){g.setColor(new Color(241,244,255));g.fillRoundRect(30,y-2,W-60,26,4,4);}
            g.setColor(new Color(96,108,136)); g.setFont(new Font("Segoe UI",Font.BOLD,11));
            g.drawString(trip[i][0],44,y+16);
            g.setColor(new Color(16,22,50)); g.setFont(new Font("Segoe UI",Font.PLAIN,11));
            g.drawString(trip[i][1],230,y+16); y+=28;
        }
        y+=14;
        g.setColor(new Color(16,22,50)); g.setFont(new Font("Segoe UI",Font.BOLD,16));
        g.drawString("Cost Breakdown",36,y);
        g.setColor(new Color(40,76,215)); g.fillRect(36,y+5,128,3); y+=22;
        double hotelC,foodC,travelC;
        if(dest!=null){ hotelC=dest.hotelPerDay*b.days; foodC=dest.foodPerDay*b.days; travelC=b.totalCost-hotelC-foodC; }
        else           { travelC=b.totalCost*0.4; hotelC=b.totalCost*0.4; foodC=b.totalCost*0.2; }
        String[][] costs={
            {"Travel / Transport",                           "Rs. "+String.format("%.0f",travelC)},
            {"Hotel Accommodation ("+b.days+" night"+(b.days>1?"s":"")+")", "Rs. "+String.format("%.0f",hotelC)},
            {"Meals & Food ("+b.days+" day"+(b.days>1?"s":"")+")",          "Rs. "+String.format("%.0f",foodC)}
        };
        for(int i=0;i<costs.length;i++){
            if(i%2==0){g.setColor(new Color(241,244,255));g.fillRoundRect(30,y-2,W-60,26,4,4);}
            g.setColor(new Color(16,22,50)); g.setFont(new Font("Segoe UI",Font.PLAIN,11));
            g.drawString(costs[i][0],44,y+16);
            g.setFont(new Font("Segoe UI",Font.BOLD,11));
            g.drawString(costs[i][1],W-158,y+16); y+=28;
        }
        g.setColor(new Color(208,216,242)); g.fillRect(30,y+6,W-60,1); y+=14;
        g.setColor(new Color(14,38,142)); g.fillRoundRect(30,y,W-60,40,8,8);
        g.setColor(Color.WHITE); g.setFont(new Font("Segoe UI",Font.BOLD,14));
        g.drawString("TOTAL AMOUNT PAID",44,y+26);
        g.drawString("Rs. "+String.format("%.0f",b.totalCost),W-158,y+26); y+=58;
        if(dest!=null){
            g.setColor(new Color(16,22,50)); g.setFont(new Font("Segoe UI",Font.BOLD,16));
            g.drawString("About "+dest.city,36,y);
            g.setColor(new Color(14,160,96)); g.fillRect(36,y+5,70,3); y+=22;
            g.setColor(new Color(238,252,244)); g.fillRoundRect(30,y-4,W-60,56,8,8);
            String[] ddata=DestPhotoCard.DEST_DATA.getOrDefault(dest.city,new String[]{"✈️","","3456E6"});
            Color dc1=new Color(52,86,230); try{dc1=Color.decode("#"+ddata[2]);}catch(Exception ignored){}
            g.setPaint(new GradientPaint(30,y-4,dc1.darker(),30+110,y-4+56,dc1));
            g.fillRoundRect(30,y-4,110,56,8,8);
            g.setFont(new Font("Segoe UI Emoji",Font.PLAIN,28)); g.setColor(Color.WHITE);
            g.drawString(ddata[0],55,y+36);
            g.setColor(new Color(60,80,100)); g.setFont(new Font("Segoe UI",Font.ITALIC,11));
            String[] words=dest.description.split(" ");
            StringBuilder l1=new StringBuilder(),l2=new StringBuilder(); int wc=0;
            for(String w:words){if(wc<7)l1.append(w).append(" ");else l2.append(w).append(" ");wc++;}
            g.drawString(l1.toString().trim(),150,y+16);
            g.drawString(l2.toString().trim(),150,y+32); y+=64;
            g.setColor(new Color(241,244,255)); g.fillRoundRect(30,y-4,W-60,28,6,6);
            g.setColor(new Color(96,108,136)); g.setFont(new Font("Segoe UI",Font.BOLD,11));
            g.drawString("Hotel: Rs."+String.format("%.0f",dest.hotelPerDay)+"/night   Food: Rs."+String.format("%.0f",dest.foodPerDay)+"/day",44,y+14);
            y+=38;
        }
        g.setColor(new Color(255,249,228)); g.fillRoundRect(30,y,W-60,62,8,8);
        g.setColor(new Color(178,128,0)); g.setFont(new Font("Segoe UI",Font.BOLD,11));
        g.drawString("IMPORTANT NOTES",44,y+18);
        g.setColor(new Color(100,80,0)); g.setFont(new Font("Segoe UI",Font.PLAIN,10));
        g.drawString("• Carry a valid government-issued ID during travel.",44,y+34);
        g.drawString("• Hotel check-in: 2:00 PM   |   Check-out: 11:00 AM.",44,y+50); y+=76;
        g.setColor(new Color(208,216,242)); g.fillRect(0,H-72,W,1);
        g.setColor(new Color(156,168,198)); g.setFont(new Font("Segoe UI",Font.PLAIN,9));
        g.drawString("SmartVoyage — India's Smart Travel Planner  |  Electronically generated — no signature required.",36,H-52);
        g.drawString("Booking ID: "+b.bookingId+"  |  Generated: "+b.bookingDate,36,H-36);
        Graphics2D gw=(Graphics2D)g.create();
        gw.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        gw.rotate(Math.toRadians(-45),W/2.0,H/2.0);
        gw.setColor(new Color(40,76,215,13)); gw.setFont(new Font("Segoe UI",Font.BOLD,64));
        gw.drawString("SMARTVOYAGE",W/2-190,H/2+22); gw.dispose();
        g.dispose();
        java.io.ByteArrayOutputStream jpgBuf=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img,"jpeg",jpgBuf);
        byte[] jpg=jpgBuf.toByteArray();
        embedInPDF(jpg,W*2,H*2,out);
    }
    void writeReportPDF(File out) throws Exception {
        List<Booking> all=dbOk?DB.loadAllBookings():new ArrayList<>();
        int W=794,H=1123;
        int rows=Math.max(1,all.size());
        int rowH=32, headerH=220, footerH=60;
        int totalH=Math.max(H,headerH+rows*rowH+footerH+80);
        BufferedImage img=new BufferedImage(W*2,totalH*2,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(2,2);
        g.setColor(new Color(244,246,253)); g.fillRect(0,0,W,totalH);
        g.setColor(new Color(52,86,230,8)); g.fillOval(-80,-80,400,400);
        g.setPaint(new GradientPaint(0,0,new Color(40,76,215),W,88,new Color(100,36,195)));
        g.fillRect(0,0,W,88);
        g.setColor(Color.WHITE); g.setFont(new Font("Segoe UI",Font.BOLD,26));
        g.drawString("SmartVoyage",36,48);
        g.setFont(new Font("Segoe UI",Font.PLAIN,13));
        g.drawString("India's Smart Travel Planner",36,68);
        g.setFont(new Font("Segoe UI",Font.BOLD,14));
        g.drawString("ADMIN BOOKING REPORT",W-240,44);
        g.setFont(new Font("Segoe UI",Font.PLAIN,11));
        g.drawString("Generated: "+new SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new Date()),W-240,64);
        g.setColor(new Color(230,236,255)); g.fillRoundRect(30,98,W-60,36,8,8);
        g.setColor(new Color(40,76,215)); g.setFont(new Font("Segoe UI",Font.BOLD,11));
        long confirmed=all.stream().filter(b->"CONFIRMED".equals(b.status)).count();
        long checked  =all.stream().filter(b->"CHECKED_OUT".equals(b.status)).count();
        double totalRev=all.stream().mapToDouble(b->b.totalCost).sum();
        g.drawString("Total Bookings: "+all.size()+"      Confirmed: "+confirmed+
                     "      Checked Out: "+checked+"      Total Revenue: Rs."+String.format("%.0f",totalRev),44,121);
        int y=152;
        g.setColor(new Color(40,76,215)); g.fillRoundRect(30,y,W-60,28,6,6);
        g.setColor(Color.WHITE); g.setFont(new Font("Segoe UI",Font.BOLD,10));
        int[] cx={44,120,236,340,415,490,560,640,W-80};
        String[] ch2={"#","Booking ID","Customer","From","To","Days","Total","Date","Status"};
        for(int i=0;i<ch2.length;i++) g.drawString(ch2[i],cx[i],y+18); y+=30;
        g.setFont(new Font("Segoe UI",Font.PLAIN,10));
        int ridx=1;
        for(Booking bk:all){
            if(ridx%2==0){g.setColor(new Color(241,244,255));g.fillRect(30,y-2,W-60,rowH-2);}
            g.setColor(TXT1);
            String[] vals={String.valueOf(ridx++),bk.bookingId,
                bk.customerName.length()>14?bk.customerName.substring(0,13)+"…":bk.customerName,
                bk.source,bk.destination,String.valueOf(bk.days),
                "Rs."+String.format("%.0f",bk.totalCost),
                bk.bookingDate.length()>12?bk.bookingDate.substring(0,12):bk.bookingDate,bk.status};
            for(int i=0;i<vals.length;i++){
                if(i==vals.length-1){
                    g.setColor("CONFIRMED".equals(bk.status)?new Color(14,160,96):new Color(150,155,180));
                    g.setFont(new Font("Segoe UI",Font.BOLD,10));
                }
                g.drawString(vals[i],cx[i],y+18);
                g.setColor(TXT1); g.setFont(new Font("Segoe UI",Font.PLAIN,10));
            }
            y+=rowH;
            g.setColor(new Color(228,232,248)); g.fillRect(30,y-1,W-60,1);
        }
        g.setColor(new Color(208,216,242)); g.fillRect(0,totalH-62,W,1);
        g.setColor(new Color(156,168,198)); g.setFont(new Font("Segoe UI",Font.PLAIN,9));
        g.drawString("SmartVoyage Admin Report  |  Confidential  |  "+new SimpleDateFormat("dd MMM yyyy").format(new Date()),36,totalH-42);
        Graphics2D gw=(Graphics2D)g.create();
        gw.rotate(Math.toRadians(-45),W/2.0,totalH/2.0);
        gw.setColor(new Color(40,76,215,10)); gw.setFont(new Font("Segoe UI",Font.BOLD,64));
        gw.drawString("SMARTVOYAGE",W/2-190,totalH/2+22); gw.dispose();
        g.dispose();
        java.io.ByteArrayOutputStream jpgBuf=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img,"jpeg",jpgBuf);
        byte[] jpg=jpgBuf.toByteArray();
        embedInPDF(jpg,W*2,totalH*2,out);
    }
    void embedInPDF(byte[] jpg,int imgW,int imgH,File out) throws Exception {
        int pw=595,ph=(int)(595.0/imgW*imgH);
        java.io.ByteArrayOutputStream buf=new java.io.ByteArrayOutputStream();
        List<Integer> offs=new ArrayList<>();
        buf.write(("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n").getBytes("ISO-8859-1"));
        offs.add(buf.size());
        buf.write(("1 0 obj\n<</Type/Catalog/Pages 2 0 R>>\nendobj\n").getBytes("ISO-8859-1"));
        offs.add(buf.size());
        buf.write(("2 0 obj\n<</Type/Pages/Kids[3 0 R]/Count 1>>\nendobj\n").getBytes("ISO-8859-1"));
        offs.add(buf.size());
        buf.write(("3 0 obj\n<</Type/Page/Parent 2 0 R/MediaBox[0 0 "+pw+" "+ph+"]/Contents 4 0 R/Resources<</XObject<</Im1 5 0 R>>>>>>\nendobj\n").getBytes("ISO-8859-1"));
        String cs="q\n"+pw+" 0 0 "+ph+" 0 0 cm\n/Im1 Do\nQ\n";
        offs.add(buf.size());
        buf.write(("4 0 obj\n<</Length "+cs.length()+">>\nstream\n"+cs+"\nendstream\nendobj\n").getBytes("ISO-8859-1"));
        offs.add(buf.size());
        String hdr5="5 0 obj\n<</Type/XObject/Subtype/Image/Width "+imgW+"/Height "+imgH+"/ColorSpace/DeviceRGB/BitsPerComponent 8/Filter/DCTDecode/Length "+jpg.length+">>\nstream\n";
        buf.write(hdr5.getBytes("ISO-8859-1")); buf.write(jpg);
        buf.write("\nendstream\nendobj\n".getBytes("ISO-8859-1"));
        int xref=buf.size();
        buf.write("xref\n".getBytes("ISO-8859-1"));
        buf.write(("0 "+(offs.size()+1)+"\n").getBytes("ISO-8859-1"));
        buf.write("0000000000 65535 f \n".getBytes("ISO-8859-1"));
        for(int o:offs) buf.write(String.format("%010d 00000 n \n",o).getBytes("ISO-8859-1"));
        buf.write(("trailer\n<</Size "+(offs.size()+1)+"/Root 1 0 R>>\nstartxref\n"+xref+"\n%%EOF\n").getBytes("ISO-8859-1"));
        try(FileOutputStream fos=new FileOutputStream(out)){ fos.write(buf.toByteArray()); }
    }
    public static void main(String[] args){
        try{ UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch(Exception ignored){}
        SwingUtilities.invokeLater(SmartVoyageUI::new);
    }
}
