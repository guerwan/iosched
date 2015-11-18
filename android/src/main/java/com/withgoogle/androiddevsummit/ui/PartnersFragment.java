/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.withgoogle.androiddevsummit.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.withgoogle.androiddevsummit.R;
import com.withgoogle.androiddevsummit.provider.ScheduleContract;
import com.withgoogle.androiddevsummit.ui.widget.CollectionView;
import com.withgoogle.androiddevsummit.ui.widget.CollectionViewCallbacks;
import com.withgoogle.androiddevsummit.util.ImageLoader;
import com.withgoogle.androiddevsummit.util.UIUtils;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.withgoogle.androiddevsummit.util.LogUtils.makeLogTag;

/**
 * A {@link android.app.Fragment} subclass used to present the list of partners.
 */
public class PartnersFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(PartnersFragment.class);
    public static String ARG_HAS_HEADER = "hasHeader";

    private ImageLoader mImageLoader;

    private static final Pattern TRIM_FROM_DISPLAY_URL_PATTERN = Pattern.compile("(^https?://)|(/$)");

    private CollectionView mCollectionView;
    private PartnersAdapter mPartnersAdapter;
    private int mDisplayCols;

    public static PartnersFragment newInstance(boolean hasHeader) {
        PartnersFragment fragment = new PartnersFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HAS_HEADER, hasHeader);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_partners, container, false);
        if (getArguments() != null && !getArguments().getBoolean(ARG_HAS_HEADER, true)) {
            rootView.findViewById(R.id.headerbar).setVisibility(View.GONE);
        } else {
            ((FrameLayout) rootView.findViewById(R.id.list_container))
                    .setForeground(getResources().getDrawable(R.drawable.bottom_shadow));
        }

        rootView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: refactor to use fragment callbacks
                getFragmentManager().popBackStack();
            }
        });

        mCollectionView = (CollectionView) rootView.findViewById(R.id.collection_view);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDisplayCols = getResources().getInteger(R.integer.partners_columns);
        mImageLoader = new ImageLoader(getActivity(), R.drawable.person_image_empty);
        LoaderManager manager = getLoaderManager();
        manager.initLoader(PartnersQuery._TOKEN, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        CursorLoader loader = null;
        if (id == PartnersQuery._TOKEN) {
            loader = new CursorLoader(getActivity(), ScheduleContract.Partners.CONTENT_URI,
                    PartnersQuery.PROJECTION, null, null, PartnersQuery.SORT);
        }
        return loader;
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == PartnersQuery._TOKEN) {
            mPartnersAdapter = new PartnersAdapter(cursor);

            mCollectionView.setCollectionAdapter(mPartnersAdapter);
            mCollectionView.updateInventory(mPartnersAdapter.getInventory());
        } else {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

    /**
     * An adapter for partners. It can also be used with {@link CollectionView}. In that case, use
     * {@link PartnersAdapter#getInventory()} to get a {@link CollectionView.Inventory} and
     * set it to the {@link CollectionView} by
     * {@link CollectionView#updateInventory(CollectionView.Inventory)}.
     */
    private class PartnersAdapter extends CursorAdapter implements CollectionViewCallbacks {
        private final Cursor mCursor;

        public PartnersAdapter(Cursor cursor) {
            super(getActivity(), cursor, 0);
            mCursor = cursor;
        }

        /**
         * Returns a new instance of {@link CollectionView.Inventory}. It always contains only one
         * {@link CollectionView.InventoryGroup}.
         *
         * @return A new instance of {@link CollectionView.Inventory}
         */
        public CollectionView.Inventory getInventory() {
            SparseIntArray levelsCount = new SparseIntArray();
            ArrayList<String> levelNames = new ArrayList<String>();
            while (mCursor.moveToNext())
            {
                int level = mCursor.getInt(PartnersQuery.LEVEL);

                int integer = levelsCount.get(level, -1);
                if(integer != -1)
                {
                    levelsCount.put(level, ++integer);
                }
                else
                {
                    levelsCount.put(level, 1);
                    String levelName = mCursor.getString(PartnersQuery.LEVEL_LABEL);
                    levelNames.add(levelName);
                }
            }
            mCursor.moveToFirst();

            CollectionView.Inventory inventory = new CollectionView.Inventory();

            int id = 0;
            int count = 0;
            int key;
            for(int i = 0; i < levelsCount.size(); i++) {
                key = levelsCount.keyAt(i);
                int number = levelsCount.get(key);
                String levelName = levelNames.get(i);
                int nbColumns;
                if(number <= 3)
                {
                    nbColumns = number;
                }
                else
                {
                   if(number % 2 == 0)
                   {
                       nbColumns = 2; //If number can be divided by 2 we set 2 columns
                   }
                   else
                       nbColumns = 3;
                }

                inventory.addGroup(new CollectionView.InventoryGroup(id)
                        .setDisplayCols(nbColumns)
                        .setItemCount(number)
                        .setDataIndexStart(count)
                        .setShowHeader(true)
                        .setHeaderLabel(levelName));
                id++;
                count += number;

            }

            return inventory;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.list_item_partner, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final String url = mCursor.getString(PartnersQuery.WEBSITE_URL);
            view.findViewById(R.id.partner_target).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!TextUtils.isEmpty(url)) {
                        Intent expertProfileIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        UIUtils.preferPackageForIntent(getActivity(), expertProfileIntent,
                                UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
                        expertProfileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        startActivity(expertProfileIntent);
                    }
                }
            });

            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            mImageLoader.loadImage(cursor.getString(PartnersQuery.LOGO_URL), imageView);
            ((TextView) view.findViewById(R.id.name)).setText(cursor.getString(PartnersQuery.NAME));
            ((TextView) view.findViewById(R.id.url)).setText(
                    TRIM_FROM_DISPLAY_URL_PATTERN.matcher(url).replaceAll(""));
            ((TextView) view.findViewById(R.id.desc)).setText(cursor.getString(PartnersQuery.DESC));
        }

        @Override
        public View newCollectionHeaderView(Context context, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.list_item_sponsor_type, parent, false);
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId, String headerLabel) {
            ((TextView)view.findViewById(R.id.name)).setText(headerLabel);
        }

        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            return newView(context, null, parent);
        }

        @Override
        public void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup,
                                           int dataIndex, Object tag) {
            setCursorPosition(dataIndex);
            bindView(view, context, mCursor);
        }

        private void setCursorPosition(int position) {
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
        }
    }

    private interface PartnersQuery {
        static final int _TOKEN = 0;
        static final String[] PROJECTION = {
                ScheduleContract.Partners._ID,
                ScheduleContract.Partners.PARTNER_ID,
                ScheduleContract.Partners.PARTNER_NAME,
                ScheduleContract.Partners.PARTNER_LOGO_URL,
                ScheduleContract.Partners.PARTNER_DESC,
                ScheduleContract.Partners.PARTNER_WEBSITE_URL,
                ScheduleContract.Partners.PARTNER_LEVEL,
                ScheduleContract.Partners.PARTNER_LEVEL_LABEL,
        };

        static final String SORT = ScheduleContract.Partners.PARTNER_LEVEL + " ASC";

        static final int ID = 1;
        static final int NAME = 2;
        static final int LOGO_URL = 3;
        static final int DESC = 4;
        static final int WEBSITE_URL = 5;
        static final int LEVEL = 6;
        static final int LEVEL_LABEL = 7;
    }

}