/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.gallery3d.ui;

import android.content.Context;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;

import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ReverseGeocoder;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

public class DetailsAddressResolver {
    private final GalleryActivity mContext;
    private final Handler mHandler;
    private AddressResolvingListener mListener;
    private Future<Address> mAddressLookupJob;

    public DetailsAddressResolver(GalleryActivity context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public String resolveAddress(double[] latlng, AddressResolvingListener listener) {
        mListener = listener;
        mAddressLookupJob = mContext.getThreadPool().submit(new AddressLookupJob(latlng), new FutureListener<Address>() {
            public void onFutureDone(final Future<Address> future) {
                mAddressLookupJob = null;
                if (!future.isCancelled()) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            updateLocation(future.get());
                        }
                    });
                }
            }
        });
        return GalleryUtils.formatLatitudeLongitude("(%f,%f)", latlng[0], latlng[1]);
    }

    private void updateLocation(Address address) {
        if (address != null) {
            Context context = mContext.getAndroidContext();
            String[] parts = {address.getAdminArea(), address.getSubAdminArea(), address.getLocality(), address.getSubLocality(), address.getThoroughfare(), address.getSubThoroughfare(), address.getPremises(), address.getPostalCode(), address.getCountryName()};

            String addressText = "";
            for (int i = 0; i < parts.length; i++) {
                if (parts[i] == null || parts[i].isEmpty()) continue;
                if (!addressText.isEmpty()) {
                    addressText += ", ";
                }
                addressText += parts[i];
            }
            String text = String.format("%s : %s", DetailsHelper.getDetailsName(context, MediaDetails.INDEX_LOCATION), addressText);
            mListener.onAddressAvailable(text);
        }
    }

    public void cancel() {
        if (mAddressLookupJob != null) {
            mAddressLookupJob.cancel();
            mAddressLookupJob = null;
        }
    }

    public interface AddressResolvingListener {
        void onAddressAvailable(String address);
    }

    private class AddressLookupJob implements Job<Address> {
        private double[] mLatlng;

        protected AddressLookupJob(double[] latlng) {
            mLatlng = latlng;
        }

        public Address run(JobContext jc) {
            ReverseGeocoder geocoder = new ReverseGeocoder(mContext.getAndroidContext());
            return geocoder.lookupAddress(mLatlng[0], mLatlng[1], true);
        }
    }
}
