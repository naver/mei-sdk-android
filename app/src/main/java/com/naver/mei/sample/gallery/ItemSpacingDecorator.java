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
package com.naver.mei.sample.gallery;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by GTPark on 2016-03-30.
 */
public class ItemSpacingDecorator extends RecyclerView.ItemDecoration {
	private int spanCount;
	private int spacing;
	private int headerCount;

	public ItemSpacingDecorator(int spanCount, int pxSpacing, int headerCount) {
		this.spanCount = spanCount;
		this.spacing = pxSpacing;
		this.headerCount = headerCount;
	}

	public ItemSpacingDecorator(int spanCount, int pxSpacing) {
		this(spanCount, pxSpacing, 0);
	}

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
		int position = parent.getChildAdapterPosition(view); // item position

		if (position < headerCount) return;

		int column = (position - headerCount) % spanCount; // item column
		outRect.left = (column == 0) ? 0 : (column == (spanCount - 1) ? spacing : spacing / 2);
		outRect.right = (column == 0) ? spacing : (column == (spanCount - 1) ? 0 : spacing / 2);
		outRect.top = position >= spanCount ? spacing : 0;
	}
}
