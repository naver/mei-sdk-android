/*
Copyright 2018 NAVER Corp.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.naver.mei.sdk.core.video;

import android.view.View;

import java.io.Serializable;

/**
 * Created by tigerbaby on 2016-11-21.
 */

public class CropOptions implements Serializable {
	int left;
	int top;
	int width;
	int height;

	public CropOptions(int left, int top, int width, int height) {
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;

//		Log.d("MEI-TEST", "left : " + this.left + " / top : " + this.top + " / width : " + this.width + " / height : " + this.height);
	}

	public CropOptions(View view, double widthRatio, double heightRatio) {
		int[] viewLocations = new int[2];
		view.getLocationInWindow(viewLocations);

//		Log.d("MEI-TEST", "BEFORE :: left : " + viewLocations[0] + " / top : " + viewLocations[1] + " / width : " + view.getWidth()+ " / height : " + view.getHeight());

		this.left = viewLocations[0];
		this.top = viewLocations[1];
		this.width = view.getWidth();
		this.height = view.getHeight();

//		this.left = (int) (viewLocations[0] * widthRatio);
//		this.top = (int) (viewLocations[1] * heightRatio);
//		this.width = (int) (view.getWidth() * widthRatio);
//		this.height = (int) (view.getHeight() * heightRatio);

//		Log.d("MEI-TEST", "AFTER :: left : " + left + " / top : " + top + " / width : " + width + " / height : " + height);
//		Log.d("MEI-TEST", "widthRatio : " + widthRatio + " / heightRatio : " + heightRatio);
	}

	public int getLeft() {
		return left;
	}

	public int getTop() {
		return top;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

}
