package io.nlopez.smartlocation.location;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import io.nlopez.smartlocation.LocationAccuracy;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.utils.Logger;

/**
 * Created by mrm on 20/12/14.
 */
public class GooglePlayServicesLocationProvider implements LocationProvider, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status> {

    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Logger logger;
    private SmartLocation.OnLocationUpdatedListener listener;
    private boolean started = false;
    private boolean oneFix = false;

    @Override
    public void init(Context context, SmartLocation.OnLocationUpdatedListener listener, boolean oneFix, LocationAccuracy accuracy, Logger logger) {
        if (!started) {
            this.client = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            client.connect();
            this.listener = listener;

            // TODO handle accuracy
            this.locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            this.logger = logger;
            this.oneFix = oneFix;
        } else {
            logger.d("already started");
        }
    }

    @Override
    public void start() {
        logger.d("start oneFix=" + oneFix);
        if (client.isConnected()) {
            startUpdating();
        } else {
            started = true;
        }
    }

    private void startUpdating() {
        LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this).setResultCallback(this);
    }

    @Override
    public void stopUpdates() {
        logger.d("stopUpdates");
        if (client.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
            client.disconnect();
        }
        started = false;
    }

    @Override
    public Location getLastLocation() {
        return client.isConnected() ? LocationServices.FusedLocationApi.getLastLocation(client) : null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        // ??
        logger.d("onConnected");
        if (started) {
            startUpdating();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        logger.d("onConnectionSuspended " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        logger.d("onConnectionFailed");

    }

    @Override
    public void onLocationChanged(Location location) {
        listener.onLocationUpdated(location);
        if (client.isConnected() && oneFix) {
            logger.d("disconnecting because recurrence = once");
            client.disconnect();
        }
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            logger.d("Locations update request successful");

        } else if (status.hasResolution()) {
            // TODO this
            logger.d("Unable to register, but we can solve this");
            /*
            status.startResolutionForResult(
                    context,     // your current activity used to receive the result
                    RESULT_CODE); // the result code you'll look for in your
            // onActivityResult method to retry registering
            */
        } else {
            // No recovery. Weep softly or inform the user.
            logger.e("Registering failed: " + status.getStatusMessage());
        }
    }
}
