/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.MultiSelectListPreference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("RestrictedApi")
public class MultiSelectListPreferenceDialogController extends PreferenceDialogController {

    private static final String SAVE_STATE_VALUES =
            "MultiSelectListPreferenceDialogController.values";
    private static final String SAVE_STATE_CHANGED =
            "MultiSelectListPreferenceDialogController.changed";
    private static final String SAVE_STATE_ENTRIES =
            "MultiSelectListPreferenceDialogController.entries";
    private static final String SAVE_STATE_ENTRY_VALUES =
            "MultiSelectListPreferenceDialogController.entryValues";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Set<String> mNewValues = new HashSet<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mPreferenceChanged;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    CharSequence[] mEntries;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    CharSequence[] mEntryValues;

    public static MultiSelectListPreferenceDialogController newInstance(String key) {
        MultiSelectListPreferenceDialogController controller =
                new MultiSelectListPreferenceDialogController();
        controller.getArgs().putString(ARG_KEY, key);
        return controller;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final MultiSelectListPreference preference = getListPreference();

            if (preference.getEntries() == null || preference.getEntryValues() == null) {
                throw new IllegalStateException(
                        "MultiSelectListPreference requires an entries array and " +
                                "an entryValues array.");
            }

            mNewValues.clear();
            mNewValues.addAll(preference.getValues());
            mPreferenceChanged = false;
            mEntries = preference.getEntries();
            mEntryValues = preference.getEntryValues();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SAVE_STATE_VALUES, new ArrayList<>(mNewValues));
        outState.putBoolean(SAVE_STATE_CHANGED, mPreferenceChanged);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mNewValues.clear();
        mNewValues.addAll(savedInstanceState.getStringArrayList(SAVE_STATE_VALUES));
        mPreferenceChanged = savedInstanceState.getBoolean(SAVE_STATE_CHANGED, false);
        mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
        mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
    }

    private MultiSelectListPreference getListPreference() {
        return (MultiSelectListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        final int entryCount = mEntryValues.length;
        final boolean[] checkedItems = new boolean[entryCount];
        for (int i = 0; i < entryCount; i++) {
            checkedItems[i] = mNewValues.contains(mEntryValues[i].toString());
        }
        builder.setMultiChoiceItems(mEntries, checkedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            mPreferenceChanged |= mNewValues.add(
                                    mEntryValues[which].toString());
                        } else {
                            mPreferenceChanged |= mNewValues.remove(
                                    mEntryValues[which].toString());
                        }
                    }
                });
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        final MultiSelectListPreference preference = getListPreference();
        if (positiveResult && mPreferenceChanged) {
            final Set<String> values = mNewValues;
            if (preference.callChangeListener(values)) {
                preference.setValues(values);
            }
        }
        mPreferenceChanged = false;
    }
}
