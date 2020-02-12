import java.io.*;  // Get the Input Output libraries
import java.net.*; // Get the Java networking libraries
import java.util.StringTokenizer;

//MyWebServer is program to encourage a deeper understanding of
//http requests and responses and how a client can use a web browser
//to communicate with a server that sends back requested files/information.
//This web server sends .txt, .html, and .java files to the requester
//Not all aspects of the program are implemented fully yet, such as the fake
//cgi form and the ability to travel through/among the directories and files

class WebServWorker extends Thread {   
	Socket sock;                   		 
	WebServWorker (Socket s) {sock = s;} 
	
	//This run() method is a modificaton of the MyListener class we were given
	//we are creating a web server that sends back valid text streams to
	//the client based on their request in the firefox browser
	
	public void run(){
		// create input and output streams
		PrintStream out = null;
		BufferedReader in = null;

		try {

			//open connections to the socket
			out = new PrintStream(sock.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			//creating an end of line byte array to append to any string we need to
			//so we do not have to continue to write this out
			final byte[] EOL = {(byte) '\r', (byte) '\n'};
			
			//capture the GET request from the client and save it in variable
			//to be parsed out - to get the file name and content type, etc
			//print it out to the client moreso as a confirmation to them
			String http_request = in.readLine();
			out.println(http_request);
			out.flush();
			
			//make sure that access is denied to any upper directories
			//for security purposes - we dont want to allow access to any
			//other directories in our system through the use of ".."
			if(http_request.endsWith("..") == true){
				out.print("Access Denied!!");	
				RuntimeException error = new RuntimeException();	
				throw error;
			}

			//print out http response and the content length we got
			//from the input - this is going to be moved elsewhere
			//System.out.println("HTTP/1.1 200 OK");
			//System.out.println("Content Length: " + http_request.length());

			//extract/parse the name of the file from the GET request
			//through the string tokenizer. it will be in the format of
			//  "/dog.txt" and will need to be edited further
			String fileName = null;
			StringTokenizer token = new StringTokenizer(http_request, " ");
			if(token.nextToken().equals("GET") && token.hasMoreElements())
			{
				fileName = token.nextToken();	//save the name of the requested file ex) /dog.txt
			}

			//test the name
			//System.out.println(fileName);
			
			//get content type from this method
			String contentType = getContentType(fileName);

			
			//if the request is for the addnums.html we call this method
			//we know this because the http request will contain cgi
			//note: this method is not complete
			if (fileName.contains("cgi")) {
				addNums(fileName, out, contentType);
			}

			//if client requests this we print out directory tree by
			//calling createDir method - method also not working so
			//is commented out here for now
			else if (fileName.endsWith("/")) {
				System.out.println("Testing");
				//createDirectory(out);
			}
			
			//otherwise,  we are sending the file name and output stream, 
			//and this method will display the requested file - this does work
			else {
				displayRequestedFile(fileName, http_request, contentType, out);
			}

			sock.close(); // close this connection, but does not close server itself
		} catch (IOException x) {
			System.out.println("Connection reset. Listening again...");
		}
	}


	//MIME info: This method takes in the file name and depending on what
	//type of file was requested it sets the contentType to the proper MIME type
	public String getContentType(String fileName) {

		String contentType = null;

		//both txt and java files are considered to be plain text here
		if (fileName.endsWith(".txt") || fileName.endsWith(".java")) {
			contentType = "text/plain";
		}
		else if (fileName.endsWith(".html")) {
			contentType = "text/html";
		}
		//any other type is not supported for this particular project
		//but we could add more like gif, etc.
		else {
			contentType = "Not Supported";
		}

		return contentType;
	};


	//display the file that was requested to the client
	//sends the response header and body as well as the actual
	//content of the file

	public void displayRequestedFile(String fileName, String http_request, String contentType, PrintStream out) throws IOException {

		//the file is currently, for example, /dog.txt so we need to correct that

		String[] requestedFile = fileName.split("/");
		String correctFileName = requestedFile[1];
		
		//System.out.println("Testing new file name: " + correctFileName);

		//http response header containing all necessary information
		//and is displayed in the browser for the client
		out.print("HTTP/1.1 200 OK\n" + "Content-Length: " + http_request.length() + "\nContent-Type: " + contentType + "\r\n\r\n");  
		
		//we are also going to log some of that information to the console
		//here to ensure everything is working properly
		System.out.println("Currently serving the file: " + correctFileName + "\n with content type " + contentType + "\n and size: " + http_request.length());  

		//send the contents of the file as well using a file input stream
		File file = new File(correctFileName);
		byte[] data = new byte[1000];
		FileInputStream input = new FileInputStream(file);
		input.read(data);

		//send the data from the file to the client that requested it
		out.write(data, 0, 1000);
		out.flush();
		input.close();
	};


	//handle / create the addnums html file -- use FileWriter. this is unfinished
	//this method takes in the request string and parses it out
	//to get the name, and the numbers entered by the client and then
	//does some work to actually add the numbers and send them back
	//THIS IS A WORK IN PROGRESS
	
	public void addNums(String file, PrintStream out, String contentType) {
		
		//first, lets print out the header information
		out.println("HTTP/1.1 200 OK \n Content-Type: " + contentType + "\r\n\r\n");  // header info
		
		//from d2l - the get request looks like so:
		//GET /cgi/addnums.fake-cgi?person=Matilda&num1=4&num2=5 HTTP/1.1
		//we need to split to get the different parts
		
		String[] numInput = file.split(" ");
		String begin = numInput[0];
		
		//the following few lines split by the - symbol and then the &
		String[] next = begin.split("-");
		String queryEnd = next[1];
		String[] inputInfo = queryEnd.split("&");
		
		String name = inputInfo[0];
		String num1 = inputInfo[1];
		String num2 = inputInfo[2];
		
		//get the final values
		//save name and numbers that were entered
		//have to parse the integers

		String nameFinal = name.split("=")[1];
		String num1final = num1.split("=")[1];
		Integer number1 = Integer.parseInt(num1final);

		String num2final = num2.split("=")[1];
		Integer number2 = Integer.parseInt(num2final);

		//actually compute the final result of the two numbers entered
		Integer finalResult = number1 + number2;
		
		//once done, create the new html file and add to it
		File newPage = new File("Sum.html");
		
		try {
			FileWriter createHTMLpage = new FileWriter(newPage, false);
			createHTMLpage.write("<html><head></head><body>"); 
	
			//add in the information needed here
			createHTMLpage.write("Name entered: " + nameFinal);
			createHTMLpage.write("Answer: " + number1 + " + " + number2 + " is " + finalResult);
			
			//close the filewriter
			createHTMLpage.close();
		} catch (IOException e) {
			System.out.println("unable to create new html page");
		}
	};

	
	
	//this code was mostly given on d2l, using it to practice
	//getCanonicalPath and displaying the root directory/understanding
	//where we are in the file system
	public void showDir(String fileName, PrintStream out) {
		File f = new File(fileName);
		try{
			String directoryRoot = f.getCanonicalPath();
			System.out.print("Directory root is: " + directoryRoot + "\n");
			out.println("Directory Root: " + directoryRoot);

		}catch (Throwable e){e.printStackTrace();}

	}


	//work in progress to display directory contents including folders and files
	//a good portion of this code was given to us on D2L but we needed
	//to make the files links instead of just listing them
	
public void createDirectory(PrintStream out) throws IOException {

		File newPage = new File("Directory.html");
		FileWriter createHTML = new FileWriter(newPage, false);
		createHTML.write("<html><head></head>");

		try {
			// Create file object for the root directory
			//File f1 = new File ( "." ) ;

			// Get all the files and directory under your directory
			//and print them to the console
			File[] strFilesDirs = newPage.listFiles ( );

			for ( File f: strFilesDirs ) {
				
				String fileName = f.getName();
				if ( f.isDirectory ( ) ) 
					System.out.println ( "Directory: " + f ) ;
				else if ( f.isFile ( ) )
					System.out.println ( "File: " + f + 
							" (" + f.length ( ) + ")" ) ;
				
				//prints out the files on the browser
				//<a href=".\MyWebServer.java">.\MyWebServer.java</a><br>
				createHTML.write("<a href=\"" + fileName + "\">/" + fileName + "</a> <br>");
			}
			
			
			createHTML.flush();
			createHTML.write("</body></html>");
			createHTML.close();
		}
		
        catch (IOException x){
            System.out.println ("Could not create directory");
        }
	}


//start up the server and wait for requests from a client
//when a request arrives, spawn a worker to handle
public class MyWebServer {

	public static final boolean controlSwitch = true;

	public void main(String a[]) throws IOException {
		int q_len = 6; 
		int port = 2540;
		Socket sock;

		ServerSocket servsock = new ServerSocket(port, q_len);

		//start up the webserver and spawn a new worker thread
		System.out.println("Kris D's Web Server running at 2540.\n");
		while (controlSwitch) {
			// wait for the next client connection:
			sock = servsock.accept();
			new WebServWorker (sock).start(); 
			// try{Thread.sleep(10000);} catch(InterruptedException ex) {}
			}
		}
	}
}
