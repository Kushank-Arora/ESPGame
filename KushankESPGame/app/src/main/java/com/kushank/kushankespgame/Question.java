package com.kushank.kushankespgame;

import android.widget.RadioButton;

import java.util.Map;

/**
 * Created by Kushank on 03-09-2017.
 */
//Datastucture to store a question.
public class Question {
    // url to the image question
    String url;

    //url to all the secondary images and their ids.
    Map<String, String> secondaryImg;

    //radiobutton for all the secondary images.
    Map<String, RadioButton> secondaryAns;


    public String getUrl() {
        return url;
    }

    public Map<String, String> getSecondaryImg() {
        return secondaryImg;
    }

    public Map<String, RadioButton> getSecondaryAns() {
        return secondaryAns;
    }

    public void setSecondaryImg(Map<String, String> secondaryImg) {
        this.secondaryImg = secondaryImg;
    }

    public void setSecondaryAns(Map<String, RadioButton> secondaryAns) {
        this.secondaryAns = secondaryAns;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
