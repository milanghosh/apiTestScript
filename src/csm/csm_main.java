package csm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.xml.soap.SAAJResult;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

//import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

public class csm_main {
	static String oppath = "C:\\Users\\MilanGhosh\\Desktop\\Eclipse\\"; /* Path of input file and log file */ 
	static String oppath1 = "C:\\Users\\MilanGhosh\\Desktop\\Eclipse\\";/* Path of sys log file */
static String logtime;
static StringBuilder result = new StringBuilder();  
	
	public static void main(String[] args) throws IOException, JSchException, InterruptedException {
		
		Session session = server_conn("172.24.174.85","dbillse","Comviva@123");
		csm_main readxlsx = new csm_main();
	
		/*JFileChooser c = new JFileChooser();
		c.showOpenDialog(c);
		String filename = c.getSelectedFile().getAbsolutePath();
		System.out.println("Test Case File Name is : "+ filename);
		*/
		
		String filename = oppath+"4G_scenarios.xlsx";
		String headers[] = {"TC","MSISDN","OP1","OP2","CIRCLE"};
	
		int headlength = headers.length;
		int cellindexes[] ;
		InputStream XlsxFileToRead = null;
		Workbook workbook = null;
	
		try {
			XlsxFileToRead = new FileInputStream(filename);
			try {
				workbook = WorkbookFactory.create(XlsxFileToRead);
			} catch (EncryptedDocumentException e) {
				e.printStackTrace();
			} catch (InvalidFormatException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Sheet sheet = workbook.getSheetAt(0);
		int lastrow = sheet.getLastRowNum();
		
		cellindexes = readxlsx.header_index_search(sheet,headers,lastrow,headlength);
		//System.out.println(cellindexes[0]+","+cellindexes[1]+","+cellindexes[2]+","+cellindexes[3]);
		
		for (int startrow = cellindexes[0]+1;startrow<=lastrow;startrow++){
			Row row  = sheet.getRow(startrow);
			System.out.println("Test Case row number is : "+startrow);	

			   result.append(System.getProperty("line.separator"));
			   result.append(System.getProperty("line.separator"));
			  result.append("Test Case row number is : "+startrow);
			
			  
		String tc_values[] =readxlsx.test_data_search(cellindexes,sheet,row,headlength);

		  result.append(System.getProperty("line.separator"));
		  result.append("Test Case ID is : "+tc_values[0]);
			StringBuilder result1 = null;  
	    	result1 = new StringBuilder();
	    	 result1.append(System.getProperty("line.separator"));
			  result1.append("Test Case ID is : "+tc_values[0]);
			file_writer(result1, oppath);
		System.out.println("Test Case Values are : ");
		   result.append(System.getProperty("line.separator"));
		    result.append("Test Case Values are : ");
for (int i=0;i<tc_values.length;i++)
{
	System.out.println("Value of header "+headers[i]+" is : "+tc_values[i]);
	result.append(System.getProperty("line.separator"));
	result.append("Value of header "+headers[i]+" is : "+tc_values[i]);s
}

String api = url_creator(tc_values);
api = "curl -ivk "+"\""+api+"\"";
boolean log =false; 
Calendar cal = Calendar.getInstance();
SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
System.out.println( sdf.format(cal.getTime()) );
result.append(System.getProperty("line.separator"));
result.append( sdf.format(cal.getTime()) );
//logtime = sdf.format(cal.getTime());

log = channel(api,session,log,"CSM curl");

Thread.sleep(3000);

String cmd0 =  "cat /App/App/jboss-eap-4.3/jboss-as/server/SE/log/server.log | grep \"Request Land on CSM From APP NODE\" | tail -1f | awk '{print $2}' ";
log = channel(cmd0,session,log,"CSM server timestamp");
if (log == true) {
	result.append("Logs found = true");
}
else
{
	result.append("Logs found = false");
}
String cmd =  "sed -n " +"\""+"/Request Land on CSM From APP NODE/,/Final Response send to APP Node/p"+"\"" +" /App/App/jboss-eap-4.3/jboss-as/server/SE/log/server.log | awk '$2 >= "+"\""+logtime+"\""+"'";
log = channel(cmd,session,log,"CSM server logs");
if (log == true) {
	result.append("Logs found = true");
}
else
{
	result.append("Logs found = false");
}
		}	
		server_disconn(session);
		file_writer(result, oppath1);
	}
	
private static String url_creator(String[] tc_values) throws IOException {
		String urlPattern = "http://172.24.174.85:8280/reqUrl?" ;
		//String headers[] = {"TC","MSISDN","OP1","OP2","Circle"};
		
		String op1= tc_values[2];
		String op2= tc_values[3];
		//String bearer =tc_values[2];
		String msisdn = tc_values[1];
		String circlecode=tc_values[4];
		
	/*	HttpURLConnection con = null;
		DataInputStream is =null;
		int errorCode = 0;
	    int READ_TIME_OUT =3000;
	    int CONNECT_TIME_OUT =3000;
	    String str, resp = "";
	    URL url=null;
	    */
	   String api = urlPattern + "msisdn="+msisdn+"&authKey=gprsAct&msg=ACTRECH&op1="+op1+"&op2="+op2+"&op3=-1&op4=4G3GTEST&op5="+circlecode+"&bearer=Flexi"+"&mode=2";
	 /*   try {
			url = new URL(urlPattern + "msisdn="+msisdn+"&authKey=gprsAct&msg=ACTRECH&op1="+op1+"&op2="+op2+"&op3=-1&op4=PLAN_2G&op5="+circlecode+"&bearer=Flexi"+"&mode=2");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}	
*/
   	
/*	try {
			con = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		con.setReadTimeout(READ_TIME_OUT);
		con.setConnectTimeout(CONNECT_TIME_OUT);

		try {
			errorCode = con.getResponseCode();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			is = new DataInputStream(con.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (null != ((str = is.readLine()))) {
			if (str.length() > 0) {
				str = str.trim();
				if (!str.equals("")) {
					resp += str;
				}
			}
		}
	*/
	   result.append(System.getProperty("line.separator"));
	   result.append(" CSM URL to be hit is "+api );;
		System.out.println(" CSM URL to be hit is "+api );		
	//	System.out.println("Response from CSM = " +resp );

		return api;
}
	
	private static boolean channel(String cmd,Session session,boolean log,String logname) throws JSchException, IOException {
		Channel channel = session.openChannel("exec");
		((com.jcraft.jsch.ChannelExec)channel).setCommand(cmd);
	    channel.connect();  
		System.out.println("Executing query: "+cmd);
		 result.append(System.getProperty("line.separator"));
		   result.append("Executing query: "+cmd);
			
		InputStream in = (InputStream) channel.getInputStream();
	    	BufferedReader  br = new BufferedReader(new InputStreamReader(in));
	      
	    	StringBuilder result1 = null;  
	    	result1 = new StringBuilder();
	    	
	       String line = null;
	     
	       // String logname = logname;
	       result1.append(System.getProperty("line.separator"));
	       result1.append(logname);
	       result1.append(System.getProperty("line.separator"));
	      
	       while ((line = br.readLine()) != null) {
	    	   	 result1.append(line);
	    	   	 if(logname.contains("timestamp")) {
	    	   		 logtime = line;
	    	   		 System.out.println("Setting value of logtimestamp as "+logtime);
	    			 result.append(System.getProperty("line.separator"));
	    			   result.append("Setting value of logtimestamp as "+logtime);
	    	   	 }
	            result1.append(System.getProperty("line.separator"));
	            log =true;

	        }
	       file_writer(result1,oppath);
		return log;
	}
	private static void file_writer(StringBuilder result,String oppath) throws IOException {
		String logfile = oppath+"log.txt";
		File file = new File(logfile);
		BufferedWriter writer = null;
		try {
		    writer = new BufferedWriter(new FileWriter(file,true));
		    writer.write(result.toString());
		    writer.close();
		    System.out.println("logs written in file "+logfile);
		    result.append(System.getProperty("line.separator"));
			result.append("logs written in file "+logfile);
		} finally {
		    if (writer != null) writer.close();
		}	
	}	

	private static void server_disconn(Session session) {

Session[] Session = {session};
int size = Session.length;
for (int a=0; a<size; a++)
{
Session[a].disconnect();
int b=a+1;
System.out.println("Disconnected from OM Node "+b+".....");
result.append(System.getProperty("line.separator"));
result.append("Disconnected from OM Node "+b+".....");
}
}

	private static Session server_conn(String hostname,String username,String password){
	     JSch jsch = new JSch();
	     Session session = null;
	     System.out.println("Connecting to server " +hostname+ " .....");


	     result.append("Connecting to server " +hostname+ " .....");
	     result.append(System.getProperty("line.separator"));
	     
	     try {
	  	session = jsch.getSession(username, hostname, 22);
     Properties config = new Properties();
     config.put("StrictHostKeyChecking", "no");
     session.setConfig(config);
     session.setPassword(password);
     session.connect(); 
     System.out.println("Connected successfully to server " +hostname+ " .....");

	     result.append(System.getProperty("line.separator"));
	     result.append("Connected successfully to server " +hostname+ " .....");
	     
} catch (JSchException e) {
   e.printStackTrace();
	  System.out.println("Problem in connecting to server " +hostname+ "'Please check the configurations'...");
	  result.append(System.getProperty("line.separator"));
	     result.append("Problem in connecting to server " +hostname+ "'Please check the configurations'...");
   }
return session;
}

		private String[] test_data_search(int[] cellindexes, Sheet sheet, Row row, int headlength) {
		
String tc_values[] = new String [cellindexes.length-1];
/*
	for (int startrow = cellindexes[0]+1;startrow<=lastrow;startrow++){
	Row row  = sheet.getRow(startrow);
System.out.println("Test Case row number is : "+startrow);	
*/	
for (int i=1;i<=headlength;i++){
	
	int cellno = cellindexes[i];
	Cell cell = row.getCell(cellno);
	
	if (cell == null)
	{
		System.out.println("Value is BLANK");
		//System.out.println("");
		tc_values[i-1] = "";
	}
	else{ 
	if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
	//	System.out.println(cell.getStringCellValue() + " ");
		tc_values[i-1] = cell.getStringCellValue();
	} 
else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
		
		if (DateUtil.isCellDateFormatted(cell)) {
           System.out.println("Date value is : "+cell.getDateCellValue());
           Format formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
           String reportDate = formatter.format(cell.getDateCellValue());
           System.out.println(reportDate);
           tc_values[i-1]=reportDate;
       } else {
           
		//System.out.println(cell.getStringCellValue() + " ");
		cell.setCellType(Cell.CELL_TYPE_STRING);
		tc_values[i-1] = cell.getStringCellValue();
       }
	}
	if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
		System.out.println("Value is blank : ");
		tc_values[i-1] = "";
	}	else if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
		cell.setCellType(Cell.CELL_TYPE_STRING);
		//System.out.println(cell.getBooleanCellValue() + " ");
		tc_values[i-1] = cell.getStringCellValue();
}
	}
	}
	return tc_values;
	
