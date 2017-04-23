import java.awt.Button;
import java.awt.Event;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.StringTokenizer;

/**
 * @author Mahesh Manohar
 * @id 1001396134
 * ChatAppClient implements the client side programming in the messaging system
 */
class ChatAppClient extends Frame implements Runnable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;			
	Socket client_Socket;    									//client socket
	TextField message_payload;									//Text field to type the message or payload
	TextArea displayText;										//Shows the messages communicated and the list of user online
	TextField tf_con;											//Text field to type in the target client name
	Button buttonSend,buttonClose,buttonCon;					//Send, logout and connect buttons
	String targetClient;										//Target client name
	String userName;											//Registered user name
	Thread thread=null;											//client thread
	DataOutputStream dataOutputStream;							//output stream
	DataInputStream dataInputStream;							//input stream
	String listOfUsers ="";										//list of users online
	
	
	/**Open a socket on port 8080. Open the input and the output streams.
	 * @param registered user userName
	 * @throws Exception
	 */
	ChatAppClient(String userName,String visible) throws Exception
	{
		super(userName);										
		client_Socket=new Socket("127.0.0.1",8080);										//Open a socket on port 8080. 
		dataInputStream=new DataInputStream(client_Socket.getInputStream()); 			//open input stream
		dataOutputStream=new DataOutputStream(client_Socket.getOutputStream());        	//open output stream
		dataOutputStream.writeUTF(userName+"_"+visible);								//sends registered user name and his visibility argeement to server
		String messageServer = dataInputStream.readUTF();								//reads response from server
		StringTokenizer stringTokenizer=new StringTokenizer(messageServer,"$");			//Tokenizes server response with $ as delimiter

		String response=stringTokenizer.nextToken();									//response header
		String dateR=stringTokenizer.nextToken();										//current time
		String contentLengthR=stringTokenizer.nextToken();								//Length of the content
		String msg=stringTokenizer.nextToken();											//message from server
			
		if(msg.equalsIgnoreCase("reject")){												// server sends a reject message
			System.exit(0);											
		}else{
			listOfUsers = msg;
			this.userName=userName;
			message_payload=new TextField(50);
			displayText=new TextArea(50,50);
			buttonSend=new Button("Send");
			buttonClose=new Button("Logout");
			tf_con=new TextField(50);
			buttonCon=new Button("Connect");
			thread=new Thread(this);										
			thread.start();																//Starts the current client thread
		}
	}
	@SuppressWarnings("deprecation")
	void setup()
	{
		setSize(500,300);																//sets the size of the grid
		setLayout(new GridLayout(2,1));													//sets the layout

		add(displayText);																//adds text area to the layout
		displayText.append("List of users online :"+listOfUsers);						//adds list of users who are online
		Panel p=new Panel();															//creates a panel object
		p.add(message_payload);															//adds payload text box to panel
		p.add(buttonSend);																//adds send button to panel
		p.add(buttonClose);																//adds logout button to panel
		p.add(tf_con);																	//adds  text box to add target client to  panel
		p.add(buttonCon);																//adds connect button to panel

		add(p);																			//adds panel to layout
		show();        																	//shows the window
	}
	/* (non-Javadoc)
	 * @see java.awt.Component#action(java.awt.Event, java.lang.Object)
	 * performs action on a button click
	 */
	@SuppressWarnings("deprecation")
	public boolean action(Event e,Object o)
	{

		String postRequest=null;								//request header
		String host=null;										//host 127.0.0.1
		String date=null;										//current time
		String contentLength=null;								//length of the content
		String contentType=null;								//type of the content
		String language=null;									//language of the content
		String connection=null;									//connection status of the client

		if(e.arg.equals("Connect")){							//User requests to connect to a target client

			try {
				postRequest="\r\nHEAD " + client_Socket.getInetAddress() + " HTTP/1.1\r\n"; //head request with inet address and http version
				host="Host : 127.0.0.1\r\n";									  			//host
		        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				date="Date : "+timestamp +"\r\n";											//current time
				connection="Connection : " + client_Socket.isConnected();					//client is connected or not
				
				targetClient=tf_con.getText().toString();									//target client user name
				dataOutputStream.writeUTF(postRequest+ "$"+ host+ "$" + date + "$" + " "+ "$" + " "+ "$" + " " + "$" + connection + "$" +
						userName+"$"+targetClient + "$" + "CON" +"$"+ targetClient );		//sends header and connection details to server
				tf_con.setText("");															//clears the text box
			} catch (Exception e1) {
				e1.printStackTrace();
			}    
		}
		else if(e.arg.equals("Send"))														//client requests to send the payload
		{
			try
			{if(targetClient==null){														//target client is not found
				displayText.append("\n" + "You are not connected to any client!!");    		//notifies client that there is no target client to connect to
				message_payload.setText("");
			}else{
				
				postRequest="\r\nPOST " + client_Socket.getInetAddress() + " HTTP/1.1\r\n";	//post request header with inet and http version
				host="Host : 127.0.0.1\r\n";												//host
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				date="Date : "+timestamp +"\r\n";											//current time
				contentLength="Content-Length: " + message_payload.getText().length() + "\r\n";	//length of content
				contentType="Content-Type: text; charset=utf-8\r\n";						//type of content : text
				language="Accept-Language : en-us\r\n";										//Acceptable language set as US English
				connection="Connection : " + client_Socket.isConnected();					//Status of client connection
				
				dataOutputStream.writeUTF(postRequest+ "$"+ host+ "$" + date + "$" + contentLength+ "$" + contentType+ "$" + language + "$" + connection + "$" +
						userName+"$"+targetClient + "$"  + "DATA" + "$" + message_payload.getText().toString());    //sends header and payload to server
				displayText.append("\n" + userName + " Says:" + message_payload.getText().toString());    			//displays the message on UI
				message_payload.setText("");														 				//clears the text box
			}	

			}
			catch(Exception ex)
			{
			}    
		}
		else if(e.arg.equals("Logout"))															//client requests to logout
		{
			try
			{
				postRequest="\r\nHEAD " + client_Socket.getInetAddress() + " HTTP/1.1\r\n";		//head request header with inet and http verison
				host="Host : 127.0.0.1\r\n";													//host
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				date="Date : "+timestamp +"\r\n";												//current time
				connection="Connection : " + client_Socket.isClosed();							//client closed

				dataOutputStream.writeUTF(postRequest+ "$"+ host+ "$" + date + "$" +  " "+ "$" + " "+ "$" + " " + "$" + connection + "$" +
						userName+"$"+userName + "$" + "LOGOUT" +"$"+ targetClient );			//sends HEADER and details to close the connection to server
				System.exit(1);																	//closes the window
				StringTokenizer stringTokenizer=new StringTokenizer(dataInputStream.readUTF(),"$");		//Tokenizes the message from server

				String response=stringTokenizer.nextToken();											//response header
				String dateR=stringTokenizer.nextToken();												//current time
				String contentLengthR=stringTokenizer.nextToken();										//length of content
				String msg=stringTokenizer.nextToken();													//client's message to server

				System.out.println(msg);																//displays message
			}
			catch(Exception ex)
			{
			}

		}

		return super.action(e,o);
	}
	/**
	 * Starting the client function by initializing all the needed things
	 * @param args User name and visibility agreement (Y or N) of the client to be registered 
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception
	{
		ChatAppClient Client1=new ChatAppClient(args[0],args[1]);		//one arguement with user name of client
		Client1.setup();                																//opens the client UI window
	}    
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * Thread run method which receives all the messaging from server and displays it on the text area of the client.
	 */
	public void run()
	{        
		while(true)
		{
			try
			{
				String msg=dataInputStream.readUTF();											//reads message from  server
				StringTokenizer stringTokenizer=new StringTokenizer(msg,"$");					//string tokenizes the message

				String response=stringTokenizer.nextToken();									//response header
				String dateR=stringTokenizer.nextToken();										//current time
				String contentLengthR=stringTokenizer.nextToken();								//length of content
				String message=stringTokenizer.nextToken();										//message from the server

				if(message.contains("SelfConnect")){
					displayText.append("\n Cannot connect to self!!"); 
					targetClient=null;															//sets target client to null
				}
				
				else if(message.contains("yourreject")){										//existing client tries to connect to third client
					targetClient =message.split("_")[1];										//sets back the target client
					displayText.append("\n You are connected to some other client!!");  		//displays the message
					
				}
				else if(message.equals("reject")){										//target client already connected
					displayText.append("\n" + targetClient + " is connected to some other client!!");  
					targetClient=null;													//sets target client to null
				}else if(message.equals("nolog")){										//target client not logged in
					displayText.append("\n" + targetClient + " is not logged in!!");  
					targetClient=null;													//sets target client to null
				}
				else if(message.equals("connect")){										//registered and target clients connected
					displayText.append("\n" + targetClient + " connected!!");  
				}else if(message.equals("notconnected")){								//target client is not connected to the registered client
					displayText.append("\n" + targetClient+" is not connected to you yet!!");  
				}else if(message.equals("connectedToSomeElse")){						//target client connected to another client
					displayText.append("\n" + targetClient+" is connected to connected to someone else!!");  
				}
				else{
					displayText.append( "\n" + targetClient + " Says :" + message);		//displays the communicated messages between connected clients
				}

			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
