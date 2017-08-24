import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.ApiContextInitializer;
// import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
 
public class RpdCoddBot extends TelegramLongPollingBot 
{
	private boolean bWaitEnterDay_ = false;
	private boolean bWaitEnterYstd_ = false;
	private boolean bWaitEnterWeek_ = false;
	
	public static void main(String[] args) 
	{
		
		ApiContextInitializer.init();
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
		try {
        	Core.initialize();
        	Core.Run();
        	
        	
			telegramBotsApi.registerBot(new RpdCoddBot());
//		} catch (TelegramApiException e) {
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public String getBotUsername() //"ИМЯ_ПОЛЬЗОВАТЕЛЯ_ВАШЕГО_БОТА"; 
	{
		switch(ConfigLocal.nDebugMode_)
		{
		case 0: // productive
			return "rpdcoddbot";
		case 1: // test contour
			return "mbr123bot";
		case 2: // dev contour
			return "mbr123devbot";
		case 3: // dev contour 2
			return "testrpdcoddbot";
		}
		Core.log_.error("Debug mode not found!!");
		return "mbr123devbot";
	}
 
	@Override
	public String getBotToken() // "ВАШ_ТОКЕН";
	{
		switch(ConfigLocal.nDebugMode_)
		{
		case 0: // productive
			return "343451250:AAEh2uNONMOt_xW70qqf28GFlsngzbZKMNg";
		case 1: // test contour
			return "431017377:AAG5ZAhp58QCI9O5c_8LWMQ3TTm9s0p9y-Q";
		case 2: // dev contour
			return "418928118:AAF6x2qOdnkUrjChKC0RC5vGLsHh-7K4apE";
		case 3: // dev contour 2
			return "423736077:AAHf2S72q6bZLKaxsWirFrgNHWNuzrQC4kU";
		}
		Core.log_.error("Debug mode not found!!");
		return "418928118:AAF6x2qOdnkUrjChKC0RC5vGLsHh-7K4apE";
	}

	@Override
	public void onUpdateReceived(Update update) 
	{

		Message message = update.getMessage();
		if (message == null || !message.hasText())
			return ;

		String sCmd = message.getText().trim().toLowerCase().replace("  ", " ");
		if (sCmd.equalsIgnoreCase(ConfigLocal.cmdHelp_))
			sendMsg(message, ConfigLocal.descHelp_);
		else if (sCmd.equalsIgnoreCase(ConfigLocal.cmdKill9_))
		{
			sendMsg(message, "Service will be stopped");				
			Core.stop(); // terminate service				
		} else if(bWaitEnterWeek_)
		{
			bWaitEnterWeek_ = false;
			sendMsg(message, Core.GetRpdForType(true, sCmd));
		} else if(bWaitEnterYstd_)
		{
			bWaitEnterYstd_ = false;
			sendMsg(message, Core.GetRpdForType(false, sCmd));
		} else if (sCmd.equalsIgnoreCase(ConfigLocal.cmdRpdYstd_))
		{
			bWaitEnterYstd_ = true;
			sendMsg(message, "Выберите тип отчета");
//			sendMsg(message, Core.GetRpdForType(false, ConfigLocal.cmdRpdOnTrips_));
		} else if (sCmd.equalsIgnoreCase(ConfigLocal.cmdRpdWeek_))
		{
			bWaitEnterWeek_ = true;			
			sendMsg(message, "Выберите тип отчета");
		} else if (sCmd.equalsIgnoreCase(ConfigLocal.cmdRpdYstdTrips_))
		{			
			sendMsg(message, Core.GetRpdForType(false, ConfigLocal.cmdRpdOnTrips_));
		} else if (sCmd.equalsIgnoreCase(ConfigLocal.cmdRpdYstdTS_))
		{
			sendMsg(message, Core.GetRpdForType(false, ConfigLocal.cmdRpdOnTS_));
		} else if (sCmd.startsWith(ConfigLocal.cmdRpdDay_.toLowerCase()))
		{
			if (sCmd.equalsIgnoreCase(ConfigLocal.cmdRpdDay_))
			{
				bWaitEnterDay_ = true;
				sendMsg(message, "Введите дату в формате dd.mm");
			} else
				sendMsg(message, Core.GetRpdDay(sCmd));
		} else if(sCmd.length() == 8 && sCmd.indexOf('.') == 5 || sCmd.length() == 5 && sCmd.indexOf('.') == 2) // "Пн 25.06" or "14.05"
		{
			sendMsg(message, Core.GetRpdDay("день " + (sCmd.length() == 8 ? sCmd.substring(3) : sCmd)));
		} else
			sendMsg(message, ConfigLocal.cmdUknown_);

	}
 
	private void sendMsg(Message message, String text) 
	{
		SendMessage sendMessage = new SendMessage();
		sendMessage.enableMarkdown(true);
		sendMessage.setChatId(message.getChatId().toString());
		sendMessage.setReplyToMessageId(message.getMessageId());
		sendMessage.setText(text);
		sendMessage.setParseMode("HTML");
		sendMessage.enableHtml(true);
		
		ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
		kb.setResizeKeyboard(true);
		List<KeyboardRow> lsKb = new ArrayList<KeyboardRow>();

		if(bWaitEnterDay_)
		{
			bWaitEnterDay_ = false;
			List<KeyboardRow> lsRow = new ArrayList<KeyboardRow>();
			for(int i = 0; i < 7; i++)
				lsRow.add(new KeyboardRow());
				
			long msec = System.currentTimeMillis() + 7*Core.dayInMSec_; // full current week
			for(int week = 0; week < 4; week++)
			{
				int d = 0;
				for(String sDt : Core.GetWeek(msec))
					lsRow.get(d++).add(new KeyboardButton(sDt));

				msec -= 7 * Core.dayInMSec_;
			}
			for(KeyboardRow row : lsRow)
				lsKb.add(row);
			Collections.reverse(lsKb);
		} else if(bWaitEnterWeek_ || bWaitEnterYstd_)
		{
			List<KeyboardRow> lsRow = new ArrayList<KeyboardRow>();
			KeyboardRow row1 = new KeyboardRow();
			row1.add(new KeyboardButton(ConfigLocal.cmdRpdOnTime_));
			row1.add(new KeyboardButton(ConfigLocal.cmdRpdOnTS_));

			KeyboardRow row2 = new KeyboardRow();
			row2.add(new KeyboardButton(ConfigLocal.cmdRpdOnShed_));
			row2.add(new KeyboardButton(ConfigLocal.cmdRpdOnTrips_));
				
			lsKb.add(row1);
			lsKb.add(row2);
		} else {
			List<KeyboardRow> lsRow = new ArrayList<KeyboardRow>();
			KeyboardRow row1 = new KeyboardRow();
			row1.add(new KeyboardButton(ConfigLocal.cmdRpdWeek_));
			row1.add(new KeyboardButton(ConfigLocal.cmdRpdYstdTS_));

			KeyboardRow row2 = new KeyboardRow();
			row2.add(new KeyboardButton(ConfigLocal.cmdRpdDay_));
			row2.add(new KeyboardButton(ConfigLocal.cmdRpdYstdTrips_));
				
			lsKb.add(row1);
			lsKb.add(row2);

//			KeyboardRow row = new KeyboardRow();
//			row.add(new KeyboardButton("День"));
//			row.add(new KeyboardButton("Вчера"));
//			row.add(new KeyboardButton("Неделя"));
//			lsKb.add(row);
		}
		kb.setKeyboard(lsKb);
		sendMessage.setReplyMarkup(kb);
		try {
			sendMessage(sendMessage);
//		} catch (TelegramApiException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}