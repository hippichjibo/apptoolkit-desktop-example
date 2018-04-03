package com.jibo.apptoolkit_desktop_example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import okhttp3.Authenticator;
import okhttp3.Response;
import okhttp3.Route;

import org.apache.http.HttpResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

// handles refreshing the access token
// use an object which implements the OnTokenRetrievedListener interface in order to get 
// notified of the events
class TokenRefresher implements Authenticator {
    private String mAccessToken = "";
    private String mTokenType = "";
    private String mUsername = "";
    private String mPassword = "";
    private String mClientId = "";
    private String mClientSecret = "";
    private String mEndpointUrl = "";
    private HttpClient mClient = null;
    private OnTokenRetrievedListener mOnTokenRetrievedListener = null;

    TokenRefresher( String accessToken,
                    String tokenType,
                    String username,
                    String password,
                    String clientId, 
                    String clientSecret,
                    String endpointUrl,
                    OnTokenRetrievedListener onTokenRetrievedListener) {

        mAccessToken = accessToken;
        mTokenType = tokenType;
        mUsername = username;
        mPassword = password;
        mClientId = clientId;
        mClientSecret = clientSecret;
        mEndpointUrl = endpointUrl;
        mOnTokenRetrievedListener = onTokenRetrievedListener;
        mClient = new DefaultHttpClient();
    }

    @Override
    public okhttp3.Request authenticate(Route route, Response response) throws IOException {
        if (responseCount(response) >= 2) {
            // If both the original call and the call with refreshed token failed,
            // it will probably keep failing, so don't try again.
            //and lets clear what we have saved as token
            RefreshFailure();
            return null;
        }

        // the url to request an access token
        URI refreshTokenUri = URIBuilder.empty().withScheme("https").addPath(mEndpointUrl).addPath("token").toURI();
        HttpPost refreshTokenPost = new HttpPost(refreshTokenUri);

        refreshTokenPost.setHeader("User-Agent", "Mozilla/5.0");
        // tell the server we're expecting json data
        refreshTokenPost.setHeader("Accept", "application/json");
        // tell the server we're sending json data
        refreshTokenPost.setHeader("Content-type", "application/json");

        // the parameters required for REST call to request an access token
        JSONObject param = new JSONObject();
        try {
            param.put("grant_type", "password");
            param.put("client_id", mClientId);
            param.put("client_secret", mClientSecret);
            param.put("username", mUsername);
            param.put("password", mPassword);
        }
        catch(JSONException ex) {
            System.out.println("Error adding elements to the JSON object: " + ex.getMessage());
            RefreshFailure();
            return null;
        }

        // add the parameters to the REST call
        StringEntity entity = new StringEntity(param.toString(), StandardCharsets.UTF_8.displayName());
        refreshTokenPost.setEntity(entity);

        // send the request and wait for a response
        HttpResponse refreshTokenResponse = mClient.execute(refreshTokenPost);
        System.out.println("\nSending 'POST' request to URL : " + refreshTokenPost.getURI().toString());
        System.out.println("Post parameters : " + refreshTokenPost.getEntity());
        System.out.println("Response Code : " + refreshTokenResponse.getStatusLine().getStatusCode());
        System.out.println("reason phrase? : " + refreshTokenResponse.getStatusLine().getReasonPhrase());
        
        // a successful response
        if(refreshTokenResponse.getStatusLine().getStatusCode() == 200) {
            // feeds the REST call response to a byte array output stream and converts that to a string
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            refreshTokenResponse.getEntity().writeTo(outputStream);
            String getTokenResponseString = new String(outputStream.toByteArray(),"UTF-8");

            try {
                // parse the string as a JSON object to get the access token
                JSONObject responseJson = new JSONObject(getTokenResponseString);
                System.out.println("We got a new access token: " + responseJson.getString("access_token"));
                mAccessToken = responseJson.getString("access_token");
            } catch(JSONException ex) {
                System.out.println("Error parsing refresh token response: " + ex.getMessage());
                RefreshFailure();
                return null;
            }
                
            // communicate the new access token through the token listener object
            if (mOnTokenRetrievedListener != null) {
                mOnTokenRetrievedListener.onTokenRetrieved(mAccessToken);
            }

            return response.request().newBuilder()
                .header("Authorization", mTokenType + " " + mAccessToken)
                .build();
        }
        else {
            RefreshFailure();
            return null;
        }
    }

    private void RefreshFailure() {
        if (mOnTokenRetrievedListener != null) {
            mOnTokenRetrievedListener.onTokenRefreshFailure();
        }
        mAccessToken = "";
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}