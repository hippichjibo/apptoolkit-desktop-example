package com.jibo.apptoolkit_desktop_example;

import com.jibo.apptoolkit_desktop_example.JiboWebSocketListener;
import com.jibo.apptoolkit_desktop_example.ErrorInterceptor;
import com.jibo.apptoolkit_desktop_example.JiboX509TrustManager;
import com.jibo.apptoolkit.protocol.CommandLibrary;
import com.jibo.apptoolkit.protocol.utils.Commons;

import java.net.URI;

import org.codehaus.httpcache4j.uri.QueryParam;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.security.KeyStore;
import java.util.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

import org.json.JSONArray;
import java.nio.charset.StandardCharsets;

import okio.Buffer;

public class App {
    // each app should have a unique client id and client secret

    private static String mClientId = "client-id-here";
    private static String mClientSecret = "client-secret-here";

    private static String mUsername = "username-or-prompt";
    private static String mPassword = "password-or-prompt";
    
    private static String mAccessToken = "";
    private static String mTokenType = "";

    public static void main( String[] args ) {
        
        final String endpoint = Commons.PROD_ENDPOINT;
        
        try {
            HttpClient client = new DefaultHttpClient();

            // exchange the oauth code for an access token
            {
                // build the url receive the access token
                URI getTokenUri = URIBuilder.empty().withScheme("https").addPath(endpoint).addPath("token").toURI();
                HttpPost getTokenPost = new HttpPost(getTokenUri);
                getTokenPost.setHeader("User-Agent", "Mozilla/5.0");
                // tell the server we want to accept json as a response
                getTokenPost.setHeader("Accept", "application/json");
                // tell the server our data is json
                getTokenPost.setHeader("Content-type", "application/json");

                // the required parameters for requesting an access token
                JSONObject param = new JSONObject();
                param.put("grant_type", "password");
                param.put("client_id", mClientId);
                param.put("client_secret", mClientSecret);
                param.put("username", mUsername);
                param.put("password", mPassword);
    
                // attach the json parameters to the POST request
                StringEntity entity = new StringEntity(param.toString(), StandardCharsets.UTF_8.displayName());
                getTokenPost.setEntity(entity);

                // execute the POST request and synchronously wait for a response
                HttpResponse getTokenResponse = client.execute(getTokenPost);
                System.out.println("\nSending 'POST' request to URL : " + getTokenPost.getURI().toString());
                System.out.println("Post parameters : " + getTokenPost.getEntity());
                System.out.println("Response Code : " + getTokenResponse.getStatusLine().getStatusCode());
                System.out.println("reason phrase? : " + getTokenResponse.getStatusLine().getReasonPhrase());

                // load the response into a byte array output stream and translate the byte array into a string
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                getTokenResponse.getEntity().writeTo(outputStream);
                String getTokenResponseString = new String(outputStream.toByteArray(),"UTF-8");

                // turn the response string into a JSONObject which we can parse for the expected response fields
                JSONObject responseJson = new JSONObject(getTokenResponseString);
                mTokenType = responseJson.getString("token_type");
                System.out.println("We got an access token: " + responseJson.getString("access_token"));
                mAccessToken = responseJson.getString("access_token");
            }

            String robotFriendlyId = "";

            // get the list of robot id's we're allowed to connect to
            {
                // build the url to retrieve the list of robots you're allowed to connect to from this app
                URI getRobotListUri = URIBuilder.empty().withScheme("https").addPath(endpoint).addPath("rom").addPath("v1").addPath("robots").toURI();
                HttpGet getRobotListGet = new HttpGet(getRobotListUri);
                getRobotListGet.setHeader("User-Agent", "Mozilla/5.0");
                // tell the server we're expecting json as a response
                getRobotListGet.setHeader("Accept", "application/json");
                // tell the server we're sending json data
                getRobotListGet.setHeader("Content-type", "application/json");
                // the token type and access token are provided in the header
                getRobotListGet.setHeader("Authorization", mTokenType + " " + mAccessToken);
                
                // make the call to the server and synchronously wait for a response
                HttpResponse getRobotListResponse = client.execute(getRobotListGet);

                // load the response into a byte array output stream and convert that to a string
                System.out.println("Response Code : " + getRobotListResponse.getStatusLine().getStatusCode());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                getRobotListResponse.getEntity().writeTo(outputStream);
                String getRobotListResponseString = new String(outputStream.toByteArray(),"UTF-8");
                
                System.out.println("response string: " + getRobotListResponseString);
                // parse the string into a JSON object to query for the list of robots
                JSONObject getRobotListJsonObject = new JSONObject(getRobotListResponseString);
                JSONArray robotListJsonArray = getRobotListJsonObject.getJSONArray("data");

                // bomb out of the app early if we got an empty list of robots to connect to
                if(robotListJsonArray.length() == 0) {
                    System.out.println("Empty robot list.");
                    return;
                }
                // for the purposes of demonstration, we are just grabbing the first robot off this list
                // ideally you should know which robot you are connecting to and verify the robot name against 
                // the list of robots returned from this REST call
                robotFriendlyId = robotListJsonArray.getJSONObject(0).getString("robotName");
            }

            // setup client with robot ids
            {
                // create the url to create the certificate
                URI certificateCreationUri = URIBuilder.empty().withScheme("https").addPath(endpoint).addPath("rom").addPath("v1").addPath("certificates").toURI();
                HttpPost certificateCreationPost = new HttpPost(certificateCreationUri);
                certificateCreationPost.setHeader("User-Agent", "Mozilla/5.0");
                // let the server know we're expecting json in the response
                certificateCreationPost.setHeader("Accept", "application/json");
                // let the server know we're sending json data
                certificateCreationPost.setHeader("Content-type", "application/json");
                // the token type and access token are provided in the header
                certificateCreationPost.setHeader("Authorization", mTokenType + " " + mAccessToken);

                // provide the robot friendly id name to indicate which robot we want to connect to
                JSONObject param = new JSONObject();
                param.put("friendlyId", robotFriendlyId);
    
                StringEntity entity = new StringEntity(param.toString(), StandardCharsets.UTF_8.displayName());
                certificateCreationPost.setEntity(entity);

                // send the request and wait for a response
                HttpResponse certificateCreationResponse = client.execute(certificateCreationPost);
                System.out.println("Response Code : " + certificateCreationResponse.getStatusLine().getStatusCode());

                // turn the response into a byte array output stream and convert that to a string
                // there is no data we need to save from this response. this just prints the response to the console
                // for demonstration purposes.
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                certificateCreationResponse.getEntity().writeTo(outputStream);
                String certificateCreationResponseString = new String(outputStream.toByteArray(),"UTF-8");
                System.out.println("response string: " + certificateCreationResponseString);
            }

            String robotIpAddress = "";
            // some arbitrary number of times.  on a good connection this should resolve after 5 - 6 tries tops.
            // much more than that is there is likely some kind of problem. 
            final int MAX_TRIES = 40;
            int numTries = 0;
            boolean gotCertificate = false;
            String p12Certificate = "";
            String certificateFingerprint = "";
            // certificate retrieval
            {
                // we loop until we're able to successfully able to get the certificate data back from the server
                // since the certificate creation is an asynchronous operation from when we requested the certificate creation
                // this shouldn't take too long
                while(!gotCertificate && numTries++ < MAX_TRIES) {
                    // create the url to retrieve the certificate. 
                    // the robot id is supplied as a parameter
                    URI certificateRetrievalUri = URIBuilder.empty().withScheme("https")
                    .addPath(endpoint)
                    .addPath("rom")
                    .addPath("v1")
                    .addPath("certificates")
                    .addPath("client")
                    .addParameter(new QueryParam("friendlyId", robotFriendlyId))
                    .toURI();
                    HttpGet certificateRetrievalGet = new HttpGet(certificateRetrievalUri);
                    certificateRetrievalGet.setHeader("User-Agent", "Mozilla/5.0");
                    // let the server know we expect json as a response
                    certificateRetrievalGet.setHeader("Accept", "application/json");
                    // let the server know we're sending json data
                    certificateRetrievalGet.setHeader("Content-type", "application/json");
                    // supply the token tye and the access token to retreive the certificate data
                    certificateRetrievalGet.setHeader("Authorization", mTokenType + " " + mAccessToken);

                    // send the request and wait for a response
                    System.out.println("\nSending 'GET' request to URL : " + certificateRetrievalGet.getURI().toString());
                    HttpResponse certificateRetrievalResponse = client.execute(certificateRetrievalGet);
                    System.out.println("Response Code : " + certificateRetrievalResponse.getStatusLine().getStatusCode());

                    // a 200 code means success
                    if(certificateRetrievalResponse.getStatusLine().getStatusCode() == 200) {
                        // flag that we got the certificate so we stop looping
                        gotCertificate = true;
                        
                        // feed the response to a byte array output stream and convert that to a string
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        certificateRetrievalResponse.getEntity().writeTo(outputStream);
                        String certificateRetrievalResponseString = new String(outputStream.toByteArray(),"UTF-8");

                        // turn the string into a JSON object so we can query for certificate data
                        JSONObject certificateRetrievalJsonObject = new JSONObject(certificateRetrievalResponseString);

                        // the ip address of the robot which we will connect to via websocket
                        robotIpAddress = certificateRetrievalJsonObject.getJSONObject("data").getJSONObject("payload").getString("ipAddress");
                        // the base64 encoded p12 certificate
                        p12Certificate = certificateRetrievalJsonObject.getJSONObject("data").getString("p12");
                        // the unique identifier for this certificate in the x509 binary format
                        certificateFingerprint = certificateRetrievalJsonObject.getJSONObject("data").getString("fingerprint");

                        System.out.println("Recieved a certificate! Robot ip address: " + robotIpAddress);
                    }
                    else {
                        certificateRetrievalGet.abort();
                        Thread.sleep(5000);
                    }
                }

                // bomb out of the applictation if we're unable to get a certificate
                if(!gotCertificate) {
                    System.out.println("Unable to retrieve a certificate. Exiting application.");
                    return;
                }
            }

            // connect directly to robot
            {
                // create a keychain store in memory using the PKCS12 syntax
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                Buffer buffer = new Buffer();
                // decode the base64 encoded p12 certificate
                // use an empty string as the password
                keyStore.load(buffer.write(Base64.getDecoder().decode(p12Certificate)).inputStream(), "".toCharArray());
                buffer.close();
        
                // specify that the certificate fingerprint is using a x509 binary format 
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                // provide an empty password
                keyManagerFactory.init(keyStore, "".toCharArray());
                X509TrustManager trustManager = new JiboX509TrustManager(certificateFingerprint);
                
                // specify TLS as the SSL protocol
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{ trustManager }, null);
        
                SSLSocketFactory socketFactory = sslContext.getSocketFactory();
                final String ipAddress = robotIpAddress;
                // build an httpClient capable of handling errors from the socket
                // * provide a means of refreshing the token
                // * verify the ipaddress we're connecting to matches the hostname specified in the p12 certificate
                // * since we use a headless oauth implementation, instead of actually requesting a refresh token
                //      we just cache the user name and password and request another access token
                OkHttpClient httpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(socketFactory, trustManager)
                    .hostnameVerifier(new HostnameVerifier(){
                    
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return ipAddress.equals(hostname);
                        }
                    })
                    .addInterceptor(new ErrorInterceptor())
                    .authenticator(new TokenRefresher(mAccessToken, mTokenType, mUsername, mPassword, mClientId, mClientSecret, endpoint, null))
                    .build();
            
                // build our requester using our websocket url
                Request request = new Request.Builder().url(new StringBuilder(Commons.SOCKET_PROTOCOL).append(robotIpAddress)
                    .append(":").append(Commons.SOCKET_PORT).toString()).build();

                // our websocket listener receives events from the websocket such as when the socket connects / disconnects / receives messages / etc
                JiboWebSocketListener webSocketListener = new JiboWebSocketListener(sslContext, ipAddress);

                WebSocket webSocket = httpClient.newWebSocket(request, webSocketListener);
                // clean up the websocket properly if the program exits
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        System.out.println("Closing websocket");
                        webSocketListener.getCommandLibrary().disconnect();
                        // a websocket close code of 1000 signals that this is a purposeful / non error close
                        webSocket.close(1000, "Application exiting");

                        // keep the application open until the socket has fully shutdown
                        while(!webSocketListener.closed()) {
                            try {
                                Thread.sleep(100);
                            }
                            catch(Exception ex) {
                                System.out.println("thread exception: " + ex.getMessage());
                            }
                        }

                        System.out.println("Application exiting.");
                    }
                });
                
                // wait for a session to be ready
                while(!webSocketListener.sessionReady()) {
                    Thread.sleep(100);
                }
                
                // send a hello world command which blocks on the current thread until we receive a success response back from the robot
                Object lockObj = new Object();
                synchronized(lockObj) {
                    CommandLibrary commandLibrary = webSocketListener.getCommandLibrary();
                    commandLibrary.play.say("hello world", new JiboOnCommandResponseListener(){
                        @Override
                        public void onSuccess(String transactionID) {
                            // on success comes back from the websocket thread
                            // we signal the main thread that we have received a response
                            if(lockObj != null) {
                                synchronized(lockObj) {
                                    lockObj.notify();
                                }
                            }
                        }
                    });
                    lockObj.wait();
                }

                // keep the application open until the user quits (Ctrl+c)
                while(true) {
                    Thread.sleep(100);
                }
            }

        } catch(Exception ex) {
            System.out.println("Something went wrong: " + ex.toString() + " " + ex.getLocalizedMessage());
            System.out.println("Stack: ");
            for(int i = 0; i < ex.getStackTrace().length; ++i) {
                StackTraceElement stackElement = ex.getStackTrace()[i];
                System.out.println("   " + stackElement.getFileName() + " " + stackElement.getClassName() + " " + stackElement.getMethodName() + " " + stackElement.getLineNumber());
            }
        }
    }
}
