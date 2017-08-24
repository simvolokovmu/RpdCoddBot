import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Core 
{
	static public Logger log_ = Logger.getLogger(Core.class.getName());
	static final ConfigLocal config_ = new ConfigLocal ();
	static public HashMap<String, String> mapLabels_ = new HashMap<String, String>();
	static final long dayInMSec_ = 24*60*60*1000;
	
	static public DataStore dsTlg_ = new DataStore ();
	static public DataStore dsMail_ = null;

	public static void put(DataStore.type_map type, String key, String rptHead, String rptCommerce, String rptMgt)
	{
		Map<String, String> mapTlg = getMap(dsTlg_, type), mapMail = getMap(dsMail_, type);

		String sRptResOnlyCommerce = rptHead;
		sRptResOnlyCommerce += "<pre>";
		sRptResOnlyCommerce += rptCommerce;
		String sRptResWithMGT = sRptResOnlyCommerce;

		if(rptMgt != null)
			sRptResWithMGT += rptMgt;
		sRptResOnlyCommerce += "\n</pre>";
		sRptResWithMGT += "\n</pre>";
		
		switch(ConfigLocal.nAddMosGorTrans_)
		{
		case 0:
			mapTlg.put(key, sRptResOnlyCommerce);
			if(mapMail != null)
				mapMail.put(key, sRptResOnlyCommerce);
			break;
		case 1:
			mapTlg.put(key, sRptResWithMGT);
			if(mapMail != null)
				mapMail.put(key, sRptResWithMGT);
			break;
		case 2:
			mapTlg.put(key, sRptResWithMGT);
			if(mapMail != null)
				mapMail.put(key, sRptResOnlyCommerce);
			break;
		default:
			log_.error("Param add_mosgortrans must be 0, 1 or 2");
		}
		log_.info("RptComm=" + sRptResOnlyCommerce.replace('\n', '\t'));
		if(rptMgt != null)
			log_.info("RptMGT=" + sRptResWithMGT.replace('\n', '\t'));
	}

	public static Map<String, String> getMap(DataStore ds, DataStore.type_map type)
	{
		if(ds == null)
			return null;
		
		switch (type) 
		{
		case DS_Trips:
			return ds.mapTrips_;
		case DS_TS :
			return ds.mapTS_;
		case DS_Shed :
			return ds.mapShed_;
		case DS_Time :
			return ds.mapTime_;
		}
		return null;
	}
	
	public static boolean isLoadFromInbox()
	{
		return ConfigLocal.nLoadFromInbox_ == 1;
	}
	
	public static boolean isProductiveMode()
	{
		return ConfigLocal.nDebugMode_ == 0;
	}

	private static void initializeLogger()
	{
		Properties logProperties = new Properties();
		try {
			// load our log4j properties / configuration file
			logProperties.load(new FileInputStream(ConfigLocal.LOG_PROPERTIES_FILE));
			PropertyConfigurator.configure(logProperties);
			log_.info("Logging initialized.");
		} catch(IOException e) {
			throw new RuntimeException("Unable to load logging property " + ConfigLocal.LOG_PROPERTIES_FILE);
		}
	}

	public static void initDir(String path) throws Exception
	{
		if (!Files.exists(Paths.get(path)) && !((new File(path)).mkdirs()))
			throw new Exception("Faild create dir: " + path);
	}

	private static void initMapLabels()
	{
		mapLabels_.clear();
//		mapLabels_.put("Гепарт", "Гепарт");
		mapLabels_.put("ТаксоПарк20", "Т.Парк20");
		mapLabels_.put("Трансавтолиз","Т.Автолиз");
//		mapLabels_.put("ГорТакс","");
//		mapLabels_.put("ТК «Рико»","");
		mapLabels_.put("Альфа Грант","АльфГрант");
//		mapLabels_.put("АвтоКарз","");
//		mapLabels_.put("Транс-Вей","");
	}	
	
	public static void stop()
	{
		Core.log_.info("Proccess correctly stopped");
		System.exit(0);
	}
	
	public static void initialize () throws Exception 
    {
    	initializeLogger();
    	initMapLabels();
    	ConfigLocal.mail_.log_ = Core.log_;
    	config_.Load(ConfigLocal.CFG_PROPERTIES_FILE);

    	if(ConfigLocal.nMailSmtp_ != 0)
    		dsMail_ = new DataStore();
    	
    	initDir(ConfigLocal.dirStore_);
    	initDir(ConfigLocal.dirInbox_);
		ConfigLocal.mail_.dirAttSave_ = ConfigLocal.dirInbox_;
		
    	if(ConfigLocal.nMailPop3_ != 0 || ConfigLocal.nMailSmtp_ != 0)
    	{
    		initDir(ConfigLocal.dirEMail_);
    		ConfigLocal.mail_.dirEmlPop3Save_ = ConfigLocal.dirEMail_; 
    		ConfigLocal.mail_.dirEmlSmtpSave_ = ConfigLocal.dirEMail_;
    	}
    }

	public static boolean moveFile(String srcPF, String dstDir) throws Exception
	{	
		return null != Files.move (Paths.get(srcPF), Paths.get(dstDir), StandardCopyOption.REPLACE_EXISTING);
	}

	public static void Run()
	{
		try {
		 Runnable threadMainByPeriod = new Runnable() 
		 {
		      public void run() 
		      {
		  		ExecutorService service = Executors.newSingleThreadExecutor();
				try {
				    Runnable threadSlaveWithInterrupted = new Runnable() {
				        @Override
				        public void run() 
				        {
				        	final String uuid = UUID.randomUUID().toString();
				        	
					        long time0 = System.currentTimeMillis();
							log_.info("Start RpdReport " + uuid);

							RpdReport rpt = new RpdReport (); 
							rpt.run ();
							
					    	long time1 = System.currentTimeMillis();
					        log_.info("Finish RpdReport " + uuid + " time(sec): " + (time1 - time0)/1000.);
				        }
				    };

				    Future<?> f = service.submit(threadSlaveWithInterrupted);
				    f.get(Core.config_.nTimerSeconds_, TimeUnit.SECONDS);     // attempt the task for n seconds
				}	catch (final InterruptedException e)	{
				    // The thread was interrupted during sleep, wait or join
			        log_.info("ExecutorService: The thread was interrupted during sleep, wait or join");
				} catch (final TimeoutException e) {
				    // Took too long!
			        log_.info("ExecutorService: Took too long!");
				}	catch (final ExecutionException e) {
				    // An exception from within the Runnable task
			        log_.info("ExecutorService: An exception from within the Runnable task");
				}	finally {
			        log_.info("ExecutorService: ShutDown");
				    service.shutdown();
				}
		      }
		    };
		    
			ScheduledExecutorService service = Executors
	                .newSingleThreadScheduledExecutor();
			service.scheduleAtFixedRate(threadMainByPeriod, 0, Core.config_.nTimerSeconds_ + 1, TimeUnit.SECONDS);
			
//			SendMail mail = new SendMail();
//			mail.SendMailAtt("h:/_workspace/RpdCoddBot/Store/1498731948036.json", "1498731948036.json");

		} catch (Exception e)
		{
	        log_.error("ThreadMain : " + e.getMessage());
		}
    }
	
	public static String getAppendReport (Map<String, String> map, String key)
	{
		String res = map.get(key);
		if(res == null)
			return "";
		return res;		
	}
	
	public static String GetReportAll (String sDt)
	{
//		String sResp = new String();
		String res = "";
		res += getAppendReport (Core.dsTlg_.mapTime_, sDt);
		res += getAppendReport (Core.dsTlg_.mapShed_, sDt);
		res += getAppendReport (Core.dsTlg_.mapTrips_, sDt);
		res += getAppendReport (Core.dsTlg_.mapTS_, sDt);
		if(res.isEmpty())
			return sDt + " - Нет данных";
		return res;		
	}
	
	public static String GetDateFormatted(long msec)
	{
		Date date = new Date(msec);
		DateFormat formatter = new SimpleDateFormat("E dd.MM", new Locale("ru")); // Mo/Tu/We/Th/Fr/Sa/Su for English
		return formatter.format(date);
	}
	/*
	public static String GetRpdYstd()
	{
		String sDtYstd = GetDateFormatted(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
		return GetReportAll(sDtYstd);
//		String sResp = new String();
//		String res = Core.mapDtRpt_.get(sDtYstd);
//		sResp += res == null ? sDtYstd + " - Нет данных" : res; 
		
//		return sResp; 
	}
*/
	public static LinkedList<String> GetWeek(long msec)
	{
		LinkedList<String> lsDateWeek = new LinkedList<String>();
		while(true)
		{
			msec -= dayInMSec_;
			String dt = GetDateFormatted(msec);
			if(lsDateWeek.isEmpty())
			{
				if (dt.startsWith("Вс"))
					lsDateWeek.push(dt);
				continue;
			}
			lsDateWeek.push(dt);
			if (dt.startsWith("Пн"))
				break;
		}
		return lsDateWeek;
	}
/*	
	public static String GetRpdWeek()
	{
		String sResp = new String();
		
		for(String dtCur : GetWeek(System.currentTimeMillis()))
		{
			sResp += GetReportAll(dtCur);
			sResp += "\n";
		}
*/		
//		sResp += "<b>   Пн 19.06\nПеревозчик  План  Факт  %</b>\n<pre>Гепарт 3009 2965 98.5%\nТ.Парк20 4861 4671 96%\nТ.Автолиз 19198 17488 91.8%\nГорТакси 719 713 99.1%\nТК «Рико» 1138 1068 93.8%\nАльфГрант 1797 1664 92.5%\nАвтоКарз 336 336 100%\nТранс-Вей 106 97 91.5%\nМГТ 130692 89264 68.3%</pre>";	
/*
		String sResp = new String();
		for(String dtCur : GetWeek(System.currentTimeMillis()))
		{
			String res = Core.mapDtRpt_.get(dtCur);
			sResp += res == null ? dtCur + " - Нет данных" : res; 
			sResp += "\n";
		}
*/
//		return sResp; 
//	}

	public static String GetRpdDay(String sCmd)
	{
		String [] sarr = sCmd.split(" ");
		String dtRes = null;
		if(sarr.length == 2)
		{
			long msec = System.currentTimeMillis();
			for(int dd = 0; dd < 365; dd++, msec -= 24 * 60 * 60 * 1000)
			{
				String dt = GetDateFormatted(msec);
				int index = dt.indexOf(sarr[1]);
				if(index == 3 || index == 4 && dt.indexOf("01.") == 3) // after day of week
				{
					dtRes = dt;
					break;
				}
			}
		}
		
		if(dtRes == null)
			return "Неверный формат команды \"день дд.мм\".\nПример команды: \"день 29.06\"";
		
		String sResp = new String();
//		sResp += dtRes + "\n";
		
//		String res = Core.mapDtRpt_.get(dtRes);
		String res = GetReportAll(dtRes);
		sResp += res == null ? dtRes + " - Нет данных" : res; 
		
		return sResp; 
	}

	public static String GetRpdForType (boolean bForWeek, String sCmd)
	{
		Map<String, String> mapSrc;
		if (sCmd.equalsIgnoreCase(ConfigLocal.cmdRpdOnTrips_))
		{
			mapSrc = Core.dsTlg_.mapTrips_;
		} else if (sCmd.equalsIgnoreCase(ConfigLocal.cmdRpdOnTS_))
		{
			mapSrc = Core.dsTlg_.mapTS_;
		} else if (sCmd.equalsIgnoreCase(ConfigLocal.cmdRpdOnShed_))
		{
			mapSrc = Core.dsTlg_.mapShed_;
		} else if (sCmd.equalsIgnoreCase(ConfigLocal.cmdRpdOnTime_))
		{
			mapSrc = Core.dsTlg_.mapTime_;
		} else
			return ConfigLocal.cmdUknown_;
		
		String sRptRes = new String();
		
		if(bForWeek)
		{
			for(String dtCur : GetWeek(System.currentTimeMillis()))
			{
				String sRes = getAppendReport (mapSrc, dtCur);
				if(sRes.isEmpty())
					sRptRes += dtCur + " - Нет данных\n";
				else
					sRptRes += sRes;
			}
		} else {
			String dtYstd = GetDateFormatted(System.currentTimeMillis() - dayInMSec_);
			String sRes = getAppendReport (mapSrc, dtYstd);
			if(sRes.isEmpty())
				sRptRes += dtYstd + " - Нет данных\n";
			else
				sRptRes += sRes;
		}
		return sRptRes;
	}
}
    