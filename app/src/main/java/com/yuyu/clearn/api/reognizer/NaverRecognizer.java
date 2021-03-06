package com.yuyu.clearn.api.reognizer;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.naver.speech.clientapi.SpeechConfig;
import com.naver.speech.clientapi.SpeechConfig.EndPointDetectType;
import com.naver.speech.clientapi.SpeechConfig.LanguageType;
import com.naver.speech.clientapi.SpeechRecognitionException;
import com.naver.speech.clientapi.SpeechRecognitionListener;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.naver.speech.clientapi.SpeechRecognizer;
import com.yuyu.clearn.R;

// 네이버 음성 인식 라이브러리로 이벤트를 처리하기 위해 구현한 클래스
public class NaverRecognizer implements SpeechRecognitionListener {

    private final String TAG = NaverRecognizer.class.getSimpleName();

    private Handler handler;
    private SpeechRecognizer speechRecognizer;

    public NaverRecognizer(Context context, Handler handler, String clientId) {
        this.handler = handler;
        try {
            speechRecognizer = new SpeechRecognizer(context, clientId);
        } catch (SpeechRecognitionException e) {
            Log.e(TAG, String.valueOf(e));
        }
        speechRecognizer.setSpeechRecognitionListener(this);
    }

    public SpeechRecognizer getSpeechRecognizer() {
        return speechRecognizer;
    }

    public void recognize() {
        try {
            speechRecognizer.recognize(new SpeechConfig(
                    LanguageType.KOREAN,
                    EndPointDetectType.AUTO));
        } catch (SpeechRecognitionException e) {
            Log.e(TAG, String.valueOf(e));
        }
    }

    @Override
    @WorkerThread
    public void onInactive() {
        Message msg = Message.obtain(handler, R.id.clientInactive);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onReady() {
        Message msg = Message.obtain(handler, R.id.clientReady);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onRecord(short[] speech) {
        Message msg = Message.obtain(handler, R.id.audioRecording, speech);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onPartialResult(String result) {
        Message msg = Message.obtain(handler, R.id.partialResult, result);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onEndPointDetected() {
    }

    @Override
    @WorkerThread
    public void onResult(SpeechRecognitionResult result) {
        Message msg = Message.obtain(handler, R.id.finalResult, result);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onError(int errorCode) {
        Message msg = Message.obtain(handler, R.id.recognitionError, errorCode);
        msg.sendToTarget();
    }

    @Override
    @WorkerThread
    public void onEndPointDetectTypeSelected(EndPointDetectType epdType) {
        Message msg = Message.obtain(handler, R.id.endPointDetectTypeSelected, epdType);
        msg.sendToTarget();
    }
}
