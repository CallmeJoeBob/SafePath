package pistolpropulsion.com.safepath;

/**
 * Created by Zhuo.C on 10/20/2018.
 */

public class UserFirebase {
    private String email;
    private String pwd;
    private String contact;
    private String name;

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getContact() { return contact; }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }
}
