package com.yuyu.clearn.retrofit;

import com.google.gson.annotations.SerializedName;

// Retrofit 라이브러리로 Restful 통신을 위해 구현한 클래스
public class Member {

    @SerializedName("v_num")
    private int v_num;

    @SerializedName("m_token")
    private String m_token;

    public int getV_num() {
        return v_num;
    }

    public void setV_num(int v_num) {
        this.v_num = v_num;
    }

    public String getM_token() {
        return m_token;
    }

    public void setM_token(String m_token) {
        this.m_token = m_token;
    }

}