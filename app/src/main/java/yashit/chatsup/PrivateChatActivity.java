package yashit.chatsup;

import android.annotation.SuppressLint;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import yashit.chatsup.DataObjects.ChatMessage;
import yashit.chatsup.DataObjects.Message;
import yashit.chatsup.DataObjects.UserProfile;

public class PrivateChatActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private EditText message;
    private RecyclerView rv;

    private DatabaseReference root;
    private ChatMessageAdapter adapter;

    private String username;
    private ChatMessage sessionChat = new ChatMessage();
    private String selectedUserUID;

    private boolean nodeFound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_chat);

        username = getIntent().getStringExtra("SELECTED_USERNAME");

        FirebaseDatabase.getInstance().getReference().child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            UserProfile user = snapshot.getValue(UserProfile.class);
                            if(user.getFullName().equals(username)) {
                                selectedUserUID = snapshot.getKey();
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });


        this.setTitle(username);

        message = (EditText) findViewById(R.id.msg_input);
        rv = (RecyclerView) findViewById(R.id.chatPageRecyclerView);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        assert mUser != null;
        adapter = new ChatMessageAdapter(sessionChat.getMessages(),mUser.getDisplayName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            rv.setAdapter(adapter);
        }
        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        rv.setItemAnimator(new DefaultItemAnimator());

        FirebaseDatabase.getInstance().getReference().child("chat")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            ChatMessage chat = snapshot.getValue(ChatMessage.class);
                            if(chat.getSender().equals(mUser.getUid()) && chat.getReceiver().equals(selectedUserUID)) {
                                Toast.makeText(PrivateChatActivity.this, "Got Node", Toast.LENGTH_SHORT).show();
                                sessionChat = chat;
                                nodeFound = true;
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    public void sendChat(View view) {
        final String messageText = message.getText().toString();
        if(!messageText.isEmpty()) {
            Calendar c = Calendar.getInstance();
            System.out.println("Current time => "+c.getTime());

            @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            final String formattedDate = df.format(c.getTime());
            final Message newMessage = new Message(messageText, mUser.getUid(), formattedDate);
//            chatObject.add(new ChatMessage(messageText, mUser.getDisplayName(), username));
            final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.hasChild("chat")) {
                        if(nodeFound) {
                            sessionChat.addMessages(newMessage);
                        }
                        else {
                            sessionChat = new ChatMessage(mUser.getUid(),selectedUserUID);
                            sessionChat.addMessages(newMessage);
                        }
                    }
                    else {
                        sessionChat = new ChatMessage(mUser.getUid(),selectedUserUID);
                        sessionChat.addMessages(newMessage);
                        String key = rootRef.child("chat").push().getKey();
                        rootRef.child("chat").child(key).setValue(sessionChat);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            adapter.notifyDataSetChanged();
        }
        else {
            Toast.makeText(PrivateChatActivity.this, "Please Enter a Message", Toast.LENGTH_SHORT).show();
        }
    }
}
