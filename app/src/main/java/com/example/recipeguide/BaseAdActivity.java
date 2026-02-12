package com.example.recipeguide;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

public class BaseAdActivity {
    private final Activity activity;
    private final int containerId, bannerId;
    private final String placementId;
    private BannerAdView bannerAd;
    private View adContainer;

    public BaseAdActivity(Activity activity, int containerId, int bannerId, String placementId) {
        this.activity    = activity;
        this.containerId = containerId;
        this.bannerId    = bannerId;
        this.placementId = placementId;
    }

    public void load() {
        adContainer = activity.findViewById(containerId);
        bannerAd    = activity.findViewById(bannerId);

        adContainer.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        adContainer.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        BannerAdSize size = calculateSize();
                        bannerAd.setAdSize(size);
                        bannerAd.setAdUnitId(placementId);
                        bannerAd.setBannerAdEventListener(new BannerAdEventListener() {
                            @Override
                            public void onImpression(@Nullable ImpressionData impressionData) {

                            }

                            @Override
                            public void onReturnedToApplication() {

                            }

                            @Override
                            public void onLeftApplication() {

                            }

                            @Override
                            public void onAdClicked() {

                            }

                            @Override public void onAdLoaded()      { }
                            @Override public void onAdFailedToLoad(@NonNull AdRequestError e) { }
                            // … другие колбэки
                        });
                        bannerAd.loadAd(new AdRequest.Builder().build());
                    }
                });
    }

    private BannerAdSize calculateSize() {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int screenDp = Math.round(dm.heightPixels / dm.density);
        int widthPx = adContainer.getWidth() == 0 ? dm.widthPixels : adContainer.getWidth();
        int widthDp = Math.round(widthPx / dm.density);
        return BannerAdSize.inlineSize(activity, widthDp, screenDp / 17);
    }

    public void destroy() {
        if (bannerAd != null) bannerAd.destroy();
    }
}