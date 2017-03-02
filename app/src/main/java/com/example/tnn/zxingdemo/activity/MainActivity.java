package com.example.tnn.zxingdemo.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tnn.zxingdemo.R;
import com.example.tnn.zxingdemo.encoding.EncodingUtils;
import com.yxp.permission.util.lib.PermissionInfo;
import com.yxp.permission.util.lib.PermissionUtil;
import com.yxp.permission.util.lib.callback.PermissionOriginResultCallBack;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * 该工程配置了动态获取权限
 */
public class MainActivity extends Activity implements OnClickListener{

	private static final int PHOTO_PIC = 4;//扫描二维码
	public static final int NONE = 0;
	public static final int PHOTOHRAPH = 1;// 拍照
	public static final int PHOTOZOOM = 2; // 缩放
	public static final int PHOTORESOULT = 3;// 结果
	public static final String IMAGE_UNSPECIFIED = "image/*";
	private Bitmap bitmaplogo;//logo

	private EditText contentEditText = null;
	private ImageView qrcodeImageView = null;
	private String  imgPath = null;

	private TextView tvShow;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupViews();
	}

	private void setupViews() {
		contentEditText = (EditText) findViewById(R.id.editText1);
		findViewById(R.id.button1).setOnClickListener(this);
		findViewById(R.id.button3).setOnClickListener(this);
		findViewById(R.id.sellogo).setOnClickListener(this);
		qrcodeImageView = (ImageView) findViewById(R.id.img1);
		tvShow = (TextView) findViewById(R.id.tvshow);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		imgPath = null;
		if(requestCode == PHOTO_PIC){
			if(resultCode == RESULT_OK){
				String result = data.getExtras().getString("result");
				tvShow.setText("扫描结果："+result);
			}
		}else{
			if (resultCode == NONE){
				return;
			}
			// 拍照
			if (requestCode == PHOTOHRAPH) {
				//设置文件保存路径这里放在跟目录下
				File picture = new File(Environment.getExternalStorageDirectory() + "/temp.jpg");
				startPhotoZoom(Uri.fromFile(picture));
			}

			if (data == null) {
				return;
			}

			// 读取相册缩放图片
			if (requestCode == PHOTOZOOM) {
				startPhotoZoom(data.getData());
			}
			// 处理结果
			if (requestCode == PHOTORESOULT) {
				Bundle extras = data.getExtras();
				try {
					if (extras != null) {
						File file = new File(getFilesDir() + "/temp.jpg");

						bitmaplogo = extras.getParcelable("data");
						FileOutputStream stream = new FileOutputStream(file);
						bitmaplogo.compress(Bitmap.CompressFormat.JPEG, 80, stream);// (0 - 100)压缩文件
						stream.flush();
						stream.close();
					}
				} catch (Exception e) {}
			}
		}
	}

	@SuppressLint("InlinedApi")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button1:
				//获取界面输入的内容
				String content = contentEditText.getText().toString();
				//判断内容是否为空
				if ("".equals(content)) {
					Toast.makeText(MainActivity.this, "请输入要写入二维码的内容...", Toast.LENGTH_SHORT).show();
					return;
				}

				try {
					//生成二维码图片，第一个参数是二维码的内容，第二个参数是正方形图片的边长，单位是像素
					Bitmap qrcodeBitmap = EncodingUtils.createQRCode(content, 600, 600, bitmaplogo);
					qrcodeImageView.setImageBitmap(qrcodeBitmap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case R.id.button3:
				PermissionUtil.getInstance().request(MainActivity.this, new String[]{Manifest.permission.CAMERA},
						new PermissionOriginResultCallBack() {
							@Override
							public void onResult(List<PermissionInfo> acceptList, List<PermissionInfo> rationalList, List<PermissionInfo> deniedList) {
								if (!acceptList.isEmpty()) {
									//Toast.makeText(MainActivity.this, acceptList.get(0).getName() + " is accepted", Toast.LENGTH_SHORT).show();
									//跳转到拍照界面扫描二维码
									Intent intent3 = new Intent(MainActivity.this, CaptureActivity.class);
									startActivityForResult(intent3, PHOTO_PIC);
								}
								if (!rationalList.isEmpty()) {
									Toast.makeText(MainActivity.this, rationalList.get(0).getName() + " is rational", Toast.LENGTH_SHORT).show();
								}
								if (!deniedList.isEmpty()) {
									Toast.makeText(MainActivity.this, "请先打开相机权限"/*deniedList.get(0).getName() + " is denied"*/, Toast.LENGTH_SHORT).show();
									startAppSettings();
								}
							}
						});
				break;
			case R.id.sellogo:
				selDialog();
				break;
			default:
				break;
		}
	}

	//启动应用的设置
	void startAppSettings(){
		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.parse("package:" + getPackageName()));
		startActivity(intent);
	}

	/**
	 * 相片选择
	 */
	public void selDialog() {
		final String[] sel = new String[] { "拍照", "相册" };
		new AlertDialog.Builder(this)
				.setTitle("请选择")
				.setIcon(android.R.drawable.ic_dialog_info)
				.setItems(sel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
							intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri
									.fromFile(new File(Environment.getExternalStorageDirectory(),"temp.jpg")));
							startActivityForResult(intent, PHOTOHRAPH);
						} else if (which == 1) {
							Intent intent = new Intent(Intent.ACTION_PICK, null);
							intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_UNSPECIFIED);
							startActivityForResult(intent, PHOTOZOOM);
						}
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}
	/**
	 * 图片处理
	 * @param uri
	 */
	public void startPhotoZoom(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, IMAGE_UNSPECIFIED);
		intent.putExtra("crop", "true");
		// aspectX aspectY 是宽高的比例
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		// outputX outputY 是裁剪图片宽高
		intent.putExtra("outputX", 100);
		intent.putExtra("outputY", 100);
		intent.putExtra("return-data", true);
		startActivityForResult(intent, PHOTORESOULT);
	}


}
