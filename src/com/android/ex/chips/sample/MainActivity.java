/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2015 blunden
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
 * limitations under the License.
 */
package com.android.ex.chips.sample;

import android.app.Activity;
import android.os.Bundle;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientsEditor;

public class MainActivity extends Activity {
    private RecipientsEditor mRecipientsEditor;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecipientsEditor = (RecipientsEditor) findViewById(R.id.phone_retv);
        initRecipientsEditor();
    }

    // Get the recipients editor ready to be displayed onscreen.
    private void initRecipientsEditor() {
        mRecipientsEditor.setAdapter(new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this));
        mRecipientsEditor.setText(null);
    }
}
