package com.africastalking;

import android.content.res.Resources;
import android.text.TextUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by jay on 7/13/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class, Resources.class})
@PowerMockIgnore("javax.net.ssl.*")
public class SMSServiceTest {

    private SMSService sms;

    @Before
    public void setup() throws IOException {
        AfricasTalking.initialize("sandbox", "localhost");
        sms = AfricasTalking.getSmsService();
    }

    @Test
    public void send() throws Exception {
        assertNotNull("SMS not sent", sms.send("Test sms", "", new String[]{"+250784476268"}));
        assertEquals("SMS not sent", "Success", sms.send("Test sms", "", new String[]{"+250784476268"}).getSMSMessageData().getRecipients().get(0).getStatus());
    }

    @Test
    public void sendBulk() throws Exception {
        assertNotNull("Bulk SMS not sent", sms.sendBulk("Test sms", "", new String[]{"+250784476268"}));
    }

    @Test
    public void sendPremium() throws Exception {
        assertNotNull("Premium SMS not sent", sms.sendPremium("Test", "", "", new String[]{""}));
    }

    @Test
    public void fetchMessage() throws Exception {
        assertNotNull("Fetch message failed", sms.fetchMessage());
    }

    @Test
    public void fetchSubscription() throws Exception {
        assertNotNull("Fetch Subscription failed", sms.fetchSubscription("","",""));
    }

    @Test
    public void createSubscription() throws Exception {
        assertNotNull("Create subscription", sms.createSubscription("","",""));
    }

}