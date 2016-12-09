package com.yuyu.clearn.activity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.vr.sdk.widgets.video.VrVideoView;
import com.google.vr.sdk.widgets.video.VrVideoView.Options;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.yuyu.clearn.R;
import com.yuyu.clearn.api.AudioWriterPCM;
import com.yuyu.clearn.api.NaverRecognizer;
import com.yuyu.clearn.retrofit.Video;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public class VideoActivity extends AppCompatActivity {

    // 이전 로그인 액티비티에서 전달받은 값 v_num과 m_token을 서버에 request 이후
    // 일치하는 데이터의 여러 정보를 response 받음
    public interface PostVideo {
        @FormUrlEncoded
        @POST("api/video")
        Call<Video> video(@Field("v_num") int v_num,
                          @Field("m_token") String m_token);
    }

    // 동영상 시청이 끝났을 경우 v_num을 request하여
    // 해당 v_num을 가진 동영상의 v_finish를 Y로 update함
    public interface PostFinish {
        @FormUrlEncoded
        @POST("api/finish")
        Call<Void> finish(@Field("v_num") int v_num);
    }

    // 동영상 시청이 중단되었을 경우 v_num과 v_ctime을 request하여
    // 해당 v_num을 가진 동영상의 v_ctime을 update함
    public interface PostSave {
        @FormUrlEncoded
        @POST("api/save")
        Call<Void> save(@Field("v_num") int v_num,
                        @Field("v_ctime") long v_ctime);
    }

    @BindView(R.id.video_view)
    VrVideoView video_view;

    private static final String TAG = VideoActivity.class.getSimpleName();
    private static final String CLIENT_ID = "hgjHh11TeYg649dN5zT1";

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;
    private AudioWriterPCM writer;
    private Options options;
    private Thread pThread;

    private boolean isPaused;
    private long v_ctime;

    // 음성 인식 이벤트 과정
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                break;

            case R.id.finalResult:
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                for (String result : results) {
                    // 음성 인식 이벤트 처리
                    if (result.contains("종료")) {
                        exit();
                        break;
                    } else if (result.contains("재생")) {
                        isPaused = true;
                        video_view.playVideo();
                        break;
                    } else if (result.contains("정지")) {
                        isPaused = false;
                        video_view.pauseVideo();
                        break;
                    } else if (result.contains("앞으로")) {
                        video_view.seekTo(v_ctime += 20000);
                        break;
                    } else if (result.contains("뒤로")) {
                        video_view.seekTo(v_ctime -= 20000);
                        break;
                    } else if (result.contains("처음으로")) {
                        video_view.seekTo(v_ctime = 0);
                        break;
                    }
                }
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }
                break;

            case R.id.clientInactive:
                if (writer != null) {
                    writer.close();
                }
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        ButterKnife.bind(this);
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        uiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        uiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        uiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        handler = new RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);
        options = new Options();
        options.inputFormat = Options.FORMAT_DEFAULT;
        options.inputType = Options.TYPE_MONO;
        // 동영상이 클릭 되었을 경우 음성 인식 이벤트를 1.5초간 받음
        // 추후 카드보드의 버튼으로도 대체 할 예정
        video_view.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP && !naverRecognizer.getSpeechRecognizer().isRunning()) {
                naverRecognizer.recognize();
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        naverRecognizer.getSpeechRecognizer().stop();
                    }
                }.sendEmptyMessageDelayed(0, 1500);
            }
            return true;
        });
        // 동영상 시작 시 풀 스크린 모드로 진행
        // 취소 버튼 무효화(음성 인식 이벤트 '종료'로 값을 처리해야 되기 때문)
        video_view.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_STEREO);
        video_view.fullScreenDialog.setCancelable(false);
        // PostVideo 인터페이스를 사용해 이전 로그인 액티비티에서 전달받은 값
        // v_num과 m_token을 서버에 request 이후 response 받은 여러 정보들을 사용
        Call<Video> videoCall = new Retrofit.Builder()
                .baseUrl(LoginActivity.BASE + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PostVideo.class)
                .video(getIntent().getIntExtra("v_num", -1), getIntent().getStringExtra("m_token"));
        videoCall.enqueue(new Callback<Video>() {
                              @Override
                              public void onResponse(Call<Video> call, Response<Video> response) {
                                  Video repo = response.body();
                                  try {
                                      video_view.loadVideo(Uri.parse(LoginActivity.BASE + repo.getV_uri()), options);
                                  } catch (IOException e) {
                                      Log.e(TAG, String.valueOf(e));
                                  }
                                  video_view.seekTo(repo.getV_ctime());
                                  // 미디어 플레이어 혹은 비디오 플레이어가 종료되면 실행되는 인터페이스인
                                  // OnCompletionListener가 VrVideoView에 없는 관계로 직접 구현함
                                  // Thread를 돌려서 해당 동영상이 종료되면(총 재생 시간보다 현재 재생 시간이 많거나 같을 경우)
                                  // PostFinish 인터페이스를 사용해 이전 로그인 액티비티에서 전달받은 값 v_num을 서버에 request
                                  // 이후 해당 v_num의 v_finish의 값을 N에서 Y로 update
                                  Runnable runnable = () -> {
                                      while (!pThread.isInterrupted()) {
                                          v_ctime = video_view.getCurrentPosition();
                                          Log.e(v_ctime + "", video_view.getDuration() + "");
                                          if (v_ctime >= video_view.getDuration() && video_view.getDuration() != -1) {
                                              if (repo.getV_finish().equals("N")) {
                                                  Call<Void> finishCall = new Retrofit.Builder()
                                                          .baseUrl(LoginActivity.BASE + "/")
                                                          .addConverterFactory(GsonConverterFactory.create())
                                                          .build()
                                                          .create(PostFinish.class)
                                                          .finish(getIntent().getIntExtra("v_num", -1));
                                                  finishCall.enqueue(new Callback<Void>() {
                                                      @Override
                                                      public void onResponse(Call<Void> call, Response<Void> response) {
                                                      }

                                                      @Override
                                                      public void onFailure(Call<Void> call, Throwable t) {
                                                      }
                                                  });
                                              }
                                              exit();
                                          }
                                      }
                                  };
                                  pThread = new Thread(runnable);
                                  pThread.start();
                              }

                              @Override
                              public void onFailure(Call<Video> call, Throwable t) {
                                  Log.e(TAG, String.valueOf(t));
                              }
                          }

        );
    }

    @Override
    public void onPause() {
        super.onPause();
        naverRecognizer.getSpeechRecognizer().release();
        video_view.pauseRendering();
        v_ctime = video_view.getCurrentPosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        naverRecognizer.getSpeechRecognizer().initialize();
        video_view.resumeRendering();
        video_view.seekTo(v_ctime);
        if (isPaused) {
            video_view.pauseVideo();
        } else {
            video_view.playVideo();
        }
    }

    // OnCompletionListener 역할을 대신 수행하는 Thread를 종료하고,
    // 현재 재생 시간을 PostSave 인터페이스를 사용해 이전 로그인 액티비티에서 전달받은 값
    // v_num과 현재 재생 시간인 v_ctime을 서버에 request
    // 이후 해당 v_num에 v_ctime을 update
    // 어플을 종료
    public void exit() {
        pThread.interrupt();
        Call<Void> saveCall = new Retrofit.Builder()
                .baseUrl(LoginActivity.BASE + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PostSave.class)
                .save(getIntent().getIntExtra("v_num", -1), v_ctime);
        saveCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                video_view.fullScreenDialog.dismiss();
                video_view.pauseRendering();
                video_view.shutdown();
                finish();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }

    private static class RecognitionHandler extends Handler {
        private final WeakReference<VideoActivity> mActivity;

        RecognitionHandler(VideoActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }
}