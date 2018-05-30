package com.jibo.apptoolkit_desktop_example;

import java.io.InputStream;
import com.jibo.apptoolkit.protocol.model.EventMessage;
import com.jibo.apptoolkit.protocol.CommandRequester.OnCommandResponseListener;

// an empty class which implements all the methods which could be triggered by
// any command response.  
class JiboOnCommandResponseListener implements OnCommandResponseListener {
    /**
     * Emitted on successful transaction
     * @param transactionID ID of the successful transation
     */
    @Override
    public void onSuccess(String transactionID) {}

    /**
     * Emitted on error
     * @param transactionID ID of the failed transaction
     * @param errorMessage Description of the error
     */
    @Override
    public void onError(String transactionID, String errorMessage) {}

    /**
     * Emitted on event error
     * @param transactionID ID of the failed transaction
     * @param errorData See EventMessage.ErrorEvent.ErrorData
     */
    @Override
    public void onEventError(String transactionID, EventMessage.ErrorEvent.ErrorData errorData) {}

    /**
     * Emitted on websocket error
     */
    @Override
    public void onSocketError() {}

    /**
     * Emitted on an event
     * @param transactionID ID of the transaction
     * @param event See EventMessage.BaseEvent
     */
    @Override
    public void onEvent(String transactionID, EventMessage.BaseEvent event) {}

    /**
     * Emitted when Jibo takes a photo
     * @param transactionID ID of the transaction
     * @param event See EventMessage.TakePhotoEvent
     * @param inputStream Input stream of the photo
     */
    @Override
    public void onPhoto(String transactionID, EventMessage.TakePhotoEvent event, InputStream inputStream) {}

    /**
     * Emitted when the video stream is ready
     * @param transactionID ID of the transaction
     * @param event See EventMessage.VideoReadyEvent
     * @param inputStream Input stream of the video recording
     */
    @Override
    public void onVideo(String transactionID, EventMessage.VideoReadyEvent event, final InputStream inputStream) {}

    /**
     * Emitted when Jibo listen what we say to it
     * @param transactionID ID of the transaction
     * @param speech what Jibo is understanding
     */
    @Override
    public void onListen(String transactionID, String speech) {}

    /**
     * Emitted when there's an error in parsing information from the robot
     */
    @Override
    public void onParseError() {}
}