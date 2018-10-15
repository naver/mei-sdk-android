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

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.naver.mei.sdk.core.utils.MeiFileUtils;
import com.naver.mei.sdk.error.MeiLog;
import com.naver.mei.sdk.error.MeiSDKErrorType;
import com.naver.mei.sdk.error.MeiSDKException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Created by tigerbaby on 2017-01-23.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class MeiVideoDecoder {
	private static final String VIDEO = "video/";
	private static final int UNKNOWN_TRACK = -1;
	private static final int MAX_FRAME_SEEK_COUNT = 150;
	private static final int MAX_RESOLUTION = 1280;
	private static final int ORIENTATION_PORTRAIT = 90;
	private static final int ORIENTATION_PORTRAIT_REVERSE = 270;

	private MediaCodec mediaCodec;
	private MediaExtractor mediaExtractor;
	private OutputSurface outputSurface;

	private MediaFormat mediaFormat;
	private String mimeType;

	private int trackIndex = UNKNOWN_TRACK;
	private int frameWidth;
	private int frameHeight;
	private int rotation;
	private boolean needInvert = true;
	private long timeoutUs = 100000;

	public void setDataSource(String path) {
		try {
			mediaExtractor = new MediaExtractor();
			mediaExtractor.setDataSource(path);

			setTrackIndex();
		} catch (IOException e) {
			MeiLog.e("setDataSource error", e);
		}
	}

	public void setRotation(int rotation) {
		this.rotation = rotation;
	}

	public void start() {
		try {
			if (trackIndex == UNKNOWN_TRACK) {
				MeiLog.e("Video Track Not Found.");
				throw new MeiSDKException(MeiSDKErrorType.VIDEO_TO_GIF_FAILED_TO_LOAD_VIDEOTRACK);
			}

			frameWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
			frameHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);

//			scalingSize();

//			int largeEdge = frameWidth > frameHeight ? frameWidth : frameHeight;
//			int smallEdge = frameWidth < frameHeight ? frameWidth : frameHeight;

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
				outputSurface = new OutputSurface(frameWidth, frameHeight);

				if (frameWidth > MAX_RESOLUTION || frameHeight > MAX_RESOLUTION) {
					throw new MeiSDKException(MeiSDKErrorType.NOT_AVAILABLE_RESOLUTION);
				}
			} else {
				if (rotation == ORIENTATION_PORTRAIT || rotation == ORIENTATION_PORTRAIT_REVERSE) {
					outputSurface = new OutputSurface(frameHeight, frameWidth);
					needInvert = false;
				} else {
					outputSurface = new OutputSurface(frameWidth, frameHeight);
				}
			}

			MeiLog.d("W/H/ROTATION : " + frameWidth + ", " + frameHeight + ", " + rotation);

			mediaCodec = MediaCodec.createDecoderByType(mimeType);
			mediaCodec.configure(mediaFormat, outputSurface.getSurface(), null, 0);
			mediaCodec.start();
		} catch (Exception e) {
			MeiLog.e("extract frame error", e);
		}
	}

	private void scalingSize() {
		while (frameWidth > 1000 || frameHeight > 1000) {
			frameWidth /= 2;
			frameHeight /= 2;
		}
	}

	private void setTrackIndex() {
		int trackCount = mediaExtractor.getTrackCount();
		for (int index = 0; index < trackCount; index++) {
			MediaFormat format = mediaExtractor.getTrackFormat(index);
			String mime = format.getString(MediaFormat.KEY_MIME);
			if (mime.startsWith(VIDEO)) {
				trackIndex = index;
				mediaFormat = format;
				mimeType = mime;
				mediaExtractor.selectTrack(trackIndex);
				break;
			}
		}
	}

	public Bitmap getFrame(long frameTimeUs) {
		if (mediaCodec == null) return null;

		MeiLog.d("target frame timeMs : " + frameTimeUs);
		mediaExtractor.seekTo(frameTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

		Bitmap result = null;

		try {
			boolean hasOutput = false;
			boolean isTargetFrame = false;
			int retryCount = 0;

			ByteBuffer inputBuffer;
			ByteBuffer[] inputBuffers;
			int inputIndex;

			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

			while (!hasOutput && retryCount++ < MAX_FRAME_SEEK_COUNT) {
				inputIndex = mediaCodec.dequeueInputBuffer(timeoutUs);

				if (inputIndex >= 0) {
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {     // for android 4.x version
						inputBuffers = mediaCodec.getInputBuffers();
						inputBuffer = inputBuffers[inputIndex];
					} else {                        // for android 5.0 or higher
						inputBuffer = mediaCodec.getInputBuffer(inputIndex);
					}

					int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
					long presentationTimeUs = mediaExtractor.getSampleTime();
					isTargetFrame = presentationTimeUs <= frameTimeUs ? false : true;

					if (sampleSize > 0) {
						mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
						mediaExtractor.advance();
					} else {
						mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						break;
					}
				}

				int outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeoutUs);

				switch (outputIndex) {
					case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
						MeiLog.d("MediaCodec : INFO_OUTPUT_FORMAT_CHANGED");
						break;
					case MediaCodec.INFO_TRY_AGAIN_LATER:
						MeiLog.d("MediaCodec : INFO_TRY_AGAIN_LATER");
						break;
					case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
						MeiLog.d("MediaCodec : INFO_TRY_AGAIN_LATER");
						break;
					default:    //outputIndex bigger than 0
						mediaCodec.releaseOutputBuffer(outputIndex, true);

						if (isTargetFrame) {
							outputSurface.awaitNewImage();
							outputSurface.drawImage(needInvert);
							result = outputSurface.getBitmap(rotation);
							hasOutput = true;
						}
						break;
				}
			}
		} catch (Exception e) {
			MeiLog.e("video to gif process failed. ", e);
		}

		return result;
	}

	public void finish() {
		if (mediaCodec != null) {
			mediaCodec.release();
		}

		if (mediaExtractor != null) {
			mediaExtractor.release();
		}
	}

	/**
	 * https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/OutputSurface.java
	 * http://bigflake.com/mediacodec/ExtractMpegFramesTest.java.txt
	 */
	private static class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {
		private TextureRender textureRender;
		private SurfaceTexture surfaceTexture;
		private Surface surface;
		private EGL10 egl10;

		private EGLDisplay eglDisplay = EGL10.EGL_NO_DISPLAY;
		private EGLContext eglContext = EGL10.EGL_NO_CONTEXT;
		private EGLSurface eglSurface = EGL10.EGL_NO_SURFACE;
		int width;
		int height;

		private Object frameSyncObject = new Object();     // guards frameAvailable
		private boolean frameAvailable;
		private ByteBuffer pixelBuffer;                       // used by getBitmap()

		/**
		 * Creates a SampleOutputSurface backed by a pbuffer with the specified dimensions.  The
		 * new EGL context and surface will be made current.  Creates a Surface that can be passed
		 * to MediaCodec.configure().
		 */
		public OutputSurface(int width, int height) {
			if (width <= 0 || height <= 0) {
				throw new IllegalArgumentException();
			}
			egl10 = (EGL10) EGLContext.getEGL();
			this.width = width;
			this.height = height;

			eglSetup();
			makeCurrent();
			setup();
		}

		/**
		 * Creates interconnected instances of SampleTextureRender, SurfaceTexture, and Surface.
		 */
		private void setup() {
			textureRender = new TextureRender();
			textureRender.surfaceCreated();

			surfaceTexture = new SurfaceTexture(textureRender.getTextureId());

			// This doesn't work if this object is created on the thread that CTS started for
			// these test cases.
			//
			// The CTS-created thread has a Looper, and the SurfaceTexture constructor will
			// create a Handler that uses it.  The "frame available" message is delivered
			// there, but since we're not a Looper-based thread we'll never see it.  For
			// this to do anything useful, SampleOutputSurface must be created on a thread without
			// a Looper, so that SurfaceTexture uses the main application Looper instead.
			//
			// Java language note: passing "this" out of a constructor is generally unwise,
			// but we should be able to get away with it here.
			surfaceTexture.setOnFrameAvailableListener(this);

			surface = new Surface(surfaceTexture);
			pixelBuffer = ByteBuffer.allocateDirect(width * height * 4);
			pixelBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}

		/**
		 * Prepares EGL.  We want a GLES 2.0 context and a surface that supports pbuffer.
		 */
		private void eglSetup() {
			final int EGL_OPENGL_ES2_BIT = 0x0004;
			final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

			eglDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
			if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
				throw new RuntimeException("unable to get EGL14 display");
			}
			int[] version = new int[2];
			if (!egl10.eglInitialize(eglDisplay, version)) {
				eglDisplay = null;
				throw new RuntimeException("unable to initialize EGL14");
			}

			// Configure EGL for pbuffer and OpenGL ES 2.0, 24-bit RGB.
			int[] attribList = {
					EGL10.EGL_RED_SIZE, 8,
					EGL10.EGL_GREEN_SIZE, 8,
					EGL10.EGL_BLUE_SIZE, 8,
					EGL10.EGL_ALPHA_SIZE, 8,
					EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
					EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
					EGL10.EGL_NONE
			};
			EGLConfig[] configs = new EGLConfig[1];
			int[] numConfigs = new int[1];
			if (!egl10.eglChooseConfig(eglDisplay, attribList, configs, configs.length,
					numConfigs)) {
				throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
			}

			// Configure context for OpenGL ES 2.0.
			int[] attrib_list = {
					EGL_CONTEXT_CLIENT_VERSION, 2,
					EGL10.EGL_NONE
			};
			eglContext = egl10.eglCreateContext(eglDisplay, configs[0], EGL10.EGL_NO_CONTEXT,
					attrib_list);
			checkEglError("eglCreateContext");
			if (eglContext == null) {
				throw new RuntimeException("null context");
			}

			// Create a pbuffer surface.
			int[] surfaceAttribs = {
					EGL10.EGL_WIDTH, width,
					EGL10.EGL_HEIGHT, height,
					EGL10.EGL_NONE
			};
			eglSurface = egl10.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttribs);
			checkEglError("eglCreatePbufferSurface");
			if (eglSurface == null) {
				throw new RuntimeException("surface was null");
			}
		}

		/**
		 * Discard all resources held by this class, notably the EGL context.
		 */
		public void release() {
			if (eglDisplay != EGL10.EGL_NO_DISPLAY) {
				egl10.eglDestroySurface(eglDisplay, eglSurface);
				egl10.eglDestroyContext(eglDisplay, eglContext);
				//egl10.eglReleaseThread();
				egl10.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
						EGL10.EGL_NO_CONTEXT);
				egl10.eglTerminate(eglDisplay);
			}
			eglDisplay = EGL10.EGL_NO_DISPLAY;
			eglContext = EGL10.EGL_NO_CONTEXT;
			eglSurface = EGL10.EGL_NO_SURFACE;

			surface.release();

			// this causes a bunch of warnings that appear harmless but might confuse someone:
			//  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
			//surfaceTexture.release();

			textureRender = null;
			surface = null;
			surfaceTexture = null;
		}

		/**
		 * Makes our EGL context and surface current.
		 */
		public void makeCurrent() {
			if (!egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
				throw new RuntimeException("eglMakeCurrent failed");
			}
		}

		/**
		 * Returns the Surface.
		 */
		public Surface getSurface() {
			return surface;
		}

		/**
		 * Latches the next buffer into the texture.  Must be called from the thread that created
		 * the SampleOutputSurface object.  (More specifically, it must be called on the thread
		 * with the EGLContext that contains the GL texture object used by SurfaceTexture.)
		 */
		public void awaitNewImage() {
			final int TIMEOUT_MS = 2500;

			synchronized (frameSyncObject) {
				while (!frameAvailable) {
					try {
						// Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
						// stalling the test if it doesn't arrive.
						frameSyncObject.wait(TIMEOUT_MS);
						if (!frameAvailable) {
							// TODO: if "spurious wakeup", continue while loop
							throw new RuntimeException("frame wait timed out");
						}
					} catch (InterruptedException ie) {
						// shouldn't happen
						throw new RuntimeException(ie);
					}
				}
				frameAvailable = false;
			}

			// Latch the data.
			textureRender.checkGlError("before updateTexImage");
			surfaceTexture.updateTexImage();
		}

		/**
		 * Draws the data from SurfaceTexture onto the current EGL surface.
		 *
		 * @param invert if set, render the image with Y inverted (0,0 in top left)
		 */
		public void drawImage(boolean invert) {
			textureRender.drawFrame(surfaceTexture, invert);
		}

		// SurfaceTexture callback
		@Override
		public void onFrameAvailable(SurfaceTexture st) {
			synchronized (frameSyncObject) {
				if (frameAvailable) {
					throw new RuntimeException("frameAvailable already set, frame could be dropped");
				}
				frameAvailable = true;
				frameSyncObject.notifyAll();
			}
		}

		/**
		 * get the current frame to disk as a PNG image.
		 *
		 * @modify by tigerbaby
		 */
		public Bitmap getBitmap(int rotation) throws IOException {
			// glReadPixels gives us a ByteBuffer filled with what is essentially big-endian RGBA
			// data (i.e. a byte of red, followed by a byte of green...).  To use the Bitmap
			// constructor that takes an int[] array with pixel data, we need an int[] filled
			// with little-endian ARGB data.
			//
			// If we implement this as a series of buf.get() calls, we can spend 2.5 seconds just
			// copying data around for a 720p frame.  It's better to do a bulk get() and then
			// rearrange the data in memory.  (For comparison, the PNG compress takes about 500ms
			// for a trivial frame.)
			//
			// So... we set the ByteBuffer to little-endian, which should turn the bulk IntBuffer
			// get() into a straight memcpy on most Android devices.  Our ints will hold ABGR data.
			// Swapping B and R gives us ARGB.  We need about 30ms for the bulk get(), and another
			// 270ms for the color swap.
			//
			// We can avoid the costly B/R swap here if we do it in the fragment shader (see
			// http://stackoverflow.com/questions/21634450/ ).
			//
			// Having said all that... it turns out that the Bitmap#copyPixelsFromBuffer()
			// method wants RGBA pixels, not ARGB, so if we create an empty bitmap and then
			// copy pixel data in we can avoid the swap issue entirely, and just copy straight
			// into the Bitmap from the ByteBuffer.
			//
			// Making this even more interesting is the upside-down nature of GL, which means
			// our output will look upside-down relative to what appears on screen if the
			// typical GL conventions are used.  (For ExtractMpegFrameTest, we avoid the issue
			// by inverting the frame when we render it.)
			//
			// Allocating large buffers is expensive, so we really want pixelBuffer to be
			// allocated ahead of time if possible.  We still get some allocations from the
			// Bitmap / PNG creation.

			pixelBuffer.rewind();
			GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
					pixelBuffer);

			Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			pixelBuffer.rewind();
			bitmap.copyPixelsFromBuffer(pixelBuffer);

			if (rotation != 0) {
				android.graphics.Matrix matrix = new android.graphics.Matrix();

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					matrix.setScale(-1, 1); //left-right inverse
					matrix.postRotate(rotation);
				}

				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			}

