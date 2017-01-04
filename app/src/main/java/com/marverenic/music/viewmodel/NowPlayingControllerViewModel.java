package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableInt;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.marverenic.music.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.OldPlayerController;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class NowPlayingControllerViewModel extends BaseObservable {

    private static final String TAG_PLAYLIST_DIALOG = "AppendPlaylistDialog";

    private Context mContext;
    private FragmentManager mFragmentManager;

    @Inject MusicStore mMusicStore;
    @Inject ThemeStore mThemeStore;

    @Nullable
    private Song mSong;
    private boolean mPlaying;
    private boolean mUserTouchingProgressBar;
    private Animation mSeekBarThumbAnimation;

    private final ObservableInt mSeekbarPosition;
    private final ObservableInt mCurrentPositionObservable;
    private Subscription mPositionSubscription;

    public NowPlayingControllerViewModel(Fragment fragment) {
        this(fragment.getContext(), fragment.getFragmentManager());
    }

    public NowPlayingControllerViewModel(Context context, FragmentManager fragmentManager) {
        mContext = context;
        mFragmentManager = fragmentManager;

        mCurrentPositionObservable = new ObservableInt();
        mSeekbarPosition = new ObservableInt();

        JockeyApplication.getComponent(mContext).inject(this);
    }

    public void setSong(@Nullable Song song) {
        mSong = song;
        notifyPropertyChanged(BR.songTitle);
        notifyPropertyChanged(BR.artistName);
        notifyPropertyChanged(BR.albumName);
        notifyPropertyChanged(BR.songDuration);
        notifyPropertyChanged(BR.positionVisibility);
        notifyPropertyChanged(BR.seekbarEnabled);
    }

    public void setPlaying(boolean playing) {
        mPlaying = playing;
        notifyPropertyChanged(BR.togglePlayIcon);

        if (mPlaying) {
            pollPosition();
        } else {
            stopPollingPosition();
        }

        if (!mUserTouchingProgressBar) {
            mSeekbarPosition.set(OldPlayerController.getCurrentPosition());
            mCurrentPositionObservable.set(OldPlayerController.getCurrentPosition());
        }
    }

    private void pollPosition() {
        if (mPositionSubscription != null) {
            return;
        }

        mPositionSubscription = Observable.interval(200, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(tick -> OldPlayerController.getCurrentPosition())
                .subscribe(
                        position -> {
                            mCurrentPositionObservable.set(position);
                            if (!mUserTouchingProgressBar) {
                                mSeekbarPosition.set(position);
                            }
                        },
                        throwable -> {
                            Timber.e("pollPositionSubscription: failed to update position");
                        });

    }

    private void stopPollingPosition() {
        if (mPositionSubscription != null) {
            mPositionSubscription.unsubscribe();
            mPositionSubscription = null;
        }
    }

    @Bindable
    public String getSongTitle() {
        if (mSong == null) {
            return mContext.getResources().getString(R.string.nothing_playing);
        } else {
            return mSong.getSongName();
        }
    }

    @Bindable
    public String getArtistName() {
        if (mSong == null) {
            return mContext.getResources().getString(R.string.unknown_artist);
        } else {
            return mSong.getArtistName();
        }
    }

    @Bindable
    public String getAlbumName() {
        if (mSong == null) {
            return mContext.getString(R.string.unknown_album);
        } else {
            return mSong.getAlbumName();
        }
    }

    @Bindable
    public int getSongDuration() {
        return OldPlayerController.getDuration();
    }

    @Bindable
    public boolean getSeekbarEnabled() {
        return mSong != null;
    }

    @Bindable
    public Drawable getTogglePlayIcon() {
        if (mPlaying) {
            return ContextCompat.getDrawable(mContext, R.drawable.ic_pause_36dp);
        } else {
            return ContextCompat.getDrawable(mContext, R.drawable.ic_play_arrow_36dp);
        }
    }

    public ObservableInt getSeekBarPosition() {
        return mSeekbarPosition;
    }

    public ObservableInt getCurrentPosition() {
        return mCurrentPositionObservable;
    }

    @Bindable
    public int getPositionVisibility() {
        if (mSong == null) {
            return View.INVISIBLE;
        } else {
            return View.VISIBLE;
        }
    }

    @ColorInt
    public int getSeekBarHeadTint() {
        return mThemeStore.getAccentColor();
    }

    @Bindable
    public int getSeekBarHeadVisibility() {
        if (mUserTouchingProgressBar) {
            return View.VISIBLE;
        } else {
            return View.INVISIBLE;
        }
    }

    @Bindable
    public Animation getSeekBarHeadAnimation() {
        Animation animation = mSeekBarThumbAnimation;
        mSeekBarThumbAnimation = null;
        return animation;
    }

    private void animateSeekBarHeadOut() {
        mSeekBarThumbAnimation = AnimationUtils.loadAnimation(mContext, R.anim.slider_thumb_out);
        mSeekBarThumbAnimation.setInterpolator(mContext, android.R.interpolator.accelerate_quint);
        notifyPropertyChanged(BR.seekBarHeadAnimation);

        long duration = mSeekBarThumbAnimation.getDuration();
        new Handler().postDelayed(() -> notifyPropertyChanged(BR.seekBarHeadVisibility), duration);
    }

    private void animateSeekBarHeadIn() {
        mSeekBarThumbAnimation = AnimationUtils.loadAnimation(mContext, R.anim.slider_thumb_in);
        mSeekBarThumbAnimation.setInterpolator(mContext, android.R.interpolator.decelerate_quint);
        notifyPropertyChanged(BR.seekBarHeadAnimation);
        notifyPropertyChanged(BR.seekBarHeadVisibility);
    }

    @Bindable
    public float getSeekBarHeadMarginLeft() {
        return mSeekbarPosition.get() / (float) getSongDuration();
    }

    public View.OnClickListener onMoreInfoClick() {
        return v -> {
            if (mSong == null) {
                return;
            }

            PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            menu.inflate(mSong.isInLibrary()
                    ? R.menu.instance_song_now_playing
                    : R.menu.instance_song_now_playing_remote);
            menu.setOnMenuItemClickListener(onMoreInfoItemClick(mSong));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMoreInfoItemClick(Song song) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.menu_item_navigate_to_artist:
                    mMusicStore.findArtistById(song.getArtistId()).subscribe(
                            artist -> {
                                mContext.startActivity(ArtistActivity.newIntent(mContext, artist));
                            },
                            throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });

                    return true;
                case R.id.menu_item_navigate_to_album:
                    mMusicStore.findAlbumById(song.getAlbumId()).subscribe(
                            album -> {
                                mContext.startActivity(AlbumActivity.newIntent(mContext, album));
                            },
                            throwable -> {
                                Timber.e(throwable, "Failed to find album");
                            });

                    return true;
                case R.id.menu_item_add_to_playlist:
                    new AppendPlaylistDialogFragment.Builder(mContext, mFragmentManager)
                            .setSongs(song)
                            .showSnackbarIn(R.id.now_playing_artwork)
                            .show(TAG_PLAYLIST_DIALOG);
                    return true;
            }
            return false;
        };
    }

    public View.OnClickListener onSkipNextClick() {
        return v -> {
            OldPlayerController.skip();
            setSong(OldPlayerController.getNowPlaying());
        };
    }

    public View.OnClickListener onSkipBackClick() {
        return v -> {
            OldPlayerController.previous();
            setSong(OldPlayerController.getNowPlaying());
        };
    }

    public View.OnClickListener onTogglePlayClick() {
        return v -> {
            OldPlayerController.togglePlay();
            setPlaying(OldPlayerController.isPlaying());
        };
    }

    public OnSeekBarChangeListener onSeek() {
        return new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekbarPosition.set(progress);
                if (fromUser) {
                    notifyPropertyChanged(BR.seekBarHeadMarginLeft);

                    if (!mUserTouchingProgressBar) {
                        // For keyboards and non-touch based things
                        onStartTrackingTouch(seekBar);
                        onStopTrackingTouch(seekBar);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mUserTouchingProgressBar = true;
                stopPollingPosition();
                animateSeekBarHeadIn();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mUserTouchingProgressBar = false;
                animateSeekBarHeadOut();

                OldPlayerController.seek(seekBar.getProgress());
                mCurrentPositionObservable.set(seekBar.getProgress());

                if (mPlaying) {
                    pollPosition();
                }
            }
        };
    }

    @BindingAdapter("onSeekListener")
    public static void bindOnSeekListener(SeekBar seekBar, OnSeekBarChangeListener listener) {
        seekBar.setOnSeekBarChangeListener(listener);
    }

    @BindingAdapter("marginLeft_percent")
    public static void bindPercentMarginLeft(View view, float percent) {
        View parent = (View) view.getParent();

        int leftOffset = (int) (parent.getWidth() * percent) - view.getWidth() / 2;

        leftOffset = Math.min(leftOffset, parent.getWidth() - view.getWidth());
        leftOffset = Math.max(leftOffset, 0);

        BindingAdapters.bindLeftMargin(view, leftOffset);
    }

}
