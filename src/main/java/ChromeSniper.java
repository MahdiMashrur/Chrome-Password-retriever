import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

//import com.sun.jna. platform.win32.Crypt32Util;
//import com.sun.jna.platform.KeyboardUtils;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Crypt32Util;

import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class ChromeSniper
{
	public static boolean shorten=true;
	public static boolean obfus=false;
	public static void main( String args[] ) throws InterruptedException, IOException{
		boolean overwrite=false;
		ArrayList<Character>flags=parseFlags(args);
//		if (!has('q',flags)) intro();
//		if (has('o',flags)) obfus=true;
//		if (has('f',flags)) shorten=false;
		if (!(System.getProperty("os.name").contains("Windows"))){
			System.out.printf("Your OS (%s) is not supported! :(",System.getProperty("os.name"));
			System.exit(-1);
		}
		ArrayList<String>list=getChromeInfo();
		File pwdDump=new File(System.getProperty("user.dir")+"\\pwd.dump");
		if (pwdDump.exists()) if (pwdDump.delete()) overwrite=true;
		FileOutputStream dumpFile=new FileOutputStream(pwdDump);
		System.out.printf("=======Chrome information for %s=======\n\n",System.getProperty("user.name"));
		for (String s:list){
			System.out.println(s);
			dumpFile.write(s.getBytes());
			dumpFile.write("\n".getBytes());
		}
		dumpFile.flush();
		dumpFile.close();
		if (overwrite) System.out.println("Old dump file overwritten!");
		System.out.printf("Passwords dumped to %s\\pwd.dump, exiting.",System.getProperty("user.dir"));
		System.exit(0);
	}
	public static ArrayList<Character> parseFlags(String[]args){
		if ((args.length!=0)&&(args[0].contains("help"))){ System.out.printf("Usage: java -jar ChromeRecuv.jar FLAGS\n\n\t-o=obfuscate\n\t-q=quiet\n\t-f=fullUrl"); System.exit(-1); }
		ArrayList<Character>toRet=new ArrayList<Character>();
		for(String s:args){
			if ((s.contains("-"))&&(s.length()==2)){
				toRet.add((char)s.charAt(1));
			}
		}
		return toRet;
	}
	public static boolean has(Object a,ArrayList<?>list){
		if (find(a,list)!=-1) return true;
		return false;
	}
	public static int find(Object a,ArrayList<?>list){
		int i=0;
		for (Object b:list){
			if (b.equals(a)) return i;
			i++;
		}
		return -1;
	}
	public static void intro() throws InterruptedException, IOException{
		flushScr();
		String s="\r\n" +
				 "    Chrome Password Decrypter\r\n\n";
		for (char c:s.toCharArray()){
			System.out.print(c);
			Thread.sleep(3);
		}
//		while (!KeyboardUtils.isPressed(70)){
//			Thread.sleep(1);
//		}
		flushScr();
	}
	public static void flushScr(){
		for (int i=0;i<100;i++) System.out.println();
	}
	public static ArrayList<String>getChromeInfo(){
		ArrayList<String>toRet=new ArrayList<String>();
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			String location= "jdbc:sqlite:C:\\Users\\"+System.getProperty("user.name")+"\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Login Data" ;
			c = DriverManager.getConnection(location);
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery( "SELECT * FROM logins" );
			//ResultSet rs = stmt.executeQuery("SELECT origin_url, username_value, password_value FROM logins");
			while ( rs.next() ) {
				String url = rs.getString("action_url");
				if (url==null) url="Not found/corrupted";
				else if ((url.length()>40)&&(shorten)) url=url.substring(0, 40)+"...";
				String username = rs.getString("username_value");
				if (username==null) username="Not found/corrupted";
				InputStream passwordHashStream = rs.getBinaryStream("password_value");
				if (!obfus) toRet.add(String.format("URL:%s\nUsername:%-35s | Password:%-20s\n",url,username,encryptedBinaryStreamToDecryptedString(passwordHashStream)));
				else toRet.add(String.format("URL:%s\nUsername:%-35s | Password:<Obfuscation Mode Enabled>\n",url,username));
			}
			rs.close();
			stmt.close();
			c.close();
		} catch ( Exception e ) {
			System.exit(0);
		}
		return toRet;
	}
	public static String encryptedBinaryStreamToDecryptedString(InputStream is) throws IOException{
		StringBuilder toRet2=new StringBuilder();
		byte[]toRet=Crypt32Util.cryptUnprotectData(hexStringToByteArray(streamToString(is)));
		for (byte b:toRet){
			toRet2.append((char)b);
		}
		return toRet2.toString();
	}
	public static byte[]hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
	public static String streamToString(InputStream b) throws IOException{
		StringBuilder toRet=new StringBuilder();
		String s;
		while (b.available()>0){
			s=String.format("%s",Integer.toHexString(b.read()));
			if (s.length()==1) toRet.append("0"+s+"");
			else toRet.append(s+"");
		}
		b.close();
		return toRet.toString();
	}
}