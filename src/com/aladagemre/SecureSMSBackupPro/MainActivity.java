package com.aladagemre.SecureSMSBackupPro;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jasypt.util.text.BasicTextEncryptor;

import com.aladagemre.SecureSMSBackupPro.R;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {
	private List<Sms> mSmsList;
	private TextView mBackupState;
	private TextView mEncryptionLabel, mEncryptionLabel2;
	private Button mBackupButton;
	private String mDirectoryName;
	private Switch mEncryptCheck;
	private EditText mEncryptionKey, mEncryptionKey2;
	private Switch mZipFiles;
	private HashMap<String, String> contactsCache = new HashMap<String, String>();

	public static boolean isValidPhoneNumber(CharSequence target) {
		return !(target == null || TextUtils.isEmpty(target))
				&& android.util.Patterns.PHONE.matcher(target).matches();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mBackupButton = (Button) findViewById(R.id.backup_sms_button);
		mBackupButton.setOnClickListener(this);
		mBackupState = (TextView) findViewById(R.id.back_up_status);
		mEncryptCheck = (Switch) findViewById(R.id.encryptCheck);
		mEncryptionLabel = (TextView) findViewById(R.id.encryptionLabel);
		mEncryptionLabel2 = (TextView) findViewById(R.id.encryptionLabel2);
		mEncryptionKey = (EditText) findViewById(R.id.encryptionKey);
		mEncryptionKey2 = (EditText) findViewById(R.id.encryptionKey2);
		mZipFiles = (Switch) findViewById(R.id.zipFiles);

		mEncryptCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if (mEncryptCheck.isChecked()) {
					mEncryptionKey.setVisibility(mEncryptionKey.VISIBLE);
					mEncryptionLabel.setVisibility(mEncryptionLabel.VISIBLE);
					mEncryptionKey2.setVisibility(mEncryptionKey2.VISIBLE);
					mEncryptionLabel2.setVisibility(mEncryptionLabel2.VISIBLE);
				} else {
					mEncryptionKey.setVisibility(mEncryptionKey.GONE);
					mEncryptionLabel.setVisibility(mEncryptionLabel.GONE);
					mEncryptionKey2.setVisibility(mEncryptionKey2.GONE);
					mEncryptionLabel2.setVisibility(mEncryptionLabel2.GONE);
				}
			}
		});
	}

	private String getContactByAddr(Context context, final String number) {
		if (contactsCache.containsKey(number))
			return contactsCache.get(number);
		Uri personUri = Uri.withAppendedPath(
				ContactsContract.PhoneLookup.CONTENT_FILTER_URI, number);
		Cursor cur = null;

		cur = context.getContentResolver().query(personUri,
				new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME },
				null, null, null);
		try {
			if (cur != null && cur.moveToFirst()) {
				int nameIdx = cur
						.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
				String name = cur.getString(nameIdx);
				cur.close();
				contactsCache.put(number, name);
				return name;
			} else {
				contactsCache.put(number, number);
				Log.i("SMSBACKUP", "Cursor null " + number);
			}
		} finally {
			if (cur != null)
				cur.close();
		}
		return number;
	}

	@Override
	public void onClick(View view) {

		if (mEncryptCheck.isChecked()) {
			if (mEncryptionKey.getText().length() > 0) {
				if (mEncryptionKey.getText().toString()
						.equals(mEncryptionKey2.getText().toString())) {
				} else {
					mBackupState.setText(getString(R.string.key_mismatch));
					return;
				}
			} else {
				mBackupState.setText(getString(R.string.empty_key));
				return;
			}
		}

		mBackupButton.setEnabled(false);
		mBackupState.setText(R.string.start_backup);
		LoadSMSTask loadSMSTask = new LoadSMSTask();
		loadSMSTask.execute();
	}

	private void writeSmsBackUpToFiles() {
		BasicTextEncryptor encryptor = new BasicTextEncryptor();
		if (mEncryptCheck.isChecked() && mEncryptionKey.getText().length() > 0) {
			if (mEncryptionKey.getText().toString()
					.equals(mEncryptionKey2.getText().toString())) {
				encryptor.setPassword(mEncryptionKey.getText().toString());
			} else {
				return;
			}
		}

		HashMap<String, List<Sms>> contactsSms = new HashMap<String, List<Sms>>();
		for (Sms sms : mSmsList) {
			List<Sms> smsList = contactsSms.get(sms.getAddress());
			if (smsList == null)
				smsList = new ArrayList<Sms>();
			smsList.add(sms);
			contactsSms.put(sms.getAddress(), smsList);
		}
		File sdCard = Environment.getExternalStorageDirectory();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HHmm");
		SimpleDateFormat logdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

		mDirectoryName = sdCard.getAbsolutePath() + "/SecureSMSBackup/"
				+ sdf.format(new Date());
		File dir = new File(mDirectoryName);
		dir.mkdirs();
		ArrayList<String> filenames = new ArrayList<String>();
		for (Map.Entry<String, List<Sms>> entry : contactsSms.entrySet()) {
			String data = "";
			String name = entry.getKey();
			List<Sms> smsList = entry.getValue();
			for (Sms sms : smsList) {
				Date d = new Date(Long.parseLong(sms.getTime()));
				String time = logdf.format(d).toString();
				data += "[" + time + "] ";
				if (sms.getFolderName().equals("inbox")) {
					data += name + ": ";
				} else {
					data += getString(R.string.out_message_contact_name);
				}
				data += sms.getMsg() + "\n";
			}

			if (mEncryptCheck.isChecked()) {
				data = encryptor.encrypt(data);
			}

			File file = new File(dir, name + ".txt");
			filenames.add(file.getAbsolutePath());

			FileOutputStream f = null;
			try {
				f = new FileOutputStream(file);
				OutputStreamWriter myOutWriter = new OutputStreamWriter(f,
						"utf8");
				myOutWriter.append(data);
				myOutWriter.close();
				f.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (f != null)
					try {
						f.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}

		if (mZipFiles.isChecked()) {
			String[] array = new String[filenames.size()];
			array = filenames.toArray(array);
			Compress c = new Compress(array, dir + "/combined.zip");
			c.zip();
			for (String filename : filenames) {
				File file = new File(filename);
				file.delete();
			}
		}
	}

	class LoadSMSTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... voids) {
			Cursor smsCursor = getContentResolver().query(
					Uri.parse("content://sms/"), null, null, null, "date ASC");
			mSmsList = new ArrayList<Sms>();
			Sms objSms;
			int totalSMS = smsCursor.getCount();
			if (smsCursor.moveToFirst()) {
				for (int i = 0; i < totalSMS; i++) {
					objSms = new Sms();
					objSms.setId(smsCursor.getString(smsCursor
							.getColumnIndexOrThrow("_id")));
					String number = smsCursor.getString(smsCursor
							.getColumnIndexOrThrow("address"));
					if (isValidPhoneNumber(number))
						number = PhoneNumberUtils.stripSeparators(number);
					objSms.setAddress(getContactByAddr(MainActivity.this,
							number));
					String message = smsCursor.getString(smsCursor
							.getColumnIndexOrThrow("body"));
					objSms.setMsg(message);
					objSms.setReadState(smsCursor.getString(smsCursor
							.getColumnIndex("read")));
					objSms.setTime(smsCursor.getString(smsCursor
							.getColumnIndexOrThrow("date")));
					if (smsCursor.getString(
							smsCursor.getColumnIndexOrThrow("type")).contains(
							"1")) {
						objSms.setFolderName("inbox");
					} else {
						objSms.setFolderName("sent");
					}

					mSmsList.add(objSms);
					smsCursor.moveToNext();
				}
			}
			smsCursor.close();

			writeSmsBackUpToFiles();
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			mBackupButton.setEnabled(true);
			mBackupState.setText(getString(R.string.backup_completed,
					mDirectoryName));
		}
	}
}
