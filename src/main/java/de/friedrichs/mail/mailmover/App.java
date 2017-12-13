/*
 */
package de.friedrichs.mail.mailmover;

import com.sun.mail.imap.IMAPFolder;

import java.io.File;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 *
 * @author AFR
 */
public class App {

    public App() {
    }

    public void moveMessages(Configuration config) {
        Properties props = System.getProperties();
        props.setProperty("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(props, null);
        Store store = null;
        try {
            store = session.getStore("imaps");

            IMAPFolder sourceFolder = null;
            IMAPFolder targetFolder = null;
            System.out.println("Connecting to IMAP server: " + config.getString("url"));

            try {
                store.connect(
                        config.getString("url"),
                        config.getString("username"),
                        config.getString("passwort"));

                sourceFolder = (IMAPFolder) store.getFolder(config.getString("sourceFolder"));
                targetFolder = (IMAPFolder) store.getFolder(config.getString("targetFolder"));

                sourceFolder.open(Folder.READ_WRITE);
                targetFolder.open(Folder.READ_WRITE);

                // creates a search criterion
                SearchTerm unseen = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
                SearchTerm subject = new SubjectTerm(config.getString("subjectTerm"));
                SearchTerm fromFritz = new SearchTerm() {
                    private static final long serialVersionUID = 7557221265328624292L;

                    @Override
                    public boolean match(Message message) {
                        try {
                            if (message.getFrom()[0].toString().equals(config.getString("fromTerm"))) {
                                return true;
                            }
                        } catch (MessagingException ex) {
                            System.err.println("MessagingException: " + ex.getLocalizedMessage());
                        }
                        return false;
                    }
                };

                SearchTerm all = new AndTerm(new SearchTerm[]{subject, fromFritz});

                Message[] search = sourceFolder.search(unseen);
                System.out.println("Messages: " + search.length);

                if (search.length > 0) {
                    Message[] search1 = sourceFolder.search(all, search);
                    System.out.println("Messages: " + search1.length);

                    if (search1.length > 0) {
                        sourceFolder.moveMessages(search1, targetFolder);

                    }
                }

                if (sourceFolder.isOpen()) {
                    sourceFolder.close(true);
                }

                if (targetFolder.isOpen()) {
                    targetFolder.close(false);
                }
            } catch (MessagingException ex) {
                System.err.println("MessagingException gefangen: " + ex.getLocalizedMessage());
            } finally {
                if (sourceFolder != null && sourceFolder.isOpen()) {
                    try {
                        sourceFolder.close(true);
                    } catch (MessagingException ex) {
                        //nothing
                    }
                }

                if (targetFolder != null && targetFolder.isOpen()) {
                    try {
                        targetFolder.close(false);
                    } catch (MessagingException ex) {
                        //nothing
                    }
                }
            }

        } catch (NoSuchProviderException ex) {
            System.err.println("NoSuchProviderException gefangen: " + ex.getLocalizedMessage());
        } finally {
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (MessagingException ex) {
                    //nothing
                }
            }
        }

    }

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.err.println("Config Datei nicht angegeben!");
        }

        App app = new App();
        Configurations configs = new Configurations();
        try {
            Configuration config = configs.properties(new File(args[0]));
            app.moveMessages(config);
        } catch (ConfigurationException cex) {
            System.err.println("ConfigurationException gefangen: " + cex.getLocalizedMessage());
            System.exit(1);
        }
        System.exit(0);
    }

}
