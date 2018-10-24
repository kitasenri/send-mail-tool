package sample;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * https://qiita.com/yoh-nak/items/bff51637fa4f558b37ac
 */
public class MailSend {

    public enum MAIL_TYPE {

        TEXT("text/html; charset=ISO-2022-JP"),
        HTML("text/plain; charset=ISO-2022-JP");

        public String contentType;

        private MAIL_TYPE(String contentType) {
            this.contentType = contentType;
        }
    };

    private static Properties properties;
    private static final String PROPERTY_FILE_PATH = "mail.properties";

    private static final String ENCODE = "ISO-2022-JP";
    private static final String CONTENT_TYPE_NAME = "Content-Type";

    private static final String CONTENT_TRANSFER_ENCODING_VALUE = "7bit";
    private static final String CONTENT_TRANSFER_ENCODING_NAME = "Content-Transfer-Encoding";

    public static void main(String[] args) throws MessagingException, IOException {

        MAIL_TYPE mail = MAIL_TYPE.TEXT;
        if ( 1 <= args.length ) {
            if ( "text".equals(args[0]) ) mail = MAIL_TYPE.TEXT;
        }

        properties = new Properties();
        properties.load(Files.newBufferedReader(
            Paths.get(PROPERTY_FILE_PATH),
            StandardCharsets.UTF_8)
        );

        final Properties props = new Properties();

        String host = properties.getProperty("SMTP_SERVER_HOST");
        String port = properties.getProperty("SMTP_SERVER_PORT");

        props.setProperty("mail.smtp.host", host);
        props.setProperty("mail.smtp.port", port);

        props.setProperty("mail.smtp.timeout", "60000");
        props.setProperty("mail.smtp.connectiontimeout", "60000");

        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.socketFactory.port", "465");
        props.setProperty("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        final Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {

                String smtpID = properties.getProperty("MAIL_AUTH_ID");
                String smtpPass = properties.getProperty("MAIL_AUTH_PASS");

                return new PasswordAuthentication(smtpID, smtpPass);
            }
        });

        final MimeMessage message = new MimeMessage(session);
        try {

            String fromAddress = properties.getProperty("MAIL_FROM_ADDRESS");
            String fromName = properties.getProperty("MAIL_FROM_NAME");

            final Address from = new InternetAddress(
                fromAddress,
                fromName,
                ENCODE
            );
            message.setFrom(from);

            String toAddress = properties.getProperty("MAIL_TO_ADDRESS");
            String toName = properties.getProperty("MAIL_TO_NAME");

            final Address to = new InternetAddress(
                toAddress,
                toName,
                ENCODE
            );
            message.addRecipient(Message.RecipientType.TO, to);

            // Title
            String mailTitle = properties.getProperty("MAIL_TITLE");
            message.setSubject(mailTitle, ENCODE);

            // Body
            String mailBody = readAll(
                properties.getProperty("MAIL_BODY_FILE"),
                properties.getProperty("MAIL_BODY_FILE_ENCODE")
            );
            message.setText(mailBody, ENCODE);

            // Meta Info
            message.addHeader("X-Mailer", "blancoMail 0.1");
            message.setHeader( CONTENT_TRANSFER_ENCODING_NAME, CONTENT_TRANSFER_ENCODING_VALUE );
            message.setHeader(CONTENT_TYPE_NAME, mail.contentType);
            message.setSentDate(new Date());

        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Send mail
        try {
            Transport.send(message);
        } catch (AuthenticationFailedException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }

    private static String readAll(final String path, final String charset) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), charset);
    }

}
