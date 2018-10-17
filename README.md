

# MEI-Android SDK
*What's faster than the other Android gif encoder*

**MEI** : MEme Interchage  
**MEI-SDK** : help you to generate MEME contents


## MEI-SDK 소개
* MEI-SDK는 Fast GIF 인코딩 기능과 GIF 생성에 필요한 View 컴포넌트를 지원합니다.
* aar 파일을 통해 GIF 인코딩 및 GIF 생성과 관련된 뷰 컴포넌트를 이용할 수 있습니다.
* 샘플앱을 통해 주요 기능들을 사용해볼 수 있습니다.

## 주요 지원 기능
* 다수 이미지들을 통한 GIF 이미지 생성
* 비디오로 부터 GIF 이미지 생성 (4.1 이상)
* 스크린 녹화를 통한 GIF 이미지 생성 (5.0 이상)
* 카메라를 통한 GIF 이미지 생성
* 일반 이미지와 GIF 간 합성
* GIF와 GIF 간 합성
* WYSIWYG 캔버스 뷰 지원
* 스티커 뷰를 통한 이미지 합성


## 사용법

### 다수 이미지들을 통한 GIF 이미지 생성 (GIF Encoder)
* MEI-SDK는 빠른 GIF 인코딩을 지원합니다.
* https://d2.naver.com/helloworld/1565302

```java
// GIF encoder 인스턴스 생성
MeiGifEncoder encoder = MeiGifEncoder.newInstance();
encoder.setQuality(10);
encoder.setColorLevel(7);
encoder.setDelay(200);

// 인코딩 리스너 정의
EncodingListener encodingListener = new EncodingListener() {
	@Override
	public void onSuccess() {
		// ...
	}

	@Override
	public void onError(Exception ex) {
		// ...
	}

	@Override
	public void onProgress(double progress) {
		// 작업 진행 상황이 전달 됨. 0 ~ 1 사이의 값
	}
};

// Case 1. Bitmaps to GIF File
List<Bitmap> bitmaps = ... ;     // 이미지 bitmap list
String outputFielPath = ... ;    // GIF를 저장할 파일 경로
encoder.encodeByBitmaps(bitmaps, outputFilePath, encodingListener);

// Case 2. Bitmaps to OutputStream
OutputStream outputStream = new ByteArrayOutputStream();
encoder.encodeByBitmaps(bitmaps, outputStream, encodingListener);


// Case 3. Image paths to GIF File
List<String> imagePaths = ... ;     // 이미지 파일 경로 list
String outputFielPath = ... ;       // GIF를 저장할 파일 경로
encoder.encodeByImagePaths(imagePaths, outputFilePath, encodingListener);

// Case 4. Image paths to OutputStrearm
OutputStream outputStream = new ByteArrayOutputStream();
encoder.encodeByBitmaps(imagePaths, outputStream, encodingListener);
```

quality
* 옵션 값이 낮을수록 품질이 좋아지지만 인코딩에 더 많은 시간이 소요됩니다.
* default 값은 10이며, 30 가량의 값을 사용해도 괜찮습니다.

colorLevel
* 값이 높을수록 품질은 좋아지지만 인코딩에 더 많은 시간이 소요됩니다.
* 6~8까지 설정 가능합니다.
* default 값은 7이며, 6으로 세팅할 경우 품질 저하를 느낄 수 있습니다.

delay
* 프레임 간의 딜레이 값이며 단위는 ms 입니다.
* default 값은 100 입니다.

## 이미지 합성
MEI-SDK를 사용하면 약간의 코드로 강력한 이미지 합성 기능을 사용할 수 있습니다.

MEI-SDK를 이용한 이미지 합성은 배경 이미지 + 스티커 오버레이 방식으로 이루어집니다.

| 합성 요소 | 허용 타입 |
|:---|:---|
| 배경 이미지 | 모든 이미지 타입(JPG, PNG, BMP, Animaged Gif...) |
| 이미지 스티커 | 모든 이미지 타입(JPG, PNG, BMP, Animaged Gif...) |
| 텍스트 스티커 | 일반 텍스트 |

아래에서는 MEI-SDK를 활용한 실질적인 합성 과정을 소개합니다.<br />

---

### 캔버스를 통한 이미지 합성

