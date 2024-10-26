package com.example.scabbard;

import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;

public final class StartSplashActivity extends StartActivity implements Animation.AnimationListener {

    private static final int ANIM_TIME = 1700;
    private View mImageView;
    private View mIconView;
    private View mNameView;
    private View mDebugView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 初始化视图
        mImageView = findViewById(R.id.iv_splash_bg);
        mIconView = findViewById(R.id.iv_splash_icon);
        mNameView = findViewById(R.id.iv_splash_name);
        mDebugView = findViewById(R.id.tv_splash_debug);

        initView();
        initData();
    }

    private void initView() {
        // 初始化动画
        AlphaAnimation aa = new AlphaAnimation(0.4f, 1.0f);
        aa.setDuration(ANIM_TIME * 2);
        aa.setAnimationListener(this);
        mImageView.startAnimation(aa);

        ScaleAnimation sa = new ScaleAnimation(
                0, 1, 0, 1,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        sa.setDuration(ANIM_TIME);
        mIconView.startAnimation(sa);

        RotateAnimation ra = new RotateAnimation(
                180, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(ANIM_TIME);
        mNameView.startAnimation(ra);

        // 使用 ImmersionBar 设置状态栏和导航栏
        ImmersionBar.with(this)
                .fullScreen(true)                       // 全屏显示
                .hideBar(BarHide.FLAG_HIDE_STATUS_BAR)   // 隐藏状态栏
                .transparentNavigationBar()              // 透明导航栏
                .init();
    }


    //调试
    protected void initData() {
    }

    private boolean hasPermissionGranted = false;


    private void requestPermission() {
        // 动态获取所需的权限
        String[] requiredPermissions = getRequiredPermissions();

        XXPermissions.with(this)
                .permission(requiredPermissions)  // 使用 getRequiredPermissions 返回的权限列表
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean all) {
                        hasPermissionGranted = true;
                        if (isAnimationEnded) {
                            startStartActivity();
                        }
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean quick) {
                        if (quick) {
                            Toast.makeText(StartSplashActivity.this, R.string.common_permission_fail, Toast.LENGTH_SHORT).show();
                            XXPermissions.startPermissionActivity(StartSplashActivity.this, permissions);
                        } else {
                            Toast.makeText(StartSplashActivity.this, R.string.common_permission_hint, Toast.LENGTH_SHORT).show();
                            handler.postDelayed(() -> requestPermission(), 1000);
                        }
                    }
                });
    }


    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // Android 13 及以上
            return new String[]{
                    Permission.READ_MEDIA_IMAGES,
                    Permission.READ_MEDIA_VIDEO,
                    Permission.READ_MEDIA_AUDIO
            };
        } else {
            return new String[]{
                    Permission.READ_EXTERNAL_STORAGE
            };
        }
    }


    // 标志变量，用于检查动画是否已结束
    private boolean isAnimationEnded = false;

    @Override
    public void onAnimationEnd(Animation animation) {
        requestPermission();
        isAnimationEnded = true;
        // 如果权限已授予，则跳转到StartActivity
        if (hasPermissionGranted) {
            startStartActivity();
        }
    }

    // 启动StartActivity并结束当前SplashActivity的方法
    private void startStartActivity() {
        startActivity(new Intent(StartSplashActivity.this, StartActivity.class));
        finish();
    }


    @Override
    public void onAnimationStart(Animation animation) {}


    @Override
    public void onAnimationRepeat(Animation animation) {}
}
