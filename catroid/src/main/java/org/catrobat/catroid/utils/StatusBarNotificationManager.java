/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2018 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.utils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.transfers.ProjectUploadService;
import org.catrobat.catroid.ui.MainMenuActivity;

import static org.catrobat.catroid.common.Constants.EXTERNAL_STORAGE_ROOT_EXPORT_DIRECTORY;

public final class StatusBarNotificationManager {
	public static final String EXTRA_PROJECT_NAME = "projectName";
	public static final int MAXIMUM_PERCENT = 100;
	private static final String TAG = StatusBarNotificationManager.class.getSimpleName();
	public static final String ACTION_UPDATE_POCKET_CODE_VERSION = "update_pocket_code_version";
	public static final String ACTION_RETRY_UPLOAD = "retry_upload";
	public static final String ACTION_CANCEL_UPLOAD = "cancel_upload";

	private static final StatusBarNotificationManager INSTANCE = new StatusBarNotificationManager();
	public static final String CHANNEL_ID = "pocket_code_notification_channel_id";

	private int notificationId;
	private SparseArray<NotificationData> notificationDataMap = new SparseArray<>();
	private Context context;
	private NotificationManager notificationManager;
	private int progressPercent;

	private StatusBarNotificationManager() {
	}

	public static StatusBarNotificationManager getInstance() {
		return INSTANCE;
	}

	private void initNotificationManager(Context context) {
		if (notificationManager == null) {
			notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			createNotificationChannel(context);
		}
		this.context = context;
	}

	public int createUploadNotification(Context context, String programName) {
		if (context == null || programName == null) {
			return -1;
		}

		Intent uploadIntent = new Intent(context, MainMenuActivity.class);
		uploadIntent.setAction(Intent.ACTION_MAIN);
		uploadIntent = uploadIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, uploadIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationData data = new NotificationData(context, pendingIntent, R.drawable.ic_stat, programName,
				R.string.notification_upload_title_pending, R.string.notification_upload_title_finished,
				R.string.notification_upload_pending, R.string.notification_upload_finished);

		int id = createNotification(context, data);
		showOrUpdateNotification(id, 0);
		return id;
	}

	public int createCopyNotification(Context context, String programName) {
		if (context == null || programName == null) {
			return -1;
		}

		Intent copyIntent = new Intent(context, MainMenuActivity.class);
		copyIntent.setAction(Intent.ACTION_MAIN).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
				.putExtra(EXTRA_PROJECT_NAME, programName);

		PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, copyIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationData data = new NotificationData(context, pendingIntent, R.drawable.ic_stat, programName,
				R.string.notification_copy_title_pending, R.string.notification_title_open,
				R.string.notification_copy_pending, R.string.notification_copy_finished);

		int id = createNotification(context, data);
		showOrUpdateNotification(id, 0);
		return id;
	}

	public int createSaveProjectToExternalMemoryNotification(Context context, String programName) {
		if (context == null || programName == null) {
			return -1;
		}
		PendingIntent doesNothingPendingIntent = PendingIntent.getActivity(context, -1, new Intent(),
				PendingIntent.FLAG_ONE_SHOT);

		NotificationData data = new NotificationData(doesNothingPendingIntent, R.drawable.ic_stat, programName,
				context.getString(R.string.notification_save_project_to_external_storage_title_pending),
				context.getString(R.string.notification_save_project_to_external_storage_title_open),
				context.getString(R.string.notification_save_project_to_external_storage_pending),
				context.getString(R.string.notification_save_project_to_external_storage_open, EXTERNAL_STORAGE_ROOT_EXPORT_DIRECTORY));

		int id = createNotification(context, data);
		showOrUpdateNotification(id, 0);
		return id;
	}

	public int createDownloadNotification(Context context, String programName) {
		if (context == null || programName == null) {
			return -1;
		}
		initNotificationManager(context);

		Intent downloadIntent = new Intent(context, MainMenuActivity.class);
		downloadIntent.setAction(Intent.ACTION_MAIN).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
				.putExtra(EXTRA_PROJECT_NAME, programName);

		PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, downloadIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationData data = new NotificationData(context, pendingIntent, R.drawable.ic_stat, programName,
				R.string.notification_download_title_pending, R.string.notification_title_open,
				R.string.notification_download_pending, R.string.notification_download_finished);

		return createNotification(context, data);
	}

