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
			// if we are in the foreground, just surface the payload, else post it to the statusbar
            if (PushPlugin.isInForeground()) {
				extras.putBoolean("foreground", true);
			}
			else {
				extras.putBoolean("foreground", false);

                // Send a notification if there is a message
                String message = extras.getString("message");
                try {
                    JSONObject data = new JSONObject(message);
                    if(data.has("aps")) {
                        JSONObject aps = data.getJSONObject("aps");
                        createNotification(context, aps);
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "Could not process push notification due to JSON parsing error: message");
                }
            }
            PushPlugin.sendExtras(extras);
        }
	}

    public void createNotification(Context context, JSONObject aps)
    {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String appName = getAppName(this);

        Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
