package com.example.nfctransfreing;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final int REQUEST_SELECT_IMAGE = 100;
    private ImageView imageView;
    private NfcAdapter nfcAdapter;
    private boolean isWriteMode = false;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private Button selectImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectImageButton = findViewById(R.id.select_image_button);
        imageView = findViewById(R.id.image_view);

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_SELECT_IMAGE);
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter ndefIntentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            intentFiltersArray = new IntentFilter[] { ndefIntentFilter };
//            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
//            nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
            nfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
            nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
            nfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    System.out.println(bitmap);
                    byte[] byteArray = stream.toByteArray();
                    if (nfcAdapter != null) {
                        NdefMessage message = new NdefMessage(
                                new NdefRecord[] {
                                        NdefRecord.createMime("text/plain", "Hello, world!".getBytes())
                                }
                        );
                        NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "image/png".getBytes(), new byte[0], byteArray);
                        System.out.println("RECORD==================" +record.toString());
//                        NdefMessage message = new NdefMessage(new NdefRecord[] { record });
                        System.out.println("\nSENDING VIA NFC...======================" + message.toString()+"\n");
                        nfcAdapter.setNdefPushMessage(message, this);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        System.out.println("TAG FOUND======================================================");
        System.out.println(tag.toString());
        String[] techList = tag.getTechList();
        for (String tech : techList) {
            System.out.println("Supported tech: " + tech);
        }
        IsoDep isoDep = IsoDep.get(tag);
        System.out.println(isoDep.toString());
        if (isoDep != null) {
            System.out.println("NOT NULL======================================");
            try {
                isoDep.connect();
                byte[] command = new byte[]{(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x07, (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x01, (byte) 0x00};
                byte[] result = isoDep.transceive(command);
                System.out.println(Arrays.toString(result));
                if (result != null) {
                    final String resultString = new String(result);
                    System.out.println("RESULT===================================" + resultString);
//                    final Uri imageUri = Uri.parse(resultString);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    final Bitmap bmp = BitmapFactory.decodeByteArray(result, 0, result.length, options);
                    System.out.println(bmp);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bmp);
                        }
                    });

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    isoDep.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            System.out.println("+++++++++++++++++++++++++++++++++++");
        }
    }
}