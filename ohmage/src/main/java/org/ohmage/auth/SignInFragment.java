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

package org.ohmage.auth;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.ohmage.app.OhmageService;
import org.ohmage.app.R;
import org.ohmage.fragments.TransitionFragment;
import org.ohmage.models.AccessToken;
import org.ohmage.models.User;

import javax.inject.Inject;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Activity which attempts to log the user in with their email and password
 */
public class SignInFragment extends TransitionFragment {

    private static final String TAG = SignInFragment.class.getSimpleName();

    @Inject OhmageService ohmageService;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;

    /**
     * Callbacks for the activity to handle create button pressed
     */
    private Callbacks mCallbacks;

    private String mEmail;

    public SignInFragment() {
        setDefaultAnimation(R.anim.slide_in_top, R.anim.slide_out_top);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in_ohmage, container, false);

        // Set up the login form.
        mEmailView = (EditText) view.findViewById(R.id.email);
        mPasswordView = (EditText) view.findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptSignIn();
                    return true;
                }
                return false;
            }
        });

        if (!TextUtils.isEmpty(mEmail)) {
            mEmailView.setText(mEmail);
            mEmailView.setEnabled(false);
        }

        view.findViewById(R.id.sign_in_email_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        attemptSignIn();
                    }
                });

        return view;
    }

    /**
     * Called when the user clicks the create button. This will callback to the activity if
     * the values supplied by the user are valid.
     */
    public void attemptSignIn() {

        mEmailView.setError(null);

        final String password = mPasswordView.getText().toString();
        final String email = mEmailView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {

            // First notify the activity that the account is being created
            if (mCallbacks != null) {
                mCallbacks.onAccountSignInOhmage();
            }

            // Actually make the request to get the access token
            ohmageService.getAccessToken(email, password,
                    new OhmageService.CancelableCallback<AccessToken>() {
                        @Override
                        public void success(AccessToken accessToken, Response response) {
                            ((AuthenticatorActivity) getActivity())
                                    .createAccount(email, accessToken);
                        }

                        @Override public void failure(RetrofitError error) {
                            Response r = error.getResponse();

                            // If it is a 409 they just have to verify their e-mail, so they can log in
                            if (r != null && r.getStatus() == 409) {
                                User user = new User();
                                user.email = email;
                                ((AuthenticatorActivity) getActivity()).createAccount(user, password);
                            } else {
                                ((AuthenticatorActivity) getActivity()).onRetrofitError(error);
                            }
                        }
                    });
        }
    }

    public void setEmail(String email) {
        this.mEmail = email;
    }

    public static interface Callbacks {
        /**
         * Called when the account is being signed in
         */
        void onAccountSignInOhmage();
    }
}