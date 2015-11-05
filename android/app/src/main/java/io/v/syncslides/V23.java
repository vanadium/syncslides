// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncslides;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Handles Vanadium initialization.
 */
public class V23 {

    private static final String TAG = "V23";
    private static final int BLESSING_REQUEST = 201;
    private Context mContext;
    private VContext mVContext;
    private Blessings mBlessings = null;

    public static class Singleton {
        private static volatile V23 instance;

        public static V23 get() {
            V23 result = instance;
            if (instance == null) {
                synchronized (Singleton.class) {
                    result = instance;
                    if (result == null) {
                        instance = result = new V23();
                    }
                }
            }
            return result;
        }
    }

    // Singleton.
    private V23() {
    }

    public void init(Context context, Activity activity) {
        if (mBlessings != null) {
            return;
        }
        mContext = context;
        mVContext = V.init(mContext);
        Blessings blessings = loadBlessings();
        if (blessings == null) {
            // Get the signed-in user's email to generate the blessings from.
            String userEmail = SignInActivity.getUserEmail(mContext);
            activity.startActivityForResult(
                    BlessingService.newBlessingIntent(mContext, userEmail), BLESSING_REQUEST);
            return;
        }
        configurePrincipal(blessings);
    }

    /**
     * To be called from an Activity's onActivityResult method, e.g.
     *     public void onActivityResult(
     *         int requestCode, int resultCode, Intent data) {
     *         if (V23.Singleton.get().onActivityResult(
     *             getApplicationContext(), requestCode, resultCode, data)) {
     *           return;
     *         }
     *     }
     */
    public boolean onActivityResult(
            Context context, int requestCode, int resultCode, Intent data) {
        if (requestCode != BLESSING_REQUEST) {
            return false;
        }
        try {
            Log.d(TAG, "unpacking blessing");
            byte[] blessingsVom = BlessingService.extractBlessingReply(resultCode, data);
            Blessings blessings = (Blessings) VomUtil.decode(blessingsVom, Blessings.class);
            BlessingsManager.addBlessings(mContext, blessings);
            configurePrincipal(blessings);
//            DB.Singleton.get(androidCtx).init();
        } catch (BlessingCreationException e) {
            throw new IllegalStateException(e);
        } catch (VException e) {
            throw new IllegalStateException(e);
        }
        return true;
    }

    private Blessings loadBlessings() {
        try {
            // See if there are blessings stored in shared preferences.
            return BlessingsManager.getBlessings(mContext);
        } catch (VException e) {
            Log.w(TAG, "Cannot get blessings from prefs: " + e.getMessage());
        }
        return null;
    }

    private void configurePrincipal(Blessings blessings) {
        try {
            VPrincipal p = V.getPrincipal(mVContext);
            p.blessingStore().setDefaultBlessings(blessings);
            p.blessingStore().set(blessings, new BlessingPattern("..."));
            VSecurity.addToRoots(p, blessings);
            mBlessings = blessings;
        } catch (VException e) {
            Log.e(TAG, String.format(
                    "Couldn't set local blessing %s: %s", blessings, e.getMessage()));
        }
    }


}
