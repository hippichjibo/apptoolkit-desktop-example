package com.jibo.apptoolkit_desktop_example;

import com.jibo.apptoolkit.protocol.OnConnectionListener;
import com.jibo.apptoolkit.protocol.CommandRequester;

// an empty class which implements an empty implementation for all types of callbacks
// which could be called from the CommandRequester and the websocket which connects to the robot
class JiboOnConnectionListener implements OnConnectionListener {
    /** We succesfully connect to the robot */
    @Override
    public void onConnected() {
    }

    /** We've started sending commands to the robot */
    @Override
    public void onSessionStarted(CommandRequester CommandRequester) {
    }

    /** We were unable to connect from the robot */
    @Override
    public void onConnectionFailed(Throwable throwable) {
    }

    /** We disconnected from the robot */
    @Override
    public void onDisconnected(int code) {
    }
}