MEI-SDK에서 제공하는 캔버스 뷰를 사용해 이미지를 합성하는 것은 **가장 쉽고 강력한 방식** 입니다.<br />
사용자는 캔버스 뷰에 보이는 그대로 합성 결과물을 얻을 수 있습니다. (WYSIWYG) <br />
아래는 캔버스 뷰의 기본 구조를 나타냅니다.<br />
<img width="400" src="/doc/structure_canvas_view.png?raw=true" />
>*캔버스 뷰의 크기와 출력 영역 크기는 다르며, 최종 결과물 영역은 백그라운드 이미지 영역에 해당합니다.*<br />

<br />

#### 1. MeiCanvasView 생성

##### (1) Activity XML에 MeiCanvasView 선언
캔버스 뷰 사용을 위해 XML에 정의합니다.

```xml
<com.naver.mei.sdk.view.MeiCanvasView
    android:id="@+id/mei_canvas"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

--

#### 2. Activity Class에서 캔버스 뷰 참조 획득

캔버스 뷰에 다양한 조작(스티커 추가 등)을 위해 MeiCanvasView 레퍼런스를 획득합니다.

```java
// declare java fields
MeiCanvasView meiCanvasView;

@Override
protected void onCreate(Bundle savedInstanceState) {
    meiCanvasView = findViewById(R.id.mei_canvas);
}
```

--

#### 3. 캔버스 뷰에 Background 이미지 추가
캔버스 뷰에는 반드시 하나의 배경 이미지가 지정되어야 합니다.<br />
배경 이미지의 유형으로는 '단일 이미지'와 '다중 이미지(MultiFrame)'가 있습니다.

##### (1) 단일 이미지를 배경 이미지로 지정
하나의 Uri를 배경 이미지로 지정 하는 방식입니다.<br />
이미지 피커(갤러리)에서 JPG, GIF 등의 이미지를 하나만 선택해 배경으로 지정할 경우가 이에 해당합니다.

```java
// 배경 이미지의 Uri를 획득한다.
Uri uri = getBackgroundImageUri();

// 캔버스 뷰에 배경 이미지를 지정한다.
meiCanvasView.setBackgroundImageURI(uri);

// Optional. 배경이 GIF인 경우 재생 방향을 조절할 수 있다. FORWARD, REVERSE, BOOMERANG
meiCanvasView.setBackgroundPlayDirection(PlayDirection.FORWARD);

// Optional. 배경이 GIF인 경우 재생 속도를 조절할 수 있다. 스티커가 있다면 함께 재생 속도가 조절된다.
meiCanvasView.setSpeedRatio(1.0);

// Optional. 배경 이미지의 가로세로비를 조절할 수 있다. 세팅하지 않을 경우 원본비율을 유지한다.
meiCanvasView.setAspectRatio(1.0);

```


##### (2) 다중 이미지를 배경 이미지로 지정
캔버스 뷰에 여러 이미지 파일을 배경 이미지로 전달해 GIF와 같이 만들 수 있습니다.<br />
여러 이미지들의 경로와 배치 정책, 재생 방향을 전달하면 애니메이션 이미지로 표현됩니다.<br />

> 다중 이미지를 배경으로 지정하는 것은 단일 이미지를 지정하는 것보다 훨씬 복잡합니다.<br />
> MeiSDK는 기본적으로 많이 쓰이는 이미지 배치 방식을 기정의해 옵션으로 제공합니다.<br />
> 프레임 크기, 세밀한 배치 등을 하드 코딩 하기 위해서는 [MeiSDK for Power User] 편을 참조해주세요.

```java
// 여러 배경 이미지의 로컬 경로를 획득한다.
List<String> imagePaths = getBackgroundImagePaths();

// 캔버스 뷰에 배경 이미지를 지정한다.
meiCanvasView.setBackgroundMultiFrame(
    imagePaths,
    100, // 프레임 딜레이
    FrameAlignment.KEEP_ORIGINAL_RATIO, // 프레임 정렬 방식. 아래 주석 참조
    PlayDirection.REVERSE // 역방향 재생
);

// Optional. 배경이 MultiFrame인 경우 프레임 정렬 방식을 변경할 수 있다.
// FrameAlignment.KEEP_ORIGINAL_RATIO : 원본비율 유지
// FrameAlignment.FIT_SHORT_AXIS_CENTER_CROP : 화면 맞춤. 짧은 축 기준으로 크롭 및 가운데 정렬
meiCanvasView.setBackgroundMultiFrameAlignment(FrameAlignment.FIT_SHORT_AXIS_CENTER_CROP);

// Optional. 재생 방향을 조절할 수 있다. FORWARD, REVERSE, BOOMERANG
meiCanvasView.setBackgroundPlayDirection(PlayDirection.FORWARD);

