// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncslides;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import java.util.Calendar;
import java.util.Locale;

import io.v.syncslides.db.DB;
import io.v.syncslides.model.Deck;
import io.v.syncslides.model.DynamicList;
import io.v.syncslides.model.ListListener;
import io.v.v23.verror.VException;

/**
 * Provides a list of decks to be shown in the RecyclerView of the
 * DeckChooserFragment.
 */
public class DeckListAdapter extends RecyclerView.Adapter<DeckListAdapter.ViewHolder>
        implements ListListener {
    private static final String TAG = "DeckListAdapter";

    private final DB mDB;
    private DynamicList<Deck> mDecks;

    public DeckListAdapter(DB db) {
        mDB = db;
    }

    /**
     * Starts background monitoring of the underlying data.
     */
    public void start() {
        mDecks = mDB.getDecks();
        mDecks.addListener(this);
    }

    /**
     * Stops any background monitoring of the underlying data.
     */
    public void stop() {
        mDecks.removeListener(this);
        mDecks = null;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.deck_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int deckIndex) {
        final Deck deck = mDecks.get(deckIndex);

        // TODO(afergan): Set actual date here.
        final Calendar cal = Calendar.getInstance();
        holder.mToolbarLastOpened.setText("Opened on "
                + cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US) + " "
                + cal.get(Calendar.DAY_OF_MONTH) + ", " + cal.get(Calendar.YEAR));

        holder.mToolbarLastOpened.setVisibility(View.VISIBLE);
        holder.mToolbarLiveNow.setVisibility(View.GONE);
        holder.mToolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_delete_deck:
                    // TODO(kash): Implement delete.
                    // mDB.deleteDeck(deck.getId());
                    return true;
            }
            return false;
        });

        holder.mToolbarTitle.setText(deck.getTitle());
        holder.mThumb.setImageBitmap(deck.getThumb());
        holder.mThumb.setOnClickListener(v -> {
            Log.d(TAG, "Clicking through to PresentationActivity.");
            String sessionId;
            try {
                sessionId = mDB.createSession(deck.getId());
            } catch (VException e) {
                handleError(v.getContext(), "Could not view deck.", e);
                return;
            }
            startPresentationActivity(v.getContext(), sessionId);
        });
    }

    @Override
    public int getItemCount() {
        return mDecks.getItemCount();
    }

    @Override
    public void onError(Exception e) {
        // TODO(kash): Not sure what to do here...  Call start()/stop() to reset
        // the DynamicList?
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mThumb;
        public final Toolbar mToolbar;
        public final TextView mToolbarTitle;
        public final TextView mToolbarLiveNow;
        public final TextView mToolbarLastOpened;

        public ViewHolder(final View itemView) {
            super(itemView);
            mThumb = (ImageView) itemView.findViewById(R.id.deck_thumb);
            mToolbar = (Toolbar) itemView.findViewById(R.id.deck_card_toolbar);
            mToolbarTitle = (TextView) itemView.findViewById(R.id.deck_card_toolbar_title);
            mToolbarLiveNow = (TextView) itemView.findViewById(R.id.deck_card_toolbar_live_now);
            mToolbarLastOpened =
                    (TextView) itemView.findViewById(R.id.deck_card_toolbar_last_opened);
            mToolbar.inflateMenu(R.menu.deck_card);
        }
    }

    private void startPresentationActivity(Context ctx, String sessionId) {
        Intent intent = new Intent(ctx, PresentationActivity.class);
        intent.putExtra(PresentationActivity.SESSION_ID_KEY, sessionId);
        ctx.startActivity(intent);
    }

    private void handleError(Context ctx, String msg, Throwable throwable) {
        Log.e(TAG, msg + ": " + Log.getStackTraceString(throwable));
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

}
