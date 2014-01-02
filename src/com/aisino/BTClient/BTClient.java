package com.aisino.BTClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.aisino.BTClient.DeviceListActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
//import android.view.Menu;            //如使用菜单加入此三包
//import android.view.MenuInflater;
//import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class BTClient extends Activity implements SmartKeyInterf {

	private final static int REQUEST_CONNECT_DEVICE = 1;

	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // SPP
																					// Service
																					// UUID

	private InputStream is;
	// private TextView text0;
	private EditText edit0;
	private TextView dis;
	private ScrollView sv;
	private String smsg = "";
	private String fmsg = "";

	public String filename = "";
	BluetoothDevice _device = null;
	BluetoothSocket _socket = null;
	boolean _discoveryFinished = false;
	boolean bRun = true;
	boolean bThread = false;

	boolean bHex = true;

	private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// text0 = (TextView)findViewById(R.id.Text0);
		edit0 = (EditText) findViewById(R.id.Edit0);
		sv = (ScrollView) findViewById(R.id.ScrollView01);
		dis = (TextView) findViewById(R.id.in);

		if (_bluetooth == null) {
			Toast.makeText(this, R.string.cannot_open_bluetooth,
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		new Thread() {
			public void run() {
				if (_bluetooth.isEnabled() == false) {
					_bluetooth.enable();
				}
			}
		}.start();
	}

	public void onSendButtonClicked(View v) {
		if (bHex == true) {
			try {
				OutputStream os = _socket.getOutputStream();
				byte[] toWrite = hexStr2Bytes(edit0.getText().toString());
				if (toWrite != null) {
					os.write(toWrite);
				} else {
					Toast.makeText(getApplicationContext(), "Hex2Bytes Error.",
							Toast.LENGTH_SHORT).show();
				}
			} catch (IOException e) {
			}
		} else {
			int i = 0;
			int n = 0;
			try {
				OutputStream os = _socket.getOutputStream();
				byte[] bos = edit0.getText().toString().getBytes();
				for (i = 0; i < bos.length; i++) {
					if (bos[i] == 0x0a)
						n++;
				}
				byte[] bos_new = new byte[bos.length + n];
				n = 0;
				for (i = 0; i < bos.length; i++) { // br is 0a in phone, changed
													// to
													// 0d 0a before sending
					if (bos[i] == 0x0a) {
						bos_new[n] = 0x0d;
						n++;
						bos_new[n] = 0x0a;
					} else {
						bos_new[n] = bos[i];
					}
					n++;
				}

				os.write(bos_new);
			} catch (IOException e) {
			}
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				_device = _bluetooth.getRemoteDevice(address);

				try {
					_socket = _device.createRfcommSocketToServiceRecord(UUID
							.fromString(MY_UUID));
				} catch (IOException e) {
					Toast.makeText(this, R.string.connection_failed,
							Toast.LENGTH_SHORT).show();
				}

				Button btn = (Button) findViewById(R.id.btn_connect);
				try {
					_socket.connect();
					Toast.makeText(
							this,
							R.string.connect + _device.getName()
									+ R.string.succeed, Toast.LENGTH_SHORT)
							.show();
					btn.setText(R.string.disconnect);
				} catch (IOException e) {
					try {
						Toast.makeText(this, R.string.connection_failed,
								Toast.LENGTH_SHORT).show();
						_socket.close();
						_socket = null;
					} catch (IOException ee) {
						Toast.makeText(this, R.string.connection_failed,
								Toast.LENGTH_SHORT).show();
					}

					return;
				}

				// open accept thread
				try {
					is = _socket.getInputStream();
				} catch (IOException e) {
					Toast.makeText(this, R.string.fail_to_receive_data,
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (bThread == false) {
					ReadThread.start();
					bThread = true;
				} else {
					bRun = true;
				}
			}
			break;
		default:
			break;
		}
	}

	Thread ReadThread = new Thread() {

		public void run() {
			int num = 0;
			byte[] buffer = new byte[1024];
			byte[] buffer_new = new byte[1024];
			int i = 0;
			int n = 0;
			bRun = true;

			while (true) {
				try {
					while (is.available() == 0) {
						while (bRun == false) {
						}
					}
					while (true) {
						num = is.read(buffer);
						if (bHex == true) {
							String s0 = byte2HexStr(buffer, 0, num);
							if (s0 == null) {
//								Toast.makeText(getApplicationContext(), "",
//										Toast.LENGTH_SHORT);
							} else {
								fmsg += s0;
								smsg += s0;
							}
							if (is.available() == 0)
								break;
						} else {
							n = 0;

							String s0 = new String(buffer, 0, num);
							fmsg += s0; // save received data
							for (i = 0; i < num; i++) {
								if ((buffer[i] == 0x0d)
										&& (buffer[i + 1] == 0x0a)) {
									buffer_new[n] = 0x0a;
									i++;
								} else {
									buffer_new[n] = buffer[i];
								}
								n++;
							}
							String s = new String(buffer_new, 0, n);
							smsg += s; // write to buffer
							if (is.available() == 0)
								break;
						}

					}

					handler.sendMessage(handler.obtainMessage());
				} catch (IOException e) {
				}
			}
		}
	};

	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			dis.setText(smsg);
			sv.scrollTo(0, dis.getMeasuredHeight());
		}
	};

	public void onDestroy() {
		super.onDestroy();
		if (_socket != null)
			try {
				_socket.close();
			} catch (IOException e) {
			}
		// _bluetooth.disable();
	}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { MenuInflater
	 * inflater = getMenuInflater(); inflater.inflate(R.menu.option_menu, menu);
	 * return true; }
	 */

	/*
	 * @Override public boolean onOptionsItemSelected(MenuItem item) { switch
	 * (item.getItemId()) { case R.id.scan: if(_bluetooth.isEnabled()==false){
	 * Toast.makeText(this, "Open BT......", Toast.LENGTH_LONG).show(); return
	 * true; } // Launch the DeviceListActivity to see devices and do scan
	 * Intent serverIntent = new Intent(this, DeviceListActivity.class);
	 * startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE); return
	 * true; case R.id.quit: finish(); return true; case R.id.clear: smsg="";
	 * ls.setText(smsg); return true; case R.id.save: Save(); return true; }
	 * return false; }
	 */

	public void onConnectButtonClicked(View v) {
		if (_bluetooth.isEnabled() == false) {
			Toast.makeText(this, R.string.opening_bt, Toast.LENGTH_LONG).show();
			return;
		}

		Button btn = (Button) findViewById(R.id.btn_connect);
		if (_socket == null) {
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		} else {
			try {

				is.close();
				_socket.close();
				_socket = null;
				bRun = false;
				btn.setText(R.string.connect);
			} catch (IOException e) {
			}
		}
		return;
	}

	public void onHexButtonClicked(View v) {
		Button btn = (Button) findViewById(R.id.btn_hex_ascii);
		if (bHex == true) {
			btn.setText(R.string.ascii);
			bHex = false;
		} else {
			btn.setText(R.string.hex);
			bHex = true;
		}
	}

	public void onSaveButtonClicked(View v) {
		Save();
	}

	public void onClearButtonClicked(View v) {
		smsg = "";
		fmsg = "";
		dis.setText(smsg);
		return;
	}

	public void onQuitButtonClicked(View v) {
		finish();
	}

	private void Save() {
		LayoutInflater factory = LayoutInflater.from(BTClient.this);
		final View DialogView = factory.inflate(R.layout.sname, null);
		new AlertDialog.Builder(BTClient.this)
				.setTitle(R.string.file_name)
				.setView(DialogView)
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								EditText text1 = (EditText) DialogView
										.findViewById(R.id.sname);
								filename = text1.getText().toString();

								try {
									if (Environment.getExternalStorageState()
											.equals(Environment.MEDIA_MOUNTED)) {
										filename = filename + ".txt";
										File sdCardDir = Environment
												.getExternalStorageDirectory();
										File BuildDir = new File(sdCardDir,
												"/data");
										if (BuildDir.exists() == false)
											BuildDir.mkdirs();
										File saveFile = new File(BuildDir,
												filename);
										FileOutputStream stream = new FileOutputStream(
												saveFile);
										stream.write(fmsg.getBytes());
										stream.close();
										Toast.makeText(BTClient.this,
												R.string.save_succeed,
												Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(BTClient.this,
												R.string.sdcard_not_found,
												Toast.LENGTH_LONG).show();
									}

								} catch (IOException e) {
									return;
								}

							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
							}
						}).show();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aisino.BTClient.SmartKeyInterf#sendCmd(byte[], int, int)
	 */
	@Override
	public boolean sendCmd(byte[] src, int offset, int length) {
		try {
			OutputStream os = _socket.getOutputStream();
			os.write(src, offset, length);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aisino.BTClient.SmartKeyInterf#rcvResp()
	 */
	@Override
	public byte[] rcvResp() {
		byte[] buf = new byte[1024];
		try {
			InputStream is = _socket.getInputStream();
			int read = is.read(buf);
			if (read > 0) {
				byte[] ret = new byte[read];
				System.arraycopy(buf, 0, ret, 0, read);
				return ret;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] hexStr2Bytes(String src) {
		try {
			int l = src.length() / 2;
			System.out.println(l);
			byte[] ret = new byte[l];
			for (int i = 0; i < l; i++) {
				int index = i * 2;
				int v = Integer.parseInt(src.substring(index, index + 2), 16);
				ret[i] = (byte) v;
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String byte2HexStr(byte[] b, int offset, int length) {
		try {
			String stmp = "";
			StringBuilder sb = new StringBuilder("");
			for (int n = offset; n < offset + length; n++) {
				stmp = Integer.toHexString(b[n] & 0xFF);
				sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
				sb.append(" ");
			}
			return sb.toString().toUpperCase().trim();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}