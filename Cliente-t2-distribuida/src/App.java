import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


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
			post(endpointServer+"/peer", 
					mapPeer(ip, name, hash, fileName));
			
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
			            }
			        }, 0, 1000);

			    }
			  }.start();
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
}