//			MeiFileUtils.createFileFromBitmap(bitmap);
			return bitmap;
		}

		/**
		 * Checks for EGL errors.
		 */
		private void checkEglError(String msg) {
			int error;
			if ((error = egl10.eglGetError()) != EGL10.EGL_SUCCESS) {
				throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
			}
		}
	}

	/**
	 * https://android.googlesource.com/platform/cts/+/kitkat-release/tests/tests/media/src/android/media/cts/TextureRender.java
	 * http://bigflake.com/mediacodec/ExtractMpegFramesTest.java.txt
	 */
	private static class TextureRender {
		private static final int FLOAT_SIZE_BYTES = 4;
		private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
		private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
		private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
		private final float[] triangleVerticesData = {
				// X, Y, Z, U, V
				-1.0f, -1.0f, 0, 0.f, 0.f,
				1.0f, -1.0f, 0, 1.f, 0.f,
				-1.0f, 1.0f, 0, 0.f, 1.f,
				1.0f, 1.0f, 0, 1.f, 1.f,
		};

		private FloatBuffer triangleVertices;

		private static final String VERTEX_SHADER =
				"uniform mat4 uMVPMatrix;\n" +
						"uniform mat4 uSTMatrix;\n" +
						"attribute vec4 aPosition;\n" +
						"attribute vec4 aTextureCoord;\n" +
						"varying vec2 vTextureCoord;\n" +
						"void main() {\n" +
						"    gl_Position = uMVPMatrix * aPosition;\n" +
						"    vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
						"}\n";

		private static final String FRAGMENT_SHADER =
				"#extension GL_OES_EGL_image_external : require\n" +
						"precision mediump float;\n" +      // highp here doesn't seem to matter
						"varying vec2 vTextureCoord;\n" +
						"uniform samplerExternalOES sTexture;\n" +
						"void main() {\n" +
						"    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
						"}\n";

		private float[] mvpMatrix = new float[16];
		private float[] stMatrix = new float[16];

		private int program;
		private int textureId = -12345;
		private int mvpMatrixHandle;
		private int stMatrixHandle;
		private int positionHandle;
		private int textureHandle;

		public TextureRender() {
			triangleVertices = ByteBuffer.allocateDirect(
					triangleVerticesData.length * FLOAT_SIZE_BYTES)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			triangleVertices.put(triangleVerticesData).position(0);

			Matrix.setIdentityM(stMatrix, 0);
		}

		public int getTextureId() {
			return textureId;
		}

		/**
		 * Draws the external texture in SurfaceTexture onto the current EGL surface.
		 */
		public void drawFrame(SurfaceTexture st, boolean invert) {
			checkGlError("onDrawFrame start");
			st.getTransformMatrix(stMatrix);

			if (invert) {
				stMatrix[5] = -stMatrix[5];
				stMatrix[13] = 1.0f - stMatrix[13];
			}

			// (optional) clear to green so we can see if we're failing to set pixels
			GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

			GLES20.glUseProgram(program);
			checkGlError("glUseProgram");

			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

			triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
			GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false,
					TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
			checkGlError("glVertexAttribPointer maPosition");
			GLES20.glEnableVertexAttribArray(positionHandle);
			checkGlError("glEnableVertexAttribArray positionHandle");

			triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
			GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false,
					TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
			checkGlError("glVertexAttribPointer textureHandle");
			GLES20.glEnableVertexAttribArray(textureHandle);
			checkGlError("glEnableVertexAttribArray textureHandle");

			Matrix.setIdentityM(mvpMatrix, 0);
			GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
			GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
			checkGlError("glDrawArrays");

			GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
		}

		/**
		 * Initializes GL state.  Call this after the EGL surface has been created and made current.
		 */
		public void surfaceCreated() {
			program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
			if (program == 0) {
				throw new RuntimeException("failed creating program");
			}

			positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
			checkLocation(positionHandle, "aPosition");
			textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
			checkLocation(textureHandle, "aTextureCoord");

			mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
			checkLocation(mvpMatrixHandle, "uMVPMatrix");
			stMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix");
			checkLocation(stMatrixHandle, "uSTMatrix");

			int[] textures = new int[1];
			GLES20.glGenTextures(1, textures, 0);

			textureId = textures[0];
			GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
			checkGlError("glBindTexture textureId");

			GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
					GLES20.GL_NEAREST);
			GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
					GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
					GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
					GLES20.GL_CLAMP_TO_EDGE);
			checkGlError("glTexParameter");
		}

		/**
		 * Replaces the fragment shader.  Pass in null to reset to default.
		 */
		public void changeFragmentShader(String fragmentShader) {
			if (fragmentShader == null) {
				fragmentShader = FRAGMENT_SHADER;
			}
			GLES20.glDeleteProgram(program);
			program = createProgram(VERTEX_SHADER, fragmentShader);
			if (program == 0) {
				throw new RuntimeException("failed creating program");
			}
		}

		private int loadShader(int shaderType, String source) {
			int shader = GLES20.glCreateShader(shaderType);
			checkGlError("glCreateShader type=" + shaderType);
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
			return shader;
		}

		private int createProgram(String vertexSource, String fragmentSource) {
			int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
			if (vertexShader == 0) {
				return 0;
			}
			int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
			if (pixelShader == 0) {
				return 0;
			}

			int program = GLES20.glCreateProgram();
			if (program == 0) {
			}
			GLES20.glAttachShader(program, vertexShader);
			checkGlError("glAttachShader");
			GLES20.glAttachShader(program, pixelShader);
			checkGlError("glAttachShader");
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				GLES20.glDeleteProgram(program);
				program = 0;
			}
			return program;
		}

		public void checkGlError(String op) {
			int error;
			while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
				throw new RuntimeException(op + ": glError " + error);
			}
		}

		public static void checkLocation(int location, String label) {
			if (location < 0) {
				throw new RuntimeException("Unable to locate '" + label + "' in program");
			}
		}
	}
}