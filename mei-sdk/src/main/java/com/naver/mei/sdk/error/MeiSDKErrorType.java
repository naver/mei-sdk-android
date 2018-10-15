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
package com.naver.mei.sdk.error;

/**
 * Created by Naver on 2016-09-29.
 */
public enum MeiSDKErrorType {

	/**
	 * 1XX : error of image composite or create gif
	 */
	FAILED_TO_LOAD_BACKGROUND(101, "failed to load background"),
	FAILED_TO_LOAD_IMAGE(102, "failed to load image"),
	FAILED_TO_SAVE_IMAGE(103, "failed to save image"),
	FAILED_TO_COMPOSITE_IMAGE(105, "failed to composite image"),
	FAILED_TO_CREATE_GIF(106, "failed to create gif"),
	FAILED_TO_CREATE_GIF_NO_FRAME(107, "failed to create gif. There is no frame to create a gif. must be at least one"),
	UNKNOWN_META_TYPE(109, "unknown meta type"),
	FRAME_INDEX_OUT_OF_BOUND(110, "out of bound frame index"),
	INVALID_COLOR_LEVEL_VALUE(120, "invalid color level. (6 to 8)"),
	INVALID_MAX_WIDTH_RATIO_VALUE(130, "invalid max width ratio. (0.5f to 0.9f)"),
	INVALID_URI(140, "invalid uri"),

	/**
	 * 15X : error of video operation
	 */
	FAILED_TO_SCREEN_RECORD(151, "failed to screen record"),
	NOT_AVAILABLE_OS_VERSION_FOR_DECODING(152, "not available on this device. it works with jellybean OS or higher"),
	VIDEO_TO_GIF_FPS_CANNOT_EXCEED_10(153, "fps cannot exceed 10"),
	VIDEO_TO_GIF_FAILED_TO_LOAD_VIDEOTRACK(154, "failed to load video track"),
	NOT_AVAILABLE_RESOLUTION(155, "resolution cannot exceed 1280"),

	/**
	 * 2XX : error of environment
	 */
	NEED_PERMISSION_READ_EXTERNAL_STORAGE(201, "need READ_EXTERNAL_STORAGE permission"),
	NEED_PERMISSION_WRITE_EXTERNAL_STORAGE(202, "need WRITE_EXTERNAL_STORAGE permission"),
	NEED_PERMISSION_RECORD_AUDIO(203, "need RECORD_AUDIO permission"),
	NEED_PERMISSION_CAMERA(204, "need CAMERA permission"),
	FAILED_TO_ACCESS_CAMERA(211, "failed to access camera"),
	FAILED_TO_ACCESS_STORAGE(212, "failed to access storage"),
	NOT_AVAILABLE_OS_VERSION_FOR_RECORDING(213, "not available on this device. it works with jellybean OS or higher"),

	/**
	 * 3XX : error of SDK usage
	 */
	NEED_INITIALIZE(301, "sdk does not initialized. do init()"),
	NEED_EVENT_LISTENER(302, "event listener does not set"),
	NEED_CAMERA_INIT(303, "camera not initialized. do init()"),
	STORAGE_SPACE_FULL(304, "storage space is full."),
	STORAGE_PATH_NOT_VALID(305, "storage path is not vaild."),

	/**
	 * 4XX : error of image crop
	 */
	INVALID_CROP_WINDOW_PADDING_RATIO(401, "Cannot set initial crop window padding value to a number < 0 or >= 0.5"),
	INVALID_ASPECT_RATIO(402, "Cannot set aspect ratio value to a number less than or equal to 0."),
	INVALID_LINE_THICKNESS(403, "Cannot set line thickness value to a number less than 0."),
	INVALID_CORNER_THICKNESS(404, "Cannot set corner thickness value to a number less than 0."),
	INVALID_GUIDE_LINE_THICKNESS(405, "Cannot set guidelines thickness value to a number less than 0."),
	INVALID_MIN_CROP_WINDOW_HEIGHT(406, "Cannot set min crop window height value to a number < 0 "),
	INVALID_MIN_CROP_RESULT_WIDTH(407, "Cannot set min crop result width value to a number < 0 "),
	INVALID_MIN_CROP_RESULT_HEIGHT(408, "Cannot set min crop result height value to a number < 0 "),
	INVALID_MAX_CROP_RESULT_WIDTH(409, "Cannot set max crop result width to smaller value than min crop result width"),
	INVALID_MAX_CROP_RESULT_HEIGHT(410, "Cannot set max crop result height to smaller value than min crop result height"),

	/**
	 * 5XX : error of SDK cache
	 */
	FAILED_TO_LOAD_CACHE_DIRECTORY(501, "failed to load cache directory."),
	FAILED_TO_LOAD_CACHE_ORIGINAL_FILE(502, "failed to load cache original file."),
	FAILED_TO_CREATE_CACHE_FILE(503, "failed to create cache file."),
	FAILED_TO_RENAME_CACHE_FILE(504, "failed to rename cache file."),
	FAILED_TO_READ_CACHE(505, "failed to read cache data."),
	UNKNOWN_CACHE_TYPE(510, "unknown cache type. can not calculate cache size.");


	private int code;
	private String message;

	MeiSDKErrorType(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}