	private int createNotification(Context context, NotificationData data) {
		initNotificationManager(context);

		PendingIntent doesNothingPendingIntent = PendingIntent.getActivity(context, -1, new Intent(),
				PendingIntent.FLAG_ONE_SHOT);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
		notificationBuilder.setContentTitle(data.getNotificationTitleWorking())
			.setContentText(data.getNotificationTextWorking())
			.setSmallIcon(data.getNotificationIcon())
			.setOngoing(true)
			.setContentIntent(doesNothingPendingIntent);

		data.setNotificationBuilder(notificationBuilder);
		notificationDataMap.put(notificationId, data);

		return notificationId++;
	}

	public Notification showOrUpdateNotification(int id, int progressInPercent) {
		NotificationData notificationData = notificationDataMap.get(id);
		if (notificationData == null) {
			return null;
		}
		progressPercent = progressInPercent;
		NotificationCompat.Builder notificationBuilder = notificationData.getNotificationBuilder();
		notificationBuilder.setProgress(100, progressInPercent, false);
		notificationManager.notify(id, notificationBuilder.build());

		if (progressInPercent == 100) {
			notificationBuilder.setContentTitle(notificationData.getNotificationTitleDone())
				.setContentText(notificationData.getNotificationTextDone())
				.setProgress(0, 0, false)
				.setAutoCancel(true)
				.setContentIntent(notificationData.getPendingIntent())
				.setOngoing(false);
			notificationManager.notify(id, notificationBuilder.build());
		}
		return notificationBuilder.build();
	}

	public void abortProgressNotificationWithMessage(int id, @StringRes int changeDoneTextID) {

		NotificationData notificationData = notificationDataMap.get(id);
		if (notificationData == null) {
			return;
		}
		notificationData.setNotificationTextDone(context.getString(changeDoneTextID));
		notificationDataMap.put(id, notificationData);

		showOrUpdateNotification(id, MAXIMUM_PERCENT);
	}

	public void showUploadRejectedNotification(int id, int statusCode, String serverAnswer, Bundle bundle) {
		NotificationData notificationData = notificationDataMap.get(id);

		if (notificationData == null) {
			Log.d(TAG, "NotificationData is null.");
			return;
		}
		NotificationCompat.Builder notificationBuilder = notificationData.getNotificationBuilder();
		notificationData.setNotificationTextDone(serverAnswer);

		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		notificationBuilder.setContentTitle(context.getResources().getText(R.string.notification_upload_rejected))
				.setContentText(serverAnswer)
				.setTicker(context.getResources().getText(R.string.notification_upload_rejected))
				.setSound(alarmSound)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(serverAnswer))
				.setProgress(0, 0, false)
				.setAutoCancel(true)
				.setPriority(Notification.PRIORITY_MAX)
				.setOngoing(false);

		switch (statusCode) {
			case Constants.STATUS_CODE_INTERNAL_SERVER_ERROR:
			case Constants.STATUS_CODE_UPLOAD_MISSING_DATA:
			case Constants.STATUS_CODE_UPLOAD_INVALID_CHECKSUM:
			case Constants.STATUS_CODE_UPLOAD_COPY_FAILED:
			case Constants.STATUS_CODE_UPLOAD_UNZIP_FAILED:
			case Constants.STATUS_CODE_UPLOAD_MISSING_XML:
			case Constants.STATUS_CODE_UPLOAD_RENAME_FAILED:
			case Constants.STATUS_CODE_UPLOAD_SAVE_THUMBNAIL_FAILED:
				Intent actionIntentRetryUpload = new Intent(context, NotificationActionService.class)
						.setAction(ACTION_RETRY_UPLOAD);
				actionIntentRetryUpload.putExtra("bundle", bundle);

				PendingIntent actionPendingIntentRetryUpload = PendingIntent.getService(context, id,
						actionIntentRetryUpload, PendingIntent.FLAG_CANCEL_CURRENT);
				notificationBuilder.addAction(android.R.drawable.ic_popup_sync,
						context.getResources().getString(R.string.notification_upload_retry), actionPendingIntentRetryUpload);

				Intent actionIntentCancelUpload = new Intent(context, NotificationActionService.class)
						.setAction(ACTION_CANCEL_UPLOAD);
				actionIntentCancelUpload.putExtra("bundle", bundle);
				PendingIntent actionPendingIntentCancelUpload = PendingIntent.getService(context, id,
						actionIntentCancelUpload, PendingIntent.FLAG_ONE_SHOT);
				notificationBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel,
						context.getResources().getString(R.string.cancel), actionPendingIntentCancelUpload);

