package pistolpropulsion.com.safepath;

/**
 * Created by Zhuo.C on 10/20/2018.
 */

// Install the Java helper library from twilio.com/docs/libraries/java
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsSender {
    // Find your Account Sid and Auth Token at twilio.com/console
    public static final String ACCOUNT_SID =
            "AC5956494ab58835f2a919f58da6802314";
    public static final String AUTH_TOKEN =
            "85982bd78a5b5a86b91de27aac4f8270";

    public static void main(String[] args) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        Message message = Message
                .creator(new PhoneNumber("+18057289001"), // to
                        new PhoneNumber("+14049155283"), // from
                        "Where's Your Friend!?")
                .create();

        System.out.println(message.getSid());
    }
}
