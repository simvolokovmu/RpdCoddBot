import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.log4j.Logger;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.FilenameUtils;

public class SendMail 
{
//	public String mailFrom_ = "adimotcontrol@gmail.com";
//	public String mailTo_ = "adimotcontrol@gmail.com";
//	public String sText_ = "No spam to my email, please!";
	public String mail_smtp_username_;// = "adimotcontrol@gmail.com";
	public String mail_smtp_password_;// = "Adimot2017";

	public String mail_smtp_starttls_;// = "true";
	public String mail_smtp_auth_;// = "true";
	public String mail_smtp_host_;// = "smtp.gmail.com";
	public String mail_smtp_port_;// = "587";

	public String mail_pop3_protocol_;// = "pop3";
	public String mail_pop3_host_;// = "pop.gmail.com";
	public String mail_pop3_port_;// = "995";
	public String mail_pop3_user_;// = "77agat@gmail.com";
	public String mail_pop3_pwd_;// = "$((2043192";
	
	public String dirAttSave_ = "./Inbox";
	public String dirEmlSmtpSave_ = "./EmailSmtp";
	public String dirEmlPop3Save_ = "./EmailPop3";
	
	public Logger log_;

	public void RunSmtp (String mailFrom, String mailTo, String sSubject, String sText, String sPFAtt, String sFNAtt)
    {
        try {
        	Properties props = new Properties();
            props.put("mail.smtp.starttls.enable", mail_smtp_starttls_);
            props.put("mail.smtp.auth", mail_smtp_auth_);
            props.put("mail.smtp.host", mail_smtp_host_);
            props.put("mail.smtp.port", mail_smtp_port_);
            props.put("mail.smtp.user", mail_smtp_username_);
            props.put("mail.smtp.password", mail_smtp_password_);            

            Session session = Session.getInstance(props,
              new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mail_smtp_username_, mail_smtp_password_);
                }
              });        	

            // -- Create a new message --
            final MimeMessage message = new MimeMessage(session);
            
