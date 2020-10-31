import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;


public class App {
	public static String endpointServer = "empty";
	public static String ip = "empty";
	public static String name = "empty";
	public static String hash = "empty";
	public static String fileName = "empty";
	
    public static void main(String[] args) {
    	//IP::13.13.13.13
    	//NAME::DanielOliveira
    	//HASH::5f9b549a516b0b0902f09921
    	//FILENAME::dgo/git/...
    	//SERVER::http://0cc544a67828.ngrok.io/api/v1/resources
    	
    	for (String arg : args) {

			/*
			 * Caso o usuário passo o argumento minusculo ou maiusculo,
			 * receberemos da mesma forma, assim evitamos o CASE SENSITIVE.
			 */
			arg = arg.trim();

			if (arg.contains("IP:")) {

				String[] argIp = arg.split("::");
				ip = argIp[1];
			} else if (arg.contains("FILENAME:")) {
					String[] argFileName = arg.split("::");
					fileName = argFileName[1];
			} else if (arg.contains("NAME:")) {
				String[] argName = arg.split("::");
				name = argName[1];
			} else if (arg.contains("HASH:")) {
				String[] argHash = arg.split("::");
				hash = argHash[1];
			} else if (arg.contains("SERVER:")) {
				String[] argServer = arg.split("::");
				endpointServer = argServer[1];
			} 
    	}
			
			System.out.println(endpointServer);
			System.out.println(ip);
			System.out.println(name);
			System.out.println(hash);
			System.out.println(fileName);
			
    	try {
			post(endpointServer+"/peer", mapPeer(ip, name, hash, fileName));
			
			new Thread() {

			    @Override
			    public void run() {
			    	
			    	Timer timer = new Timer();
			        timer.schedule(new TimerTask() {
			            public void run() {
			                try {
			                    get(endpointServer + "/peer/heartbeat/" + name);
			                } catch (Exception e) {
			                    System.out.print("HeartBeat failed");
			                    e.printStackTrace();
			                }
			      }}, 0, 1000);}}.start();
			  
			      
			  new Thread(openSocketServer).start();
			  
			  
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	//System.out.println(hey);

    }
    
    public static ArrayList<String> get(String urlRequest) {
    	try {

            URL url = new URL(urlRequest);//your url i.e fetch data from .
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP Error code : "
                        + conn.getResponseCode());
            }
            InputStreamReader in = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(in);
            String nextLine;
            ArrayList<String> output = new ArrayList<String>();
            while ((nextLine = br.readLine()) != null) {
                System.out.println(nextLine);
                output.add(nextLine);
            }
            conn.disconnect();
            return output;

        } catch (Exception e) {
            System.out.println("Exception in NetClientGet:- " + e);
        }
    	return null;
    }

    public static void post(String urlRequest, String body) throws IOException {
    	URL url = new URL (urlRequest);
		
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("POST");
		
		con.setRequestProperty("Content-Type", "application/json; utf-8");
		con.setRequestProperty("Accept", "application/json");
		
		con.setDoOutput(true);
		
		//JSON String need to be constructed for the specific resource. 
		//We may construct complex JSON using any third-party JSON libraries such as jackson or org.json
		String jsonInputString = body;
		
		try(OutputStream os = con.getOutputStream()){
			byte[] input = jsonInputString.getBytes("utf-8");
			os.write(input, 0, input.length);			
		}

		int code = con.getResponseCode();
		System.out.println(code);
		
		try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))){
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			System.out.println(response.toString());
		}
		
    }
    
    public static String mapPeer(String ip, String name, String hash, String fileName) {
    	return "{\"ip\": \""+ip+"\", \"name\": \""+name+"\", "
				+ "\"files\": ["
				+ "    {"
				+ "      \"hash\": \""+hash+"\","
				+ "      \"name\": \""+fileName+"\""
				+ "    }"
				+ "  ]}";
    }
    
    private static Runnable openSocketServer = () -> {
        try {
            ServerSocket server = null;

            server = new ServerSocket(4444);
            System.out.println("Socket Server created ");

            boolean done = false;

            while (!done) {
                Socket client = server.accept(); // get sender
                DataInputStream dIn = new DataInputStream(client.getInputStream());
                byte messageType = dIn.readByte(); // read message

                switch (messageType) {
                    case 1: // file solicitation
                        String fileToSend = dIn.readUTF();
                        System.out.println("Requested file: " + fileToSend);
                        try {
                            Socket peerServer = new Socket(client.getInetAddress(), 4444);

                            DataOutputStream dOut = new DataOutputStream(peerServer.getOutputStream());

                            FileInputStream fis = new FileInputStream(fileToSend);
                            String[] file = fileToSend.split("\\.");
                            String[] fileName = file[0].split(Pattern.quote(File.separator));
                            String extension = "." + fileName[fileName.length - 1] + "-" + file[1] + ".";
                            byte[] extensionByte = extension.getBytes();
                            for (byte b : extensionByte) {
                                dOut.write(b);
                            }
                            byte[] buffer = new byte[4096];
                            int count;
                            while ((count = fis.read(buffer)) >= 0) {
                                dOut.write(buffer, 0, count);
                            }


                            fis.close();
                            dOut.close();

                        } catch (Exception e) {
                            System.out.println("Erro ao enviar arquivo ao solicitante");
                            e.printStackTrace();
                        }
                        break;
                    default: // receiving file
                        System.out.println("File received");

                        DataInputStream dis = new DataInputStream(client.getInputStream());
                        byte[] searchBuffer = dis.readAllBytes();
                        byte[] fileContent = new byte[0];
                        byte[] fileExtension = new byte[0];
                        byte[] fileName = new byte[0];
                        boolean hasName = false;
                        boolean hasFileExtension = false;
                        int fileNameLastByte = 0;
                        int count = 0;
                        for (byte infoByte : searchBuffer) {
                            // verify if byte == "-"
                            if (infoByte == 45) {
                                if (!hasName) {
                                    fileName = Arrays.copyOfRange(searchBuffer, 0, count);
                                    fileNameLastByte = count;
                                    hasName = true;
                                }
                            }
                            // verify if byte == "."
                            if (infoByte == 46) {
                                if (!hasFileExtension) {
                                    fileExtension = Arrays.copyOfRange(searchBuffer, fileNameLastByte + 1, count);
                                    fileContent = Arrays.copyOfRange(searchBuffer, count + 1, searchBuffer.length);
                                    hasFileExtension = true;
                                }
                            }
                            count++;
                        }

                        FileOutputStream fos = new FileOutputStream("" + new String(fileName) + "." + new String(fileExtension));

                        int totalRead = 0;
                        for (byte info : fileContent) {
                            fos.write(info);
                            totalRead++;
                        }
                        System.out.println("Read " + totalRead + " bytes.");

                        fos.close();
                        dis.close();
                        break;
                }
                client.close();
                dIn.close();
            }

        } catch (IOException e) {
            System.out.println("Failed to create socket server");
            e.printStackTrace();
        }
    };
}