// Optional. 재생 속도를 조절할 수 있다. 스티커가 있다면 함께 재생 속도가 조절된다.
meiCanvasView.setSpeedRatio(1.0);

// Optional. 배경 이미지의 가로세로비를 조절할 수 있다. 세팅하지 않을 경우 원본비율을 유지한다.
meiCanvasView.setAspectRatio(1.0);

```

|KEEP_ORIGINAL_RATIO|FIT_SHORT_AXIS_CENTER_CROP|
|:---:|:---:|
|<img width=300 src="/doc/keep_origin_ratio.gif" />|<img width=300 src="/doc/fit_screen_center_crop.gif" />|

> 추가 옵션이 필요하신 경우 별도로 요청주시기 바랍니다.

<br />

--

#### 4. MeiCanvasView에 StickerView 추가하기
스티커 뷰는 크게 TextStickerView와 ImageStickerView로 구분됩니다.<br />
스티커 뷰에 대한 상세한 설명은 [이곳](/MEI/MEI-Android/wiki/StickerView)을 참고하세요.<br />
캔버스 뷰에 스티커 뷰를 추가하기만 하면 아래와 같은 기능들을 사용할 수 있습니다.
* 스티커 확대 축소
* 스티커 회전
* 스티커 드래그 & 드랍
* 배경 이미지와 스티커 이미지간의 애니메이션 동기화

<img src="/doc/sample_add_sticker.gif" />


##### (1) 텍스트 스티커 뷰 생성

```java
// 텍스트 스티커 뷰 인스턴스 생성
TextStickerView textStickerView = new TextStickerView(this); // this == activity

// 텍스트 스티커 뷰 초기값 세팅
textStickerView.setText("Write Text");

// 텍스트 스티커 뷰 설정
textStickerView.setTextSize(20);
textStickerView.setTextColor(Color.parseColor("#121212"));
textStickerView.setControlButtonImage(R.drawable.arrow);  // optional
textStickerView.setDeleteButtonImage(R.drawable.delete);  // optional
textStickerView.setMaxEditTextWidthRatio(0.8f);  // 화면대비 최대 너비 비율, default: 0.9f

// 캔버스 뷰에 스티커 뷰 추가
// stickerView, x좌표, y좌표
meiCanvasView.addStickerView(textStickerView, 50, 50);
```


##### (2) 이미지 스티커 뷰

```java
// ImageStickerView 인스턴스 생성
ImageStickerView imageStickerView = new ImageStickerView(this); // this == activity

// 이미지 리소스 할당
textStickerView.setImageResourceId(R.id.sticker); // from Resource ID
imageStickerView.setImageUri(imageUri); // from Uri

// 이미지 스티커 설정
textStickerView.setControlButtonImage(R.drawable.arrow);  // optional
textStickerView.setDeleteButtonImage(R.drawable.delete);  // optional

// 캔버스 뷰에 스티커 뷰 추가
// stickerView, x좌표, y좌표
meiCanvasView.addStickerView(textStickerView, 50, 50);
```

--

#### 5. 이미지 합성
ImageCompositor에 캔버스 뷰를 전달하는 것만으로 보이는 그대로 합성할 수 있습니다.<br />
ImageCompositor는 내부적으로 쓰레드를 생성하므로 별도의 쓰레드 처리를 하지 않아도 됩니다.

```java
MeiSDK.createImageCompositor()
    .setMeiCanvasView(meiCanvasView)
    .setEventListener(new MeiEventListener() {
        // 합성은 비동기로 동작하므로 콜백 지정이 필수적
        @Override
        public void onSuccess(final String resultFilePath) {
            // 이미지 합성 성공 시의 로직 작성.
        }

        @Override
        public void onFail(MeiSDKErrorType meiSDKErrorType) {
            // 이미지 합성 실패 시의 로직 작성
        }

        @Override
        public void onProgress(final double progress) {
            // 이미지 합성 도중의 진행 상황에 대한 콜백.
            // 한 장의 프레임에 대한 합성이 완료되면 콜백 메서드가 호출 됨
	}
    })
    .setSavedFilePath(resultPath) // 출력 결과물이 저장될 경로
    .setOutputWidth(640) // 출력 결과물의 width 사이즈. 이에 맞춰 리사이즈 됨
    .composite();

    // 이미지 합성이 시작되고 난 이후 합성 진행 중임을 노출할 코드 추가
}

