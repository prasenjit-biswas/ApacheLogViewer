import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream.GetField;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import javax.swing.JOptionPane;

//import LogViewer.MyUserInfo;



import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * 
 * This class is intended to run on local system
 *
 */
public class ApacheEzTestLogViewer {

	/**
	 * @param args
	 */
	public static void main(String[] arg) {
		
		String userName = "";
		String password = "";
		
		StringBuilder timeInMins = new StringBuilder();
		StringBuilder hostName = new StringBuilder();
		StringBuilder filePath = new StringBuilder();
		StringBuilder logDate = new StringBuilder();
		StringBuilder command = new StringBuilder();;
		
		String propaertFilePath = "/home/eztusr/ApacheLogViewer/ApacheEztestLogViewer.properties";
		
		try{
			if(arg.length == 2){
				userName = arg[0];
				password = arg[1];
				loadProperties(propaertFilePath,hostName,timeInMins,filePath, command);
				System.out.println(hostName+"   "+timeInMins+"  "+filePath+" "+command );
				String result = getLog(userName, hostName.toString(), password, Integer.parseInt(timeInMins.toString()), command.toString(), logDate);
				createFile(hostName.toString(), logDate, result, filePath.toString());
			}else{
				System.out.println(" Please provide UserName, Password in argument.");
			}
		}catch(Exception ex){
			System.out.println(" Please provide UserName, Password in argument.");
			ex.printStackTrace();
		}
	}

	
	public static void loadProperties(String propaertFilePath,StringBuilder hostName, StringBuilder timeInMins, StringBuilder filePath, StringBuilder command) throws Exception{
		Properties prop = new Properties();
		try{
			prop.load(new FileInputStream(propaertFilePath));
			hostName.append(prop.getProperty("host"));
			timeInMins.append(prop.getProperty("timeDiffInMins"));
			command.append(prop.getProperty("cmd"));
			filePath.append(prop.getProperty("storeFilePath"));
		}catch(Exception ex){
			System.out.println(" Problem while loading properties file from location "+propaertFilePath+" message : "+ex.getMessage());
			ex.printStackTrace();
			throw ex;
		}
	}
	
