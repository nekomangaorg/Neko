/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package androidx.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AlertDialog;
import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.RestoreViewOnCreateController;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Abstract base class which presents a dialog associated with a
 * {@link androidx.preference.DialogPreference}. Since the preference object may
 * not be available during fragment re-creation, the necessary information for displaying the dialog
 * is read once during the initial call to {@link #onCreate(Bundle)} and saved/restored in the saved
 * instance state. Custom subclasses should also follow this pattern.
 */
public abstract class PreferenceDialogController extends RestoreViewOnCreateController implements
        DialogInterface.OnClickListener {

    protected static final String ARG_KEY = "key";

    private static final String SAVE_DIALOG_STATE_TAG = "android:savedDialogState";
    private static final String SAVE_STATE_TITLE = "PreferenceDialogController.title";
    private static final String SAVE_STATE_POSITIVE_TEXT = "PreferenceDialogController.positiveText";
    private static final String SAVE_STATE_NEGATIVE_TEXT = "PreferenceDialogController.negativeText";
    private static final String SAVE_STATE_MESSAGE = "PreferenceDialogController.message";
    private static final String SAVE_STATE_LAYOUT = "PreferenceDialogController.layout";
    private static final String SAVE_STATE_ICON = "PreferenceDialogController.icon";

    private DialogPreference mPreference;

    private CharSequence mDialogTitle;
    private CharSequence mPositiveButtonText;
    private CharSequence mNegativeButtonText;
    private CharSequence mDialogMessage;
    private @LayoutRes int mDialogLayoutRes;

    private BitmapDrawable mDialogIcon;

    /** Which button was clicked. */
    private int mWhichButtonClicked;

    private Dialog dialog;
    private boolean dismissed;

    @NonNull
    @Override
    final protected View onCreateView(@NonNull LayoutInflater inflater,
                                      @NonNull ViewGroup container,
                                      @Nullable Bundle savedViewState) {

        onCreate(savedViewState);

        dialog = onCreateDialog(savedViewState);
        //noinspection ConstantConditions
        dialog.setOwnerActivity(getActivity());
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                PreferenceDialogController.this.dismissDialog();
            }
        });
        if (savedViewState != null) {
            Bundle dialogState = savedViewState.getBundle(SAVE_DIALOG_STATE_TAG);
            if (dialogState != null) {
                dialog.onRestoreInstanceState(dialogState);
            }
        }
        return new View(getActivity());//stub view
    }

    public void onCreate(Bundle savedInstanceState) {
        final Controller rawController = getTargetController();
        if (!(rawController instanceof DialogPreference.TargetFragment)) {
            throw new IllegalStateException("Target controller must implement TargetFragment" +
                    " interface");
        }

        final DialogPreference.TargetFragment controller =
                (DialogPreference.TargetFragment) rawController;

        final String key = getArgs().getString(ARG_KEY);
        if (savedInstanceState == null) {
            mPreference = (DialogPreference) controller.findPreference(key);
            mDialogTitle = mPreference.getDialogTitle();
            mPositiveButtonText = mPreference.getPositiveButtonText();
            mNegativeButtonText = mPreference.getNegativeButtonText();
            mDialogMessage = mPreference.getDialogMessage();
            mDialogLayoutRes = mPreference.getDialogLayoutResource();

            final Drawable icon = mPreference.getDialogIcon();
            if (icon == null || icon instanceof BitmapDrawable) {
                mDialogIcon = (BitmapDrawable) icon;
            } else {
                final Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(),
                        icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(bitmap);
                icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                icon.draw(canvas);
                mDialogIcon = new BitmapDrawable(getResources(), bitmap);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharSequence(SAVE_STATE_TITLE, mDialogTitle);
        outState.putCharSequence(SAVE_STATE_POSITIVE_TEXT, mPositiveButtonText);
        outState.putCharSequence(SAVE_STATE_NEGATIVE_TEXT, mNegativeButtonText);
        outState.putCharSequence(SAVE_STATE_MESSAGE, mDialogMessage);
        outState.putInt(SAVE_STATE_LAYOUT, mDialogLayoutRes);
        if (mDialogIcon != null) {
            outState.putParcelable(SAVE_STATE_ICON, mDialogIcon.getBitmap());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mDialogTitle = savedInstanceState.getCharSequence(SAVE_STATE_TITLE);
        mPositiveButtonText = savedInstanceState.getCharSequence(SAVE_STATE_POSITIVE_TEXT);
        mNegativeButtonText = savedInstanceState.getCharSequence(SAVE_STATE_NEGATIVE_TEXT);
        mDialogMessage = savedInstanceState.getCharSequence(SAVE_STATE_MESSAGE);
        mDialogLayoutRes = savedInstanceState.getInt(SAVE_STATE_LAYOUT, 0);
        final Bitmap bitmap = savedInstanceState.getParcelable(SAVE_STATE_ICON);
        if (bitmap != null) {
            mDialogIcon = new BitmapDrawable(getResources(), bitmap);
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;

        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(mDialogTitle)
                .setIcon(mDialogIcon)
                .setPositiveButton(mPositiveButtonText, this)
                .setNegativeButton(mNegativeButtonText, this);

        View contentView = onCreateDialogView(context);
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        } else {
            builder.setMessage(mDialogMessage);
        }

        onPrepareDialogBuilder(builder);

        // Create the dialog
        final Dialog dialog = builder.create();
        if (needInputMethod()) {
            requestInputMethod(dialog);
        }

        return dialog;
    }

    @Override
    protected void onSaveViewState(@NonNull View view, @NonNull Bundle outState) {
        super.onSaveViewState(view, outState);
        Bundle dialogState = dialog.onSaveInstanceState();
        outState.putBundle(SAVE_DIALOG_STATE_TAG, dialogState);
    }

    @Override
    protected void onAttach(@NonNull View view) {
        super.onAttach(view);
        dialog.show();
    }

    @Override
    protected void onDetach(@NonNull View view) {
        super.onDetach(view);
        dialog.hide();
    }

    @Override
    protected void onDestroyView(@NonNull View view) {
        super.onDestroyView(view);
        dialog.setOnDismissListener(null);
        dialog.dismiss();
        dialog = null;
        mPreference = null;
    }

    /**
     * Display the dialog, create a transaction and pushing the controller.
     *
     * @param router The router on which the transaction will be applied
     */
    public void showDialog(@NonNull Router router) {
        showDialog(router, null);
    }

    /**
     * Display the dialog, create a transaction and pushing the controller.
     *
     * @param router The router on which the transaction will be applied
     * @param tag    The tag for this controller
     */
    public void showDialog(@NonNull Router router, @Nullable String tag) {
        dismissed = false;
        router.pushController(RouterTransaction.with(this)
                .pushChangeHandler(new SimpleSwapChangeHandler(false))
                .popChangeHandler(new SimpleSwapChangeHandler(false))
                .tag(tag));
    }

    /**
     * Dismiss the dialog and pop this controller
     */
    public void dismissDialog() {
        if (dismissed) {
            return;
        }
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
        getRouter().popController(this);
        dismissed = true;
    }

    @Nullable
    protected Dialog getDialog() {
        return dialog;
    }

    /**
     * Get the preference that requested this dialog. Available after {@link #onCreate(Bundle)} has
     * been called on the {@link PreferenceFragmentCompat} which launched this dialog.
     *
     * @return The {@link DialogPreference} associated with this
     * dialog.
     */
    public DialogPreference getPreference() {
        if (mPreference == null) {
            final String key = getArgs().getString(ARG_KEY);
            final DialogPreference.TargetFragment controller =
                    (DialogPreference.TargetFragment) getTargetController();
            mPreference = (DialogPreference) controller.findPreference(key);
        }
        return mPreference;
    }

    /**
     * Prepares the dialog builder to be shown when the preference is clicked.
     * Use this to set custom properties on the dialog.
     * <p>
     * Do not {@link AlertDialog.Builder#create()} or
     * {@link AlertDialog.Builder#show()}.
     */
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    }

    /**
     * Returns whether the preference needs to display a soft input method when the dialog
     * is displayed. Default is false. Subclasses should override this method if they need
     * the soft input method brought up automatically.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    protected boolean needInputMethod() {
        return false;
    }

    /**
     * Sets the required flags on the dialog window to enable input method window to show up.
     */
    private void requestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /**
     * Creates the content view for the dialog (if a custom content view is
     * required). By default, it inflates the dialog layout resource if it is
     * set.
     *
     * @return The content View for the dialog.
     * @see DialogPreference#setLayoutResource(int)
     */
    protected View onCreateDialogView(Context context) {
        final int resId = mDialogLayoutRes;
        if (resId == 0) {
            return null;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(resId, null);
    }

    /**
     * Binds views in the content View of the dialog to data.
     * <p>
     * Make sure to call through to the superclass implementation.
     *
     * @param view The content View of the dialog, if it is custom.
     */
    protected void onBindDialogView(View view) {
        View dialogMessageView = view.findViewById(android.R.id.message);

        if (dialogMessageView != null) {
            final CharSequence message = mDialogMessage;
            int newVisibility = View.GONE;

            if (!TextUtils.isEmpty(message)) {
                if (dialogMessageView instanceof TextView) {
                    ((TextView) dialogMessageView).setText(message);
                }

                newVisibility = View.VISIBLE;
            }

            if (dialogMessageView.getVisibility() != newVisibility) {
                dialogMessageView.setVisibility(newVisibility);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mWhichButtonClicked = which;
    }

    public abstract void onDialogClosed(boolean positiveResult);
}
