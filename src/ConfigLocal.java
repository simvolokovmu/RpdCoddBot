import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public class ConfigLocal 
{
	public static final String LOG_PROPERTIES_FILE = "./Log4J.properties";
	public static final String CFG_PROPERTIES_FILE = "./config.properties";

	public static final String dirStore_ = "./Store";
	public static final String dirInbox_ = "./Inbox";
	public static final String dirEMail_ = "./EMail";

	public static final String cmdUknown_ = "Неизвестная команда\nДля получения списка команд наберите help\n";
	public static final String cmdHelp_ = "help";
	public static final String cmdRpdYstd_ = "Вчера";
	public static final String cmdRpdWeek_ = "Неделя";
	public static final String cmdRpdDay_ = "День";

	public static final String cmdRpdYstdTrips_ = "Вчера Рейсы";
	public static final String cmdRpdYstdTS_ = "Вчера Выпуск";
	
	public static final String cmdRpdOnTrips_ = "Рейсы";
	public static final String cmdRpdOnTS_ = "Выпуск ТС";
	public static final String cmdRpdOnShed_ = "Зачет рейсов";
	public static final String cmdRpdOnTime_ = "Пунктуальность"; 
	
	public static final String cmdKill9_ = "kill -9";

	private static int nCurTimesForFullReload_ = 0;
	private static int nTimesForFullReload_;
	public static int nDaysAlwaysReload_;

	public static int nMailSmtp_;	 
	public static int nMailPop3_;	 
	public static SendMail mail_ = new SendMail();
	public static String sMailFrom_;
	public static String sMailTo_;
	
//	final public static String rptHeader_ = "<b>Перевозчик  План    Факт     %</b>";
	final public static String tableHeaderTrips_= "<b>Перевозчик План   Рейсы    %</b>";
	final public static String tableHeaderTS_   = "<b>Перевозчик План  Выпуск    %</b>";
	final public static String tableHeaderShed_ = "<b>Перевозчик План   Зачет    %</b>";
	final public static String tableHeaderTime_ = "<b>Перевозчик План   Пункт.   %</b>";

	
	final public static String sCommSummaryDesc_ = "Комм.Итог";
//	final public static String sCommSummaryDesc_ = "Всего";
//	final public static String sCommSummaryDesc_ = "  Всего  ";
//	final public static String sCommSummaryDesc_ = "<pre>&#8721;</pre>Комерс";
	
	public static final String descHelp_ = 
"Список команд:\n" +
"\"Вчера : План-Факт за вчерашний день\n"+
"\"Неделя\" - План-Факт за прошлую неделю пн-вс\n"+
"\"День dd.mm\" - План-Факт за указанный день, например \"день 29.06\"\n" +
"\"help\" - Справка о командах\n";
	
	public static String httpPrefix_;
	public static String userName_;
	public static String password_;
	
	public static Integer nTimerSeconds_;
	public static Integer nAddMosGorTrans_;  // 0 - no add, 1 - add to email and tlg, 2 - add only tlg
	public static Integer nLoadFromInbox_;
	public static Integer nDebugMode_;
	public static Integer nDaysFullLoad_;

	
	public static boolean isNeedFullReload()
	{
		return 0 == (ConfigLocal.nCurTimesForFullReload_++) % ConfigLocal.nTimesForFullReload_;
	}
	
	public String GetPropertyCtrl (Properties prop, String key) throws Exception
	{
		String sVal = prop.getProperty(key).trim();
		if(sVal == null)
			throw new Exception ("Property not found : " + key);
		return sVal;
	}

	public static String encodeUnicodeEscapesSingle(String s) 
	{
		int len= s.length();
		StringBuffer sb= new StringBuffer(len);
		for (int k=0; k<len; k++) 
		{
			char c= s.charAt(k);
			if (c=='\\' && k+6<=len && s.charAt(k+1)=='u') 
			{
				sb.append("\\uu"); 
				k++;
			} else if (c>=128) {
				sb.append("\\u" + StringUtils.leftPad(Integer.toHexString(c).toUpperCase(), 4, '0'));
			} else {
				sb.append(c);
			}
		}
	   return sb.toString();
	}
	
	public void Load(String pathFile) throws Exception
	{
		Properties prop = new Properties();
		InputStream in = null;
		in = new FileInputStream(pathFile); 

		try {
			prop.load(in);
			in.close(); 
		} catch (Exception e) {
			if(in != null)
			    in.close(); 
			throw new Exception ("Config file has errors: " + pathFile + " Errors: " + e.getMessage());
		}
		
		httpPrefix_ = GetPropertyCtrl(prop, "http_prefix");
		userName_ = GetPropertyCtrl(prop, "http_user");
		password_ = GetPropertyCtrl(prop, "http_pwd");
		
		nTimesForFullReload_ = Integer.parseInt(GetPropertyCtrl(prop, "times_full_reload"));
		nDaysAlwaysReload_ = Integer.parseInt(GetPropertyCtrl(prop, "days_always_reload"));
		nTimerSeconds_ = Integer.parseInt(GetPropertyCtrl(prop, "load_rpt_timer_in_sec"));

		nAddMosGorTrans_ = Integer.parseInt(GetPropertyCtrl(prop, "add_mosgortrans")); // 0 - no add, 1 - add to email and tlg, 2 - add only tlg
		nLoadFromInbox_ = Integer.parseInt(GetPropertyCtrl(prop, "load_from_inbox"));		
		nDebugMode_ = Integer.parseInt(GetPropertyCtrl(prop, "debug_mode"));	
		nDaysFullLoad_ = Integer.parseInt(GetPropertyCtrl(prop, "days_full_load"));	

		nMailSmtp_ = Integer.parseInt(GetPropertyCtrl(prop, "mail_smtp"));	
		nMailPop3_ = Integer.parseInt(GetPropertyCtrl(prop, "mail_pop3"));	
		sMailTo_ = GetPropertyCtrl(prop, "mail_to");	
		sMailFrom_ = GetPropertyCtrl(prop, "mail_from");	

		mail_.mail_smtp_username_ = GetPropertyCtrl(prop, "mail_smtp_username");
		mail_.mail_smtp_password_ = GetPropertyCtrl(prop, "mail_smtp_password");

		mail_.mail_smtp_starttls_ = GetPropertyCtrl(prop, "mail_smtp_starttls");
		mail_.mail_smtp_auth_ = GetPropertyCtrl(prop, "mail_smtp_auth");
		mail_.mail_smtp_host_ = GetPropertyCtrl(prop, "mail_smtp_host");
		mail_.mail_smtp_port_ = GetPropertyCtrl(prop, "mail_smtp_port");
		
		mail_.mail_pop3_protocol_ = GetPropertyCtrl(prop,  "mail_pop3_protocol");
		mail_.mail_pop3_host_ = GetPropertyCtrl(prop,  "mail_pop3_host");
		mail_.mail_pop3_port_ = GetPropertyCtrl(prop,  "mail_pop3_port");
		mail_.mail_pop3_user_ = GetPropertyCtrl(prop,  "mail_pop3_user");
		mail_.mail_pop3_pwd_ = GetPropertyCtrl(prop,  "mail_pop3_pwd");
	}
}
