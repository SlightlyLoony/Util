package com.dilatush.util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Deque;
import java.util.Properties;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Tom Dilatush  tom@dilatush.com
 */
public class GMail {

    private static final int MAX_QUEUE_SIZE = 100;  // maximum number of enqueued messages to be sent...
    private static final int EMAIL_INBOX_CHECK_INTERVAL_SECONDS = 60;

    private static final GMail INSTANCE = new GMail();

    private GMailConfiguration configuration;
    private EmailSender emailSender;
    private EmailReceiver emailReceiver;


    private GMail() {
    }


    private void init( final GMailConfiguration _configuration ) {
        configuration = _configuration;
        emailSender = new EmailSender();
        emailReceiver = new EmailReceiver();
    }


    public static void start( final GMailConfiguration _configuration ) {
        INSTANCE.init( _configuration );
    }


    public static void send( final String _recipient, final String _subject, final String _message ) {
        INSTANCE.emailSender.send( _recipient, _subject, _message );
    }


    public static void send( final EmailMessage _message ) {
        INSTANCE.emailSender.send( _message.recipient, _message.subject, _message.message );
    }


    public static EmailMessage receive() {
        return INSTANCE.emailReceiver.messages.poll();
    }


    private class EmailReceiver extends Thread {

        private final Deque<EmailMessage> messages;

        private EmailReceiver() {
            messages = new ConcurrentLinkedDeque<>();
            setDaemon( true );
            setName( "Email Receiver" );
            start();
        }


        @Override
        public void run() {

            while( !isInterrupted()) {

                try {

                    // Get our session...
                    Properties properties = new Properties();
                    properties.put( "mail.pop3.host",            "pop.gmail.com" );
                    properties.put( "mail.pop3.port",            "995"           );
                    properties.put( "mail.pop3.starttls.enable", "true"          );
                    Session emailSession = Session.getDefaultInstance( properties );

                    // Create our POP3 store object and connect with the server...
                    Store store = emailSession.getStore( "pop3s" );
                    store.connect( "pop.gmail.com", configuration.getAccount(), configuration.getPassword());

                    // Get our folder as read/write...
                    Folder inbox = store.getFolder( "INBOX" );
                    inbox.open( Folder.READ_WRITE );

                    // Fetch messages from the folder, queue them and and mark them for deletion...
                    Message[] messageobjs = inbox.getMessages();

                    for( Message message : messageobjs ) {
                        String content = "";
                        Object contentRaw = message.getContent();
                        if( contentRaw instanceof String )
                            content = (String) contentRaw;
                        else if( contentRaw instanceof Multipart ) {
                            Multipart parts = (Multipart) contentRaw;
                            for( int i = 0; i < parts.getCount(); i++ ) {
                                BodyPart part = parts.getBodyPart( i );
                                if( part.getContentType().startsWith( "text/plain" )) {
                                    content = part.getContent().toString();
                                    break;
                                }
                            }
                        }
                        EmailMessage emailMessage = new EmailMessage( message.getFrom()[0].toString(), message.getSubject(), content );
                        messages.add( emailMessage );
                        message.setFlag( Flags.Flag.DELETED, true );
                    }

                    // Now close the inbox and the store, expunging deleted messages from the inbox...
                    inbox.close( true );
                    store.close();

                    // wait until it's time to check the inbox again...
                    sleep( 1000 * EMAIL_INBOX_CHECK_INTERVAL_SECONDS );
                }
                catch( Exception _e ) {
                    Logger.log( _e );
                }
            }
        }
    }


    private class EmailSender extends Thread {

        private final BlockingDeque<EmailMessage> messages;


        private EmailSender() {
            messages = new LinkedBlockingDeque<>( MAX_QUEUE_SIZE );
            setDaemon( true );
            setName( "Email Sender" );
            start();
        }


        private synchronized void send( final String _recipient, final String _subject, final String _message ) {

            EmailMessage message = new EmailMessage( _recipient, _subject, _message );

            // if our queue is full, throw away the oldest entries until we have room (should only ever be one thrown away)...
            while( messages.size() >= MAX_QUEUE_SIZE ) {
                messages.poll();
            }

            // now that we know we have enough room, add the new entry...
            messages.offer( message );
        }


        @Override
        public void run() {

            while( !isInterrupted() ) {

                try {

                    EmailMessage message = messages.take();

                    Properties props = new Properties();
                    props.put( "mail.smtp.host", "smtp.gmail.com" );
                    props.put( "mail.smtp.socketFactory.port", "465" );
                    props.put( "mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory" );
                    props.put( "mail.smtp.auth", "true" );
                    props.put( "mail.smtp.port", "465" );

                    Authenticator authenticator = new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication( configuration.getAccount(), configuration.getPassword() );
                        }
                    };
                    Session session = Session.getInstance( props, authenticator );

                    Message msg = new MimeMessage( session );
                    msg.setFrom( new InternetAddress( configuration.getAccount() ) );
                    msg.setRecipients( Message.RecipientType.TO, InternetAddress.parse( message.recipient ) );
                    msg.setSubject( message.subject );
                    msg.setText( message.message );

                    Transport.send( msg );
                }
                catch( Exception _e ) {
                    Logger.log( _e );
                }
            }
        }
    }


    public static class EmailMessage {
        public final String recipient;
        public final String subject;
        public final String message;


        private EmailMessage( final String _recipient, final String _subject, final String _message ) {
            recipient = _recipient;
            message = _message;
            subject = _subject;
        }
    }
}
