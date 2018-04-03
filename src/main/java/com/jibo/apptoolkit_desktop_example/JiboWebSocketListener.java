package com.jibo.apptoolkit_desktop_example;

import javax.net.ssl.SSLContext;

import com.jibo.apptoolkit.protocol.CommandLibrary;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

// a class to listen for events from the websocket connected to the robot
// upon creating the websocket and having a successful open socket event we automatically send the session request
// this class has a sessionReady method to poll to signal when we have a successful session
class JiboWebSocketListener extends WebSocketListener {
	private static final int EXIT_CODE_PROTOCOL_ERROR = 1002;

	private SSLContext mSslContext = null;
	// the robot ip address we're connected to via websocket
	private String mIpAddress = null;

	private WebSocket mWebSocket = null;
	private CommandLibrary mCommandLibrary = null;
	// a variable to signal when we have a valid session ready
	private boolean mSessionReady = false;

	// used for thread safety
	private Object mLockObj;

	// track whether the websocket is connected or not
	private boolean mClosed = true;

	JiboWebSocketListener(SSLContext sslContext, String ipAddress) {
		mSslContext = sslContext;
		mIpAddress = ipAddress;
		mSessionReady = false;
		mLockObj = new Object();
	}

	public CommandLibrary getCommandLibrary() {
		return mCommandLibrary;
	}

	// poll this method to signal when the session request command has come back as successful
	public boolean sessionReady() {
		return mSessionReady;
	}

	// poll this method to signal if the websocket is connected or not
	public boolean closed() {
		synchronized(mLockObj) {
			return mClosed;
		}
	}

	@Override
	public void onOpen(WebSocket webSocket, Response response) {
		synchronized(mLockObj) {
			mClosed = false;
		}

		System.out.println("Socket opened");
		mCommandLibrary = new CommandLibrary(mSslContext, webSocket, mIpAddress, new JiboOnConnectionListener(){
			@Override
			public void onConnected() {
				System.out.println("Connected to robot");
			}

			@Override
			public void onSessionStarted(CommandLibrary commandLibrary) {
				System.out.println("Session started");
				mSessionReady = true;
			}
		});
		// the websocket has connected. request a new session
		mCommandLibrary.startSession();
	}

	@Override
	public void onMessage(WebSocket webSocket, String text) {
		System.out.println("Receiving Message : " + text);
		if(mCommandLibrary != null) {
			mCommandLibrary.parseJiboResponse(text);
		}
	}

	@Override
	public void onMessage(WebSocket webSocket, ByteString bytes) {
		System.out.println("Received byte string message");
	}

	@Override
	public void onClosing(WebSocket webSocket, int code, String reason) {
		System.out.println("Closing : " + code + " / " + reason);

		disconnect(code);
	}

	@Override
	public void onClosed(WebSocket webSocket, int code, String reason) {
		synchronized(mLockObj) {
			System.out.println("Closed : " + code + " / " + reason);
			mClosed = true;
		}
	}

	@Override
	public void onFailure(WebSocket webSocket, Throwable t, Response response) {
		System.out.println("Error : " + t.getMessage());

		disconnect(EXIT_CODE_PROTOCOL_ERROR);
	}

	private void disconnect(int code) {
		if(mWebSocket != null) {
			mWebSocket.close(code, "Goodbye!");
		}
	}
}
