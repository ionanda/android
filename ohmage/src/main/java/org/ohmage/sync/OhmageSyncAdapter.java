/*
 * Copyright (C) 2014 ohmage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ohmage.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.auth.AuthenticationException;
import org.ohmage.app.Ohmage;
import org.ohmage.app.OhmageService;
import org.ohmage.auth.AuthUtil;
import org.ohmage.auth.Authenticator;
import org.ohmage.models.Ohmlet;
import org.ohmage.models.Ohmlet.Member;
import org.ohmage.models.Ohmlets;
import org.ohmage.operators.ContentProviderSaver.ContentProviderSaverObserver;
import org.ohmage.provider.OhmageContract;

import java.io.IOException;

import javax.inject.Inject;

import retrofit.RetrofitError;
import rx.Observable;
import rx.observables.BlockingObservable;
import rx.util.functions.Func1;

/**
 * Handle the transfer of data between a server the ohmage app using the Android sync adapter
 * framework.
 */
public class OhmageSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String IS_SYNCADAPTER = "is_syncadapter";

    @Inject AccountManager am;

    @Inject OhmageService ohmageService;

    @Inject Gson gson;

    private static final String TAG = OhmageSyncAdapter.class.getSimpleName();

    /**
     * Set up the sync adapter
     */
    public OhmageSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Ohmage.app().getApplicationGraph().inject(this);
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public OhmageSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        Ohmage.app().getApplicationGraph().inject(this);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, final SyncResult syncResult) {
        // Check for authtoken
        String token = null;
        try {
            token = am.blockingGetAuthToken(account, AuthUtil.AUTHTOKEN_TYPE, true);
        } catch (OperationCanceledException e) {
            syncResult.stats.numSkippedEntries++;
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
        } catch (AuthenticatorException e) {
            syncResult.stats.numAuthExceptions++;
        }

        // If the token wasn't found or there was a problem, we can stop now
        if (token == null || syncResult.stats.numSkippedEntries > 0 ||
            syncResult.stats.numIoExceptions > 0 || syncResult.stats.numAuthExceptions > 0)
            return;

        Log.d(TAG, "state of ohmlets sync");
        final String userId = am.getUserData(account, Authenticator.USER_ID);

        // TODO: add modififed flag and timestamp to know which things to upload to the server

        // First, sync ohmlet join state. As described by the people field.
        Cursor cursor = null;
        try {
            cursor = provider.query(OhmageContract.Ohmlets.CONTENT_URI,
                    new String[]{
                            OhmageContract.Ohmlets.OHMLET_ID,
                            OhmageContract.Ohmlets.OHMLET_MEMBERS},
                    null, null, null);

            while (cursor.moveToNext()) {
                Member.List members = gson.fromJson(cursor.getString(1), Member.List.class);
                final Member localMember = members.getMember(userId);

                BlockingObservable.from(ohmageService.getOhmlet(cursor.getString(0))).first(
                        new Func1<Ohmlet, Boolean>() {
                            @Override public Boolean call(Ohmlet ohmlet) {
                                Member remoteMember = ohmlet.people.getMember(userId);
                                try {
                                    if (localMember != null) {
                                        if (remoteMember == null ||
                                            localMember.role != remoteMember.role) {
                                            // Check for join verification code to send
                                            if (localMember.code != null) {
                                                String code = localMember.code;
                                                localMember.code = null;
                                                ohmageService.updateMemberForOhmlet(ohmlet.ohmletId,
                                                        localMember, code);
                                            } else {
                                                ohmageService.updateMemberForOhmlet(ohmlet.ohmletId,
                                                        localMember);
                                            }
                                        }
                                    }
                                    if (localMember == null && remoteMember != null) {
                                        ohmageService.removeUserFromOhmlet(ohmlet.ohmletId, userId);
                                    }

                                    if (localMember == null && remoteMember != null) {
                                    } else if (remoteMember != null) {

                                    }
                                } catch (AuthenticationException e) {
                                    syncResult.stats.numAuthExceptions++;
                                } catch (RetrofitError e) {
                                    syncResult.stats.numIoExceptions++;
                                }
                                return true;
                            }
                        });
            }
            cursor.close();

            // Don't continue if there are errors above
            if (syncResult.stats.numIoExceptions > 0 || syncResult.stats.numAuthExceptions > 0)
                return;

            // Second, synchronize all data
            // TODO: only download new streams and surveys with different versions (filter?)
            // TODO: this probably needs to be in a transaction some how? It needs to deal with the case where it wants to sync an ohmlet that the user is trying to interact with at the same time.
            // TODO: handle errors that occur here using the syncResult
            Observable<Ohmlet> ohmlets = ohmageService.getCurrentStateForUser(userId).flatMap(
                    new Func1<Ohmlets, Observable<Ohmlet>>() {
                        @Override
                        public Observable<Ohmlet> call(
                                Ohmlets ohmlets) {
                            return Observable
                                    .from(ohmlets);
                        }
                    }).flatMap(new RefreshOhmlet()).cache();
            ohmlets.subscribe(new ContentProviderSaverObserver(true));

            // TODO: clean up old ohmlets

            // TODO: download streams and surveys that are not part of ohmlets

        } catch (AuthenticationException e) {
            syncResult.stats.numAuthExceptions++;
        } catch (RemoteException e) {
            syncResult.stats.numIoExceptions++;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public class RefreshOhmlet implements Func1<Ohmlet, Observable<Ohmlet>> {
        @Override public Observable<Ohmlet> call(Ohmlet ohmlet) {
            return ohmageService.getOhmlet(ohmlet.ohmletId);
        }
    }

    public static Uri appendSyncAdapterParam(Uri uri) {
        return uri.buildUpon().appendQueryParameter(IS_SYNCADAPTER, "true").build();
    }
}