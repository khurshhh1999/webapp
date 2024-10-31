package services;

import com.sendgrid.Content;
import com.sendgrid.EmailAddress;
import com.sendgrid.Mail;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${EMAIL_FROM}")
    private String fromEmail;

    public void sendEmail(String toEmail, String subject, String contentText) {
        EmailAddress from = new EmailAddress(fromEmail);
        EmailAddress to = new EmailAddress(toEmail);
        Content content = new Content("text/plain", contentText);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            
            // Optional: Log the response for debugging
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("Headers: " + response.getHeaders());
            
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send email", ex);
        }
    }
}
