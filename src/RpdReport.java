import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;

public class RpdReport 
{
	public void run() 
    {
		try {
			if(ConfigLocal.nMailPop3_ == 1)
				ConfigLocal.mail_.RunPop3();
			
			if(Core.isLoadFromInbox())
				LoadFromInbox();
			else
				LoadReports();
		} catch (Exception e) {
	    	Core.log_.error(e.getClass().getName() + " : " + e.getMessage());
		}
    }

	public String getFormatString(String str, int nFmt, boolean addSpaceToBegining)
	{
		String sDummy = "                    ";
		if(nFmt <= str.length() || nFmt >= sDummy.length())
			return str;
		
		if(addSpaceToBegining)
			return sDummy.substring(0, nFmt - str.length()) + str;
		
		return str + sDummy.substring(0, nFmt - str.length());
	}
	
	public String getJSonValue(String src, String sKey, int nFmt)
	{
		String sTag = (String)"\"" + sKey + "\":";
		int nIndexStart = src.indexOf(sTag);
		if(nIndexStart == -1)
			return "";
		String sAfter = src.substring(nIndexStart + sTag.length()); // 2 is ":
		
		int nIndexEnd = sAfter.indexOf(",");
		if(nIndexEnd == -1)
			return "";
		
		return getFormatString(sAfter.substring(0, nIndexEnd), nFmt, true);
	}
	
	private void LoadReports()
	{
		long msec = System.currentTimeMillis();
		final boolean bFullReload = ConfigLocal.isNeedFullReload();
		Core.log_.info("FullReload: " + bFullReload);
		
//		ConfigLocal.mail_.RunSmtp("rpdcodd@gmail.com", "rpdcodd@gmail.com", "test", "sText", "h:/mail_1499425874520.json", null);
//		ConfigLocal.mail_.RunPop3();//"rpdcodd@gmail.com", "rpdcodd@gmail.com", "test", "sText", "h:/mail_1499425874520.json", null);
		
		DateFormat formatter = new SimpleDateFormat("YYYY-MM-dd");
		for(int dd = 0; dd < ConfigLocal.nDaysFullLoad_; dd++)
		{
			msec -= 24 * 60 * 60 * 1000;
			String dtXml = formatter.format(new Date(msec));

			if (!bFullReload && dd >= ConfigLocal.nDaysAlwaysReload_ || dtXml.equals("2017-05-05")) // // day of run RPD. Early report not available 
			{
				Core.log_.info("Break load reports for days: " + dd);
				break;
			}
			
			ProcReportForDt(msec, dtXml);
		}
		
		try {
			Gson gson = new Gson();
			String jsonTlg = gson.toJson(Core.dsTlg_);
			SaveJsonToStore(jsonTlg, "tlg_");
			if(ConfigLocal.nMailSmtp_ == 1)
			{
				String jsonMail = gson.toJson(Core.dsMail_);
				String sPFjson = SaveJsonToStore(jsonMail, "mail_");
				ConfigLocal.mail_.RunSmtp(ConfigLocal.sMailFrom_, ConfigLocal.sMailTo_, "rpdcodd", "Please, not spam!", sPFjson, null);
			}
		} catch (Exception e)
		{
			Core.log_.error(e);
		}
	}
	
	private String SaveJsonToStore(String json, String sFNPrefix) throws IOException
	{
		byte[] utf8 = json.getBytes("UTF-8"); 
		
		String sFN = sFNPrefix + (new Long(System.currentTimeMillis()).toString()) + ".json";
		String sPFjson = ConfigLocal.dirStore_ + '/' + sFN;
		FileUtils.writeByteArrayToFile(new File(sPFjson), utf8);
		return sPFjson;
	}
	
