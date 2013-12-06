/*
 * Copyright (C) 2013 ohmage
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

package org.ohmage.dagger;

import android.accounts.AccountManager;
import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.Bus;

import org.ohmage.app.Endpoints;
import org.ohmage.app.MainActivity;
import org.ohmage.app.OhmageService;
import org.ohmage.app.OhmageErrorHandler;
import org.ohmage.app.OkHttpStack;
import org.ohmage.auth.AuthHelper;
import org.ohmage.auth.AuthenticateFragment;
import org.ohmage.auth.Authenticator;
import org.ohmage.auth.CreateAccountFragment;
import org.ohmage.auth.SignInFragment;
import org.ohmage.requests.AccessTokenRequest;
import org.ohmage.requests.CreateUserRequest;
import org.ohmage.streams.StreamContentProvider;
import org.ohmage.sync.StreamSyncAdapter;
import org.ohmage.tasks.LogoutTaskFragment;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

@Module(
        injects = {
                MainActivity.class,
                AuthenticateFragment.class,
                Authenticator.class,
                CreateAccountFragment.class,
                SignInFragment.class,
                AccessTokenRequest.class,
                CreateUserRequest.class,
                LogoutTaskFragment.class,
                StreamContentProvider.class,
                StreamSyncAdapter.class
        },
        complete = false,
        library = true
)
public class OhmageModule {

    @Provides @Singleton RequestQueue provideRequestQueue(@ForApplication Context context) {
        return Volley.newRequestQueue(context, new OkHttpStack());
    }

    @Provides @Singleton Bus provideBus() {
        return new Bus();
    }

    @Provides @Singleton AccountManager provideAccountManager(@ForApplication Context context) {
        return AccountManager.get(context);
    }

    @Provides @Singleton AuthHelper provideAuthHelper(@ForApplication Context context) {
        return new AuthHelper(context);
    }

    @Provides OhmageService provideOhmageService(@ForApplication Context context) {
        // Create an HTTP client that uses a cache on the file system.
        OkHttpClient okHttpClient = new OkHttpClient();
        try {
            HttpResponseCache cache = new HttpResponseCache(context.getCacheDir(), 1024);
            okHttpClient.setResponseCache(cache);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient.setAuthenticator(new OhmageAuthenticator());

        // Create the Gson object
//        Gson gson = new GsonBuilder()
//                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
//                .create();

        Executor executor = Executors.newCachedThreadPool();
//        RequestInterceptor requestInterceptor = new RequestInterceptor() {
//            @Override public void intercept(RequestFacade request) {
//                request.addHeader("Authorization", "ohmage " + );
//            }
//        }
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setExecutors(executor, executor)
//                .setConverter(new GsonConverter())
                .setClient(new OkClient(okHttpClient))
//                .setClient(new Client() {
//
//                    public
//
//                    @Override public Response execute(Request request) throws IOException {
//                        request.
//                        return null;
//                    }
//                })
                .setErrorHandler(new OhmageErrorHandler())
                .setServer(Endpoints.API_ROOT)
//                .setRequestInterceptor(new OhmageAuthenticator())
                .build();
        return restAdapter.create(OhmageService.class);

    }
}