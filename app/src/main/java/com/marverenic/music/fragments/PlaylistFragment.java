package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.PlaylistSection;
import com.marverenic.music.instances.section.SpacerSingleton;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.List;

import javax.inject.Inject;

public class PlaylistFragment extends BaseFragment {

    @Inject PlaylistStore mPlaylistStore;

    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private PlaylistSection mPlaylistSection;
    private List<Playlist> mPlaylists;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
        mPlaylistStore.getPlaylists()
                .compose(bindToLifecycle())
                .subscribe(
                        playlists -> {
                            mPlaylists = playlists;
                            setupAdapter();
                        },
                        Throwable::printStackTrace);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mRecyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        mRecyclerView.addItemDecoration(
                new DividerDecoration(getActivity(), R.id.instance_blank, R.id.empty_layout));

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        if (mAdapter == null) {
            setupAdapter();
        } else {
            mRecyclerView.setAdapter(mAdapter);
        }

        int paddingH = (int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView = null;
    }

    private void setupAdapter() {
        if (mRecyclerView == null || mPlaylists == null) {
            return;
        }

        if (mPlaylistSection != null) {
            mPlaylistSection.setData(mPlaylists);
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new HeterogeneousAdapter();
            mRecyclerView.setAdapter(mAdapter);

            mPlaylistSection = new PlaylistSection(mPlaylists);
            mAdapter.addSection(mPlaylistSection);
            mAdapter.addSection(new SpacerSingleton(
                    PlaylistSection.ID, (int) getResources().getDimension(R.dimen.list_height)));
            mAdapter.setEmptyState(new LibraryEmptyState(getActivity(), null) {
                @Override
                public String getEmptyMessage() {
                    return getString(R.string.empty_playlists);
                }

                @Override
                public String getEmptyMessageDetail() {
                    return getString(R.string.empty_playlists_detail);
                }

                @Override
                public String getEmptyAction1Label() {
                    return "";
                }

                @Override
                public String getEmptyAction2Label() {
                    return "";
                }
            });
        }
    }
}