	private void ProcReportForDt(long msec, String dtXml)
	{
		String myURLCommerce = ConfigLocal.httpPrefix_ + "/reportdata/getPlanParams?routeObject=[{\"id\":\"script.RstrRoutesHierarchy.comm\"}]&startDate=" + dtXml +
				"T00:00:00&endDate=" + dtXml + "T23:59:59&group=carrier";
		
		String sResponseCommerce = getAnswerForRequest(myURLCommerce), sResponseMGT = null;		
		if(ConfigLocal.nAddMosGorTrans_ != 0)
		{
			String myURLMosGorTrans = ConfigLocal.httpPrefix_ + "/reportdata/getPlanParams?routeObject=[{\"id\":\"script.RstrRoutesHierarchy.mgt.allmgtroutes\"}]&startDate=" + dtXml +
				"T00:00:00&endDate=" + dtXml + "T23:59:59&group=carrier";
		
			sResponseMGT = getAnswerForRequest(myURLMosGorTrans);
		}
		
		if(sResponseCommerce == null && sResponseMGT == null)
			return;

		final String dtKey = Core.GetDateFormatted(msec);

//		final String rptHeadTrips = (String)"<b>     " + dtKey + "   Рейсы</b>\n" + ConfigLocal.tableHeaderTS_ + "\n";
//		final String rptHeadTS    = (String)"<b>     " + dtKey + "   Выпуск ТС</b>\n" + ConfigLocal.tableHeaderTS_ + "\n";
//		final String rptHeadShed  = (String)"<b>     " + dtKey + "   Зачет рейсов</b>\n" + ConfigLocal.tableHeaderShed_ + "\n";
//		final String rptHeadTime  = (String)"<b>     " + dtKey + "   Пунктуальность</b>\n" + ConfigLocal.tableHeaderTime_ + "\n";

		final String rptHeadTrips = (String)"<b>     " + dtKey + "</b>\n" + ConfigLocal.tableHeaderTrips_ + "\n";
		final String rptHeadTS    = (String)"<b>     " + dtKey + "</b>\n" + ConfigLocal.tableHeaderTS_ + "\n";
		final String rptHeadShed  = (String)"<b>     " + dtKey + "</b>\n" + ConfigLocal.tableHeaderShed_ + "\n";
		final String rptHeadTime  = (String)"<b>     " + dtKey + "</b>\n" + ConfigLocal.tableHeaderTime_ + "\n";
		
		String sRptTripsCommerce = ParseAnswer(sResponseCommerce, ConfigLocal.sCommSummaryDesc_, "planTrips", "factTrips", "factTrips%");
		String sRptTsCommerce = ParseAnswer(sResponseCommerce, ConfigLocal.sCommSummaryDesc_, "planTS", "factTS", "factTS%");
		String sRptShedCommerce = ParseAnswer(sResponseCommerce, ConfigLocal.sCommSummaryDesc_, "planTrips", "tripsOnSched", "tripsOnSched%");
		String sRptOnTimeCommerce = ParseAnswer(sResponseCommerce, ConfigLocal.sCommSummaryDesc_, "planTrips", "tripsOnTime", "tripsOnTime%");

		String sRptTripsMGT= ParseAnswer(sResponseMGT, null, "planTrips", "factTrips", "factTrips%");
		String sRptTsMGT = ParseAnswer(sResponseMGT, null, "planTS", "factTS", "factTS%");
		String sRptShedMGT = ParseAnswer(sResponseMGT, null, "planTrips", "tripsOnSched", "tripsOnSched%");
		String sRptOnTimeMGT = ParseAnswer(sResponseMGT, null, "planTrips", "tripsOnTime", "tripsOnTime%");
		
		Core.put(DataStore.type_map.DS_Trips, dtKey, rptHeadTrips, sRptTripsCommerce, sRptTripsMGT);
		Core.put(DataStore.type_map.DS_TS, dtKey, rptHeadTS, sRptTsCommerce, sRptTsMGT);
		Core.put(DataStore.type_map.DS_Shed, dtKey, rptHeadShed, sRptShedCommerce, sRptShedMGT);
		Core.put(DataStore.type_map.DS_Time, dtKey, rptHeadTime, sRptOnTimeCommerce, sRptOnTimeMGT);
	}
	
