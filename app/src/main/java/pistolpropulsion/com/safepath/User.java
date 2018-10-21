package pistolpropulsion.com.safepath;

public class User {
    private String email;
    private String pwd;
    private String ec;
    private String name;
    public User(String e,String p,String na,String nu){
        email = e;
        pwd = p;
        ec = nu;
        name = na;
    }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getContact() { return ec; }
}
