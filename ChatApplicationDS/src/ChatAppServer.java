import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * @author Mahesh Manohar
 * @id 1001396134
 * ChatAppServer class performs the function of the server which needs to be started before running any client
 */

class ChatAppServer 
{
	public static ArrayList<Socket> socket_client=null; 		// ArrayList of socket_client stores the Socket information of multiple clients that are logged in while the server is running
	public static ArrayList<String> usernames=null;			// ArrayList of usernames stores the Login names that user enters through command line while registering with the server. 
	public static ArrayList<String> onlineUsernames=null;	// ArrayList of usernames who agreed to be visible for other user while logging in by giving the command line arguemnet as Y 
	public static HashMap<String, String> connectedUser = null;// connectedUser is a hashmap that stores the loginname of the registered client as a key and the user name of the destination client as a value 
	InstantMessagingSystem_ServerClient instantMessagingSystem_ServerClient =null;								   // obClient is an object reference to the class AcceptClient
	ServerSocket socket_server=null;									   // socket_server is instance of  server socket class that is used by multiple clients to connect to a server
	int port;											  	 	// port is a number between 1 and 65535 that binds a particular server socket

	/**ChatAppServer constructor is invoked by passing the desired port number while instantiating the ChatAppServer class object. 
	 * Opens a server socket on port 8080
	 * Creates a Socket object from the ServerSocket to listen to and accept connections
	 * @param port
	 * @throws Exception
	 */
	ChatAppServer(int port) throws Exception
	{
		try{
			System.out.println("Server started on port number :"+port);// Prints the message on server notifying the start of the server along with port number
			this.port=port;											   // Sets the port number to the class variable port
			socket_server=new ServerSocket(port);								   // Opens a server socket on port 8080
			socket_client=new ArrayList<Socket>();					   // Instantiates ClientSockets arraylist described above
			usernames=new ArrayList<String>();						   // Instantiates LoginNames arraylist described above
			onlineUsernames=new ArrayList<String>();		
			connectedUser = new HashMap<String, String>();			   // Instantiates ConnectedUser hashmap described above

			while(true)
			{    
				Socket cSocket=socket_server.accept();        					   //Creates a Socket object from the ServerSocket to listen to and accept connections
				instantMessagingSystem_ServerClient=new InstantMessagingSystem_ServerClient(cSocket);					   // Passes the client socket object to the AcceptClient class that performs input and output data stream operations from and to client
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}
	/**passes the port number 8080 to the parameterized constructor
	 * No command line arguments needed
	 * @param args 
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public static void main(String args[]) throws Exception
	{

		ChatAppServer instantMessagingSystem_Server=new ChatAppServer(8080); 		//Passes the port number 8080 to the parameterized constructor
	}

	/**
	 * @author Mahesh Manohar
	 * InstantMessagingSystem_ServerClient Opens input and output streams and receives and sends messages from and to the clients.
	 * Connects to multiple clients simultaneously using multithreading
	 *
	 */
	class InstantMessagingSystem_ServerClient extends Thread
	{
		Socket client_Socket;			// The client socket
		DataInputStream dataInputStream;			// The input stream
		DataOutputStream dataOutputStream;			// The output stream
		String userList="";				// Users logged in
		
		
		/**Maintains the data base of clients and their login names
		 * Checks whether a user is already registered and displays a message if he is already connected
		 * Writes the list of logged in user names to the output stream
		 * @param client socket client_Socket
		 * @throws Exception
		 */
		InstantMessagingSystem_ServerClient (Socket cSocket) throws Exception
		{
			client_Socket=cSocket;

			dataInputStream=new DataInputStream(client_Socket.getInputStream());
			dataOutputStream=new DataOutputStream(client_Socket.getOutputStream());

			String msg=null;
			String response=null;
			String date=null;
			String contentLength=null;
			String LoginName=null;
			String visibleUser = null;
			String clientValue=dataInputStream.readUTF();                          			// reads the registered user name and visible parameter from client
			LoginName=clientValue.split("_")[0];
			visibleUser=clientValue.split("_")[1];
			if(connectedUser.get(LoginName)!=null){								// checks if the user is already connected
				System.out.println("User "+LoginName+" is already connected !");// prints a message that the user is already connected
				msg="reject";													// Re connection request rejected
				response="\r\n HTTP/1.1\r\n 400 Bad Request\r\n";				// HTTP response message with bad request
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				date="Date : "+timestamp +"\r\n";								//current time stamp
				contentLength="Content-Length : " +msg.length() +"\r\n";		// Length of the message sent to the client
				dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);	//writes the header and message information back to the client
				return;
			}
			connectedUser.put(LoginName, "");									// stores the registered user name as key and empty string as value
			System.out.println("User Logged In :" + LoginName);					//prints the logged in username
			usernames.add(LoginName);											//Adds the currently registered user to the arraylist of login names
			if(visibleUser.equalsIgnoreCase("Y")){
				onlineUsernames.add(LoginName);									//For storing all the users who have agreed to be visible to other users
			}
			for (String user : onlineUsernames)
			{
				userList += user + " | ";										// Adds all the registered users to the list of available users
			}
			socket_client.add(client_Socket);   									// Adds the current client socket to the list of client sockets
			
			dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ userList);	// Writes header and list of user information to the client
			start();															// Calls the run() to run multiple clients(threads) concurrently
		}

		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 * InstantMessagingSystem_ServerClient Opens input and output streams and receives and sends messages from and to the clients.
		 * Separates HTTP Request header from message and prints the both
		 * Handles the users that LOGOUT, offline or disconnected
		 */
		public void run()
		{
			while(true)
			{

				try
				{
					String msgFromUser=new String();
					msgFromUser=dataInputStream.readUTF();								// Reads the input message from the client
					StringTokenizer stringTokenizer=new StringTokenizer(msgFromUser,"$");	//Tokenizes the input string using $ as delimiter
					
					String postRequest=stringTokenizer.nextToken();							//POST HTTP request with inet address and HTTP version
					String host=stringTokenizer.nextToken();									// Host the client and server communcate on which is local host 127.0.0.1
					String date=stringTokenizer.nextToken();									//current time stamp
					String contentLength=stringTokenizer.nextToken();						// Length of the client message
					String contentType=stringTokenizer.nextToken();							//Type of the message
					String language=stringTokenizer.nextToken();								//Language in which the messages are communicated
					String connection=stringTokenizer.nextToken();							//Status of the connection with the client
					
					String userName=stringTokenizer.nextToken();    						// Username of the registered client
					String targetUser=stringTokenizer.nextToken();       						// Username of the targeted or destination client
					String messageHint=stringTokenizer.nextToken();								// Type of message such as Data, logout or to establish connection
					int usersCount=0;
					String status ="";
					
					String msg=null;
					String response=null;

					if(messageHint.equals("CON")){													//Request by client to connect to target client name by hitting the Connect button
						
						if(userName.equalsIgnoreCase(targetUser)){									//User tries to connect to himself
							System.out.println(postRequest + host + date + connection);			//Request header is printed
							System.out.println("Cannot connect to self");
							msg="SelfConnect";
							
							response="\r\n HTTP/1.1\r\n 400 Bad Request\r\n"; 					//Bad request header
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());	//current time 
							date="Date : "+timestamp +"\r\n";						
							contentLength="Content-Length : " +msg.length() +"\r\n";			//Length of the message
							dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);		//Response header and message sent back to the client
						}
						
						else if(connectedUser.get(targetUser)==null){									//target client is not logged in
							System.out.println(postRequest + host + date + connection);			//Request header is printed
							System.out.println("User "+targetUser+" is not logged in!!");			//User not logged in
							
							msg="nolog";														//Target user not logged in
							response="\r\n HTTP/1.1\r\n 400 Bad Request\r\n"; 					//Bad request header
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());	//current time 
							date="Date : "+timestamp +"\r\n";						
							contentLength="Content-Length : " +msg.length() +"\r\n";			//Length of the message
							dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);		//Response header and message sent back to the client
							
						}else if(!connectedUser.get(userName).equals("")){						//User tries to connect to another client while already connected with a client
							System.out.println(postRequest + host + date + connection);			//Prints the request header 
							System.out.println("User "+userName+" is already connected to another client!");//prints the message that the user is already connected
							
							msg="yourreject_"+connectedUser.get(userName);						//Request rejected as already connected with another client
							response="\r\n HTTP/1.1\r\n 400 Bad Request\r\n";					//Bad request header
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());	//current time 
							date="Date : "+timestamp +"\r\n";
							contentLength="Content-Length : " +msg.length() +"\r\n";			//Content length
							dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);		//writes the response header along with message to the client
						}
						else if(connectedUser.get(targetUser).equals("")||((String)connectedUser.get(targetUser)).equals(userName)){ //If a user requests to connect to a new user
							System.out.println(postRequest + host + date + connection);			//Prints the request header
							System.out.println("User "+userName+" is connected to "+targetUser);	//User gets connected to the requested user
							connectedUser.put(userName,targetUser);								//Stores the pair of connected clients in the map
							
							msg="connect";														//client is connected
							response="\r\n HTTP/1.1\r\n 100 User Connected\r\n";				//Informational message that the target client is connected
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());	//current time
							date="Date : "+timestamp +"\r\n";
							contentLength="Content-Length : " +msg.length() +"\r\n";			//content length
							dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);		//writes request header along with connect message to client
						}
						else if(connectedUser.get(targetUser)!=null && !((String)connectedUser.get(targetUser)).equals(userName)){ //Target user is already connected to another client
							System.out.println(postRequest + host + date + connection);									    //Prints request header
							System.out.println("User "+targetUser+" is already connected to another client!");			
														
							msg="reject";														//Rejects the request to connect to a target client
							response="\r\n HTTP/1.1\r\n 400 Bad Request\r\n";					//bad request header
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());	//current time
							date="Date : "+timestamp +"\r\n";
							contentLength="Content-Length : " +msg.length() +"\r\n";			//content length
							dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);		//response header along with connection rejected message
						} 
					}
					else if(messageHint.equals("LOGOUT"))											//Client requests to LOGOUT by hitting Logout button
					{
						for(usersCount=0;usersCount<usernames.size();usersCount++)
						{
							if(usernames.get(usersCount).equals(targetUser))							//Itreates through list of registered user names
							{
								usernames.remove(usersCount);										//removes the user that wants to logout
								//onlineUsernames.remove(usersCount);
								socket_client.remove(usersCount);									//removes the user's socket from list
								connectedUser.remove(targetUser);									//removes the user's connection element from list
								System.out.println(postRequest + host + date + connection);		//prints request header
								System.out.println("User " + targetUser +" Logged Out");			//prints the information of logged out client on server
								break;
							}
						}

					}
					else
					{
						String message="";														//Message payload
						while(stringTokenizer.hasMoreTokens())
						{
							message=message+" " +stringTokenizer.nextToken();								//adds the payload to the message
						}
						for(usersCount=0;usersCount<usernames.size();usersCount++)
						{
							if(usernames.get(usersCount).equals(targetUser))									//Iterates to get the target client information
							{    
								if(((String)connectedUser.get(targetUser)).equals(userName)){				//registered client matches the target client
									Socket clientSoc=(Socket)socket_client.get(usersCount);                      // target client socket information      
									DataOutputStream dataOutputStream=new DataOutputStream(clientSoc.getOutputStream());//Data outputstream
									dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ message);	//Response header and message written to client
									String from="From : "+userName +"\r\n";
									System.out.println(postRequest + host + date + from + contentLength + contentType + language+connection);//prints request header and the payload
									System.out.println(message.trim());									//trims the unused spaces in the payload						
									break;
								}else if(((String)connectedUser.get(targetUser)).equals("")){				//Target user not connected
									status="notconnected";											    //sets status to not connected
									break;
								}else if(!((String)connectedUser.get(targetUser)).equals(userName)){		//Target already connected
									status="connectedToSomeElse";										//Sets status to connectedtosomeoneelse
									break;
								}
								else{
									usersCount=usernames.size();											//gets the size of updated logged in users
									break;
								}
							}
						}
						if(usersCount==usernames.size())													//User with a target name not found
						{
							msg="I am offline";															//Target client offline
							response="\r\n HTTP/1.1\r\n 400 Bad Request\r\n";							//bad request
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());			//current time
							date="Date : "+timestamp +"\r\n";
							contentLength="Content-Length : " +msg.length() +"\r\n";					//content length
							dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);				//response header and offline message sent to the registered client through server
						}
						else if(status.equals("notconnected"))											//Target client not connected to registered client
						{
							msg="notconnected";															//Target client not connected
							response="\r\n HTTP/1.1\r\n 400 Bad Request\r\n";							//Bad request
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());			//current time
							date="Date : "+timestamp +"\r\n";
							contentLength="Content-Length : " +msg.length() +"\r\n";					//content length
							dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);				//response header and not connected message sent to the registered client through server
							
						}else if(status.equals("connectedToSomeElse"))									//Target client connected to another client
						{
							msg="connectedToSomeElse";													//connected to some other client
							response="\r\n HTTP/1.1\r\n 400 Bad Request\r\n";							//Bad request
							Timestamp timestamp = new Timestamp(System.currentTimeMillis());			//current time
							date="Date : "+timestamp +"\r\n";
							contentLength="Content-Length : " +msg.length() +"\r\n";					//Content length
							dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);				//response header and connected to another client message sent to the registered client through server
						}else{
							
						}
					}
					if(messageHint.equals("LOGOUT"))														//Client sends logout message to other client via server
					{
						String notify=stringTokenizer.nextToken();													//Notifies other client the current client is connected to
						for(usersCount=0;usersCount<usernames.size();usersCount++)									//Iterates through all the clients
						{
							if(usernames.get(usersCount).equals(notify))									//If the paired client is found
							{
								Socket clientSoc=(Socket)socket_client.get(usersCount);                          //Get the socket information  
								DataOutputStream dataOutputStream=new DataOutputStream(clientSoc.getOutputStream());    //Data output
								msg=" Logged out";														//Logged out message
								response="\r\n HTTP/1.1\r\n 100 Logged out\r\n";						//Informational response header 
								Timestamp timestamp = new Timestamp(System.currentTimeMillis());		//Current time
								date="Date : "+timestamp +"\r\n";
								contentLength="Content-Length : " +msg.length() +"\r\n";				//Content length
								dataOutputStream.writeUTF(response+"$"+date+"$"+contentLength+"$"+ msg);			//response header with logged out message to other client through the server
								break;
							}
						}
						break;
					}
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
			}        
		}
	}
}