	private void LoadFromInbox()
	{
		File folder = new File(ConfigLocal.dirInbox_);
		File[] listOfFiles = folder.listFiles();
//		TreeMap<Long, DataStore> mapLoadFile = new TreeMap<Long, DataStore>(); // for new data will be put over old data
		TreeMap<String, DataStore> mapLoadFile = new TreeMap<String, DataStore>(); // for new data will be put over old data
		Gson gson = new Gson();
		
		for (File file : listOfFiles) 
		{
		    if (!file.isFile())
		    	continue;
		    String sCurPathFile = file.getAbsolutePath();
		    try {
//		    	Path path = Paths.get(sCurPathFile);
//		    	Long filename = Long.parseLong(FilenameUtils.removeExtension(FilenameUtils.getName(sCurPathFile)));
		    	String filename = FilenameUtils.getName(sCurPathFile);
				
		    	byte[] utf8 = FileUtils.readFileToByteArray(file); 
		    	String jsonRead = new String(utf8, "UTF-8");
				DataStore ds = (DataStore) gson.fromJson(jsonRead, DataStore.class);
				mapLoadFile.put(filename, ds);

				Core.log_.info("Load map for " + filename + " rpt: " + jsonRead.replace('\n', '\t'));
		    } catch(Exception e)
		    {
		    	Core.log_.error("Can't load file " + sCurPathFile);
		    	Core.log_.error(e);
		    }

		    try {
		    	Core.moveFile(file.getAbsolutePath(), ConfigLocal.dirStore_ + '/' + file.getName());
		    } catch(Exception e)
		    {
		    	Core.log_.error("Can't move file to Store " + sCurPathFile);
		    	Core.log_.error(e);
		    }
		}
//	    for(Long key : mapLoadFile.keySet())
		for(String key : mapLoadFile.keySet())
	    	Core.dsTlg_.putAll(mapLoadFile.get(key));
	}
	
	private String getAnswerForRequest(String myURL)
	{
		String sRptRes = null;
		try {
			URL url = new URL(myURL);
			String nullFragment = null;
			URI uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), nullFragment);
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(uri); // myURL);
			httpGet.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(ConfigLocal.userName_, ConfigLocal.password_), "UTF-8", false));
		
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			
			sRptRes = EntityUtils.toString(entity);
		} catch(Exception e)
		{
			Core.log_.info ("Error for request: " + myURL);
			Core.log_.error(e);
		}
		return sRptRes;
	}

	private String ParseAnswer(String retSrc, String summaryDesc, String tagPlan, String tagFact, String tagPerc) //throws JsonParseException, JsonMappingException, IOException
	{
		if(retSrc == null || retSrc.isEmpty())
			return null;
		
		String answer = "", separator = "\"date\":\"";
		if(retSrc.indexOf(separator) == -1)
		{
			Core.log_.info("Error response: " + retSrc);
			return null;
		}
		String[] sarr = retSrc.split(separator);
		String sDate = null;

		int nFormat = 5;
		Integer nSumPlan = 0, nSumFact = 0;
		for(String sLine: sarr)
		{
			if(sDate == null)
			{
				if(sarr.length > 1)
				{
					int nIndexEnd = sarr[1].indexOf("\"");
					sDate = sarr[1].substring(0, nIndexEnd); 
				}
				continue;				
			}
			String sLabel = getJSonValue(sLine, "carrier", 0).replaceAll("\"","");
			String sLabelCompact = Core.mapLabels_.get(sLabel.trim());
			if(sLabelCompact != null)
				sLabel = sLabelCompact;

			boolean isMGT = sLabel.equals("МГТ");
				
//			int nFormat = isMGT ? 5 : 5;
			
//			String sPlan = getJSonValue(sLine, "planTrips", nFormat);
//			String sFact = getJSonValue(sLine, "factTrips", nFormat);
//			String sPerc = getJSonValue(sLine, "factTrips%", 0) + "%";			
			String sPlan = getJSonValue(sLine, tagPlan, isMGT ? nFormat + 1 : nFormat);
			String sFact = getJSonValue(sLine, tagFact, isMGT ? nFormat : nFormat);
			String sPerc = getJSonValue(sLine, tagPerc, 0) + "%";			
			
			answer += getFormatString(sLabel, isMGT ? 8 : 9, false) + " " + sPlan + " " + sFact + " " + sPerc + "\n";
			
			if(summaryDesc != null)
			{
				try {
					nSumPlan += Integer.parseInt(sPlan.trim());
					nSumFact += Integer.parseInt(sFact.trim());
				} catch(Exception e)
				{
					Core.log_.error("Parse for summary " + tagPlan + "=\"" + sPlan + "\" " + tagFact + "=\"" + sFact + "\"");
					Core.log_.error(e);
				}
			}
		}
		if(summaryDesc != null)
		{
			Double dPerc = nSumPlan == 0 ? 0. : (100. * nSumFact) / nSumPlan; 
			answer += (String)"" + getFormatString(summaryDesc, 9, false) + " " + 
					getFormatString(nSumPlan.toString(), nFormat, true) + " " + 
					getFormatString(nSumFact.toString(), nFormat, true) + " " +
					String.format("%.1f", dPerc).replace(',', '.') + "%\n";
			
		}
		return answer;
	}
}
