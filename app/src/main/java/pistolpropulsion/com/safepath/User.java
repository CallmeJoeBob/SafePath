package pistolpropulsion.com.safepath;

public class User {
    private String email;
    private String pwd;
    private String ec;
    private String name;
    private String pincode;
    public User(String e,String p,String na,String nu, String pc){
        email = e;
        pwd = p;
        ec = nu;
        name = na;
        pincode = pc;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getContact() { return ec; }
    public String getPinCode() {return pincode;}
}