//}
	}

	public int[] header_index_search(Sheet sheet, String[] headers, int lastrow, int headlength) {
		
		Row row;
		Cell cell;
		int r = 0;int c = 0;
	System.out.println("Last row number is : "+lastrow);
	result.append(System.getProperty("line.separator"));
   result.append("Last row number is : "+lastrow);
       //ArrayList<Integer> s = new ArrayList<Integer>();
       int cellindexes[] = new int[headlength+1];
       boolean find = false;
   for(int i=0;i<lastrow;i++){
	
	while(find == false){
			row  = sheet.getRow(i);
			int lastcell = row.getLastCellNum();
			System.out.println("Last cell number is : "+lastcell);
			result.append(System.getProperty("line.separator"));
		    result.append("Last cell number is : "+lastcell);
for(int k=0;k<headlength;k++){	
for(int j=0;j<lastcell;j++){
			cell = row.getCell(j);
 if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
				//System.out.println(cell.getStringCellValue());
 if (cell.getRichStringCellValue().getString().trim().equals(headers[k])) {

find = true;
System.out.println("Value matched");
result.append(System.getProperty("line.separator"));
result.append("Value matched");
System.out.println(cell.getStringCellValue());
			 		 r = row.getRowNum();  
                    System.out.println("Row number for header " +headers[k] +": " + r);
                    result.append(System.getProperty("line.separator"));
                    result.append("Row number for header " +headers[k] +": " + r);
                    
                    c = cell.getColumnIndex();
                    System.out.println("Column number for header " +headers[k] +": " + c);
                    result.append(System.getProperty("line.separator"));
                    result.append("Column number for header " +headers[k] +": " + c);
                    
                    cellindexes[k+1]=c;
                    cellindexes[0]=r;
			 	}
			}
			 	}
			}
	}
		}		
return cellindexes;
}
}