				break;
			case Constants.STATUS_CODE_UPLOAD_MISSING_CHECKSUM:
			case Constants.STATUS_CODE_UPLOAD_OLD_CATROBAT_LANGUAGE:
			case Constants.STATUS_CODE_UPLOAD_OLD_CATROBAT_VERSION:
				Intent actionIntentUpdatePocketCodeVersion = new Intent(context, NotificationActionService.class)
						.setAction(ACTION_UPDATE_POCKET_CODE_VERSION)
						.putExtra("notificationId", id);
				PendingIntent actionPendingIntentUpdatePocketCodeVersion = PendingIntent.getService(context, id,
						actionIntentUpdatePocketCodeVersion, PendingIntent.FLAG_ONE_SHOT);
				notificationBuilder.addAction(R.drawable.ic_launcher,
						context.getResources().getString(R.string.notification_open_play_store), actionPendingIntentUpdatePocketCodeVersion);
				break;

			default:
				notificationDataMap.put(id, notificationData);
				Intent openIntent = new Intent(context, MainMenuActivity.class);
				openIntent.setAction(Intent.ACTION_MAIN).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
						.putExtra(EXTRA_PROJECT_NAME, bundle.getString("projectName"));

				PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, openIntent,
						PendingIntent.FLAG_CANCEL_CURRENT);
				notificationData.setPendingIntent(pendingIntent);
				showOrUpdateNotification(id, MAXIMUM_PERCENT);
				return;
		}

		notificationBuilder.setContentIntent(notificationData.getPendingIntent());

		notificationDataMap.put(id, notificationData);
		notificationManager.notify(id, notificationBuilder.build());
	}

	public void cancelNotification(int id) {
		notificationDataMap.remove(id);
		notificationManager.cancel(id);
	}

	public int getProgressPercent() {
		return progressPercent;
	}

	public static class NotificationActionService extends IntentService {
		public NotificationActionService() {
			super(NotificationActionService.class.getSimpleName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "Received notification, action is: " + action);

			if (ACTION_UPDATE_POCKET_CODE_VERSION.equals(action)) {
				final String appPackageName = getPackageName();
				StatusBarNotificationManager.getInstance().cancelNotification(intent.getIntExtra("notificationId", 0));

				try {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				} catch (android.content.ActivityNotFoundException anfe) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
				}
				closeNotificationBar();
			}
			if (ACTION_RETRY_UPLOAD.equals(action)) {
				StatusBarNotificationManager.getInstance().cancelNotification(intent.getBundleExtra("bundle").getInt("notificationId"));

				Intent reuploadIntent = prepareReuploadIntent(intent);
				String projectName = intent.getBundleExtra("bundle").getString("projectName");
				int notificationId = StatusBarNotificationManager.getInstance().createUploadNotification(getApplicationContext(),
						projectName);
				reuploadIntent.putExtra("notificationId", notificationId);
				getApplicationContext().startService(reuploadIntent);
			}

			if (ACTION_CANCEL_UPLOAD.equals(action)) {
				StatusBarNotificationManager.getInstance().cancelNotification(intent.getBundleExtra("bundle").getInt("notificationId"));
				closeNotificationBar();
			}
		}

		private Intent prepareReuploadIntent(Intent intent) {
			String projectName = intent.getBundleExtra("bundle").getString("projectName");
			String projectDescription = intent.getBundleExtra("bundle").getString("projectDescription");
			String projectPath = intent.getBundleExtra("bundle").getString("projectPath");
			String[] sceneNames = intent.getBundleExtra("bundle").getStringArray("sceneNames");
			String token = intent.getBundleExtra("bundle").getString("token");
			String username = intent.getBundleExtra("bundle").getString("username");
			ResultReceiver receiver = intent.getBundleExtra("bundle").getParcelable("receiver");

			Intent reuploadIntent = new Intent(getApplicationContext(), ProjectUploadService.class);
			reuploadIntent.putExtra("receiver", receiver);
			reuploadIntent.putExtra("uploadName", projectName);
			reuploadIntent.putExtra("projectDescription", projectDescription);
			reuploadIntent.putExtra("projectPath", projectPath);
			reuploadIntent.putExtra("username", username);
			reuploadIntent.putExtra("token", token);
			reuploadIntent.putExtra("sceneNames", sceneNames);
			return reuploadIntent;
		}

		private void closeNotificationBar() {
			Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
			sendBroadcast(it);
		}
	}

	private void createNotificationChannel(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = context.getResources().getString(R.string.app_name);
			String description = context.getResources().getString(R.string.channel_description, name);
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);
			channel.enableVibration(false);
			channel.enableLights(false);
			channel.setSound(null, null);
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}
}
