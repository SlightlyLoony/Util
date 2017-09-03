package com.dilatush.util;

import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.Deque;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Implements a simple send/receive SMS capability using the Twilio API.
 *
 * @author Tom Dilatush  tom@dilatush.com
 */
public class SMS {

    private static final int MAX_QUEUE_SIZE = 100;  // maximum number of enqueued messages to be sent...
    private static final int SMS_INBOX_CHECK_INTERVAL_SECONDS = 60;

    private static final SMS INSTANCE = new SMS();

    private TwilioConfiguration configuration;
    private SMSSender smsSender;
    private SMSReceiver smsReceiver;


    private SMS() {
    }


    private void init( final TwilioConfiguration _configuration ) {
        configuration = _configuration;
        smsSender = new SMSSender();
        smsReceiver = new SMSReceiver();
    }


    public static void start( final TwilioConfiguration _configuration ) {
        INSTANCE.init( _configuration );
    }


    public static void send( final String _recipient, final String _message ) {
        INSTANCE.smsSender.send( _recipient, _message );
    }


    public static void send( final SMSMessage _message ) {
        INSTANCE.smsSender.send( _message.recipient, _message.message );
    }


    public static SMSMessage receive() {
        return INSTANCE.smsReceiver.messages.poll();
    }


    private class SMSReceiver extends Thread {

        private final Deque<SMSMessage> messages;

        private SMSReceiver() {
            messages = new ConcurrentLinkedDeque<>();
            setDaemon( true );
            setName( "SMS Receiver" );
            start();
        }


        @Override
        public void run() {

            while( !isInterrupted()) {

                try {

                    Twilio.init( configuration.getAccount(), configuration.getPassword() );

                    ResourceSet<Message> smsMessages = Message.reader()
                            .setTo( new PhoneNumber( configuration.getPhone() ) )   // filters for inbound messages only...
                            .read();

                    // Loop over messages and print out a property for each one.
                    for (Message smsMessage : smsMessages) {

                        String from = smsMessage.getFrom().toString();
                        String body = smsMessage.getBody();
                        String pathSID = smsMessage.getSid();

                        messages.add( new SMSMessage( from, body ) );

                        Message.deleter( pathSID ).delete();
                    }

                    // wait until it's time to check the inbox again...
                    sleep( 1000 * SMS_INBOX_CHECK_INTERVAL_SECONDS );
                }
                catch( Exception _e ) {
                    Logger.log( _e );
                }
            }
        }
    }


    private class SMSSender extends Thread {

        private final BlockingDeque<SMSMessage> messages;


        private SMSSender() {
            messages = new LinkedBlockingDeque<>( MAX_QUEUE_SIZE );
            setDaemon( true );
            setName( "SMS Sender" );
            start();
        }


        private synchronized void send( final String _recipient, final String _message ) {

            SMSMessage message = new SMSMessage( _recipient, _message );

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

                    SMSMessage message = messages.take();


                    Twilio.init( configuration.getAccount(), configuration.getPassword() );

                    com.twilio.rest.api.v2010.account.Message.creator(
                            new PhoneNumber( message.recipient ), new PhoneNumber( configuration.getPhone() ), message.message ).create();
                }
                catch( Exception _e ) {
                    Logger.log( _e );
                }
            }
        }
    }


    public static class SMSMessage {
        public final String recipient;
        public final String message;


        private SMSMessage( final String _recipient, final String _message ) {
            recipient = _recipient;
            message = _message;
        }
    }
}
