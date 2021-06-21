package com.simple.logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

public class RequestHandler implements Runnable {

   	/**
	 * Socket connected to client passed by Proxy server
	 */
	Socket clientSocket;

	/**
	 * Read data client sends to proxy
	 */
	BufferedReader proxyToClientBr;

	/**
	 * Send data from proxy to client
	 */
	BufferedWriter proxyToClientBw;

	/**
	 * Creates a ReuqestHandler object capable of servicing HTTP(S) GET requests
	 * @param clientSocket socket connected to the client
	 */
	public RequestHandler(Socket clientSocket){
		this.clientSocket = clientSocket;
		try{
			this.clientSocket.setSoTimeout(2000);
			proxyToClientBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		}
		catch ( IOException e ) {
			System.out.println( "Error buffering client socket" );
		}
	}

    public void headerLogger(BufferedReader br) {
	    System.out.println("Logging headers... \n");
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error logging headers");
        }
    }

	@Override
	public void run() {

		// Get Request from client
		String requestString;
		try{
			requestString = proxyToClientBr.readLine();
		} catch (IOException e) {
			System.out.println("Error reading request from client");
			return;
		}

		// Log Headers
        headerLogger( proxyToClientBr );

		// Parse out URL
		System.out.println("Request Received " + requestString);
		// Get the Request type
		String request = requestString.substring(0,requestString.indexOf(' '));

		// remove request type and space
		String urlString = requestString.substring(requestString.indexOf(' ')+1);

		// Remove everything past next space
		urlString = urlString.substring(0, urlString.indexOf(' '));

		// Prepend http:// if necessary to create correct URL
		if(!urlString.substring(0,4).equals("http")){
			String temp = "http://";
			urlString = temp + urlString;
		}

        System.out.println("HTTP GET for : " + urlString + "\n");
        processRequest(urlString);
	}

    private void processRequest(String urlString) {
        try {
            URL remoteURL = new URL(urlString);
            // Create a connection to remote server
            HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL.openConnection();
            proxyToServerCon.setUseCaches(false);
            proxyToServerCon.setDoOutput(true);

            // Create Buffered Reader from remote Server
            BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));

            // Send success code to client
            String line = "HTTP/1.0 200 OK\n" +
                    "Proxy-agent: ProxyServer/1.0\n" +
                    "\r\n";
            proxyToClientBw.write(line);


            // Read from input stream between proxy and remote server
            while ((line = proxyToServerBR.readLine()) != null) {
                // Send on data to client
                proxyToClientBw.write(line);
            }

            // Ensure all data is sent by this point
            proxyToClientBw.flush();

            // Close Down Resources
            if (proxyToServerBR != null) {
                proxyToServerBR.close();
            }
        }
        catch( Exception e ){
            System.out.println( "Error connecting to remote server" );
        }
    }
}