//            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailTo));
            message.setSubject(sSubject);
            message.setText(sText);

            if(sPFAtt != null)
            {
	            MimeBodyPart messageBodyPart = new MimeBodyPart();
	            Multipart multipart = new MimeMultipart();
	
	            messageBodyPart = new MimeBodyPart();
	            DataSource source = new FileDataSource(sPFAtt);
	            messageBodyPart.setDataHandler(new DataHandler(source));
	            messageBodyPart.setFileName(sFNAtt == null || sFNAtt.isEmpty() ? FilenameUtils.getBaseName(sPFAtt) : sFNAtt);
	            multipart.addBodyPart(messageBodyPart);
	
	            message.setContent(multipart);
            }
            
            try {
            	String sPFSave = dirEmlSmtpSave_ + "/smtp_" + UUID.randomUUID().toString() + ".eml";
            	message.writeTo(new FileOutputStream(new File(sPFSave)));
            } catch(Exception ee)
            {
            	log_.error(ee);
            }
            Transport.send(message);
            log_.info("Send mail success " + (sFNAtt == null ? sPFAtt : sFNAtt));
        } catch (MessagingException e) 
        {
            log_.error("Send mail error " + (sFNAtt == null ? sPFAtt : sFNAtt));
            log_.error(e);
        	
            throw new RuntimeException(e);
        }
    }

	public void RunPop3 () 
	{
/*
		String userName = "adimotcontrol";
		String password = "Adimot2017";
		properties.put("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		properties.put("mail.pop3.socketFactory.fallback", "false");
		properties.put("mail.pop3.socketFactory.port", "995");
		properties.put("mail.pop3.port", "995");
		properties.put("mail.pop3.host", "pop.gmail.com");
		properties.put("mail.pop3.user", userName);
		properties.put("mail.store.protocol", "pop3");		
*/
		Properties properties = new Properties();
		properties.put("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		properties.put("mail.pop3.socketFactory.fallback", "false");
		properties.put("mail.store.protocol", mail_pop3_protocol_); // "pop3");		
		properties.put("mail.pop3.host", mail_pop3_host_);
		properties.put("mail.pop3.socketFactory.port", mail_pop3_port_);
		properties.put("mail.pop3.port", mail_pop3_port_);
		properties.put("mail.pop3.user", mail_pop3_user_);

		boolean bReceivedKillProcess = false;
		
		Session session = Session.getDefaultInstance(properties);
		try {
			// connects to the message store
			Store store = session.getStore("pop3");
			store.connect(mail_pop3_user_, mail_pop3_pwd_);

			// opens the inbox folder
			Folder folderInbox = store.getFolder("INBOX");
			folderInbox.open(Folder.READ_WRITE);// .READ_ONLY);

			// fetches new messages from server
			Message[] messages = folderInbox.getMessages();
			List<File> attachments = new ArrayList<File>();

			for (Message msg : messages) 
			{
				Address[] fromAddress = msg.getFrom();
				if(fromAddress.length == 0)
				{
					log_.info("Field From is Empty - ignored");
					continue;
				}
					
				String from = fromAddress[0].toString();
				String subject = msg.getSubject();
//				String toList = parseAddresses(msg.getRecipients(RecipientType.TO));
//				String ccList = parseAddresses(msg.getRecipients(RecipientType.CC));
				String sentDate = msg.getSentDate().toString();
				log_.info("Subj:<" + subject + "> From:<" + MimeUtility.decodeText(from) + "> Date:<" + sentDate + ">");
				String contentType = msg.getContentType();

				File fileEml = null;
				try {
//					String sDirForAttach = ConfigLocal.dirInputXml_;
					fileEml = new File(dirEmlPop3Save_ + "/pop3_" + UUID.randomUUID().toString() + ".eml");
					msg.writeTo(new FileOutputStream(fileEml));
					msg.setFlag(Flags.Flag.DELETED, true);
					
					if(from.indexOf("@transport.mos.ru") == -1 && from.indexOf("@gmail.com") == -1)
					{
						log_.info("Message not from DTM and gmail - ignored");
						continue;
					}
					
					if(contentType.equalsIgnoreCase("text/plain"))
					{
					     String content = (String)msg.getContent();
					     //And am writing some logic to put it as CLOB field in DB.
					} else {
					     MimeMultipart multipart = (MimeMultipart) msg.getContent();
					     for (int j = 0; j < multipart.getCount(); j++) 
					     {
							BodyPart bodyPart = multipart.getBodyPart(j);
							String sFN = bodyPart.getFileName();
							if(sFN == null)
								continue;
							
							String sFNClear = MimeUtility.decodeText(sFN); 
							String sPFAtt = dirAttSave_ + '/' + sFNClear;
							
							File fTemp = new File(sPFAtt);
							if (fTemp.exists() && !fTemp.isDirectory()) 
							{ // get new file name
								sPFAtt = dirAttSave_ + '/' + sFNClear + '.' + 
										UUID.randomUUID().toString() + '.' + sFNClear.replaceAll("^.*\\.(.*)$", "$1");
							}
					        
							File f = new File(sPFAtt);
					        FileOutputStream fos = new FileOutputStream(f);
					        byte[] buf = new byte[4096];
					        int bytesRead;
							
					        InputStream stream = bodyPart.getInputStream();
					        while ((bytesRead = stream.read(buf)) != -1)
					            fos.write(buf, 0, bytesRead);
					        
					        fos.close();
					     }
					}
				} catch(Exception e) {
					log_.info("Error proceed eml: " + e.toString());
					e.printStackTrace();
				}
			}

			// disconnect
			folderInbox.close(true); // false);
			store.close();
		} catch (NoSuchProviderException ex) {
			log_.info("No provider for protocol: "); // + protocol);
			log_.error(ex);//.printStackTrace();
		} catch (MessagingException ex) {
			log_.info("Could not connect to the message store");
			log_.error(ex); //ex.printStackTrace();
		} catch (Exception ex) {
			log_.info("Error: " + ex.toString());
			log_.error(ex); //			ex.printStackTrace();
		}
		
		if(bReceivedKillProcess)
			Core.stop();
	}
	
//	public static String cleanContentType(MimePart mp, String contentType) {
	public static String cleanCharSet(String contentType) 
	{
        try {
            ContentType content = new ContentType(contentType);
            String charset = MimeUtility.decodeText(content.getParameter("charset"));
            charset = charset.replace("\"", "");
            content.setParameter("charset", charset);
            
            return content.toString();
        } catch (MessagingException | UnsupportedEncodingException ex) {
            return contentType;
        }
    }

	private String parseAddresses(Address[] address)
	{
		String listAddress = "";	
		if (address != null)
			for (int i = 0; i < address.length; i++) 
				listAddress += address[i].toString() + ", ";
		
		if (listAddress.length() > 1) 
			listAddress = listAddress.substring(0, listAddress.length() - 2);
		return listAddress;
	}

}