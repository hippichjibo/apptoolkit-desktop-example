package com.jibo.apptoolkit_desktop_example;

// implement this interface and pass it to the constructor of the TokenRefresher class
// in order to get event feedback when token refresh events occur
interface OnTokenRetrievedListener {
    void onTokenRetrieved(String accessToken);

    void onTokenRefreshFailure();
}