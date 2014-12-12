package com.plugin.gcm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	private static final String TAG = "GCMIntentService";
	
	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(TAG, "onMessage - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
                // Send a notification if there is a message
                String message = extras.getString("message");
                Bundle immediateExtras = new Bundle(extras);
                Bundle delayedExtras = new Bundle(extras);
                try {
        			// if there is an onclick action, don't send it until user clicks on the notification
                	// remove onclick from immediate data, add it to notification.
                	JSONObject immediateData = new JSONObject(message);
                	JSONObject delayedData = new JSONObject(message);
                	immediateData.remove("onclick");
                	immediateExtras.putString("message", immediateData.toString(0));
                    if (PushPlugin.isInForeground()) {
                    	immediateExtras.putBoolean("foreground", true);
        			}
        			else {
        				immediateExtras.putBoolean("foreground", false);
        				delayedExtras.putBoolean("foreground", false);
	                    if(delayedData.has("aps")) {
	                        JSONObject aps = delayedData.getJSONObject("aps");
	                        if(delayedData.has("onclick")) {
	                        	delayedData.remove("aps");
	                        	delayedData.remove("actions");
	                        	delayedExtras.putString("message", delayedData.toString(0));
	                        	createNotification(context, aps, delayedExtras);
	                        } else {
	                        	createNotification(context, aps, null);
	                        }
	                    }
        			}
                } catch (JSONException e) {
                    Log.d(TAG, "Could not process push notification due to JSON parsing error: " + e.toString());
                }

                PushPlugin.sendExtras(immediateExtras);
		}
	}

    public void createNotification(Context context, JSONObject aps, Bundle extras)
    {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String appName = getAppName(this);

        Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(extras != null) {
            notificationIntent.putExtra("pushBundle", extras);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        int defaults = Notification.DEFAULT_ALL;
        
        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(context)
                .setDefaults(defaults)
                .setSmallIcon(context.getApplicationInfo().icon)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        try {
            if (aps.has("alert")){
                mBuilder.setContentText(aps.getString("alert"));
                mBuilder.setTicker(aps.getString("alert"));
                mBuilder.setContentTitle("Remotium");
            }
            if (aps.has("badge")){
                mBuilder.setNumber(aps.getInt("badge"));
            }

            int notId = 0;
            if (aps.has("category")){
                notId = aps.getString("category").hashCode();
            }

            mNotificationManager.notify((String) appName, notId, mBuilder.build());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Could not process push notification due to JSON parsing error: aps");
        }

    }
	
	private static String getAppName(Context context)
	{
		CharSequence appName = 
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());
		
		return (String)appName;
	}
	
	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
