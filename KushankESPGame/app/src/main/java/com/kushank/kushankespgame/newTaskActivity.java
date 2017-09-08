package com.kushank.kushankespgame;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

//Activity to get questions for the new Task.
public class newTaskActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseUser firebaseUser;
    private DatabaseReference questionsRef;
    private ChildEventListener mChildEventListener;
    private Map<String, Integer> mpQues;
    private LinearLayout layout;
    private Map<DatabaseReference, ChildEventListener> mpQuesList;
    private Map<String, Question> questions;
    long timeStart=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        layout = (LinearLayout) findViewById(R.id.layoutNewTask);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        progressBar.setVisibility(View.VISIBLE);

        DatabaseReference taskIdRef = mFirebaseDatabase.getReference().child("users").child(firebaseUser.getUid()).push();
        questionsRef = taskIdRef.child("ques");

        //create a node with justCreated = true in the users/{userid}/{taskid}/
        taskIdRef.child("justCreated").setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(newTaskActivity.this, "An Error Occured!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(newTaskActivity.this, "Task Created!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mpQues = new HashMap<>();
        mpQuesList = new HashMap<>();
        questions = new HashMap<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        attachDatabaseReadListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachDatabaseReadListener();
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            questionsRef.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
        if (mpQuesList != null)
        {
            for(DatabaseReference key: mpQuesList.keySet())
                key.removeEventListener(mpQuesList.get(key));
        }
    }


    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener(){

                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //map to have score and question id.
                    mpQues.put(dataSnapshot.getKey(), dataSnapshot.getValue(Integer.class));

                    // create new question with the given id.
                    questions.put(dataSnapshot.getKey(), new Question());

                    //initialise maps for the question.
                    questions.get(dataSnapshot.getKey()).setSecondaryAns(new HashMap<String, RadioButton>());

                    //set event listener for all the questions.
                    DatabaseReference ref = mFirebaseDatabase.getReference().child("primImage").child(dataSnapshot.getKey());
                    mpQuesList.put(ref , getChildListener(dataSnapshot.getKey()));
                    ref.addChildEventListener(mpQuesList.get(ref));
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //update score.
                    mpQues.put(dataSnapshot.getKey(), dataSnapshot.getValue(Integer.class));
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    //remove question and its score.
                    mpQues.remove(dataSnapshot.getKey());
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            questionsRef.addChildEventListener(mChildEventListener);
        }
    }

    //get event listener for the questions.
    private ChildEventListener getChildListener(final String key) {
        return new ChildEventListener(){
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                switch(dataSnapshot.getKey())
                {
                    //if it is url, update url
                    case "url":
                        questions.get(key).setUrl(dataSnapshot.getValue(String.class));
                        updateUI();
                        break;
                    // if it is secondary images, update it.
                    case "sec":
                        GenericTypeIndicator<Map<String, String>> genericTypeIndicator = new GenericTypeIndicator<Map<String, String>>() {};
                        questions.get(key).setSecondaryImg(dataSnapshot.getValue(genericTypeIndicator));
                        updateUI();
                        break;
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    //update UI based upon the questions got.
    void updateUI(){
        timeStart = Calendar.getInstance().getTimeInMillis();
        progressBar.setVisibility(View.INVISIBLE);
        layout.removeAllViews();
        int i=0;

        final TextView timertv = new TextView(this);
        long timeElapsed = Calendar.getInstance().getTimeInMillis() - timeStart;
        timertv.setText(timeElapsed + " secs Elapsed");
        timertv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        layout.addView(timertv);

        int delay = 0; // delay for 0 sec.
        int period = 1000; // repeat every 1 sec.
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {
                try{
                    long timeElapsed = (Calendar.getInstance().getTimeInMillis() - timeStart)/1000;
                    timertv.setText(timeElapsed + " secs Elapsed");
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }, delay, period);
        /*
        Handler handler = new Handler();
        Runnable runnable = new Runnable(){
            public void run() {
                try{
                    long timeElapsed = Calendar.getInstance().getTimeInMillis() - timeStart;
                    timertv.setText(timeElapsed + " secs Elapsed");
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        handler.postAtTime(runnable, System.currentTimeMillis()+1000);
        handler.postDelayed(runnable, 1000);
        */

        for(final String quesKey: questions.keySet())
        {
            i++;
            // get the question.
            Question ques = questions.get(quesKey);

            //if the data for the question is complete, move forward, else see next question.
            if(ques.getUrl()==null || ques.getSecondaryImg()==null)
                continue;

            // set the label for the new question.
            TextView tv = new TextView(this);
            tv.setText("Ques "+i);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            layout.addView(tv);

            // set the image for the question
            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(500, 500);
            iv.setLayoutParams(layoutParams);
            layout.addView(iv);

            // get the image from the firebase storage in the imageview directly.
            Glide.with(iv.getContext())
                    .load(ques.getUrl())
                    .dontAnimate()
                    .into(iv);

            Map<String, String> mp = ques.getSecondaryImg();

            // set the radio buttons for all the options, and add it to the main layout.
            for(final String key: mp.keySet())
            {
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                final RadioButton rb = new RadioButton(this);
                questions.get(quesKey).getSecondaryAns().put(key, rb);

                linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Map<String, RadioButton> rbGroup = questions.get(quesKey).getSecondaryAns();
                        boolean isChecked = rbGroup.get(key).isChecked();
                        if(isChecked) {
                            rbGroup.get(key).setChecked(false);
                            return;
                        }
                        for(String curKey: rbGroup.keySet())
                            if(curKey.equals(key))
                                rbGroup.get(curKey).setChecked(true);
                            else
                                rbGroup.get(curKey).setChecked(false);
                    }
                });

                ImageView iv2 = new ImageView(this);
                LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(200, 200);
                iv2.setLayoutParams(layoutParams2);

                linearLayout.addView(rb);
                linearLayout.addView(iv2);

                layout.addView(linearLayout);
                Glide.with(this)
                        .load(mp.get(key))
                        .into(iv2);
            }

        }

        // Add submit and exit button.

        Button button = new Button(this);
        button.setText("Submit");
        button.setAllCaps(false);
        button.setLayoutParams(new LinearLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String,String> ans = validate();
                if(ans != null)
                {

                    firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    assert firebaseUser != null;
                    String userId = firebaseUser.getUid();
                    final int count[] = new int[1];
                    count[0]=0;
                    progressBar.setVisibility(View.VISIBLE);
                    for(String quesId: ans.keySet()){
                        // set the answer for the user and the specific question.
                        setAns(quesId, userId, ans.get(quesId), count, ans.size());
                    }
                }else{
                    Toast.makeText(newTaskActivity.this, "Some questions are unanswered!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        layout.addView(button);

        Button exit = new Button(this);
        exit.setText("Exit");
        exit.setAllCaps(false);
        exit.setLayoutParams(new LinearLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT));
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogBoxToExit();
            }
        });
        layout.addView(exit);

    }

    @Override
    public void onBackPressed() {
        showDialogBoxToExit();
    }

    //show a dialog to exit.
    //on positive response, stay on the screen.
    //On negative response, Delete the task from the database.
    private void showDialogBoxToExit() {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert firebaseUser != null;
        final String userId = firebaseUser.getUid();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to exit the task")
                .setPositiveButton("No", null )
                .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // delete the task on abnormal exit.
                        deleteTask(userId);
                    }
                });
        builder.create();
        builder.show();
    }

    //delete the task from the database.
    // which will further call firebase cloud functions to handle, database consistence.
    private void deleteTask(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        mFirebaseDatabase.getReference()
                .child("users")
                .child(userId)
                .child(questionsRef.getParent().getKey())
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressBar.setVisibility(View.INVISIBLE);
                        finish();
                    }
                });

    }

    //set the ans for the ques in the database.
    // which will further call firebase cloud functions to handle, database consistence.
    private void setAns(String quesId, String userId, String s,final int[] count , final int n) {
        mFirebaseDatabase.getReference()
                .child("primImage")
                .child(quesId)
                .child("contestant")
                .child(userId)
                .child("ans")
                .setValue(s)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {
                            count[0]++;
                            if(count[0]==n) {
                                progressBar.setVisibility(View.INVISIBLE);
                                finish();
                            }
                        }else{
                            Toast.makeText(newTaskActivity.this, "Operation Unsuccessful!", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    //called to validate the submission
    // i.e., all the questions should be answered.
    @Nullable
    private Map<String, String> validate() {
        Map<String,String> ans = new HashMap<>();
        for(String ques: questions.keySet())
        {
            boolean got = false;
            Map<String, RadioButton> tasks = questions.get(ques).getSecondaryAns();
            for(String option: tasks.keySet())
                if(tasks.get(option).isChecked()) {
                    got = true;
                    ans.put(ques, option);
                    break;
                }
            if(!got)
                return null;
        }
        return ans;
    }
}