	public static String getLog( String userName, String hostName, String password, int timeInMins, String command, StringBuilder logDate) throws Exception{
		StringBuilder theResult = new StringBuilder();
		String[] hostArr = hostName.split(",");	
		try{	
				for(int k =0; k < hostArr.length; k++){
					String host = hostArr[k];
					System.out.println(" host "+host);
					if(host == null || ("").equals(host)){
						System.out.println("HOST IS COMING AS NULL");
						throw new Exception("HOST IS COMING AS NULL");
					}
					try {
						JSch jsch = new JSch();
						Session session = jsch.getSession(userName, host, 22);
						session.setPassword(password);
						System.out.println(userName+"*" + host+"*" + password+"*" +timeInMins+"*");
			
						UserInfo ui = new MyUserInfo() {
							public void showMessage(String message) {
								System.out.println(">>>>>>>>>>>>>>>"+message);
							}
			
							public boolean promptYesNo(String message) {
								Object[] options = { "yes", "no" };
								int foo = 0;
								return foo == 0;
							}
						};
			
						session.setUserInfo(ui);
						session.connect(30000); // making a connection with timeout.
						Channel channel=session.openChannel("exec");
						//String cmd =  "cat /logs/nodeslog/ezto.log | awk '$2 >= \"currentDate\" && $2 <= \"currentDate\" && $3 >= \"previousTime,000\" && $3 <= \"currentTime,000\"'";//arg[3];
						String cmd = command;
						cmd = modifyDateAndTime(cmd,timeInMins,logDate);
						System.out.println(" cmd : "+cmd);
						channel.setInputStream(null);
			     		((ChannelExec)channel).setCommand(cmd);
						channel.setOutputStream(System.out);
						
						InputStream in = channel.getInputStream();
						 ((ChannelExec)channel).setErrStream(System.err);
						 channel.connect();
						 
						 StringBuilder sb = new StringBuilder(); 
						 
						 
						 while(true){
							 int length = in.available();
							 //System.out.println("length : "+length);
							 byte[] tmp=new byte[length];
							 /*while(length>0){
								 int i=in.read(tmp, 0, 1024);
								 if(i<0)break;
								 System.out.println(new String(tmp, 0, i));
								 sb.append(new String(tmp, 0, i));
							 }*/
							 
							 int soFar = 0;
							 int remaining;
							 int incoming;
							 while (soFar < length) 
							 {
								remaining = (int) length - soFar;
								incoming = in.read(tmp, soFar, remaining);
								System.out.println(new String(tmp));
								//sb.append(new String(tmp));
								soFar += incoming;
							 }
							 if(channel.isClosed()){
								 System.out.println("exit-status: "+channel.getExitStatus());
								 break;
							 }
						 try{Thread.sleep(1000);}catch(Exception ee){}
						 }
						 channel.disconnect();
						 session.disconnect();
						 theResult.append(sb.toString());
					} catch (Exception e) {
						System.out.println(e);
						e.printStackTrace();
						throw e;
					}
			}
		}catch(Exception ex){
			System.out.println(" Exception Occued "+ex.getMessage());
			ex.printStackTrace();
			throw ex;
		}
		return theResult.toString();
	}
	
	
	public static void createFile(String hostName, StringBuilder logDate, String result, String filePath) throws Exception{
		System.out.println("logDate" +logDate);
		String currDate = logDate.toString().replace("/", "-");
		currDate = currDate.replace(":", "-");
		hostName = hostName.replace(".", "-");
		//System.out.println("result:"+result);
		FileOutputStream fop = null;
		File file;
		String fileName = "apacheLog_"+currDate+".txt";//"apacheLog.txt";//
		if(result != null && !("").equals(result)){
			try{
				file = new File(filePath+fileName);
				if (!file.exists()) {
					file.createNewFile();
				}
				fop = new FileOutputStream(file);
				fop.write(result.getBytes());
				fop.flush();
				fop.close();
				System.out.println(" File Created with Name "+fileName+ " in Path "+filePath);
			}catch(Exception ex){
				System.out.println(" Problem with Creating file name "+fileName+"  "+ex.getMessage());
				ex.printStackTrace();
			}finally{
				try {
					if (fop != null) {
						fop.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}else{
			System.out.println(" Unable to Create file "+fileName+" as coming data is empty.");
		}
	}
	
	
	
    public static String modifyDateAndTime(String command, int givenTimeInMin, StringBuilder logDate) throws Exception{
      String pattern = "dd/MMM/yyyy:HH:mm:ss";
  	  SimpleDateFormat sdf = new SimpleDateFormat(pattern);
  	  long currentTime = System.currentTimeMillis();
  	  sdf.setTimeZone(TimeZone.getTimeZone("EST5EDT"));
  	  Date date = new Date(currentTime);
  	  String currDate = sdf.format(date);
  	  
  	  long prevTime = currentTime - (1000*60*givenTimeInMin);
  	  Date prevdate = new Date(prevTime);
	  String previousdate = sdf.format(prevdate);
  	   
  	  command = command.replace("previousTime", previousdate);
  	  command = command.replace("currentTime", currDate);
  	  logDate.append(currDate);		
  	  return command;
    }
	
	public static abstract class MyUserInfo implements UserInfo,
			UIKeyboardInteractive {
		public String getPassword() {
			return null;
		}

		public boolean promptYesNo(String str) {
			return false;
		}

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return false;
		}

		public boolean promptPassword(String message) {
			return false;
		}

		public void showMessage(String message) {
		}

		public String[] promptKeyboardInteractive(String destination,
				String name, String instruction, String[] prompt, boolean[] echo) {
			return null;
		}
	}


	}


