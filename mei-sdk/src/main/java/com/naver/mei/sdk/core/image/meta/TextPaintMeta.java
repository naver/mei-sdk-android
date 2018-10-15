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
package com.naver.mei.sdk.core.image.meta;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

import java.io.Serializable;

/**
 * Created by GTPark on 2016-04-11.
 */
public class TextPaintMeta implements Serializable {
	public final float textSize;
	public final int textColor;
	public final float strokeWidth;
	public final Paint.Join strokeJoin;
	public final Paint.Style style;
	public final String fontPath;
	public final int typefaceStyle; // text style

	public TextPaintMeta(TextPaint textPaint, String fontPath) {
		this.textSize = textPaint.getTextSize();
		this.textColor = textPaint.getColor();
		this.strokeWidth = textPaint.getStrokeWidth();
		this.strokeJoin = textPaint.getStrokeJoin();
		this.style = textPaint.getStyle();
		this.fontPath = fontPath;
		this.typefaceStyle = textPaint.getTypeface().getStyle();
	}

	public TextPaint toTextPaint() {
		TextPaint paint = new TextPaint();
		paint.setTextSize(textSize);
		paint.setColor(textColor);
		paint.setStrokeWidth(strokeWidth);
		paint.setStrokeJoin(strokeJoin);
		paint.setStyle(style);
		paint.setAntiAlias(true);
		paint.setTypeface(Typeface.defaultFromStyle(typefaceStyle));
		return paint;
	}
}

