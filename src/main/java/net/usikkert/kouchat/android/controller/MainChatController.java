
/***************************************************************************
 *   Copyright 2006-2011 by Christian Ihle                                 *
 *   kontakt@usikkert.net                                                  *
 *                                                                         *
 *   This file is part of KouChat.                                         *
 *                                                                         *
 *   KouChat is free software; you can redistribute it and/or modify       *
 *   it under the terms of the GNU General Public License as               *
 *   published by the Free Software Foundation, either version 3 of        *
 *   the License, or (at your option) any later version.                   *
 *                                                                         *
 *   KouChat is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with KouChat. If not, see <http://www.gnu.org/licenses/>.       *
 ***************************************************************************/

package net.usikkert.kouchat.android.controller;

import java.util.ArrayList;

import net.usikkert.kouchat.android.AndroidUserInterface;
import net.usikkert.kouchat.android.R;
import net.usikkert.kouchat.android.service.ChatService;
import net.usikkert.kouchat.android.service.ChatServiceBinder;
import net.usikkert.kouchat.misc.User;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Controller for the main chat.
 *
 * <p>Life cycle:</p>
 * <ul>
 *   <li>Application starts:
 *       <ul>
 *           <li>ChatService is created</li>
 *           <li>ChatService is bound</li>
 *       </ul>
 *   </li>
 *   <li>Application is hidden: ChatService is unbound</li>
 *   <li>Application is shown: ChatService is bound</li>
 *   <li>Application shuts down:
 *       <ul>
 *           <li>ChatService is unbound</li>
 *           <li>ChatService is stopped</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * @author Christian Ihle
 */
public class MainChatController extends Activity {

    private Intent chatServiceIntent;
    private ServiceConnection serviceConnection;
    private EditText mainChatInput;
    private ListView mainChatUserList;
    private TextView mainChatView;
    private AndroidUserInterface androidUserInterface;

    public MainChatController() {
        System.out.println("MainChatController " + this + ": constructor !!!!!!!!!!!!!");
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        System.out.println("MainChatController " + this + ": onCreate !!!!!!!!!!!!!");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_chat);
        chatServiceIntent = createChatServiceIntent();

        mainChatInput = (EditText) findViewById(R.id.mainChatInput);
        mainChatUserList = (ListView) findViewById(R.id.mainChatUserList);
        mainChatView = (TextView) findViewById(R.id.mainChatView);

        startService(chatServiceIntent);
        serviceConnection = createServiceConnection();
        bindService(chatServiceIntent, serviceConnection, Context.BIND_NOT_FOREGROUND);

        registerMainChatInputListener();
        makeMainChatViewScrollable();
        setupMainChatUserList();
        openKeyboard();
    }

    private ServiceConnection createServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName componentName, final IBinder iBinder) {
                System.out.println("MainChatController " + this + ": onServiceConnected !!!!!!!!!!!!!");

                final ChatServiceBinder binder = (ChatServiceBinder) iBinder;
                androidUserInterface = binder.getAndroidUserInterface();
                androidUserInterface.registerMainChatController(MainChatController.this);
            }

            @Override
            public void onServiceDisconnected(final ComponentName componentName) {
                System.out.println("MainChatController " + this + ": onServiceDisconnected !!!!!!!!!!!!!");
            }
        };
    }

    private void registerMainChatInputListener() {
        mainChatInput.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    mainChatView.append(mainChatInput.getText() + "\n");
                    mainChatInput.setText("");
                    scrollMainChatViewToBottom();

                    return true;
                }

                return false;
            }
        });
    }

    private void makeMainChatViewScrollable() {
        mainChatView.setMovementMethod(new ScrollingMovementMethod());
    }

    private void setupMainChatUserList() {
        final ArrayAdapter<User> users = new ArrayAdapter<User>(this,
                R.layout.main_chat_user_list_row, R.id.mainChatUserListLabel,
                new ArrayList<User>());

        users.add(new User("User 1", 1));
        users.add(new User("User 2", 2));

        mainChatUserList.setAdapter(users);
    }

    private void openKeyboard() {
        mainChatInput.requestFocus();
    }

    private void scrollMainChatViewToBottom() {
        final Layout layout = mainChatView.getLayout();

        // Happens sometimes when activity is hidden
        if (layout == null) {
            return;

        }

        final int scrollAmount = layout.getLineTop(mainChatView.getLineCount()) - mainChatView.getHeight();

        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0) {
            mainChatView.scrollTo(0, scrollAmount);
        }

        else {
            mainChatView.scrollTo(0, 0);
        }
    }

    @Override
    protected void onDestroy() {
        System.out.println("MainChatController " + this + ": onDestroy !!!!!!!!!!!!!");

        androidUserInterface.unregisterMainChatController();
        unbindService(serviceConnection);

        super.onDestroy();
    }

    /**
     *  Creates the main chat menu.
     *
     *  {@inheritDoc}
     *
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_chat_menu, menu);

        return true;
    }

    /**
     * Selects the actions to run after a menu item in the main chat has been selected.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainChatMenuQuit:
                return shutdownApplication();
            case R.id.mainChatMenuAbout:
                return showAboutDialog();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean showAboutDialog() {
        new AboutDialog(this);
        return true;
    }

    private boolean shutdownApplication() {
        System.out.println("MainChatController " + this + ": shutdownApplication !!!!!!!!!!!!!");

        finish();
        stopService(chatServiceIntent);

        return true;
    }

    private Intent createChatServiceIntent() {
        return new Intent(this, ChatService.class);
    }
}