```
> SavedFilePath를 지정하지 않을 경우 /SDCARD/DCIM/MEI/timestamp 경로에 저장됩니다. <br />
> OutputWidth를 지정하지 않을 경우 기본 이미지 사이즈(640)으로 지정됩니다.

---

### 로우 레벨 코딩을 통한 이미지 합성

캔버스 뷰 없이 이미지 배치 좌표, 크기와 같은 메타 데이터를 조작해 직접 합성을 시도할 수 있습니다.<br />
그러나 SDK에 대한 깊을 이해를 요하며 캔버스 뷰를 사용하는 합성보다 매우 어렵습니다.<br />
향후.. 가이드 작성 예정입니다.  [MeiSDK for Power User]



## 비디오로 부터 GIF 이미지 생성 (4.1 이상)
해상도가 높은 형상의 경우 임의로 이미지 사이즈가 작게 조절됩니다.

```java
// test.mp4 영상을 1초 ~ 3초까지 5fps의 gif로 생성하려는 경우
String videoPath = "/sdcard/dcim/test.mp4";
long startMillis = 1000;
long endMillis = 3000;
int fps = 5;
VideoToGifParams videoToGifParams = new VideoToGifParams(videoPath, startMillis, endMillis, fps);

// width와 height를 원본과 다르게 지정하는 경우에 아래 코드 추가
int targetWidth = 1280;
int targetHeight = 720;
videoToGifParams.setTargetSize(targetWidth, targetHeight);

MeiEventListener eventListener = new MeiEventListener() {
	@Override
	public void onSuccess(String savedFilePath) {
		// 성공시 저장된 경로가 반환
	}
	@Override
	public void onFail(MeiErrorType meiErrorType) {
		// 실패시 실패 원인이 전달됨
	}
	@Override
	public void onProgress(double progress) {
		// 0 ~ 1 사이의 진행 상황이 전달됨
	}
};

// 저장될 경로. 지정하지 않으면 /SDCARD/DCIM/MEI 하위에 timestamp로 저장됨
String resultFilePath = "/sdcard/dcim/result.gif";

MeiSDK.videoToGif(videoGifMetas, eventListener, resultFilePath);
```


## 스크린 녹화를 통한 GIF 이미지 생성 (5.0 이상)
화면 전체를 녹화한 뒤 비디오 영역만 Crop해 GIF 이미지를 생성할 수 있습니다.
캡쳐의 원리이기 때문에 결과물의 품질이 기대보다 다소 낮을수 있습니다.

```java
VideoView videoView = (VideoView)findViewById(R.id.video_view) // layout에서 video_view를 매핑
MeiScreenRecorder screenRecorder = new MeiScreenRecorder(this, videoView);

// quality 설정. default는 10
screenRecorder.getGifEncodingOptions().setQuality(10);
// colorLevel 설정. default는 7
screenRecorder.getGifEncodingOptions().setColorLevel(7);

String watermarkUri = ""; // 워터마크 이미지 경로. 리소스나 파일 경로
WatermarkPosition watermarkPosition = WatermarkPosition.RIGHT_BOTTOM; // 워터마크 포지션
int margin = 20; // 가장 자리로부터의 margin 값
recorder.setWatermark(watermarkUri, watermarkPosition, margin);

//width와 height를 지정하지 않으면 이미지 원래 크기로 들어가며 임의로 사이즈를 줄 수 도 있습니다.
int width = 20;
int height = 20;
recorder.setWatermark(watermarkUri, width, height, watermarkPosition, margin);

MeiQueuingEventListener eventListener = new MeiQueuingEventListener() {
	@Override
	public void onSuccess(String resultFilePath) {
		// 성공시 저장된 GIF파일의 경로가 반환
	}
	@Override
	public void onFail(MeiErrorType meiErrorType) {
		// 실패시 실패 원인이 전달됨
	}
	@Override
	public void onStop(int total) {
		// 녹화를 멈추고 결과물을 생성할 때의 총 frame 개수가 전달됨
	}
	@Override
	public void onFrameProgress(int current, int total) {
		// current : 현재 인코딩이 진행 된 frame의 수
		// total : 인입 된 총 frame의 개수
	}
};

// 레코딩을 위한 권한 confirm창을 띄움. 결과는 onActivityResult로 전달 됨
screenRecorder.notifyRecordingActionToUser();

// 레코딩 중단을 원하는 시기에 아래 메서드 호출
screenRecorder.stop();
```

```java
@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
	//user가 confirm한 경우 recording 시작.
	if (requestCode == MeiScreenRecorder.RECORDING_REQUEST_CODE && resultCode == RESULT_OK) {
			int fps = 5;  // 초당 녹화할 frame의 수
			screenRecorder.start(data, fps, eventListener);
	}
}
```


## License

```
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
```
