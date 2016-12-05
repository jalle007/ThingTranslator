package thingtranslator2.jalle.com.thingtranslator2;

import android.app.ProgressDialog;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pixplicity.easyprefs.library.Prefs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import thingtranslator2.jalle.com.thingtranslator2.Rest.ApiInterface;
import thingtranslator2.jalle.com.thingtranslator2.Rest.Translation;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public
    @BindView(R.id.txtTranslation)
    TextView txtTranslation;
    public
    @BindView(R.id.spinner)
    Spinner spinner;
    public
    @BindView(R.id.imgPhoto)
    ImageButton imgPhoto;
    public
    @BindView(R.id.imgSpeaker)
    ImageButton btnSpeaker;

    public Boolean speakerOn, firstRun;
    public TextToSpeech tts;
    public List<String> languages = new ArrayList<String>();
    public ArrayList<Language> languageList = new ArrayList<Language>();
    public ArrayAdapter<Language> spinnerArrayAdapter;
    public Language selectedLanguage;
    public static final int PICK_IMAGE_ID = 234; // the number doesn't matter
    thingtranslator2.jalle.com.thingtranslator2.MarshMallowPermission marshMallowPermission = new thingtranslator2.jalle.com.thingtranslator2.MarshMallowPermission(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ButterKnife.bind(this);

        //init our Shared preferences helper
        new Prefs.Builder()
                .setContext(this)
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setPrefsName(getPackageName())
                .setUseDefaultSharedPreference(true)
                .build();

        //  Prefs.clear();
        firstRun = Prefs.getBoolean("firstRun", true);
        spinner.setOnItemSelectedListener(this);
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    fillSpinner();


                }
            }
        });


        if (firstRun) {
            Prefs.putBoolean("firstRun", false);
            Prefs.putBoolean("speakerOn", true);
            Prefs.putString("langCode", "bs");
        }
        setSpeaker();
    }

    private void setSpeaker() {
        int id = Prefs.getBoolean("speakerOn", false) ? R.drawable.ic_volume : R.drawable.ic_volume_off;
        btnSpeaker.setImageBitmap(BitmapFactory.decodeResource(getResources(), id));
    }

    public void ToogleSpeaker(View v) {
        Prefs.putBoolean("speakerOn", !Prefs.getBoolean("speakerOn", false));
        setSpeaker();
        Toast.makeText(getApplicationContext(), "Speech " + (Prefs.getBoolean("speakerOn", true) ? "On" : "Off"), Toast.LENGTH_SHORT);
    }

    private void fillSpinner() {
        Iterator itr = tts.getAvailableLanguages().iterator();
        while (itr.hasNext()) {
            Locale item = (Locale) itr.next();
            languageList.add(new Language(item.getDisplayName(), item.getLanguage()));
        }
        //Sort that array
        Collections.sort(languageList, new Comparator<Language>() {
            @Override
            public int compare(Language o1, Language o2) {
                return o1.getlangCode().compareTo(o2.getlangCode());
            }
        });

        spinnerArrayAdapter = new ArrayAdapter<Language>(this, R.layout.spinner_item, languageList);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        String def = Prefs.getString("langCode", "bs");
        for (Language lang : languageList) {
            if (lang.getlangCode().equals(def)) {
                spinner.setSelection(languageList.indexOf(lang));
            }
        }
    }

    ;

    //Get image from  Gallery or Camera
    public void getImage(View v) {
        if (!marshMallowPermission.checkPermissionForCamera()) {
            marshMallowPermission.requestPermissionForCamera();
        } else {
            if (!marshMallowPermission.checkPermissionForExternalStorage()) {
                marshMallowPermission.requestPermissionForExternalStorage();
            } else {
                Intent chooseImageIntent = thingtranslator2.jalle.com.thingtranslator2.ImagePicker.getPickImageIntent(getApplicationContext());
                startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) return;

        switch (requestCode) {
            case PICK_IMAGE_ID:
                Bitmap bitmap = thingtranslator2.jalle.com.thingtranslator2.ImagePicker.getImageFromResult(this, resultCode, data);
                imgPhoto.setImageBitmap(null);
                imgPhoto.setBackground(null);
                imgPhoto.setImageBitmap(bitmap);
                imgPhoto.invalidate();
                imgPhoto.postInvalidate();

                File file = null;
                try {
                    file = savebitmap(bitmap, "pic.jpeg");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                uploadImage(file);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void uploadImage(File file) {
        RequestBody body = RequestBody.create(MediaType.parse("image/*"), file);
        RequestBody langCode1 = RequestBody.create(MediaType.parse("text/plain"), selectedLanguage.langCode);

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Processing image...");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();


        ApiInterface mApiService = ApiInterface.retrofit.create(ApiInterface.class);
        Call<Translation> mService = mApiService.upload(body, langCode1);
        mService.enqueue(new Callback<Translation>() {
            @Override
            public void onResponse(Call<Translation> call, Response<Translation> response) {
                progress.hide();
                Translation result = response.body();
                txtTranslation.setText(result.Translation);
                txtTranslation.invalidate();

                if (Prefs.getBoolean("speakerOn", true)) {
                    tts.speak(result.Translation, TextToSpeech.QUEUE_FLUSH, null);
                }
            }

            @Override
            public void onFailure(Call<Translation> call, Throwable t) {
                call.cancel();
                progress.hide();
                Toast.makeText(getApplicationContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    public static File savebitmap(Bitmap bmp, String fName) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + fName);
        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();
        return f;
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        selectedLanguage = (Language) spinner.getSelectedItem();
        //  langCode = languages.get(position);
        tts.setLanguage(Locale.forLanguageTag(selectedLanguage.langCode));
        Prefs.putString("langCode", selectedLanguage.langCode);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
